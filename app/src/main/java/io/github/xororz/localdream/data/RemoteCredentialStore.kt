package io.github.xororz.localdream.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.xororz.localdream.remote.RemoteTls
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed AES-GCM secret storage. Keystore material never
 * leaves the device and the encrypted preference is excluded from auto-backup.
 * Old plaintext remote_code is intentionally not migrated.
 */
internal object RemoteCredentialStore {
    private const val ALIAS = "localdream_remote_credentials_v3"
    private const val PREFS = "remote_session_secrets"
    private const val KEY = "pairing"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    fun save(context: Context, pairing: RemoteTls.Pairing) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val ciphertext = cipher.doFinal(pairing.encode().toByteArray(Charsets.US_ASCII))
        val packed = cipher.iv + ciphertext
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun load(context: Context): RemoteTls.Pairing? {
        val packed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(packed, Base64.DEFAULT)
            if (bytes.size < 12 + 16) return@runCatching null
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            }
            RemoteTls.parse(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.US_ASCII))
        }.getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
