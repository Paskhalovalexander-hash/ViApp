package com.example.vitanlyapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Структурированный ответ от AI-агента (DeepSeek).
 *
 * AI всегда возвращает JSON в этом формате:
 * ```json
 * {
 *   "response_text": "Записал завтрак! Яичница — отличный источник белка.",
 *   "food_entries": [
 *     {"name": "Яичница", "weight_g": 150, "kcal": 220, "protein": 14.0, "fat": 17.0, "carbs": 1.0}
 *   ],
 *   "commands": [
 *     {"type": "add_food"}
 *   ]
 * }
 * ```
 *
 * Поля:
 * - response_text: Текст для отображения в чате (обязательно)
 * - food_entries: Распознанные продукты (может быть пустым)
 * - commands: Команды для выполнения (может быть пустым)
 */
@Serializable
data class AgentResponse(
    /** Текстовый ответ для отображения пользователю в чате */
    @SerialName("response_text")
    val responseText: String,

    /** Список распознанных продуктов из сообщения пользователя */
    @SerialName("food_entries")
    val foodEntries: List<FoodEntry> = emptyList(),

    /** Список команд для выполнения в приложении */
    val commands: List<AgentCommand> = emptyList()
) {
    companion object {
        /**
         * Создаёт простой текстовый ответ без команд и еды.
         */
        fun textOnly(text: String) = AgentResponse(
            responseText = text,
            foodEntries = emptyList(),
            commands = emptyList()
        )

        /**
         * Создаёт ответ об ошибке.
         */
        fun error(message: String) = AgentResponse(
            responseText = "Произошла ошибка: $message",
            foodEntries = emptyList(),
            commands = emptyList()
        )
    }
}
