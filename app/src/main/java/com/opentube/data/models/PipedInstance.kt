package com.opentube.data.models

import com.google.gson.annotations.SerializedName

data class PipedInstance(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("api_url")
    val apiUrl: String,
    
    @SerializedName("locations")
    val location: String?,
    
    @SerializedName("start_page")
    val startPage: String? = null,
    
    @SerializedName("version")
    val version: String? = null,
    
    // We will calculate this locally
    var ping: Long = -1L,
    
    var isSelected: Boolean = false
)
