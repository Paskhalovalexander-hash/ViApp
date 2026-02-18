package com.example.vitanlyapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.UserGoal

/**
 * Таблица профиля пользователя.
 * Хранит все параметры для расчёта норм КБЖУ по Mifflin-St. Jeor.
 *
 * В приложении всегда один профиль (id = 1).
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,

    /** Текущий вес в кг */
    val weight: Float = 70f,

    /** Рост в см */
    val height: Int = 170,

    /** Возраст в годах */
    val age: Int = 25,

    /** Пол (MALE/FEMALE) */
    val gender: Gender = Gender.MALE,

    /** Уровень активности */
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,

    /** Цель (LOSE/GAIN/MAINTAIN) */
    val goal: UserGoal = UserGoal.MAINTAIN,

    /** Целевой вес в кг */
    val targetWeight: Float = 70f,

    /** Темп изменения веса (кг/неделя) */
    val tempo: Float = 0.5f,

    /** Дата создания профиля (timestamp) */
    val createdAt: Long = System.currentTimeMillis(),

    /** Дата последнего обновления (timestamp) */
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Создаёт профиль по умолчанию.
         */
        fun default() = UserProfileEntity()
    }
}
