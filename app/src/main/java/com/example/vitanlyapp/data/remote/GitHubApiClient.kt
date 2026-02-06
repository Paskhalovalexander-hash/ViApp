package com.example.vitanlyapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Клиент для GitHub Releases API.
 * Получает информацию о последнем релизе для проверки обновлений.
 */
class GitHubApiClient {

    companion object {
        private const val BASE_URL = "https://api.github.com"
    }

    /**
     * Получает информацию о последнем релизе репозитория.
     *
     * @param owner владелец репозитория (username или organization)
     * @param repo название репозитория
     * @return GitHubRelease или ошибка
     */
    suspend fun getLatestRelease(owner: String, repo: String): Result<GitHubRelease> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/repos/$owner/$repo/releases/latest")

                (url.openConnection() as HttpURLConnection).run {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    connectTimeout = 15_000
                    readTimeout = 15_000

                    when (responseCode) {
                        200 -> {
                            val response = inputStream.bufferedReader(Charsets.UTF_8).readText()
                            val release = parseRelease(JSONObject(response))
                            Result.success(release)
                        }
                        404 -> Result.failure(
                            NoReleasesException("Релизы не найдены для $owner/$repo")
                        )
                        else -> Result.failure(
                            Exception("GitHub API: $responseCode ${responseMessage ?: ""}")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseRelease(json: JSONObject): GitHubRelease {
        val tagName = json.optString("tag_name", "")
        val name = json.optString("name", tagName)
        val body = json.optString("body", "")
        val htmlUrl = json.optString("html_url", "")
        val publishedAt = json.optString("published_at", "")

        // Ищем APK в assets
        var apkDownloadUrl: String? = null
        var apkSize: Long = 0
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk")) {
                    apkDownloadUrl = asset.optString("browser_download_url", null)
                    apkSize = asset.optLong("size", 0)
                    break
                }
            }
        }

        return GitHubRelease(
            tagName = tagName,
            name = name,
            body = body,
            htmlUrl = htmlUrl,
            publishedAt = publishedAt,
            apkDownloadUrl = apkDownloadUrl,
            apkSize = apkSize
        )
    }
}

/**
 * Данные о GitHub релизе.
 */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String?,
    val apkSize: Long
) {
    /**
     * Извлекает версию из tag_name.
     * Поддерживает форматы: "v1.2.3", "v.1.2.3", "1.2.3", "v0.05"
     */
    fun getVersionName(): String {
        return tagName
            .removePrefix("v")
            .removePrefix(".")
            .trim()
    }

    /**
     * Извлекает versionCode из tag_name.
     * Поддерживает форматы:
     * - "v1.2.3+45" → 45 (явный versionCode после +)
     * - "v0.05" → 5 (semver: 0*10000 + 5*100 = 500, но для простых версий берём как есть)
     * - "v.0.05" → 5
     */
    fun getVersionCode(): Int {
        // Формат v1.2.3+45 (45 - явный versionCode)
        val plusIndex = tagName.indexOf('+')
        if (plusIndex != -1) {
            tagName.substring(plusIndex + 1).toIntOrNull()?.let { return it }
        }

        // Извлекаем числовые части версии
        val version = getVersionName()
        val parts = version.split(".").mapNotNull { it.toIntOrNull() }
        
        return when (parts.size) {
            3 -> parts[0] * 10000 + parts[1] * 100 + parts[2]
            2 -> parts[0] * 100 + parts[1]  // 0.05 → 5
            1 -> parts[0]
            else -> 0
        }
    }
}

class NoReleasesException(message: String) : Exception(message)
