package com.example.vitanlyapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.vitanlyapp.data.local.entity.DayEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с записями о еде за день.
 */
@Dao
interface DayEntryDao {

    /**
     * Получить все записи за указанную дату как Flow.
     * @param date дата в формате "yyyy-MM-dd"
     */
    @Query("SELECT * FROM day_entries WHERE date = :date ORDER BY createdAt DESC")
    fun getEntriesForDateFlow(date: String): Flow<List<DayEntryEntity>>

    /**
     * Получить все записи за указанную дату (suspend).
     */
    @Query("SELECT * FROM day_entries WHERE date = :date ORDER BY createdAt DESC")
    suspend fun getEntriesForDate(date: String): List<DayEntryEntity>

    /**
     * Получить запись по ID.
     */
    @Query("SELECT * FROM day_entries WHERE id = :id")
    suspend fun getById(id: Long): DayEntryEntity?

    /**
     * Вставить новую запись о еде.
     */
    @Insert
    suspend fun insert(entry: DayEntryEntity): Long

    /**
     * Вставить несколько записей.
     */
    @Insert
    suspend fun insertAll(entries: List<DayEntryEntity>)

    /**
     * Обновить запись.
     */
    @Update
    suspend fun update(entry: DayEntryEntity)

    /**
     * Удалить запись.
     */
    @Delete
    suspend fun delete(entry: DayEntryEntity)

    /**
     * Удалить запись по id.
     */
    @Query("DELETE FROM day_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Удалить записи по названию за указанную дату.
     * Регистронезависимое сравнение с обрезкой пробелов.
     * @return количество удалённых записей
     */
    @Query("DELETE FROM day_entries WHERE date = :date AND LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun deleteByNameAndDate(date: String, name: String): Int

    /**
     * Удалить все записи приёма пищи по mealSessionId за указанную дату.
     * @return количество удалённых записей
     */
    @Query("DELETE FROM day_entries WHERE date = :date AND mealSessionId = :sessionId")
    suspend fun deleteByMealSessionId(date: String, sessionId: Long): Int

    /**
     * Очистить все записи за указанную дату.
     * @return количество удалённых записей
     */
    @Query("DELETE FROM day_entries WHERE date = :date")
    suspend fun clearDay(date: String): Int

    /**
     * Получить количество записей за указанную дату.
     */
    @Query("SELECT COUNT(*) FROM day_entries WHERE date = :date")
    suspend fun getCountForDate(date: String): Int

    /**
     * Получить сумму калорий за день.
     */
    @Query("SELECT COALESCE(SUM(kcal), 0) FROM day_entries WHERE date = :date")
    suspend fun getTotalKcalForDate(date: String): Int

    /**
     * Получить сумму белков за день.
     */
    @Query("SELECT COALESCE(SUM(protein), 0) FROM day_entries WHERE date = :date")
    suspend fun getTotalProteinForDate(date: String): Float

    /**
     * Получить сумму жиров за день.
     */
    @Query("SELECT COALESCE(SUM(fat), 0) FROM day_entries WHERE date = :date")
    suspend fun getTotalFatForDate(date: String): Float

    /**
     * Получить сумму углеводов за день.
     */
    @Query("SELECT COALESCE(SUM(carbs), 0) FROM day_entries WHERE date = :date")
    suspend fun getTotalCarbsForDate(date: String): Float

    /**
     * Удалить все записи о еде.
     */
    @Query("DELETE FROM day_entries")
    suspend fun deleteAllEntries()

    /**
     * Получить все уникальные даты с записями, отсортированные по убыванию.
     * Используется для навигации по дням на плитке продуктов.
     */
    @Query("SELECT DISTINCT date FROM day_entries ORDER BY date DESC")
    fun getAllDatesFlow(): Flow<List<String>>

    /**
     * Агрегация: сумма ккал по дням за startDate..endDate, ordered ASC.
     * @return Flow of (date, kcalSum) - только дни с записями
     */
    @Query("SELECT date, COALESCE(SUM(kcal), 0) as kcalSum FROM day_entries WHERE date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date ASC")
    fun getKcalPerDayFlow(startDate: String, endDate: String): Flow<List<DayKcalRow> >

    /**
     * Максимальная сумма ккал за один день по всей БД (для стабильной шкалы Y графика).
     */
    @Query("SELECT COALESCE(MAX(daySum), 0) FROM (SELECT SUM(kcal) AS daySum FROM day_entries GROUP BY date)")
    fun getGlobalMaxDailyKcalFlow(): Flow<List<Int>>
}
