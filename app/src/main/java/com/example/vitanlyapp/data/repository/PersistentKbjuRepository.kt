package com.example.vitanlyapp.data.repository

import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.KBJUData
import com.example.vitanlyapp.domain.model.UserGoal
import com.example.vitanlyapp.domain.repository.DayEntryRepository
import com.example.vitanlyapp.domain.repository.KbjuRepository
import com.example.vitanlyapp.domain.repository.UserProfile
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Реализация KbjuRepository, которая агрегирует данные из:
 * - DayEntryRepository (текущие значения КБЖУ за день)
 * - UserProfileRepository (нормы КБЖУ по формуле Mifflin-St Jeor)
 *
 * Автоматически обновляет данные при изменениях в репозиториях.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PersistentKbjuRepository @Inject constructor(
    private val dayEntryRepository: DayEntryRepository,
    private val userProfileRepository: UserProfileRepository
) : KbjuRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _data = MutableStateFlow(KBJUData.default())

    init {
        // Подписываемся на изменения в обоих репозиториях
        scope.launch {
            combine(
                dayEntryRepository.getCurrentDateFlow()
                    .flatMapLatest { date -> dayEntryRepository.getEntriesForDateFlow(date) },
                userProfileRepository.getProfileFlow()
            ) { entries, profile ->
                // Суммируем текущие значения из записей за день
                val currentKcal = entries.sumOf { it.kcal }
                val currentProtein = entries.sumOf { it.protein.toDouble() }.toFloat()
                val currentFat = entries.sumOf { it.fat.toDouble() }.toFloat()
                val currentCarbs = entries.sumOf { it.carbs.toDouble() }.toFloat()

                // Рассчитываем нормы из профиля (или используем дефолтные)
                val norms = profile?.let { calculateNorms(it) } ?: DailyNorms.default()

                KBJUData(
                    currentCalories = currentKcal,
                    currentProtein = currentProtein.roundToInt(),
                    currentFat = currentFat.roundToInt(),
                    currentCarbs = currentCarbs.roundToInt(),
                    maxCalories = norms.kcal,
                    maxProtein = norms.protein,
                    maxFat = norms.fat,
                    maxCarbs = norms.carbs
                )
            }.collect { data ->
                _data.value = data
            }
        }
    }

    override fun getKbju(): StateFlow<KBJUData> = _data.asStateFlow()

    override fun getKbjuForDateFlow(date: String): Flow<KBJUData> =
        combine(
            dayEntryRepository.getEntriesForDateFlow(date),
            userProfileRepository.getProfileFlow()
        ) { entries, profile ->
            val currentKcal = entries.sumOf { it.kcal }
            val currentProtein = entries.sumOf { it.protein.toDouble() }.toFloat()
            val currentFat = entries.sumOf { it.fat.toDouble() }.toFloat()
            val currentCarbs = entries.sumOf { it.carbs.toDouble() }.toFloat()
            val norms = profile?.let { calculateNorms(it) } ?: DailyNorms.default()
            KBJUData(
                currentCalories = currentKcal,
                currentProtein = currentProtein.roundToInt(),
                currentFat = currentFat.roundToInt(),
                currentCarbs = currentCarbs.roundToInt(),
                maxCalories = norms.kcal,
                maxProtein = norms.protein,
                maxFat = norms.fat,
                maxCarbs = norms.carbs
            )
        }

    override suspend fun updateKbju(data: KBJUData) {
        // В persistent-версии обновление происходит автоматически
        // через изменения в DayEntryRepository и UserProfileRepository
        // Этот метод оставлен для совместимости с интерфейсом
        _data.value = data
    }

    /**
     * Рассчитывает дневные нормы КБЖУ по формуле Mifflin-St Jeor.
     *
     * BMR (базовый метаболизм):
     * - Мужчины: 10 × вес(кг) + 6.25 × рост(см) − 5 × возраст(лет) + 5
     * - Женщины: 10 × вес(кг) + 6.25 × рост(см) − 5 × возраст(лет) − 161
     *
     * TDEE = BMR × коэффициент активности
     *
     * Калории с учётом цели:
     * - Похудение: TDEE - (темп × 7700 / 7)
     * - Набор: TDEE + (темп × 7700 / 7)
     * - Поддержание: TDEE
     */
    private fun calculateNorms(profile: UserProfile): DailyNorms {
        // Базовый метаболизм (BMR) по Mifflin-St Jeor
        val bmr = when (profile.gender) {
            Gender.MALE -> {
                10 * profile.weight + 6.25 * profile.height - 5 * profile.age + 5
            }
            Gender.FEMALE -> {
                10 * profile.weight + 6.25 * profile.height - 5 * profile.age - 161
            }
        }

        // TDEE = BMR × коэффициент активности
        val tdee = bmr * profile.activityLevel.coefficient

        // Корректировка по цели (7700 ккал ≈ 1 кг жира)
        val dailyCalorieAdjustment = (profile.tempo * 7700 / 7).roundToInt()
        val targetCalories = when (profile.goal) {
            UserGoal.LOSE -> tdee - dailyCalorieAdjustment
            UserGoal.GAIN -> tdee + dailyCalorieAdjustment
            UserGoal.MAINTAIN -> tdee
        }

        val kcal = targetCalories.roundToInt().coerceAtLeast(1200)

        // Распределение макронутриентов
        // Белок: 1.8-2.2 г/кг (используем 2.0 г/кг для простоты)
        // Жиры: 0.8-1.0 г/кг (используем 1.0 г/кг)
        // Углеводы: остаток калорий
        val protein = (profile.weight * 2.0).roundToInt()
        val fat = (profile.weight * 1.0).roundToInt()

        // Калории от белка и жира
        val proteinCalories = protein * 4
        val fatCalories = fat * 9

        // Остаток на углеводы (1 г углеводов = 4 ккал)
        val carbsCalories = (kcal - proteinCalories - fatCalories).coerceAtLeast(0)
        val carbs = (carbsCalories / 4)

        return DailyNorms(kcal, protein, fat, carbs)
    }

    private data class DailyNorms(
        val kcal: Int,
        val protein: Int,
        val fat: Int,
        val carbs: Int
    ) {
        companion object {
            fun default() = DailyNorms(
                kcal = 2000,
                protein = 100,
                fat = 70,
                carbs = 250
            )
        }
    }
}
