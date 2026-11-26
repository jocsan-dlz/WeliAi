package com.npcg.practica1.di

import com.google.gson.Gson
import com.npcg.practica1.BuildConfig
import com.npcg.practica1.data.remote.WelliAiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiNetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAiOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(50, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(55, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    @Provides
    @Singleton
    fun provideWelliAiApiService(client: OkHttpClient, gson: Gson): WelliAiApiService {
        val baseUrl = BuildConfig.AI_BACKEND_BASE_URL
        require(baseUrl.isNotBlank()) {
            "Configure WELLIAI_BACKEND_URL with an HTTPS backend URL before using a release build."
        }
        require(BuildConfig.DEBUG || baseUrl.startsWith("https://")) {
            "Release builds require an HTTPS backend URL."
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WelliAiApiService::class.java)
    }
}
