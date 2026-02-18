package com.example.vitanlyapp.data.repository

import com.example.vitanlyapp.data.local.dao.ChatMessageDao
import com.example.vitanlyapp.data.local.dao.DayEntryDao
import com.example.vitanlyapp.data.local.dao.UserProfileDao
import com.example.vitanlyapp.data.local.entity.UserProfileEntity
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.UserGoal
import com.example.vitanlyapp.domain.repository.UserProfile
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория профиля пользователя.
 * Использует Room DAO для хранения данных.
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val dayEntryDao: DayEntryDao,
    private val chatMessageDao: ChatMessageDao
) : UserProfileRepository {

    override fun getProfileFlow(): Flow<UserProfile?> {
        return userProfileDao.getProfileFlow().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getProfile(): UserProfile? {
        return userProfileDao.getProfile()?.toDomain()
    }

    override suspend fun ensureProfileExists() {
        if (userProfileDao.getProfile() == null) {
            userProfileDao.insertOrUpdate(UserProfileEntity.default())
        }
    }

    override suspend fun updateWeight(weight: Float) {
        updateProfile { it.copy(weight = weight) }
    }

    override suspend fun updateHeight(height: Int) {
        updateProfile { it.copy(height = height) }
    }

    override suspend fun updateAge(age: Int) {
        updateProfile { it.copy(age = age) }
    }

    override suspend fun updateGender(gender: Gender) {
        updateProfile { it.copy(gender = gender) }
    }

    override suspend fun updateActivityLevel(activityLevel: ActivityLevel) {
        updateProfile { it.copy(activityLevel = activityLevel) }
    }

    override suspend fun updateGoal(goal: UserGoal) {
        updateProfile { it.copy(goal = goal) }
    }

    override suspend fun updateTargetWeight(targetWeight: Float) {
        updateProfile { it.copy(targetWeight = targetWeight) }
    }

    override suspend fun updateTempo(tempo: Float) {
        updateProfile { it.copy(tempo = tempo) }
    }

    /**
     * Обновляет профиль с помощью трансформации.
     * Если профиля нет — создаёт дефолтный и применяет трансформацию.
     */
    private suspend fun updateProfile(transform: (UserProfileEntity) -> UserProfileEntity) {
        val current = userProfileDao.getProfile() ?: UserProfileEntity.default()
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        userProfileDao.insertOrUpdate(updated)
    }

    override suspend fun deleteProfile() {
        userProfileDao.deleteProfile()
    }

    override suspend fun clearAllData() {
        // Удаляем все данные пользователя
        userProfileDao.deleteProfile()
        dayEntryDao.deleteAllEntries()
        chatMessageDao.clearAll()
    }

    /**
     * Конвертирует Entity в Domain модель.
     */
    private fun UserProfileEntity.toDomain() = UserProfile(
        weight = weight,
        height = height,
        age = age,
        gender = gender,
        activityLevel = activityLevel,
        goal = goal,
        targetWeight = targetWeight,
        tempo = tempo
    )
}
