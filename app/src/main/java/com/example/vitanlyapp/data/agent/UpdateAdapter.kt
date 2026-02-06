package com.example.vitanlyapp.data.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.vitanlyapp.data.remote.GitHubRelease
import com.example.vitanlyapp.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Адаптер для проверки и установки обновлений приложения.
 *
 * Обязанности:
 * - Сравнение версий приложения
 * - Определение необходимости обновления
 * - Инициализация установки APK
 *
 * НЕ делает:
 * - Не собирает Flow
 * - Не хранит UI состояние
 * - Не работает напрямую с сетью (использует Repository)
 */
@Singleton
class UpdateAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateRepository: UpdateRepository
) {

    /**
     * Проверяет наличие обновления.
     *
     * @return UpdateCheckResult с информацией о доступном обновлении
     */
    suspend fun checkForUpdate(): UpdateCheckResult {
        val currentVersionCode = getCurrentVersionCode()
        val currentVersionName = getCurrentVersionName()

        return updateRepository.getLatestRelease().fold(
            onSuccess = { release ->
                val remoteVersionCode = release.getVersionCode()

                if (remoteVersionCode > currentVersionCode) {
                    UpdateCheckResult.UpdateAvailable(
                        release = release,
                        currentVersion = currentVersionName,
                        newVersion = release.getVersionName()
                    )
                } else {
                    UpdateCheckResult.UpToDate(currentVersion = currentVersionName)
                }
            },
            onFailure = { error ->
                UpdateCheckResult.Error(
                    message = error.message ?: "Ошибка проверки обновлений",
                    exception = error
                )
            }
        )
    }

    /**
     * Скачивает APK обновления.
     *
     * @param downloadUrl URL для скачивания
     * @param onProgress коллбэк прогресса (0.0 - 1.0)
     * @return путь к скачанному файлу или ошибка
     */
    suspend fun downloadUpdate(
        downloadUrl: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return updateRepository.downloadApk(downloadUrl, onProgress)
    }

    /**
     * Запускает установку скачанного APK.
     *
     * @param apkPath путь к APK файлу
     * @return Intent для установки или ошибка
     */
    fun createInstallIntent(apkPath: String): Result<Intent> {
        return try {
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                return Result.failure(Exception("APK файл не найден"))
            }

            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            Result.success(intent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Проверяет, может ли приложение устанавливать APK из неизвестных источников.
     */
    fun canInstallFromUnknownSources(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Создаёт Intent для открытия настроек установки из неизвестных источников.
     */
    fun createUnknownSourcesSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}

/**
 * Результат проверки обновлений.
 */
sealed class UpdateCheckResult {
    
    /** Доступно обновление */
    data class UpdateAvailable(
        val release: GitHubRelease,
        val currentVersion: String,
        val newVersion: String
    ) : UpdateCheckResult()

    /** Приложение актуально */
    data class UpToDate(
        val currentVersion: String
    ) : UpdateCheckResult()

    /** Ошибка проверки */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : UpdateCheckResult()
}
