package com.opentube.data.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a GitHub Release
 * Used for checking and downloading app updates
 */
data class GithubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("body")
    val body: String,
    
    @SerializedName("published_at")
    val publishedAt: String,
    
    @SerializedName("html_url")
    val htmlUrl: String,
    
    @SerializedName("assets")
    val assets: List<GithubAsset>,
    
    @SerializedName("prerelease")
    val prerelease: Boolean,
    
    @SerializedName("draft")
    val draft: Boolean
) {
    /**
     * Extract version number from tag name (e.g., "v1.2.0" -> "1.2.0")
     */
    fun getVersionName(): String {
        return tagName.removePrefix("v").removePrefix("V")
    }
    
    /**
     * Get the APK download URL from assets
     */
    fun getApkDownloadUrl(): String? {
        return assets.firstOrNull { 
            it.name.endsWith(".apk") && it.contentType == "application/vnd.android.package-archive"
        }?.browserDownloadUrl
            ?: assets.firstOrNull { it.name.endsWith(".apk") }?.browserDownloadUrl
    }
    
    /**
     * Get APK file size in MB
     */
    fun getApkSizeMB(): Float? {
        val asset = assets.firstOrNull { it.name.endsWith(".apk") }
        return asset?.size?.let { it / (1024f * 1024f) }
    }
}

/**
 * Represents a GitHub Release Asset (downloadable file)
 */
data class GithubAsset(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("size")
    val size: Long,
    
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    
    @SerializedName("content_type")
    val contentType: String
)
