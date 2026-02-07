package com.example.vitanlyapp.data.repository

import com.example.vitanlyapp.data.local.dao.ActivityEntryDao
import com.example.vitanlyapp.data.local.entity.ActivityEntryEntity
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.repository.ActivityEntry
import com.example.vitanlyapp.domain.repository.ActivityHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория истории уровня активности.
 * Использует Room DAO для хранения данных.
 */
@Singleton
class ActivityHistoryRepositoryImpl @Inject constructor(
    private val activityEntryDao: ActivityEntryDao
) : ActivityHistoryRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getHistoryFlow(): Flow<List<ActivityEntry>> {
        return activityEntryDao.getAllEntriesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordActivity(level: ActivityLevel) {
        val today = LocalDate.now().format(dateFormatter)
        recordActivityForDate(today, level)
    }

    override suspend fun recordActivityForDate(date: String, level: ActivityLevel) {
        val entity = ActivityEntryEntity(
            date = date,
            activityLevel = level
        )
        activityEntryDao.insertOrUpdate(entity)
    }

    override suspend fun getActivityForDate(date: String): ActivityLevel? {
        return activityEntryDao.getEntryForDate(date)?.activityLevel
    }

    override suspend fun getLatestActivity(): ActivityLevel? {
        return activityEntryDao.getLatestEntry()?.activityLevel
    }

    override suspend fun clearHistory() {
        activityEntryDao.deleteAll()
    }

    /**
     * Конвертирует Entity в Domain модель.
     */
    private fun ActivityEntryEntity.toDomain() = ActivityEntry(
        id = id,
        date = date,
        activityLevel = activityLevel,
        createdAt = createdAt
    )
}
