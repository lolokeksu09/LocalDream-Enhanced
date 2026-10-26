package io.github.xororz.localdream.remote

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

/**
 * Authenticated, byte-streaming LAN gateway. The native generation backend is
 * deliberately restricted to localhost:8081. This proxy handles large JSON
 * image payloads and streamed SSE responses without buffering the payload.
 *
 * The session key authenticates peers, but HTTP itself is NOT encrypted.
 * Do not use the gateway on untrusted/shared networks.
 */
class RemoteGenerationGateway(private val sessionKey: String) {
    @Volatile private var listener: ServerSocket? = null
    private val slots = Semaphore(MAX_CONNECTIONS)
    private val workers = Executors.newFixedThreadPool(MAX_CONNECTIONS)

    @Throws(IOException::class)
    fun start() {
        val server = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(RemoteProtocol.GATEWAY_PORT))
        }
        listener = server
        thread(name = "localdream-gateway-accept", isDaemon = true) {
            while (!server.isClosed) {
                val client = try {
                    server.accept()
                } catch (_: IOException) {
                    break
                }
                if (!slots.tryAcquire()) {
                    runCatching { client.close() }
                    continue
                }
                try {
                    workers.execute {
                        try { serve(client) }
                        finally { slots.release() }
                    }
                } catch (_: RuntimeException) {
                    slots.release()
                    runCatching { client.close() }
                }
            }
        }
    }

    fun shutdown() {
        runCatching { listener?.close() }
        listener = null
        workers.shutdownNow()
    }

    private fun serve(client: Socket) {
        client.use { downstream ->
            try {
                downstream.soTimeout = HEADER_TIMEOUT_MS
                val input = downstream.getInputStream()
                val requestLine = readLine(input) ?: return
                if (!requestLine.matches(Regex("^(GET|POST) /[^\\s]* HTTP/1\\.[01]$"))) {
                    deny(downstream, 400)
                    return
                }
                var used = requestLine.length + 2
                val headers = mutableListOf<String>()
                var key: String? = null
                while (true) {
                    val line = readLine(input) ?: return
                    used += line.length + 2
                    if (used > MAX_HEADER_BYTES) {
                        deny(downstream, 400)
                        return
                    }
                    if (line.isEmpty()) break
                    val colon = line.indexOf(':')
                    if (colon <= 0) {
                        deny(downstream, 400)
                        return
                    }
                    val name = line.substring(0, colon).trim().lowercase()
                    val value = line.substring(colon + 1).trim()
                    if (name == RemoteProtocol.AUTH_HEADER.lowercase()) {
                        if (key != null) {
                            deny(downstream, 401)
                            return
                        }
                        key = value
                    } else if (name !in HOP_HEADERS) {
                        headers.add(line)
                    }
                }
                if (!MessageDigest.isEqual(
                        sessionKey.toByteArray(StandardCharsets.US_ASCII),
                        (key ?: "").toByteArray(StandardCharsets.US_ASCII),
                    )
                ) {
                    deny(downstream, 401)
                    return
                }

                Socket(InetAddress.getLoopbackAddress(), RemoteProtocol.GENERATION_PORT).use { upstream ->
                    downstream.soTimeout = STREAM_TIMEOUT_MS
                    upstream.soTimeout = STREAM_TIMEOUT_MS
                    val upstreamOutput = upstream.getOutputStream()
                    val sanitizedHeaders = headers.filterNot {
                        it.substringBefore(':').equals("host", ignoreCase = true)
                    }
                    val head = buildString {
                        append(requestLine).append("\r\n")
                        append("Host: localhost:").append(RemoteProtocol.GENERATION_PORT).append("\r\n")
                        sanitizedHeaders.forEach { append(it).append("\r\n") }
                        append("Connection: close\r\n\r\n")
                    }
                    upstreamOutput.write(head.toByteArray(StandardCharsets.ISO_8859_1))
                    upstreamOutput.flush()

                    // Half-duplex HTTP uploads and SSE replies can overlap.
                    // Do not parse or reassemble a potentially huge image body.
                    val upload = thread(name = "localdream-gateway-upload", isDaemon = true) {
                        try {
                            input.copyTo(upstreamOutput, bufferSize = 32 * 1024)
                        } catch (_: IOException) {
                            // The response side closes the connection on completion.
                        }
                    }
                    try {
                        upstream.getInputStream().copyTo(
                            downstream.getOutputStream(), bufferSize = 32 * 1024,
                        )
                    } finally {
                        runCatching { upstream.close() }
                        runCatching { downstream.close() }
                        upload.interrupt()
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "gateway connection closed: ${e.javaClass.simpleName}")
            } catch (e: Exception) {
                Log.w(TAG, "gateway error: ${e.javaClass.simpleName}")
            }
        }
    }

    private fun readLine(input: InputStream): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (bytes.size() <= MAX_LINE_BYTES) {
            val b = input.read()
            if (b < 0) return null
            if (b == 10) return bytes.toString("ISO-8859-1")
                .removeSuffix("\r")
                .takeUnless { it.any { ch -> ch.code < 32 && ch != '\t' } }
            bytes.write(b)
        }
        return null
    }

    private fun deny(socket: Socket, status: Int) {
        val reason = if (status == 401) "Unauthorized" else "Bad Request"
        val body = "{\"error\":\"request rejected\"}"
        val response = "HTTP/1.1 $status $reason\r\n" +
            "Content-Type: application/json\r\n" +
            "Content-Length: ${body.toByteArray().size}\r\n" +
            "Connection: close\r\n\r\n" + body
        runCatching { socket.getOutputStream().write(response.toByteArray(StandardCharsets.US_ASCII)) }
    }

    private companion object {
        const val TAG = "RemoteGenerationGateway"
        const val MAX_CONNECTIONS = 6
        const val MAX_LINE_BYTES = 8192
        const val MAX_HEADER_BYTES = 32 * 1024
        const val HEADER_TIMEOUT_MS = 10_000
        const val STREAM_TIMEOUT_MS = 3_600_000
        val HOP_HEADERS = setOf("connection", "proxy-connection", "keep-alive")
    }
}
