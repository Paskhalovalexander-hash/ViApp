package com.example.vitanlyapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Цель пользователя для корректировки TDEE.
 * - LOSE: дефицит калорий (похудение)
 * - GAIN: профицит калорий (набор массы)
 * - MAINTAIN: поддержание текущего веса
 */
@Serializable
enum class UserGoal(val label: String) {
    @SerialName("lose")
    LOSE("Похудеть"),

    @SerialName("gain")
    GAIN("Набрать"),

    @SerialName("maintain")
    MAINTAIN("Удержать");

    companion object {
        val default: UserGoal get() = MAINTAIN

        /**
         * Находит UserGoal по строковому значению.
         * Поддерживает как имена enum, так и JSON-значения (lose, gain, maintain).
         */
        fun fromString(value: String): UserGoal? {
            val normalized = value.lowercase()
            return when (normalized) {
                "lose" -> LOSE
                "gain" -> GAIN
                "maintain" -> MAINTAIN
                else -> entries.find { it.name.equals(value, ignoreCase = true) }
            }
        }
    }
}
