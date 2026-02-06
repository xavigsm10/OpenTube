package com.opentube.data.repository

import com.opentube.data.api.InstanceListService
import com.opentube.data.local.InstancePreferences
import com.opentube.data.models.PipedInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class InstanceRepository @Inject constructor(
    private val instanceListService: InstanceListService,
    private val instancePreferences: InstancePreferences,
    private val okHttpClient: OkHttpClient // basic client for pings
) {

    suspend fun getInstances(): List<PipedInstance> = withContext(Dispatchers.IO) {
        try {
            val instances = instanceListService.getInstances()
            // Filter only working instances (optional, or just return all)
            instances
        } catch (e: Exception) {
            // Fallback list if GitHub fails
            listOf(
                PipedInstance("Kavin (Default)", "https://pipedapi.kavin.rocks/", "US"),
                PipedInstance("Tokhmi", "https://pipedapi.tokhmi.xyz/", "US"),
                PipedInstance("MooMoo", "https://pipedapi.moomoo.me/", "US")
            )
        }
    }

    suspend fun pingInstance(url: String): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).head().build() 
        // We use HEAD to be faster, or GET / if not allowed. 
        // Piped usually has / on root returning something or 404, but at least a response.
        
        try {
            val time = measureTimeMillis {
                okHttpClient.newCall(request).execute().close()
            }
            time
        } catch (e: IOException) {
            -1L // Timeout or unreachable
        }
    }

    suspend fun saveInstance(url: String) {
        instancePreferences.setApiUrl(url)
    }
    
    fun getSelectedInstance() = instancePreferences.apiUrlFlow
}
