package io.github.xororz.localdream.utils

import io.github.xororz.localdream.remote.RemoteProtocol
import io.github.xororz.localdream.remote.RemoteSession
import io.github.xororz.localdream.remote.RemoteTls
import okhttp3.OkHttpClient

/** Shared client with pinned TLS for the active LAN generation gateway. */
object Http {
    /** Pinned client for LAN/remote backend calls. */
    val client: OkHttpClient by lazy {
        RemoteTls.sharedClient(
            OkHttpClient.Builder()
                // URL upgrading happens before connection; all native local
                // calls still use the default loopback HTTP transport.
                .addInterceptor { chain ->
                    val request = chain.request()
                    val key = RemoteSession.keyForBackend(request.url.host, request.url.port)
                    if (key == null || request.url.scheme == "https") {
                        chain.proceed(request)
                    } else {
                        chain.proceed(
                            request.newBuilder()
                                .url(request.url.newBuilder().scheme("https").build())
                                .build(),
                        )
                    }
                }
                // Attach the bearer only to the actual selected network host.
                // Redirects can never forward a secret to a different origin.
                .addNetworkInterceptor { chain ->
                    val request = chain.request()
                    val key = if (request.url.scheme == "https") {
                        RemoteSession.keyForBackend(request.url.host, request.url.port)
                    } else null
                    chain.proceed(
                        if (key != null) request.newBuilder()
                            .header(RemoteProtocol.AUTH_HEADER, key).build()
                        else request,
                    )
                },
        )
    }

    /** Plain system-CA client for public model downloads, matching upstream. */
    val downloadClient: OkHttpClient by lazy { OkHttpClient() }
}
