package com.example.vitanlyapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Клиент к DeepSeek Chat API.
 * Вызов API напрямую (временно; позже — через ChatAIAdapter + ChatRepository).
 */
class DeepSeekApiClient(
    private val apiKey: String
) {

    companion object {
        private const val BASE_URL = "https://api.deepseek.com"
        private const val MODEL = "deepseek-chat"
    }

    suspend fun sendMessage(userMessage: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("DEEPSEEK_API_KEY не задан. Добавьте в local.properties: DEEPSEEK_API_KEY=sk-..."))
        }
        try {
            val url = URL("$BASE_URL/v1/chat/completions")
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
            }.toString()

            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 60_000
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                when (responseCode) {
                    in 200..299 -> {
                        val response = inputStream.bufferedReader(Charsets.UTF_8).readText()
                        val json = JSONObject(response)
                        val content = json
                            .optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("message")
                            ?.optString("content")
                            ?: ""
                        Result.success(content.ifBlank { "(пустой ответ)" })
                    }
                    else -> Result.failure(
                        Exception("DeepSeek API: $responseCode ${responseMessage ?: ""}")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
