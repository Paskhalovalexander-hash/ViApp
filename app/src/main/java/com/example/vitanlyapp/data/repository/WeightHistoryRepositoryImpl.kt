package com.example.vitanlyapp.data.repository

import com.example.vitanlyapp.data.local.dao.WeightEntryDao
import com.example.vitanlyapp.data.local.entity.WeightEntryEntity
import com.example.vitanlyapp.domain.repository.WeightEntry
import com.example.vitanlyapp.domain.repository.WeightHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория истории веса.
 * Использует Room DAO для хранения данных.
 */
@Singleton
class WeightHistoryRepositoryImpl @Inject constructor(
    private val weightEntryDao: WeightEntryDao
) : WeightHistoryRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getHistoryFlow(): Flow<List<WeightEntry>> {
        return weightEntryDao.getAllEntriesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordWeight(weight: Float) {
        val today = LocalDate.now().format(dateFormatter)
        recordWeightForDate(today, weight)
    }

    override suspend fun recordWeightForDate(date: String, weight: Float) {
        val entity = WeightEntryEntity(
            date = date,
            weight = weight
        )
        weightEntryDao.insertOrUpdate(entity)
    }

    override suspend fun getWeightForDate(date: String): Float? {
        return weightEntryDao.getEntryForDate(date)?.weight
    }

    override suspend fun getLatestWeight(): Float? {
        return weightEntryDao.getLatestEntry()?.weight
    }

    override suspend fun clearHistory() {
        weightEntryDao.deleteAll()
    }

    /**
     * Конвертирует Entity в Domain модель.
     */
    private fun WeightEntryEntity.toDomain() = WeightEntry(
        id = id,
        date = date,
        weight = weight,
        createdAt = createdAt
    )
}
