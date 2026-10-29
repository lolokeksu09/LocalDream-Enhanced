package io.github.xororz.localdream.remote

/** Volatile active LAN link. No credentials are included in URLs or logs. */
object RemoteSession {
    private data class Session(val host: String, val secret: String, val fingerprint: String)

    @Volatile private var current: Session? = null

    val pin: String? get() = current?.fingerprint

    fun isHost(host: String): Boolean = current?.host == host.lowercase(java.util.Locale.ROOT)

    fun activate(host: String, secret: String, fingerprint: String) {
        current = Session(host.lowercase(java.util.Locale.ROOT), secret, fingerprint)
    }

    fun clear() {
        current = null
    }

    fun keyForBackend(host: String, port: Int): String? {
        val session = current ?: return null
        if (port != RemoteProtocol.GATEWAY_PORT) return null
        return session.secret.takeIf { session.host == host.lowercase(java.util.Locale.ROOT) }
    }
}
