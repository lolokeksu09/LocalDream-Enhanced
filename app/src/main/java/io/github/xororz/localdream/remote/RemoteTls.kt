package io.github.xororz.localdream.remote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import javax.security.auth.x500.X500Principal

/**
 * The out-of-band pairing string carries both a random bearer secret and a
 * SHA-256 certificate fingerprint. The fingerprint is verified DURING the TLS
 * handshake, before the bearer secret is ever transmitted.
 */
internal object RemoteTls {
    private const val ALIAS = "localdream_lan_tls_v3"
    private val hex = Regex("[0-9A-F]{32}\\.[0-9A-F]{64}")

    data class Pairing(val secret: String, val fingerprint: String) {
        fun encode(): String = "$secret.$fingerprint"
    }

    fun parse(input: String): Pairing? {
        val value = input.trim().uppercase(java.util.Locale.ROOT)
        if (!hex.matches(value)) return null
        return Pairing(value.substringBefore('.'), value.substringAfter('.'))
    }

    fun fingerprint(certificate: X509Certificate): String =
        MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
            .joinToString("") { "%02X".format(it) }

    data class HostIdentity(
        val factory: SSLServerSocketFactory,
        val fingerprint: String,
    )

    fun hostIdentity(): HostIdentity {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(ALIAS)) {
            val now = System.currentTimeMillis()
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore",
            )
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                )
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                        KeyProperties.SIGNATURE_PADDING_RSA_PSS,
                    )
                    .setCertificateSubject(X500Principal("CN=LocalDream LAN"))
                    .setCertificateSerialNumber(BigInteger.valueOf(now))
                    .setCertificateNotBefore(Date(now - 60_000L))
                    .setCertificateNotAfter(Date(now + 10L * 365 * 24 * 3600_000))
                    .build(),
            )
            generator.generateKeyPair()
        }
        val certificate = store.getCertificate(ALIAS) as X509Certificate
        val manager = object : javax.net.ssl.X509KeyManager {
            override fun getClientAliases(keyType: String?, issuers: Array<java.security.Principal>?): Array<String>? = null
            override fun chooseClientAlias(keyType: Array<String>?, issuers: Array<java.security.Principal>?, socket: java.net.Socket?): String? = null
            override fun getServerAliases(keyType: String?, issuers: Array<java.security.Principal>?): Array<String>? =
                if (keyType == "RSA") arrayOf(ALIAS) else null
            override fun chooseServerAlias(keyType: String?, issuers: Array<java.security.Principal>?, socket: java.net.Socket?): String? =
                if (keyType == "RSA") ALIAS else null
            override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
                if (alias == ALIAS) arrayOf(certificate) else null
            override fun getPrivateKey(alias: String?): java.security.PrivateKey? =
                if (alias == ALIAS) store.getKey(ALIAS, null) as? java.security.PrivateKey else null
        }
        val context = SSLContext.getInstance("TLS").apply {
            init(arrayOf<KeyManager>(manager), null, SecureRandom())
        }
        return HostIdentity(context.serverSocketFactory, fingerprint(certificate))
    }

    private val systemTrust: X509TrustManager by lazy {
        val managerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        managerFactory.init(null as KeyStore?)
        managerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private fun trustManager(allowedFingerprint: () -> String?): X509TrustManager =
        object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = systemTrust.acceptedIssuers

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                systemTrust.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                val pin = allowedFingerprint()
                if (pin != null && chain.isNotEmpty() && fingerprint(chain[0]) == pin) return
                systemTrust.checkServerTrusted(chain, authType)
            }
        }

    private fun secureClient(
        base: OkHttpClient.Builder,
        allowedPin: () -> String?,
        allowedHost: (String) -> Boolean,
    ): OkHttpClient {
        val trust = trustManager(allowedPin)
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trust), SecureRandom())
        }
        return base.sslSocketFactory(ssl.socketFactory, trust)
            .hostnameVerifier { host, session ->
                val certificate = runCatching { session.peerCertificates.firstOrNull() as? X509Certificate }.getOrNull()
                val pin = allowedPin()
                if (pin != null && allowedHost(host) && certificate != null) {
                    fingerprint(certificate) == pin
                } else {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session)
                }
            }
            .build()
    }

    /** Common client: ordinary websites still require the system CA chain. */
    fun sharedClient(builder: OkHttpClient.Builder): OkHttpClient =
        secureClient(builder, { RemoteSession.pin }, { RemoteSession.isHost(it) })

    /** Initial pairing: don't transmit a credential before TLS pin validation. */
    fun pairingClient(builder: OkHttpClient.Builder, host: String, pin: String): OkHttpClient =
        secureClient(builder.followRedirects(false).followSslRedirects(false), { pin }, { it.equals(host, true) })
}
