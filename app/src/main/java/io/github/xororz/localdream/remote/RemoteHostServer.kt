package io.github.xororz.localdream.remote

import android.util.Log
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import javax.net.ssl.SSLServerSocketFactory
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * Minimal blocking HTTP/1.1 server backing host mode's control API.
 *
 * Scope is deliberately tiny: small JSON requests and responses on a handful
 * of fixed routes, one response per connection (Connection: close). Anything
 * heavy (generation SSE, tokenize, upscale) is served by the native backend
 * on its own port, never through here.
 *
 * Every endpoint except /info requires the current session's 128-bit key.
 * The generation port is separately guarded by RemoteGenerationGateway.
 * Both control and generation ports use TLS with a pinned certificate.
 */
class RemoteHostServer(
    private val port: Int,
    private val handler: Handler,
    private val sessionKey: String,
    private val tlsFactory: SSLServerSocketFactory,
) {
    interface Handler {
        fun info(): JSONObject
        fun models(): JSONObject
        fun select(body: JSONObject): Response
        fun status(): JSONObject
        fun stop(body: JSONObject): JSONObject
    }

    data class Response(val code: Int, val body: JSONObject)

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var running = false
    private var acceptThread: Thread? = null
    private var workers: ExecutorService? = null

    @Throws(IOException::class)
    fun start() {
        val socket = tlsFactory.createServerSocket(port)
        serverSocket = socket
        running = true
        workers = Executors.newFixedThreadPool(WORKER_COUNT) { r ->
            Thread(r, "remote-host-worker")
        }
        acceptThread = Thread({
            while (running) {
                val client = try {
                    socket.accept()
                } catch (e: IOException) {
                    if (running) Log.w(TAG, "accept failed: ${e.message}")
                    break
                }
                workers?.execute { serve(client) }
            }
        }, "remote-host-accept").apply { start() }
        Log.i(TAG, "control server listening on $port")
    }

    fun shutdown() {
        running = false
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
        serverSocket = null
        workers?.shutdownNow()
        workers = null
        acceptThread = null
        Log.i(TAG, "control server stopped")
    }

    private fun serve(client: Socket) {
        try {
            client.soTimeout = SOCKET_TIMEOUT_MS
            client.use { socket ->
                val input = BufferedInputStream(socket.getInputStream())
                val request = parseRequest(input)
                val response = if (request == null) {
                    Response(400, errorBody("bad request"))
                } else {
                    route(request)
                }
                writeResponse(socket, response)
            }
        } catch (e: Exception) {
            Log.w(TAG, "request handling failed: ${e.message}")
        }
    }

    private data class Request(
        val method: String,
        val path: String,
        val body: JSONObject,
        val authorization: String?,
    )

    private fun parseRequest(input: InputStream): Request? {
        val requestLine = readLine(input) ?: return null
        val parts = requestLine.split(" ")
        if (parts.size < 2) return null
        val method = parts[0]
        // Strip any query string; routes don't use one.
        val path = parts[1].substringBefore('?')

        // Reject ambiguous/framed requests instead of treating malformed
        // Content-Length or a truncated JSON body as a harmless empty body.
        // No chunked encoding is supported by this single-request server.
        var contentLength: Int? = null
        var authorization: String? = null
        var headerBytes = 0
        while (true) {
            val line = readLine(input) ?: return null
            headerBytes += line.length + 2
            if (headerBytes > MAX_HEADER_BYTES) return null
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator <= 0) return null
            val name = line.substring(0, separator).trim().lowercase()
            val value = line.substring(separator + 1).trim()
            when (name) {
                "content-length" -> {
                    if (contentLength != null || value.isEmpty() || !value.all(Char::isDigit)) {
                        return null
                    }
                    contentLength = value.toIntOrNull() ?: return null
                    if (contentLength > MAX_BODY_BYTES) return null
                }
                "transfer-encoding" -> return null
                RemoteProtocol.AUTH_HEADER.lowercase() -> {
                    if (authorization != null) return null
                    authorization = value
                }
            }
        }
        val length = contentLength ?: 0
        if (method == "POST" && length == 0) return null
        val body = if (length > 0) {
            val bytes = ByteArray(length)
            var read = 0
            while (read < length) {
                val n = input.read(bytes, read, length - read)
                if (n <= 0) return null
                read += n
            }
            try {
                JSONObject(String(bytes, StandardCharsets.UTF_8))
            } catch (_: Exception) {
                return null
            }
        } else {
            JSONObject()
        }
        return Request(method, path, body, authorization)
    }

    // Reads one CRLF/LF-terminated header line as ISO-8859-1 (headers are
    // ASCII here; bodies are decoded separately as UTF-8).
    private fun readLine(input: InputStream): String? {
        val buffer = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (buffer.isEmpty()) null else buffer.toString()
            if (c == '\n'.code) break
            if (c != '\r'.code) buffer.append(c.toChar())
            if (buffer.length > MAX_LINE_LENGTH) return null
        }
        return buffer.toString()
    }

    private fun route(request: Request): Response {
        if (request.method == "GET" && request.path == RemoteProtocol.PATH_INFO) {
            return Response(200, handler.info())
        }
        val key = request.authorization ?: return Response(401, errorBody("unauthorized"))
        if (!RemotePairingKey.matches(sessionKey, key)) {
            return Response(401, errorBody("unauthorized"))
        }
        return when {
            request.method == "GET" && request.path == RemoteProtocol.PATH_MODELS ->
                Response(200, handler.models())

            request.method == "POST" && request.path == RemoteProtocol.PATH_SELECT ->
                handler.select(request.body)

            request.method == "GET" && request.path == RemoteProtocol.PATH_STATUS ->
                Response(200, handler.status())

            request.method == "POST" && request.path == RemoteProtocol.PATH_STOP ->
                Response(200, handler.stop(request.body))

            else -> Response(404, errorBody("not found"))
        }
    }

    private fun writeResponse(socket: Socket, response: Response) {
        val body = response.body.toString().toByteArray(StandardCharsets.UTF_8)
        val reason = when (response.code) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
        val header = buildString {
            append("HTTP/1.1 ${response.code} $reason\r\n")
            append("Content-Type: application/json\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        val output = socket.getOutputStream()
        output.write(header.toByteArray(StandardCharsets.ISO_8859_1))
        output.write(body)
        output.flush()
    }

    private fun errorBody(message: String): JSONObject = JSONObject().put("error", message)

    companion object {
        private const val TAG = "RemoteHostServer"

        // Single worker on purpose: select/stop must be processed in arrival
        // order (a reordered late stop could kill a newer selection), and
        // every route is a fast, small JSON exchange.
        private const val WORKER_COUNT = 1
        private const val SOCKET_TIMEOUT_MS = 10_000
        private const val MAX_BODY_BYTES = 64 * 1024
        private const val MAX_HEADER_BYTES = 32 * 1024
        private const val MAX_LINE_LENGTH = 8 * 1024
    }
}
