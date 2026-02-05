package com.example.vitanlyapp.domain.repository

import com.example.vitanlyapp.domain.model.FoodEntry
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий записей о еде за день.
 * Domain-интерфейс без привязки к Room/Android.
 */
interface DayEntryRepository {

    /**
     * Получить все записи за сегодня как Flow.
     */
    fun getTodayEntriesFlow(): Flow<List<DayEntry>>

    /**
     * Flow текущей даты в формате "yyyy-MM-dd".
     * Эмитит новое значение каждые 60 секунд; при смене дня подписчики получают обновление.
     */
    fun getCurrentDateFlow(): Flow<String>

    /**
     * Получить все записи за указанную дату как Flow.
     * @param date дата в формате "yyyy-MM-dd"
     */
    fun getEntriesForDateFlow(date: String): Flow<List<DayEntry>>

    /**
     * Получить все записи за сегодня (suspend).
     */
    suspend fun getTodayEntries(): List<DayEntry>

    /**
     * Добавить запись о еде.
     */
    suspend fun addEntry(entry: FoodEntry)

    /**
     * Добавить несколько записей о еде.
     */
    suspend fun addEntries(entries: List<FoodEntry>)

    /**
     * Удалить запись по id.
     */
    suspend fun deleteEntry(id: Long)

    /**
     * Удалить записи по названию за сегодня.
     */
    suspend fun deleteByName(name: String)

    /**
     * Удалить все записи приёма пищи по mealSessionId за сегодня.
     */
    suspend fun deleteByMealSessionId(sessionId: Long)

    /**
     * Очистить все записи за сегодня.
     */
    suspend fun clearToday()

    /**
     * Очистить все записи за указанную дату.
     * @param date дата в формате "yyyy-MM-dd"
     * @return количество удалённых записей
     */
    suspend fun clearDay(date: String): Int

    /**
     * Получить итоги КБЖУ за сегодня.
     */
    suspend fun getTodayTotals(): DayTotals

    /**
     * Получить итоги КБЖУ за указанную дату.
     * @param date дата в формате "yyyy-MM-dd"
     */
    suspend fun getDayTotals(date: String): DayTotals

    /**
     * Получить запись по ID.
     */
    suspend fun getEntryById(id: Long): DayEntry?

    /**
     * Повторить запись (создать копию в текущей сессии).
     * @return новая запись или null если исходная не найдена
     */
    suspend fun repeatEntry(id: Long): DayEntry?

    /**
     * Обновить вес записи с пропорциональным пересчётом КБЖУ.
     * @return обновлённая запись или null если не найдена
     */
    suspend fun updateEntryWeight(id: Long, newWeightGrams: Int): DayEntry?
}

/**
 * Domain-модель записи о еде.
 * Отделена от Entity для чистой архитектуры.
 */
data class DayEntry(
    val id: Long,
    val date: String,
    val name: String,
    val weightGrams: Int,
    val kcal: Int,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val mealType: String,
    val createdAt: Long,
    /** Emoji продукта */
    val emoji: String,
    /** ID сессии приёма пищи для группировки (timestamp первого продукта) */
    val mealSessionId: Long
)

/**
 * Итоги КБЖУ за день.
 */
data class DayTotals(
    val kcal: Int,
    val protein: Float,
    val fat: Float,
    val carbs: Float
) {
    companion object {
        fun empty() = DayTotals(0, 0f, 0f, 0f)
    }
}
