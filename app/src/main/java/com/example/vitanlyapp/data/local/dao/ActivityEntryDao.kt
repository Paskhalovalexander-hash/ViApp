package com.example.vitanlyapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vitanlyapp.data.local.entity.ActivityEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с историей уровня активности пользователя.
 */
@Dao
interface ActivityEntryDao {

    /**
     * Получить все записи активности, отсортированные по дате (новые первыми).
     */
    @Query("SELECT * FROM activity_entries ORDER BY date DESC")
    fun getAllEntriesFlow(): Flow<List<ActivityEntryEntity>>

    /**
     * Получить запись за указанную дату.
     * @param date дата в формате "yyyy-MM-dd"
     */
    @Query("SELECT * FROM activity_entries WHERE date = :date")
    suspend fun getEntryForDate(date: String): ActivityEntryEntity?

    /**
     * Получить последнюю запись (по дате).
     */
    @Query("SELECT * FROM activity_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestEntry(): ActivityEntryEntity?

    /**
     * Вставить или обновить запись активности.
     * Если запись за эту дату уже есть — заменяет её.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: ActivityEntryEntity)

    /**
     * Удалить запись по ID.
     */
    @Query("DELETE FROM activity_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Удалить все записи активности.
     */
    @Query("DELETE FROM activity_entries")
    suspend fun deleteAll()

    /**
     * Получить количество записей.
     */
    @Query("SELECT COUNT(*) FROM activity_entries")
    suspend fun getCount(): Int
}
