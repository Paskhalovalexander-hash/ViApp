package com.example.vitanlyapp.data.agent

import com.example.vitanlyapp.domain.model.AgentResponse
import com.example.vitanlyapp.domain.model.FoodEntry
import com.example.vitanlyapp.domain.repository.DayEntryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Адаптер для обработки распознанных продуктов из ответа AI.
 *
 * Отвечает за:
 * - Извлечение food_entries из AgentResponse
 * - Валидацию данных о еде
 * - Добавление записей через DayEntryRepository
 *
 * @see AgentResponse
 * @see FoodEntry
 */
@Singleton
class FoodParsingAdapter @Inject constructor(
    private val dayEntryRepository: DayEntryRepository
) {

    /**
     * Обрабатывает food_entries из ответа AI.
     * Добавляет все валидные записи в репозиторий.
     *
     * @param response ответ от AI с распознанными продуктами
     * @return результат с количеством добавленных записей
     */
    suspend fun processFoodEntries(response: AgentResponse): FoodProcessingResult {
        val entries = response.foodEntries

        if (entries.isEmpty()) {
            return FoodProcessingResult.Empty
        }

        val validEntries = entries.filter { isValidEntry(it) }
        val invalidCount = entries.size - validEntries.size

        if (validEntries.isEmpty()) {
            return FoodProcessingResult.AllInvalid(invalidCount)
        }

        return try {
            dayEntryRepository.addEntries(validEntries)
            FoodProcessingResult.Success(
                addedCount = validEntries.size,
                invalidCount = invalidCount,
                totalKcal = validEntries.sumOf { it.kcal },
                entries = validEntries
            )
        } catch (e: Exception) {
            FoodProcessingResult.Error(e.message ?: "Ошибка при добавлении записей")
        }
    }

    /**
     * Добавляет одну запись о еде напрямую.
     *
     * @param entry запись о еде
     * @return true если запись добавлена успешно
     */
    suspend fun addSingleEntry(entry: FoodEntry): Boolean {
        if (!isValidEntry(entry)) return false

        return try {
            dayEntryRepository.addEntry(entry)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверяет валидность записи о еде.
     * Запись валидна если:
     * - Название не пустое
     * - Вес > 0
     * - Калории >= 0
     * - БЖУ >= 0
     */
    private fun isValidEntry(entry: FoodEntry): Boolean {
        return entry.name.isNotBlank() &&
                entry.weightGrams > 0 &&
                entry.kcal >= 0 &&
                entry.protein >= 0f &&
                entry.fat >= 0f &&
                entry.carbs >= 0f
    }

    /**
     * Вычисляет общую статистику КБЖУ для списка записей.
     */
    fun calculateTotals(entries: List<FoodEntry>): FoodTotals {
        return FoodTotals(
            kcal = entries.sumOf { it.kcal },
            protein = entries.sumOf { it.protein.toDouble() }.toFloat(),
            fat = entries.sumOf { it.fat.toDouble() }.toFloat(),
            carbs = entries.sumOf { it.carbs.toDouble() }.toFloat()
        )
    }
}

/**
 * Результат обработки записей о еде.
 */
sealed class FoodProcessingResult {
    /** Нет записей для обработки */
    data object Empty : FoodProcessingResult()

    /** Все записи невалидны */
    data class AllInvalid(val count: Int) : FoodProcessingResult()

    /** Успешно добавлены записи */
    data class Success(
        val addedCount: Int,
        val invalidCount: Int,
        val totalKcal: Int,
        val entries: List<FoodEntry>
    ) : FoodProcessingResult()

    /** Ошибка при добавлении */
    data class Error(val message: String) : FoodProcessingResult()
}

/**
 * Суммарные нутриенты.
 */
data class FoodTotals(
    val kcal: Int,
    val protein: Float,
    val fat: Float,
    val carbs: Float
)
