package io.github.xororz.localdream.remote

/**
 * Active controller-to-host session. Only the selected host's gateway receives
 * a key. The key is never appended to a URL or logged.
 */
object RemoteSession {
    private data class Session(val host: String, val key: String)

    @Volatile private var current: Session? = null

    fun activate(host: String, key: String) {
        current = Session(host.lowercase(), key)
    }

    fun clear() {
        current = null
    }

    fun keyForBackend(host: String, port: Int): String? {
        val session = current ?: return null
        if (port != RemoteProtocol.GATEWAY_PORT) return null
        return session.key.takeIf { session.host == host.lowercase() }
    }
}
