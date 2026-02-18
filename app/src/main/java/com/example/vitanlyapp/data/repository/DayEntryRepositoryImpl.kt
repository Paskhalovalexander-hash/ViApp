package com.example.vitanlyapp.data.repository

import com.example.vitanlyapp.data.local.dao.DayEntryDao
import com.example.vitanlyapp.data.local.entity.DayEntryEntity
import com.example.vitanlyapp.data.local.entity.MealType
import com.example.vitanlyapp.domain.model.FoodEntry
import com.example.vitanlyapp.data.local.dao.DayKcalRow
import com.example.vitanlyapp.domain.repository.DayEntry
import com.example.vitanlyapp.domain.repository.DayEntryRepository
import com.example.vitanlyapp.domain.repository.DayKcalPoint
import com.example.vitanlyapp.domain.repository.DayTotals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория записей о еде.
 * Использует Room DAO для хранения данных.
 */
@Singleton
class DayEntryRepositoryImpl @Inject constructor(
    private val dayEntryDao: DayEntryDao
) : DayEntryRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    companion object {
        /** Окно группировки приёма пищи: 30 минут */
        private const val MEAL_SESSION_WINDOW_MS = 30 * 60 * 1000L
        /** Интервал проверки смены дня (мс) */
        private const val DATE_TICK_INTERVAL_MS = 60_000L
    }

    override fun getCurrentDateFlow(): Flow<String> = flow {
        while (true) {
            emit(LocalDate.now().format(dateFormatter))
            delay(DATE_TICK_INTERVAL_MS)
        }
    }.distinctUntilChanged()

    override fun getTodayEntriesFlow(): Flow<List<DayEntry>> {
        return getEntriesForDateFlow(today())
    }

    override fun getEntriesForDateFlow(date: String): Flow<List<DayEntry>> {
        return dayEntryDao.getEntriesForDateFlow(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntriesForDate(date: String): List<DayEntry> {
        return dayEntryDao.getEntriesForDate(date).map { it.toDomain() }
    }

    override suspend fun getTodayEntries(): List<DayEntry> {
        return dayEntryDao.getEntriesForDate(today()).map { it.toDomain() }
    }

    override suspend fun addEntry(entry: FoodEntry) {
        val sessionId = determineMealSessionId()
        val entity = entry.toEntity(today(), sessionId)
        dayEntryDao.insert(entity)
    }

    override suspend fun addEntries(entries: List<FoodEntry>) {
        val date = today()
        val sessionId = determineMealSessionId()
        val entities = entries.map { it.toEntity(date, sessionId) }
        dayEntryDao.insertAll(entities)
    }

    override suspend fun addEntriesForDate(date: String, entries: List<FoodEntry>) {
        val sessionId = System.currentTimeMillis()
        val entities = entries.map { it.toEntity(date, sessionId) }
        dayEntryDao.insertAll(entities)
    }

    /**
     * Определяет mealSessionId для новой записи.
     * Если есть записи за последние 30 минут - использует их sessionId.
     * Иначе создаёт новый (текущий timestamp).
     */
    private suspend fun determineMealSessionId(): Long {
        val now = System.currentTimeMillis()
        val cutoff = now - MEAL_SESSION_WINDOW_MS

        // Находим записи за сегодня, отсортированные по времени (самые новые первые)
        val todayEntries = dayEntryDao.getEntriesForDate(today())
        
        // Ищем запись, добавленную в течение последних 30 минут
        val recentEntry = todayEntries.firstOrNull { it.createdAt >= cutoff }

        return recentEntry?.mealSessionId ?: now
    }

    override suspend fun deleteEntry(id: Long) {
        dayEntryDao.deleteById(id)
    }

    override suspend fun deleteByName(name: String) {
        dayEntryDao.deleteByNameAndDate(today(), name)
    }

    override suspend fun deleteByMealSessionId(sessionId: Long) {
        dayEntryDao.deleteByMealSessionId(today(), sessionId)
    }

    override suspend fun clearToday() {
        dayEntryDao.clearDay(today())
    }

    override suspend fun clearDay(date: String): Int {
        return dayEntryDao.clearDay(date)
    }

    override suspend fun getTodayTotals(): DayTotals {
        val date = today()
        return getDayTotals(date)
    }

    override suspend fun getDayTotals(date: String): DayTotals {
        return DayTotals(
            kcal = dayEntryDao.getTotalKcalForDate(date),
            protein = dayEntryDao.getTotalProteinForDate(date),
            fat = dayEntryDao.getTotalFatForDate(date),
            carbs = dayEntryDao.getTotalCarbsForDate(date)
        )
    }

    override suspend fun getEntryById(id: Long): DayEntry? {
        return dayEntryDao.getById(id)?.toDomain()
    }

    override suspend fun repeatEntry(id: Long): DayEntry? {
        val original = dayEntryDao.getById(id) ?: return null
        val sessionId = determineMealSessionId()

        // Создаём копию с новым ID и текущим временем
        val copy = original.copy(
            id = 0, // Room сгенерирует новый ID
            createdAt = System.currentTimeMillis(),
            mealSessionId = sessionId
        )

        val newId = dayEntryDao.insert(copy)
        return dayEntryDao.getById(newId)?.toDomain()
    }

    override suspend fun updateEntryWeight(id: Long, newWeightGrams: Int): DayEntry? {
        val original = dayEntryDao.getById(id) ?: return null

        // Пропорционально пересчитываем КБЖУ
        val ratio = newWeightGrams.toFloat() / original.weightGrams.toFloat()
        val updated = original.copy(
            weightGrams = newWeightGrams,
            kcal = (original.kcal * ratio).toInt(),
            protein = original.protein * ratio,
            fat = original.fat * ratio,
            carbs = original.carbs * ratio
        )

        dayEntryDao.update(updated)
        return updated.toDomain()
    }

    /**
     * Возвращает сегодняшнюю дату в формате "yyyy-MM-dd".
     */
    private fun today(): String = LocalDate.now().format(dateFormatter)

    /**
     * Конвертирует FoodEntry (domain) в Entity.
     * @param date дата записи
     * @param mealSessionId ID сессии приёма пищи
     */
    private fun FoodEntry.toEntity(date: String, mealSessionId: Long) = DayEntryEntity(
        date = date,
        name = name,
        weightGrams = weightGrams,
        kcal = kcal,
        protein = protein,
        fat = fat,
        carbs = carbs,
        mealType = MealType.SNACK,
        emoji = emoji,
        mealSessionId = mealSessionId
    )

    /**
     * Конвертирует Entity в Domain модель.
     */
    private fun DayEntryEntity.toDomain() = DayEntry(
        id = id,
        date = date,
        name = name,
        weightGrams = weightGrams,
        kcal = kcal,
        protein = protein,
        fat = fat,
        carbs = carbs,
        mealType = mealType.label,
        createdAt = createdAt,
        emoji = emoji,
        mealSessionId = mealSessionId
    )

    override fun getAllDatesFlow(): Flow<List<String>> = dayEntryDao.getAllDatesFlow()

    override fun getKcalPerDayFlow(startDate: String, endDate: String): Flow<List<DayKcalPoint>> =
        dayEntryDao.getKcalPerDayFlow(startDate, endDate).map { rows ->
            rows.map { DayKcalPoint(it.date, it.kcalSum) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getKcalHistoryLast7DaysFlow(): Flow<List<DayKcalPoint>> =
        getCurrentDateFlow().flatMapLatest { endDate ->
            val end = LocalDate.parse(endDate, dateFormatter)
            val start = end.minusDays(6)
            val startDate = start.format(dateFormatter)
            dayEntryDao.getKcalPerDayFlow(startDate, endDate).map { rows ->
                rows.map { DayKcalPoint(it.date, it.kcalSum) }
            }
        }

    override fun getGlobalMaxDailyKcalFlow(): Flow<Int> =
        dayEntryDao.getGlobalMaxDailyKcalFlow().map { list -> list.firstOrNull() ?: 0 }
}
