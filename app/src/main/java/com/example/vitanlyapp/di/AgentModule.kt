package com.example.vitanlyapp.di

import com.example.vitanlyapp.BuildConfig
import com.example.vitanlyapp.data.agent.AppControlAdapter
import com.example.vitanlyapp.data.agent.ChatAIAdapter
import com.example.vitanlyapp.data.agent.FoodParsingAdapter
import com.example.vitanlyapp.data.orchestrator.AgentOrchestratorImpl
import com.example.vitanlyapp.domain.orchestrator.AgentOrchestrator
import com.example.vitanlyapp.domain.repository.ChatRepository
import com.example.vitanlyapp.domain.repository.DayEntryRepository
import com.example.vitanlyapp.domain.repository.ThemeRepository
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль для Agent Layer.
 *
 * Предоставляет адаптеры для:
 * - Обработки еды (FoodParsingAdapter)
 * - Выполнения команд (AppControlAdapter)
 * - Взаимодействия с AI (ChatAIAdapter)
 * - Оркестратор (AgentOrchestrator)
 */
@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    /**
     * Адаптер для обработки записей о еде из ответа AI.
     */
    @Provides
    @Singleton
    fun provideFoodParsingAdapter(
        dayEntryRepository: DayEntryRepository
    ): FoodParsingAdapter = FoodParsingAdapter(dayEntryRepository)

    /**
     * Адаптер для выполнения команд AI в приложении.
     */
    @Provides
    @Singleton
    fun provideAppControlAdapter(
        userProfileRepository: UserProfileRepository,
        dayEntryRepository: DayEntryRepository,
        themeRepository: ThemeRepository,
        chatRepository: ChatRepository,
        foodParsingAdapter: FoodParsingAdapter
    ): AppControlAdapter = AppControlAdapter(
        userProfileRepository = userProfileRepository,
        dayEntryRepository = dayEntryRepository,
        themeRepository = themeRepository,
        chatRepository = chatRepository,
        foodParsingAdapter = foodParsingAdapter
    )

    /**
     * Адаптер для взаимодействия с DeepSeek Chat API.
     */
    @Provides
    @Singleton
    fun provideChatAIAdapter(
        chatRepository: ChatRepository,
        userProfileRepository: UserProfileRepository
    ): ChatAIAdapter = ChatAIAdapter(
        chatRepository = chatRepository,
        userProfileRepository = userProfileRepository,
        apiKey = BuildConfig.DEEPSEEK_API_KEY
    )

    /**
     * Оркестратор AI-агента — фасад для удобного взаимодействия.
     * Связывает интерфейс AgentOrchestrator с реализацией AgentOrchestratorImpl.
     */
    @Provides
    @Singleton
    fun provideAgentOrchestrator(
        chatAIAdapter: ChatAIAdapter,
        appControlAdapter: AppControlAdapter,
        foodParsingAdapter: FoodParsingAdapter
    ): AgentOrchestrator = AgentOrchestratorImpl(
        chatAIAdapter = chatAIAdapter,
        appControlAdapter = appControlAdapter,
        foodParsingAdapter = foodParsingAdapter
    )
}
