package com.example.vitanlyapp.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий истории веса пользователя.
 * Domain-интерфейс без привязки к Room/Android.
 */
interface WeightHistoryRepository {

    /**
     * Получить всю историю веса как Flow (реактивно).
     * Записи отсортированы по дате (новые первыми).
     */
    fun getHistoryFlow(): Flow<List<WeightEntry>>

    /**
     * Записать вес на сегодня.
     * Если запись за сегодня уже есть — обновляет её.
     */
    suspend fun recordWeight(weight: Float)

    /**
     * Записать вес на указанную дату.
     * Если запись за эту дату уже есть — обновляет её.
     * @param date дата в формате "yyyy-MM-dd"
     */
    suspend fun recordWeightForDate(date: String, weight: Float)

    /**
     * Получить вес за указанную дату.
     * @param date дата в формате "yyyy-MM-dd"
     * @return вес в кг или null если записи нет
     */
    suspend fun getWeightForDate(date: String): Float?

    /**
     * Получить последнюю запись веса (по дате).
     */
    suspend fun getLatestWeight(): Float?

    /**
     * Удалить всю историю веса.
     */
    suspend fun clearHistory()
}

/**
 * Domain-модель записи веса.
 */
data class WeightEntry(
    val id: Long,
    val date: String,
    val weight: Float,
    val createdAt: Long
)
