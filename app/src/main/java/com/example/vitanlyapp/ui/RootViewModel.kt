package com.example.vitanlyapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitanlyapp.data.StartupPreferences
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Состояние запуска приложения.
 */
sealed class StartupState {
    data object Loading : StartupState()
    data object NeedsOnboarding : StartupState()
    data object Ready : StartupState()
}

/**
 * ViewModel для определения начального экрана приложения.
 * Проверяет наличие профиля пользователя при запуске.
 * 
 * Использует StartupPreferences для быстрой проверки без обращения к Room.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val startupPreferences: StartupPreferences
) : ViewModel() {

    private val _startupState = MutableStateFlow<StartupState>(StartupState.Loading)
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    init {
        checkProfileExists()
    }

    private fun checkProfileExists() {
        // Быстрый путь: проверяем кеш в SharedPreferences
        if (startupPreferences.hasProfile()) {
            _startupState.value = StartupState.Ready
            return
        }
        
        // Медленный путь: проверяем в Room (первый запуск или после очистки данных)
        viewModelScope.launch {
            val profile = userProfileRepository.getProfile()
            if (profile != null) {
                // Профиль есть, но флаг не установлен — восстанавливаем
                startupPreferences.setHasProfile(true)
                _startupState.value = StartupState.Ready
            } else {
                _startupState.value = StartupState.NeedsOnboarding
            }
        }
    }

    /**
     * Вызывается после завершения онбординга для перехода на главный экран.
     */
    fun onOnboardingComplete() {
        startupPreferences.setHasProfile(true)
        _startupState.value = StartupState.Ready
    }

    /**
     * Вызывается после сброса данных для перехода на онбординг.
     */
    fun onDataReset() {
        startupPreferences.setHasProfile(false)
        _startupState.value = StartupState.NeedsOnboarding
    }
}
