package io.github.xororz.localdream.remote

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Pure framing checks shared by gateway and focused JVM unit tests. */
internal object RemoteRequestFraming {
    // Generation with reference images may be large. Cap the upload anyway.
    const val MAX_BODY_BYTES = 256L * 1024 * 1024

    fun parseRequestLine(line: String): Pair<String, String>? {
        val parts = line.split(' ')
        if (parts.size != 3 || parts[2] !in listOf("HTTP/1.0", "HTTP/1.1")) return null
        val method = parts[0]
        val path = parts[1]
        if (method !in listOf("GET", "POST")) return null
        if (path !in listOf("/health", "/generate", "/tokenize", "/upscale")) return null
        if (path == "/health" && method != "GET") return null
        if (path != "/health" && method != "POST") return null
        return method to path
    }

    fun parseLength(raw: String): Long? {
        if (raw.isEmpty() || raw.any { it !in '0'..'9' }) return null
        val count = raw.toLongOrNull() ?: return null
        return count.takeIf { it <= MAX_BODY_BYTES }
    }

    fun validBody(method: String, length: Long): Boolean =
        if (method == "POST") length > 0L && length <= MAX_BODY_BYTES else length == 0L

    /** Copies EXACTLY the declared byte count; never awaits socket EOF. */
    @Throws(IOException::class)
    fun copyExactly(source: InputStream, target: OutputStream, bytes: Long) {
        require(bytes in 0..MAX_BODY_BYTES)
        var remaining = bytes
        val buffer = ByteArray(32 * 1024)
        while (remaining > 0) {
            val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read <= 0) throw IOException("truncated HTTP body")
            target.write(buffer, 0, read)
            remaining -= read
        }
    }
}
