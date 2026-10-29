package io.github.xororz.localdream.remote

import io.github.xororz.localdream.utils.Http
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Controller-side HTTPS client, certificate-pinned using the pairing code. */
class RemoteApiClient(
    val host: String,
    val port: Int = RemoteProtocol.CONTROL_PORT,
    private val sessionKey: String,
    private val fingerprint: String,
) {
    private val baseUrl = "https://$host:$port"

    /** The generation port also uses the same pinned TLS certificate. */
    val generationHost: String = "$host:" + RemoteProtocol.GATEWAY_PORT

    private val client: OkHttpClient by lazy {
        RemoteTls.pairingClient(
            Http.client.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS),
            host,
            fingerprint,
        )
    }

    suspend fun fetchInfo(): RemoteHostInfo? = withContext(Dispatchers.IO) {
        runCatching { get(RemoteProtocol.PATH_INFO)?.let(RemoteHostInfo::fromJson) }.getOrNull()
    }

    suspend fun fetchCatalog(): RemoteCatalog? = withContext(Dispatchers.IO) {
        runCatching { get(RemoteProtocol.PATH_MODELS)?.let(RemoteCatalog::fromJson) }.getOrNull()
    }

    suspend fun selectModel(modelId: String, width: Int, height: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            post(RemoteProtocol.PATH_SELECT, JSONObject().apply {
                put("model_id", modelId)
                put("width", width)
                put("height", height)
            }) != null
        }.getOrDefault(false)
    }

    suspend fun fetchStatus(): RemoteHostStatus? = withContext(Dispatchers.IO) {
        runCatching { get(RemoteProtocol.PATH_STATUS)?.let(RemoteHostStatus::fromJson) }.getOrNull()
    }

    suspend fun stopBackend(modelId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            post(RemoteProtocol.PATH_STOP, JSONObject().put("model_id", modelId)) != null
        }.getOrDefault(false)
    }

    suspend fun checkGenerationHealth(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://$generationHost/health")
                .header(RemoteProtocol.AUTH_HEADER, sessionKey)
                .get().build()
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun get(path: String): JSONObject? {
        val request = Request.Builder().url(baseUrl + path)
            .header(RemoteProtocol.AUTH_HEADER, sessionKey)
            .get().build()
        return execute(request)
    }

    private fun post(path: String, body: JSONObject): JSONObject? {
        val request = Request.Builder().url(baseUrl + path)
            .header(RemoteProtocol.AUTH_HEADER, sessionKey)
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return execute(request)
    }

    private fun execute(request: Request): JSONObject? = client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val payload = response.body?.string() ?: return null
        runCatching { JSONObject(payload) }.getOrNull()
    }

    companion object {
        private const val CONNECT_TIMEOUT_S = 4L
        private const val READ_TIMEOUT_S = 15L
    }
}
