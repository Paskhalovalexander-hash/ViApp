package com.example.vitanlyapp.data.agent

import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.AgentCommand
import com.example.vitanlyapp.domain.model.AgentResponse
import com.example.vitanlyapp.domain.model.FoodEntry
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.UserGoal
import com.example.vitanlyapp.domain.repository.ChatMessageDomain
import com.example.vitanlyapp.domain.repository.ChatRepository
import com.example.vitanlyapp.domain.repository.UserProfile
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Адаптер для взаимодействия с DeepSeek Chat API.
 *
 * Отвечает за:
 * - Формирование системного промпта с инструкциями
 * - Отправку сообщений с контекстом истории чата
 * - Парсинг JSON-ответа в AgentResponse
 * - Сохранение сообщений в ChatRepository
 *
 * @see AgentResponse
 * @see AI_CONTRACT.md
 */
@Singleton
class ChatAIAdapter @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userProfileRepository: UserProfileRepository,
    private val apiKey: String
) {

    companion object {
        private const val BASE_URL = "https://api.deepseek.com"
        private const val MODEL = "deepseek-chat"
        private const val CONTEXT_MESSAGES_LIMIT = 10
        private const val CONNECT_TIMEOUT = 30_000
        private const val READ_TIMEOUT = 60_000
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Отправляет сообщение пользователя и получает структурированный ответ.
     *
     * @param userMessage текст сообщения пользователя
     * @param saveToHistory сохранять ли сообщения в историю чата
     * @return Result с AgentResponse или ошибкой
     */
    suspend fun sendMessage(
        userMessage: String,
        saveToHistory: Boolean = true
    ): Result<AgentResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("DEEPSEEK_API_KEY не задан. Добавьте в local.properties.")
            )
        }

        // Сохраняем сообщение пользователя
        if (saveToHistory) {
            chatRepository.addUserMessage(userMessage)
        }

        var lastException: Exception? = null
        
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                // Получаем контекст
                val profile = userProfileRepository.getProfile()
                val historyMessages = chatRepository.getLastMessages(CONTEXT_MESSAGES_LIMIT)

                // Формируем запрос
                val requestBody = buildRequestBody(
                    userMessage = userMessage,
                    profile = profile,
                    historyMessages = historyMessages
                )

                // Отправляем запрос
                val rawResponse = executeApiRequest(requestBody)

                // Парсим ответ
                val agentResponse = parseAgentResponse(rawResponse)

                // Сохраняем ответ ассистента
                if (saveToHistory) {
                    chatRepository.addAssistantMessage(agentResponse.responseText)
                }

                return@withContext Result.success(agentResponse)
            } catch (e: Exception) {
                lastException = e
                // Если ошибка retryable и есть ещё попытки — повторяем с задержкой
                if (isRetryableParsingError(e) && attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS)
                } else {
                    // Не retryable или последняя попытка — выходим из цикла
                    return@repeat
                }
            }
        }

        // Все попытки исчерпаны или ошибка не retryable
        val errorResponse = AgentResponse.error(lastException?.message ?: "Неизвестная ошибка")
        if (saveToHistory) {
            chatRepository.addAssistantMessage(errorResponse.responseText)
        }
        Result.failure(lastException ?: Exception("Неизвестная ошибка"))
    }

    /**
     * Формирует тело запроса к API.
     */
    private fun buildRequestBody(
        userMessage: String,
        profile: UserProfile?,
        historyMessages: List<ChatMessageDomain>
    ): String {
        val messages = JSONArray()

        // Системный промпт
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", buildSystemPrompt(profile))
        })

        // История чата (без последнего сообщения пользователя, т.к. оно добавляется отдельно)
        for (msg in historyMessages.dropLast(1)) {
            messages.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "assistant")
                put("content", msg.text)
            })
        }

        // Текущее сообщение пользователя
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        return JSONObject().apply {
            put("model", MODEL)
            put("messages", messages)
            put("temperature", 0.7)
            put("response_format", JSONObject().apply {
                put("type", "json_object")
            })
        }.toString()
    }

    /**
     * Формирует системный промпт с инструкциями для AI.
     */
    private fun buildSystemPrompt(profile: UserProfile?): String {
        val profileInfo = profile?.let {
            """
            |Текущий профиль пользователя:
            |- Вес: ${it.weight} кг
            |- Рост: ${it.height} см
            |- Возраст: ${it.age} лет
            |- Пол: ${it.gender.label}
            |- Активность: ${it.activityLevel.label}
            |- Цель: ${it.goal.label}
            |- Целевой вес: ${it.targetWeight} кг
            |- Темп: ${it.tempo} кг/неделя
            """.trimMargin()
        } ?: "Профиль пользователя не задан."

        return """
            |Ты — AI-помощник приложения VitanlyApp для подсчёта калорий и КБЖУ.
            |
            |$profileInfo
            |
            |ФОРМАТ ОТВЕТА — ВСЕГДА JSON:
            |{
            |  "response_text": "Текст ответа для пользователя",
            |  "food_entries": [
            |    {"name": "Название", "weight_g": 100, "kcal": 150, "protein": 10.0, "fat": 5.0, "carbs": 15.0, "emoji": "🍽️"}
            |  ],
            |  "commands": [
            |    {"type": "command_type", "value": "..."}
            |  ]
            |}
            |
            |КОМАНДЫ:
            |Профиль: set_weight (кг), set_height (см), set_age (лет), set_gender (male/female), set_activity (sedentary/light/moderate/active/very_active)
            |Цели: set_goal (lose/gain/maintain), set_target_weight (кг), set_tempo (кг/неделя)
            |Еда: add_food (добавить из food_entries), delete_food (name: название), delete_meal (session_id: ID приёма пищи), clear_day (очистить день)
            |
            |ПРАВИЛА:
            |1. response_text — ОБЯЗАТЕЛЕН, это текст для чата
            |2. food_entries — массив продуктов (может быть пустым)
            |3. commands — массив команд (может быть пустым)
            |4. Если пользователь описал еду — добавь в food_entries + команду add_food
            |5. Если пользователь меняет параметры — добавь соответствующие команды
            |6. Для обычных вопросов — только response_text
            |7. БЖУ указывай на основе знаний о нутриентах продуктов
            |8. Для каждого продукта добавь подходящий emoji (🍳 яичница, 🥗 салат, 🍕 пицца и т.д.)
            |9. Отвечай дружелюбно, кратко и по делу
            |10. Если пользователь говорит на русском — отвечай на русском
        """.trimMargin()
    }

    /**
     * Выполняет HTTP-запрос к API.
     */
    private fun executeApiRequest(requestBody: String): String {
        val url = URL("$BASE_URL/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection

        return connection.run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT

            outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            when (responseCode) {
                in 200..299 -> {
                    val response = inputStream.bufferedReader(Charsets.UTF_8).readText()
                    val json = JSONObject(response)
                    json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        ?: throw Exception("Пустой ответ от API")
                }
                401 -> throw Exception("Неверный API ключ")
                429 -> throw Exception("Превышен лимит запросов")
                else -> {
                    val errorBody = errorStream?.bufferedReader()?.readText() ?: ""
                    throw Exception("API ошибка: $responseCode $responseMessage\n$errorBody")
                }
            }
        }
    }

    /**
     * Парсит JSON-ответ в AgentResponse.
     */
    private fun parseAgentResponse(rawJson: String): AgentResponse {
        return try {
            // Пробуем kotlinx.serialization
            json.decodeFromString<AgentResponse>(rawJson)
        } catch (e: Exception) {
            // Fallback на ручной парсинг
            parseManually(rawJson)
        }
    }

    /**
     * Ручной парсинг JSON (fallback).
     */
    private fun parseManually(rawJson: String): AgentResponse {
        val jsonObj = JSONObject(rawJson)

        val responseText = jsonObj.optString("response_text", "")
        if (responseText.isBlank()) {
            return AgentResponse.error("Ответ AI не содержит текста")
        }

        // Парсим food_entries
        val foodEntries = mutableListOf<FoodEntry>()
        jsonObj.optJSONArray("food_entries")?.let { array ->
            for (i in 0 until array.length()) {
                val entry = array.optJSONObject(i) ?: continue
                foodEntries.add(
                    FoodEntry(
                        name = entry.optString("name", ""),
                        weightGrams = entry.optInt("weight_g", 0),
                        kcal = entry.optInt("kcal", 0),
                        protein = entry.optDouble("protein", 0.0).toFloat(),
                        fat = entry.optDouble("fat", 0.0).toFloat(),
                        carbs = entry.optDouble("carbs", 0.0).toFloat(),
                        emoji = entry.optString("emoji", "🍽️")
                    )
                )
            }
        }

        // Парсим commands
        val commands = mutableListOf<AgentCommand>()
        jsonObj.optJSONArray("commands")?.let { array ->
            for (i in 0 until array.length()) {
                val cmd = array.optJSONObject(i) ?: continue
                parseCommand(cmd)?.let { commands.add(it) }
            }
        }

        return AgentResponse(
            responseText = responseText,
            foodEntries = foodEntries,
            commands = commands
        )
    }

    /**
     * Парсит одну команду из JSON.
     */
    private fun parseCommand(json: JSONObject): AgentCommand? {
        return when (json.optString("type")) {
            // Команды профиля
            "set_weight" -> AgentCommand.SetWeight(json.optDouble("value", 0.0).toFloat())
            "set_height" -> AgentCommand.SetHeight(json.optInt("value", 0))
            "set_age" -> AgentCommand.SetAge(json.optInt("value", 0))
            "set_gender" -> {
                val value = json.optString("value", "")
                Gender.fromString(value)?.let { AgentCommand.SetGender(it) }
            }
            "set_activity" -> {
                val value = json.optString("value", "")
                ActivityLevel.fromString(value)?.let { AgentCommand.SetActivity(it) }
            }

            // Команды целей
            "set_goal" -> {
                val value = json.optString("value", "")
                UserGoal.fromString(value)?.let { AgentCommand.SetGoal(it) }
            }
            "set_target_weight" -> AgentCommand.SetTargetWeight(json.optDouble("value", 0.0).toFloat())
            "set_tempo" -> AgentCommand.SetTempo(json.optDouble("value", 0.0).toFloat())

            // Команды еды
            "add_food" -> AgentCommand.AddFood
            "delete_food" -> AgentCommand.DeleteFood(json.optString("name", ""))
            "delete_meal" -> AgentCommand.DeleteMeal(json.optLong("session_id", 0L))
            "clear_day" -> AgentCommand.ClearDay

            else -> null
        }
    }

    /**
     * Проверяет, является ли ошибка retryable (ошибка парсинга JSON).
     * Такие ошибки возникают, когда API возвращает обрезанный/неполный JSON.
     */
    private fun isRetryableParsingError(e: Exception): Boolean {
        val message = e.message ?: return false
        return message.contains("end of input", ignoreCase = true) ||
               message.contains("Unterminated", ignoreCase = true) ||
               e is JSONException
    }

    /**
     * Очищает историю чата.
     */
    suspend fun clearHistory() {
        chatRepository.clearHistory()
    }

    /**
     * Получает количество сообщений в истории.
     */
    suspend fun getMessageCount(): Int {
        return chatRepository.getMessageCount()
    }
}
