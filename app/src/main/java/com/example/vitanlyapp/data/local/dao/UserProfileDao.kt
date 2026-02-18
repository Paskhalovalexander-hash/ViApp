package com.example.vitanlyapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.vitanlyapp.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с профилем пользователя.
 * В приложении всегда один профиль (id = 1).
 */
@Dao
interface UserProfileDao {

    /**
     * Получить профиль пользователя как Flow (реактивно).
     */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>

    /**
     * Получить профиль пользователя (suspend).
     */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    /**
     * Вставить или заменить профиль.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)

    /**
     * Обновить профиль.
     */
    @Update
    suspend fun update(profile: UserProfileEntity)

    /**
     * Удалить профиль пользователя.
     */
    @Query("DELETE FROM user_profile")
    suspend fun deleteProfile()
}
