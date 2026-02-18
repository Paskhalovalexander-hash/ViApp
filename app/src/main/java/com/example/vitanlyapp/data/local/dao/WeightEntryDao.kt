package com.example.vitanlyapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vitanlyapp.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с историей веса пользователя.
 */
@Dao
interface WeightEntryDao {

    /**
     * Получить все записи веса, отсортированные по дате (новые первыми).
     */
    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    fun getAllEntriesFlow(): Flow<List<WeightEntryEntity>>

    /**
     * Получить запись за указанную дату.
     * @param date дата в формате "yyyy-MM-dd"
     */
    @Query("SELECT * FROM weight_entries WHERE date = :date")
    suspend fun getEntryForDate(date: String): WeightEntryEntity?

    /**
     * Получить последнюю запись (по дате).
     */
    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestEntry(): WeightEntryEntity?

    /**
     * Вставить или обновить запись веса.
     * Если запись за эту дату уже есть — заменяет её.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: WeightEntryEntity)

    /**
     * Удалить запись по ID.
     */
    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Удалить все записи веса.
     */
    @Query("DELETE FROM weight_entries")
    suspend fun deleteAll()

    /**
     * Получить количество записей.
     */
    @Query("SELECT COUNT(*) FROM weight_entries")
    suspend fun getCount(): Int
}
