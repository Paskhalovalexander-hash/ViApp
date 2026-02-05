package com.example.vitanlyapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запись о еде, распознанная AI из текста пользователя.
 * Содержит название продукта, вес, нутриенты (КБЖУ) и emoji.
 *
 * Пример JSON от AI:
 * ```json
 * {"name": "Яичница", "weight_g": 150, "kcal": 220, "protein": 14.0, "fat": 17.0, "carbs": 1.0, "emoji": "🍳"}
 * ```
 */
@Serializable
data class FoodEntry(
    /** Название продукта или блюда */
    val name: String,

    /** Вес порции в граммах */
    @SerialName("weight_g")
    val weightGrams: Int,

    /** Калории (ккал) */
    val kcal: Int,

    /** Белки (г) */
    val protein: Float,

    /** Жиры (г) */
    val fat: Float,

    /** Углеводы (г) */
    val carbs: Float,

    /** Emoji продукта (генерируется AI) */
    val emoji: String = "🍽️"
) {
    companion object {
        /**
         * Суммирует несколько записей о еде в одну (для итогов дня).
         */
        fun sumOf(entries: List<FoodEntry>): FoodEntry = FoodEntry(
            name = "Итого",
            weightGrams = entries.sumOf { it.weightGrams },
            kcal = entries.sumOf { it.kcal },
            protein = entries.sumOf { it.protein.toDouble() }.toFloat(),
            fat = entries.sumOf { it.fat.toDouble() }.toFloat(),
            carbs = entries.sumOf { it.carbs.toDouble() }.toFloat(),
            emoji = "📊"
        )
    }
}
