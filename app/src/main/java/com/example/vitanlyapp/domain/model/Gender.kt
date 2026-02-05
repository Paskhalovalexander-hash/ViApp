package com.example.vitanlyapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Пол пользователя для расчёта BMR по Mifflin-St. Jeor.
 * Формула отличается для мужчин и женщин.
 */
@Serializable
enum class Gender(val label: String) {
    @SerialName("male")
    MALE("Мужской"),

    @SerialName("female")
    FEMALE("Женский");

    companion object {
        val default: Gender get() = MALE

        /**
         * Находит Gender по строковому значению.
         * Поддерживает как имена enum, так и JSON-значения (male, female).
         */
        fun fromString(value: String): Gender? {
            val normalized = value.lowercase()
            return when (normalized) {
                "male" -> MALE
                "female" -> FEMALE
                else -> entries.find { it.name.equals(value, ignoreCase = true) }
            }
        }
    }
}
