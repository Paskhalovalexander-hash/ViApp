package com.example.vitanlyapp.di

import android.content.Context
import androidx.room.Room
import com.example.vitanlyapp.data.local.VitanlyDatabase
import com.example.vitanlyapp.data.local.dao.ChatMessageDao
import com.example.vitanlyapp.data.local.dao.DayEntryDao
import com.example.vitanlyapp.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления зависимостей базы данных.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Предоставляет единственный экземпляр базы данных.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VitanlyDatabase {
        return Room.databaseBuilder(
            context,
            VitanlyDatabase::class.java,
            VitanlyDatabase.DATABASE_NAME
        )
            .addMigrations(VitanlyDatabase.MIGRATION_1_2)
            .build()
    }

    /**
     * Предоставляет DAO для профиля пользователя.
     */
    @Provides
    @Singleton
    fun provideUserProfileDao(database: VitanlyDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    /**
     * Предоставляет DAO для записей о еде.
     */
    @Provides
    @Singleton
    fun provideDayEntryDao(database: VitanlyDatabase): DayEntryDao {
        return database.dayEntryDao()
    }

    /**
     * Предоставляет DAO для сообщений чата.
     */
    @Provides
    @Singleton
    fun provideChatMessageDao(database: VitanlyDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }
}
