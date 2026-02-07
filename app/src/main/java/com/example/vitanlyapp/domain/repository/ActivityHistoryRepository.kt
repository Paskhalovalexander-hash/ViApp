package com.example.vitanlyapp.domain.repository

import com.example.vitanlyapp.domain.model.ActivityLevel
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий истории уровня активности пользователя.
 * Domain-интерфейс без привязки к Room/Android.
 */
interface ActivityHistoryRepository {

    /**
     * Получить всю историю активности как Flow (реактивно).
     * Записи отсортированы по дате (новые первыми).
     */
    fun getHistoryFlow(): Flow<List<ActivityEntry>>

    /**
     * Записать уровень активности на сегодня.
     * Если запись за сегодня уже есть — обновляет её.
     */
    suspend fun recordActivity(level: ActivityLevel)

    /**
     * Записать уровень активности на указанную дату.
     * Если запись за эту дату уже есть — обновляет её.
     * @param date дата в формате "yyyy-MM-dd"
     */
    suspend fun recordActivityForDate(date: String, level: ActivityLevel)

    /**
     * Получить уровень активности за указанную дату.
     * @param date дата в формате "yyyy-MM-dd"
     * @return уровень активности или null если записи нет
     */
    suspend fun getActivityForDate(date: String): ActivityLevel?

    /**
     * Получить последнюю запись активности (по дате).
     */
    suspend fun getLatestActivity(): ActivityLevel?

    /**
     * Удалить всю историю активности.
     */
    suspend fun clearHistory()
}

/**
 * Domain-модель записи активности.
 */
data class ActivityEntry(
    val id: Long,
    val date: String,
    val activityLevel: ActivityLevel,
    val createdAt: Long
)
