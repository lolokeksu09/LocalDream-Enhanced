package io.github.xororz.localdream.remote

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

/** A fresh 128-bit hex credential for each host-mode session. */
internal object RemotePairingKey {
    private val validKey = Regex("[0-9A-F]{32}")

    fun generate(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun normalize(input: String): String? =
        input.trim().uppercase(Locale.ROOT).takeIf { validKey.matches(it) }

    fun matches(expected: String, supplied: String?): Boolean {
        if (supplied == null) return false
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.US_ASCII),
            supplied.toByteArray(StandardCharsets.US_ASCII),
        )
    }
}
