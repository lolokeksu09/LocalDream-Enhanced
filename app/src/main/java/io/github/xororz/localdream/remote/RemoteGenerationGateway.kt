package io.github.xororz.localdream.remote

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.concurrent.thread

/**
 * TLS-only, authenticated byte-streaming proxy for the local native engine.
 * Strict request framing prevents an uploading client from starving responses,
 * smuggling requests or causing an unbounded request upload.
 */
class RemoteGenerationGateway(
    private val sessionKey: String,
    private val tlsFactory: SSLServerSocketFactory,
) {
    @Volatile private var listener: SSLServerSocket? = null
    private val slots = Semaphore(MAX_CONNECTIONS)
    private val active = ConcurrentHashMap.newKeySet<Socket>()
    private val workers = Executors.newFixedThreadPool(MAX_CONNECTIONS)

    @Throws(IOException::class)
    fun start() {
        val server = (tlsFactory.createServerSocket() as SSLServerSocket).apply {
            reuseAddress = true
            bind(InetSocketAddress(RemoteProtocol.GATEWAY_PORT))
        }
        listener = server
        thread(name = "localdream-gateway-accept", isDaemon = true) {
            while (!server.isClosed) {
                val client = try { server.accept() } catch (_: IOException) { break }
                if (!slots.tryAcquire()) {
                    runCatching { client.close() }
                    continue
                }
                active.add(client)
                try {
                    workers.execute {
                        try { serve(client) }
                        finally {
                            active.remove(client)
                            runCatching { client.close() }
                            slots.release()
                        }
                    }
                } catch (_: RuntimeException) {
                    active.remove(client)
                    slots.release()
                    runCatching { client.close() }
                }
            }
        }
    }

    fun shutdown() {
        runCatching { listener?.close() }
        listener = null
        active.forEach { runCatching { it.close() } }
        active.clear()
        workers.shutdownNow()
    }

    private fun serve(client: Socket) {
        client.use { downstream ->
            try {
                downstream.soTimeout = HEADER_TIMEOUT_MS
                val input = downstream.getInputStream()
                val line = readLine(input) ?: return
                val request = RemoteRequestFraming.parseRequestLine(line)
                if (request == null) {
                    deny(downstream, 400)
                    return
                }

                var headerBytes = line.length + 2
                var key: String? = null
                var length: Long? = null
                val forwarding = mutableListOf<String>()
                while (true) {
                    val header = readLine(input) ?: return
                    headerBytes += header.length + 2
                    if (headerBytes > MAX_HEADER_BYTES) {
                        deny(downstream, 400)
                        return
                    }
                    if (header.isEmpty()) break
                    val colon = header.indexOf(':')
                    if (colon <= 0) {
                        deny(downstream, 400)
                        return
                    }
                    val name = header.substring(0, colon).lowercase()
                    val value = header.substring(colon + 1).trim()
                    when (name) {
                        RemoteProtocol.AUTH_HEADER.lowercase() -> {
                            if (key != null) { deny(downstream, 400); return }
                            key = value
                        }
                        "content-length" -> {
                            if (length != null) { deny(downstream, 400); return }
                            length = RemoteRequestFraming.parseLength(value)
                            if (length == null) { deny(downstream, 413); return }
                            forwarding.add(header)
                        }
                        "transfer-encoding", "expect" -> {
                            // Our client always supplies Content-Length for POST.
                            // Reject ambiguous framing rather than forwarding it.
                            deny(downstream, 400)
                            return
                        }
                        "host", "connection", "proxy-connection", "keep-alive" -> Unit
                        else -> forwarding.add(header)
                    }
                }
                if (!RemotePairingKey.matches(sessionKey, key)) {
                    deny(downstream, 401)
                    return
                }
                val bodySize = length ?: 0L
                if (!RemoteRequestFraming.validBody(request.first, bodySize)) {
                    deny(downstream, 400)
                    return
                }

                Socket().use { upstream ->
                    upstream.connect(
                        InetSocketAddress("127.0.0.1", RemoteProtocol.GENERATION_PORT),
                        CONNECT_TIMEOUT_MS,
                    )
                    downstream.soTimeout = STREAM_TIMEOUT_MS
                    upstream.soTimeout = STREAM_TIMEOUT_MS
                    val output = upstream.getOutputStream()
                    val head = buildString {
                        append(line).append("\r\n")
                        append("Host: localhost:").append(RemoteProtocol.GENERATION_PORT).append("\r\n")
                        forwarding.forEach { append(it).append("\r\n") }
                        append("Connection: close\r\n\r\n")
                    }
                    output.write(head.toByteArray(StandardCharsets.ISO_8859_1))
                    output.flush()
                    // Upload exactly Content-Length bytes. Never copy until EOF:
                    // keepalive clients leave the socket open awaiting SSE data.
                    val upload = thread(name = "localdream-gateway-upload", isDaemon = true) {
                        try {
                            RemoteRequestFraming.copyExactly(input, output, bodySize)
                            output.flush()
                        } catch (_: IOException) {
                            runCatching { upstream.close() }
                        }
                    }
                    try {
                        upstream.getInputStream().copyTo(
                            downstream.getOutputStream(), bufferSize = 32 * 1024,
                        )
                    } finally {
                        runCatching { upstream.close() }
                        upload.interrupt()
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "gateway connection closed: " + e.javaClass.simpleName)
            } catch (e: Exception) {
                Log.w(TAG, "gateway failure: " + e.javaClass.simpleName)
            }
        }
    }

    private fun readLine(input: InputStream): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (bytes.size() < MAX_LINE_BYTES) {
            val b = input.read()
            if (b < 0) return null
            if (b == 10) {
                val result = bytes.toString("ISO-8859-1").removeSuffix("\r")
                return result.takeUnless { line -> line.any { ch -> ch.code < 32 && ch != '\t' } }
            }
            bytes.write(b)
        }
        return null
    }

    private fun deny(socket: Socket, status: Int) {
        val reason = when (status) {
            401 -> "Unauthorized"
            413 -> "Payload Too Large"
            else -> "Bad Request"
        }
        val body = "{\"error\":\"request rejected\"}"
        val response = "HTTP/1.1 " + status + " " + reason + "\r\n" +
            "Content-Type: application/json\r\nContent-Length: " +
            body.toByteArray().size + "\r\nConnection: close\r\n\r\n" + body
        runCatching { socket.getOutputStream().write(response.toByteArray(StandardCharsets.US_ASCII)) }
    }

    private companion object {
        const val TAG = "RemoteGenerationGateway"
        const val MAX_CONNECTIONS = 6
        const val MAX_LINE_BYTES = 8192
        const val MAX_HEADER_BYTES = 32 * 1024
        const val HEADER_TIMEOUT_MS = 10_000
        const val CONNECT_TIMEOUT_MS = 5_000
        const val STREAM_TIMEOUT_MS = 3_600_000
    }
}
