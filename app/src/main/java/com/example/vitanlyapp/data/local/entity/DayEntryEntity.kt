package com.example.vitanlyapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Таблица записей о еде за день.
 * Каждая запись — один продукт/блюдо с КБЖУ.
 *
 * Индексы:
 * - date: для быстрого получения записей за конкретный день
 * - mealSessionId: для группировки по приёмам пищи
 */
@Entity(
    tableName = "day_entries",
    indices = [
        Index(value = ["date"]),
        Index(value = ["mealSessionId"])
    ]
)
data class DayEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Дата записи в формате "yyyy-MM-dd" */
    val date: String,

    /** Название продукта или блюда */
    val name: String,

    /** Вес порции в граммах */
    val weightGrams: Int,

    /** Калории (ккал) */
    val kcal: Int,

    /** Белки (г) */
    val protein: Float,

    /** Жиры (г) */
    val fat: Float,

    /** Углеводы (г) */
    val carbs: Float,

    /** Тип приёма пищи */
    val mealType: MealType = MealType.SNACK,

    /** Время создания записи (timestamp) */
    val createdAt: Long = System.currentTimeMillis(),

    /** Emoji продукта (генерируется AI) */
    val emoji: String = "🍽️",

    /**
     * ID сессии приёма пищи (timestamp первого продукта в группе).
     * Продукты, добавленные в течение 30 минут, имеют одинаковый mealSessionId.
     */
    val mealSessionId: Long = System.currentTimeMillis()
)

/**
 * Тип приёма пищи.
 */
enum class MealType(val label: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус")
}
