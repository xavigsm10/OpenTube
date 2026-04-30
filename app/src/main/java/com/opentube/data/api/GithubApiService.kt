package com.opentube.data.api

import com.opentube.data.models.GithubRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GitHub API service for checking updates
 * Uses the public GitHub API (no authentication required for public repos)
 */
interface GithubApiService {
    
    companion object {
        const val BASE_URL = "https://api.github.com/"
        const val REPO_OWNER = "xavigsm10"
        const val REPO_NAME = "OpenTube"
    }
    
    /**
     * Get the latest release from the repository
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = REPO_OWNER,
        @Path("repo") repo: String = REPO_NAME
    ): Response<GithubRelease>
    
    /**
     * Get all releases from the repository
     */
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getAllReleases(
        @Path("owner") owner: String = REPO_OWNER,
        @Path("repo") repo: String = REPO_NAME
    ): Response<List<GithubRelease>>
}
