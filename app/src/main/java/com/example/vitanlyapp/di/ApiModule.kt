package com.example.vitanlyapp.di

import com.example.vitanlyapp.BuildConfig
import com.example.vitanlyapp.data.remote.DeepSeekApiClient
import com.example.vitanlyapp.data.remote.GitHubApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideDeepSeekApiClient(): DeepSeekApiClient =
        DeepSeekApiClient(apiKey = BuildConfig.DEEPSEEK_API_KEY)

    @Provides
    @Singleton
    fun provideGitHubApiClient(): GitHubApiClient = GitHubApiClient()
}
