package com.example.vitanlyapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Таблица истории веса пользователя.
 * Хранит записи веса по дням для отслеживания прогресса.
 *
 * Индексы:
 * - date: для быстрого поиска записи за конкретный день (уникальный)
 */
@Entity(
    tableName = "weight_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Дата записи в формате "yyyy-MM-dd" */
    val date: String,

    /** Вес в килограммах */
    val weight: Float,

    /** Время создания записи (timestamp) */
    val createdAt: Long = System.currentTimeMillis()
)
