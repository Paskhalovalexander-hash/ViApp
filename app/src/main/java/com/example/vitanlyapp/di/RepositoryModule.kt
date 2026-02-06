package com.example.vitanlyapp.di

import com.example.vitanlyapp.data.repository.ChatRepositoryImpl
import com.example.vitanlyapp.data.repository.DayEntryRepositoryImpl
import com.example.vitanlyapp.data.repository.PersistentKbjuRepository
import com.example.vitanlyapp.data.repository.PreferencesThemeRepository
import com.example.vitanlyapp.data.repository.UpdateRepositoryImpl
import com.example.vitanlyapp.data.repository.UserProfileRepositoryImpl
import com.example.vitanlyapp.domain.repository.ChatRepository
import com.example.vitanlyapp.domain.repository.DayEntryRepository
import com.example.vitanlyapp.domain.repository.KbjuRepository
import com.example.vitanlyapp.domain.repository.ThemeRepository
import com.example.vitanlyapp.domain.repository.UpdateRepository
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Привязка KbjuRepository к реализации, которая:
     * - Получает текущие КБЖУ из DayEntryRepository
     * - Рассчитывает нормы по Mifflin-St Jeor из UserProfileRepository
     */
    @Binds
    @Singleton
    abstract fun bindKbjuRepository(
        impl: PersistentKbjuRepository
    ): KbjuRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: PreferencesThemeRepository
    ): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindDayEntryRepository(
        impl: DayEntryRepositoryImpl
    ): DayEntryRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(
        impl: UpdateRepositoryImpl
    ): UpdateRepository
}
