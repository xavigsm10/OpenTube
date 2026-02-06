package com.opentube.data.api

import com.opentube.data.models.PipedInstance
import retrofit2.http.GET

interface InstanceListService {
    @GET("instances.json")
    suspend fun getInstances(): List<PipedInstance>
}
