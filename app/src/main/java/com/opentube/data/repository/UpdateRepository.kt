package com.opentube.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.opentube.BuildConfig
import com.opentube.data.api.GithubApiService
import com.opentube.data.models.GithubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing the update check result
 */
data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val apkSizeMB: Float?,
    val htmlUrl: String
)

/**
 * Data class representing download progress
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progress: Float // 0.0 to 1.0
)

/**
 * Repository for handling app updates via GitHub Releases
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubApiService: GithubApiService,
    @javax.inject.Named("GitHubClient") private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "UpdateRepository"
        private const val APK_FILE_NAME = "opentube-update.apk"
    }
    
    /**
     * Check if there's a new version available
     */
    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val response = githubApiService.getLatestRelease()
            
            if (response.isSuccessful) {
                val release = response.body()
                if (release != null && !release.draft && !release.prerelease) {
                    val latestVersion = release.getVersionName()
                    val currentVersion = BuildConfig.VERSION_NAME
                    
                    val hasUpdate = isNewerVersion(latestVersion, currentVersion)
                    
                    android.util.Log.d(TAG, "Current: $currentVersion, Latest: $latestVersion, HasUpdate: $hasUpdate")
                    
                    Result.success(UpdateInfo(
                        hasUpdate = hasUpdate,
                        currentVersion = currentVersion,
                        latestVersion = latestVersion,
                        releaseNotes = release.body,
                        downloadUrl = release.getApkDownloadUrl(),
                        apkSizeMB = release.getApkSizeMB(),
                        htmlUrl = release.htmlUrl
                    ))
                } else {
                    Result.failure(Exception("No valid release found"))
                }
            } else {
                Result.failure(Exception("Error checking updates: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error checking for updates", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download the APK with progress updates
     */
    fun downloadApk(downloadUrl: String): Flow<Result<DownloadProgress>> = flow {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                emit(Result.failure(Exception("Download failed: ${response.code}")))
                return@flow
            }
            
            val body = response.body ?: run {
                emit(Result.failure(Exception("Empty response body")))
                return@flow
            }
            
            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L
            
            // Create file in cache directory
            val apkFile = File(context.cacheDir, APK_FILE_NAME)
            
            body.byteStream().use { inputStream ->
                FileOutputStream(apkFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytes: Int
                    
                    while (inputStream.read(buffer).also { bytes = it } != -1) {
                        outputStream.write(buffer, 0, bytes)
                        bytesDownloaded += bytes
                        
                        val progress = if (totalBytes > 0) {
                            bytesDownloaded.toFloat() / totalBytes.toFloat()
                        } else {
                            0f
                        }
                        
                        emit(Result.success(DownloadProgress(
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            progress = progress
                        )))
                    }
                }
            }
            
            android.util.Log.d(TAG, "APK downloaded successfully: ${apkFile.absolutePath}")
            Unit // Explicit Unit to satisfy flow return type
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error downloading APK", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Install the downloaded APK
     */
    fun installApk(): Boolean {
        try {
            val apkFile = File(context.cacheDir, APK_FILE_NAME)
            
            if (!apkFile.exists()) {
                android.util.Log.e(TAG, "APK file not found")
                return false
            }
            
            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // For Android N and above, use FileProvider
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            context.startActivity(intent)
            return true
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error installing APK", e)
            return false
        }
    }
    
    /**
     * Delete the downloaded APK file
     */
    fun cleanupDownloadedApk() {
        try {
            val apkFile = File(context.cacheDir, APK_FILE_NAME)
            if (apkFile.exists()) {
                apkFile.delete()
                android.util.Log.d(TAG, "Cleaned up APK file")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error cleaning up APK", e)
        }
    }
    
    /**
     * Compare version strings (e.g., "1.2.0" vs "1.1.5")
     * Returns true if latestVersion is newer than currentVersion
     */
    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        try {
            val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(latestParts.size, currentParts.size)
            
            for (i in 0 until maxLength) {
                val latest = latestParts.getOrElse(i) { 0 }
                val current = currentParts.getOrElse(i) { 0 }
                
                if (latest > current) return true
                if (latest < current) return false
            }
            
            return false // Versions are equal
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error comparing versions", e)
            return false
        }
    }
}
