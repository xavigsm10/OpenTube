package com.opentube.data.api

import com.opentube.data.local.InstancePreferences
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val instancePreferences: InstancePreferences
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // Blocking here is necessary because Interceptor is synchronous
        // Ideally, we should have the URL cached in memory to avoid blocking on DataStore every time
        // But for simplicity/robustness in this context, we'll read it.
        // Optimization: Use runBlocking only if we don't have a cached value, but DataStore is fast enough for now or we can assume simple usage.
        
        // Note: For a production app, we might want to cache the "currentUrl" in a volatile variable in InstancePreferences 
        // and update it with a coroutine collector to avoid runBlocking here.
        // However, to keep it simple and robust as requested:
        
        val currentApiUrl = runBlocking {
            instancePreferences.apiUrlFlow.first()
        }

        val originalHttpUrl = request.url
        val newBaseUrl = currentApiUrl.toHttpUrlOrNull()

        if (newBaseUrl != null) {
            val newHttpUrl = originalHttpUrl.newBuilder()
                .scheme(newBaseUrl.scheme)
                .host(newBaseUrl.host)
                .port(newBaseUrl.port)
                .build()

            request = request.newBuilder()
                .url(newHttpUrl)
                .build()
        }

        return chain.proceed(request)
    }
}
