package com.example.vitanlyapp.domain.repository

import com.example.vitanlyapp.data.remote.GitHubRelease

/**
 * Репозиторий для проверки и загрузки обновлений приложения.
 */
interface UpdateRepository {

    /**
     * Получает информацию о последнем релизе из GitHub.
     *
     * @return GitHubRelease или ошибка
     */
    suspend fun getLatestRelease(): Result<GitHubRelease>

    /**
     * Скачивает APK файл.
     *
     * @param url URL для скачивания APK
     * @param onProgress коллбэк прогресса (0.0 - 1.0)
     * @return путь к скачанному файлу или ошибка
     */
    suspend fun downloadApk(
        url: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String>
}
