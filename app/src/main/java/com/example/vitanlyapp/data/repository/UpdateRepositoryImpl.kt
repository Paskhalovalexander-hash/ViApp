package com.example.vitanlyapp.data.repository

import android.content.Context
import android.os.Environment
import com.example.vitanlyapp.data.remote.GitHubApiClient
import com.example.vitanlyapp.data.remote.GitHubRelease
import com.example.vitanlyapp.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация UpdateRepository.
 * Работает с GitHub API для проверки обновлений и скачивания APK.
 */
@Singleton
class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubApiClient: GitHubApiClient
) : UpdateRepository {

    companion object {
        // GitHub username и название репозитория для проверки обновлений
        const val GITHUB_OWNER = "Paskhalovalexander-hash"
        const val GITHUB_REPO = "ViApp"
        private const val APK_FILENAME = "VitanlyApp-update.apk"
    }

    override suspend fun getLatestRelease(): Result<GitHubRelease> {
        return gitHubApiClient.getLatestRelease(GITHUB_OWNER, GITHUB_REPO)
    }

    override suspend fun downloadApk(
        url: String,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = URL(url)
            val connection = downloadUrl.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 60_000
                connect()
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("Ошибка скачивания: ${connection.responseCode}")
                )
            }

            val fileLength = connection.contentLength
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return@withContext Result.failure(
                    Exception("Недоступна папка Downloads")
                )

            val outputFile = File(downloadDir, APK_FILENAME)
            if (outputFile.exists()) {
                outputFile.delete()
            }

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytes = 0L
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        if (fileLength > 0) {
                            val progress = totalBytes.toFloat() / fileLength
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
