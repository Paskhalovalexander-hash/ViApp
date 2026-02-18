package com.example.vitanlyapp.ui.update

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitanlyapp.data.agent.UpdateAdapter
import com.example.vitanlyapp.data.agent.UpdateCheckResult
import com.example.vitanlyapp.data.remote.GitHubRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для управления состоянием обновлений.
 *
 * Обязанности:
 * - Хранит UI состояние (StateFlow)
 * - Конвертирует результаты от UpdateAdapter в UI состояние
 * - Не содержит бизнес-логику
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateAdapter: UpdateAdapter
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    /**
     * Проверяет наличие обновлений.
     * Вызывается при запуске приложения.
     */
    fun checkForUpdates() {
        if (_updateState.value is UpdateUiState.Checking) return

        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking

            when (val result = updateAdapter.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateState.value = UpdateUiState.UpdateAvailable(
                        release = result.release,
                        currentVersion = result.currentVersion,
                        newVersion = result.newVersion
                    )
                }
                is UpdateCheckResult.UpToDate -> {
                    _updateState.value = UpdateUiState.UpToDate(result.currentVersion)
                }
                is UpdateCheckResult.Error -> {
                    _updateState.value = UpdateUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Начинает скачивание обновления.
     */
    fun startDownload(downloadUrl: String) {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Downloading
            _downloadProgress.value = 0f

            val result = updateAdapter.downloadUpdate(downloadUrl) { progress ->
                _downloadProgress.value = progress
            }

            result.fold(
                onSuccess = { apkPath ->
                    _updateState.value = UpdateUiState.ReadyToInstall(apkPath)
                },
                onFailure = { error ->
                    _updateState.value = UpdateUiState.Error(
                        error.message ?: "Ошибка скачивания"
                    )
                }
            )
        }
    }

    /**
     * Создаёт Intent для установки APK.
     */
    fun getInstallIntent(apkPath: String): Intent? {
        return updateAdapter.createInstallIntent(apkPath).getOrNull()
    }

    /**
     * Проверяет разрешение на установку из неизвестных источников.
     */
    fun canInstallFromUnknownSources(): Boolean {
        return updateAdapter.canInstallFromUnknownSources()
    }

    /**
     * Создаёт Intent для открытия настроек разрешений.
     */
    fun getUnknownSourcesSettingsIntent(): Intent {
        return updateAdapter.createUnknownSourcesSettingsIntent()
    }

    /**
     * Закрывает диалог обновления.
     */
    fun dismissUpdate() {
        _updateState.value = UpdateUiState.Dismissed
    }

    /**
     * Сбрасывает состояние (для повторной проверки).
     */
    fun resetState() {
        _updateState.value = UpdateUiState.Idle
    }
}

/**
 * UI состояние обновлений.
 */
sealed class UpdateUiState {
    /** Начальное состояние */
    data object Idle : UpdateUiState()

    /** Проверка обновлений */
    data object Checking : UpdateUiState()

    /** Доступно обновление */
    data class UpdateAvailable(
        val release: GitHubRelease,
        val currentVersion: String,
        val newVersion: String
    ) : UpdateUiState()

    /** Приложение актуально */
    data class UpToDate(val currentVersion: String) : UpdateUiState()

    /** Скачивание */
    data object Downloading : UpdateUiState()

    /** Готово к установке */
    data class ReadyToInstall(val apkPath: String) : UpdateUiState()

    /** Ошибка */
    data class Error(val message: String) : UpdateUiState()

    /** Пользователь отклонил обновление */
    data object Dismissed : UpdateUiState()
}
