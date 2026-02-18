package com.example.vitanlyapp.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.UserGoal
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

/**
 * Шаги онбординга.
 */
enum class OnboardingStep {
    BIRTH_YEAR,
    GENDER,
    HEIGHT,
    WEIGHT,
    ACTIVITY,
    GOAL
}

/**
 * Состояние онбординга.
 */
data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.BIRTH_YEAR,
    val birthYear: Int = 1990,
    val gender: Gender = Gender.MALE,
    val height: Int = 170,
    val weight: Float = 70f,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: UserGoal = UserGoal.MAINTAIN,
    val isSaving: Boolean = false
) {
    val stepIndex: Int get() = currentStep.ordinal
    val totalSteps: Int get() = OnboardingStep.entries.size
    val isFirstStep: Boolean get() = currentStep == OnboardingStep.BIRTH_YEAR
    val isLastStep: Boolean get() = currentStep == OnboardingStep.GOAL

    /**
     * Рассчитывает возраст из года рождения.
     */
    val age: Int get() = Period.between(
        LocalDate.of(birthYear, 1, 1),
        LocalDate.now()
    ).years
}

/**
 * ViewModel для экрана онбординга.
 * Управляет пошаговым вводом данных профиля пользователя.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    /**
     * Переход к следующему шагу.
     */
    fun nextStep() {
        _state.update { current ->
            val nextOrdinal = current.currentStep.ordinal + 1
            if (nextOrdinal < OnboardingStep.entries.size) {
                current.copy(currentStep = OnboardingStep.entries[nextOrdinal])
            } else {
                current
            }
        }
    }

    /**
     * Переход к предыдущему шагу.
     */
    fun previousStep() {
        _state.update { current ->
            val prevOrdinal = current.currentStep.ordinal - 1
            if (prevOrdinal >= 0) {
                current.copy(currentStep = OnboardingStep.entries[prevOrdinal])
            } else {
                current
            }
        }
    }

    /**
     * Переход к конкретному шагу по индексу.
     */
    fun goToStep(index: Int) {
        if (index in 0 until OnboardingStep.entries.size) {
            _state.update { it.copy(currentStep = OnboardingStep.entries[index]) }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Обновление значений
    // ══════════════════════════════════════════════════════════════════════════

    fun updateBirthYear(year: Int) {
        _state.update { it.copy(birthYear = year.coerceIn(1920, LocalDate.now().year - 10)) }
    }

    fun updateGender(gender: Gender) {
        _state.update { it.copy(gender = gender) }
    }

    fun updateHeight(height: Int) {
        _state.update { it.copy(height = height.coerceIn(100, 250)) }
    }

    fun updateWeight(weight: Float) {
        _state.update { it.copy(weight = weight.coerceIn(30f, 300f)) }
    }

    fun updateActivityLevel(level: ActivityLevel) {
        _state.update { it.copy(activityLevel = level) }
    }

    fun updateGoal(goal: UserGoal) {
        _state.update { it.copy(goal = goal) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Сохранение профиля
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Сохраняет профиль пользователя и завершает онбординг.
     * @param onComplete вызывается после успешного сохранения
     */
    fun saveProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val currentState = _state.value
            
            // Обновляем все параметры профиля
            userProfileRepository.updateAge(currentState.age)
            userProfileRepository.updateGender(currentState.gender)
            userProfileRepository.updateHeight(currentState.height)
            userProfileRepository.updateWeight(currentState.weight)
            userProfileRepository.updateActivityLevel(currentState.activityLevel)
            userProfileRepository.updateGoal(currentState.goal)
            userProfileRepository.updateTargetWeight(currentState.weight) // По умолчанию целевой = текущий

            _state.update { it.copy(isSaving = false) }
            onComplete()
        }
    }
}
