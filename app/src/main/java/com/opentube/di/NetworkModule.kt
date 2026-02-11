package com.opentube.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.opentube.data.api.BaseUrlInterceptor
import com.opentube.data.api.InstanceListService
import com.opentube.data.api.PipedApiService
import com.opentube.data.local.InstancePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Dagger Hilt module for network dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // Default Piped API instance (Fallback)
    private const val DEFAULT_BASE_URL = "https://pipedapi.moomoo.me/"
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        baseUrlInterceptor: BaseUrlInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor) // DYNAMIC URL INJECTION
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        
        return Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL) // This is just a placeholder, Interceptor will override it
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    @Provides
    @Singleton
    fun providePipedApiService(retrofit: Retrofit): PipedApiService {
        return retrofit.create(PipedApiService::class.java)
    }

    // --- INSTANCE LIST GITHUB API ---

    @Provides
    @Singleton
    @Named("GitHubClient")
    fun provideGitHubOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideInstanceListService(
        @Named("GitHubClient") client: OkHttpClient,
        gson: Gson
    ): InstanceListService {
        return Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/TeamPiped/Piped-Backend/master/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(InstanceListService::class.java)
    }
    
    // --- GITHUB API FOR APP UPDATES ---
    
    @Provides
    @Singleton
    fun provideGithubApiService(
        @Named("GitHubClient") client: OkHttpClient,
        gson: Gson
    ): com.opentube.data.api.GithubApiService {
        return Retrofit.Builder()
            .baseUrl(com.opentube.data.api.GithubApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(com.opentube.data.api.GithubApiService::class.java)
    }
}
