package com.example.vitanlyapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Уровень физической активности для формулы TDEE (BMR × коэффициент).
 * Стандартные множители Mifflin-St. Jeor.
 */
@Serializable
enum class ActivityLevel(
    val coefficient: Float,
    val label: String
) {
    @SerialName("sedentary")
    SEDENTARY(1.2f, "Минимальная"),

    @SerialName("light")
    LIGHT(1.375f, "Низкая"),

    @SerialName("moderate")
    MODERATE(1.55f, "Умеренная"),

    @SerialName("active")
    VERY_ACTIVE(1.725f, "Высокая"),

    @SerialName("very_active")
    EXTRA_ACTIVE(1.9f, "Очень высокая");

    companion object {
        val levels: List<ActivityLevel> = entries

        fun fromCoefficient(coeff: Float): ActivityLevel =
            entries.minByOrNull { kotlin.math.abs(it.coefficient - coeff) } ?: MODERATE

        /**
         * Находит ActivityLevel по строковому значению.
         * Поддерживает как имена enum (VERY_ACTIVE), так и JSON-значения (active, very_active).
         */
        fun fromString(value: String): ActivityLevel? {
            val normalized = value.lowercase()
            return when (normalized) {
                "sedentary" -> SEDENTARY
                "light" -> LIGHT
                "moderate" -> MODERATE
                "active" -> VERY_ACTIVE
                "very_active" -> EXTRA_ACTIVE
                else -> entries.find { it.name.equals(value, ignoreCase = true) }
            }
        }

        val default: ActivityLevel get() = MODERATE
    }
}

/**
 * Расчёт TDEE (суточная норма калорий) по формуле: BMR × коэффициент активности.
 * Используется при наличии BMR (Mifflin-St. Jeor или Harris-Benedict).
 */
fun computeTdee(bmr: Float, activityCoefficient: Float): Int =
    (bmr * activityCoefficient).toInt()
