package com.example.vitanlyapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.vitanlyapp.domain.model.ActivityLevel

/**
 * Таблица истории уровня активности пользователя.
 * Хранит записи активности по дням для отслеживания изменений.
 *
 * Индексы:
 * - date: для быстрого поиска записи за конкретный день (уникальный)
 */
@Entity(
    tableName = "activity_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class ActivityEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Дата записи в формате "yyyy-MM-dd" */
    val date: String,

    /** Уровень активности */
    val activityLevel: ActivityLevel,

    /** Время создания записи (timestamp) */
    val createdAt: Long = System.currentTimeMillis()
)
