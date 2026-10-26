package io.github.xororz.localdream.utils

import io.github.xororz.localdream.remote.RemoteProtocol
import io.github.xororz.localdream.remote.RemoteSession
import okhttp3.OkHttpClient

/** Shared transport for native and remote backend requests. */
object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val key = RemoteSession.keyForBackend(request.url.host, request.url.port)
                chain.proceed(
                    if (key != null) request.newBuilder()
                        .header(RemoteProtocol.AUTH_HEADER, key)
                        .build()
                    else request,
                )
            }
            .build()
    }
}
