package com.example.vitanlyapp.domain.repository

import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.UserGoal
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий профиля пользователя.
 * Domain-интерфейс без привязки к Room/Android.
 */
interface UserProfileRepository {

    /**
     * Получить профиль как Flow (реактивно).
     */
    fun getProfileFlow(): Flow<UserProfile?>

    /**
     * Получить текущий профиль (suspend).
     */
    suspend fun getProfile(): UserProfile?

    /**
     * Создать профиль по умолчанию если его нет.
     */
    suspend fun ensureProfileExists()

    // ══════════════════════════════════════════════════════════════════════════
    // Обновление параметров профиля
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun updateWeight(weight: Float)
    suspend fun updateHeight(height: Int)
    suspend fun updateAge(age: Int)
    suspend fun updateGender(gender: Gender)
    suspend fun updateActivityLevel(activityLevel: ActivityLevel)
    suspend fun updateGoal(goal: UserGoal)
    suspend fun updateTargetWeight(targetWeight: Float)
    suspend fun updateTempo(tempo: Float)

    /**
     * Удалить профиль пользователя.
     */
    suspend fun deleteProfile()

    /**
     * Удалить все данные пользователя (профиль, записи о еде, историю чата).
     */
    suspend fun clearAllData()
}

/**
 * Domain-модель профиля пользователя.
 * Отделена от Entity для чистой архитектуры.
 */
data class UserProfile(
    val weight: Float,
    val height: Int,
    val age: Int,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val goal: UserGoal,
    val targetWeight: Float,
    val tempo: Float
)
