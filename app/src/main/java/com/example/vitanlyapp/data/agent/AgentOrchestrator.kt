package com.example.vitanlyapp.data.agent

import com.example.vitanlyapp.domain.model.AgentResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Оркестратор AI-агента.
 *
 * Фасад, объединяющий все адаптеры Agent Layer:
 * - ChatAIAdapter — отправка сообщений в DeepSeek API
 * - AppControlAdapter — выполнение команд
 * - FoodParsingAdapter — обработка еды
 *
 * Упрощает взаимодействие с AI для ViewModel и UseCase.
 *
 * @see ChatAIAdapter
 * @see AppControlAdapter
 * @see FoodParsingAdapter
 */
@Singleton
class AgentOrchestrator @Inject constructor(
    private val chatAIAdapter: ChatAIAdapter,
    private val appControlAdapter: AppControlAdapter,
    private val foodParsingAdapter: FoodParsingAdapter
) {

    /**
     * Обрабатывает сообщение пользователя полностью:
     * 1. Отправляет в AI и получает ответ
     * 2. Выполняет команды из ответа
     * 3. Возвращает результат обработки
     *
     * @param userMessage текст сообщения пользователя
     * @return результат обработки
     */
    suspend fun processMessage(userMessage: String): AgentProcessingResult {
        // Отправляем сообщение в AI
        val aiResult = chatAIAdapter.sendMessage(userMessage)

        return when {
            aiResult.isSuccess -> {
                val response = aiResult.getOrThrow()

                // Выполняем команды
                val commandResults = if (response.commands.isNotEmpty()) {
                    appControlAdapter.executeCommands(response)
                } else {
                    emptyList()
                }

                AgentProcessingResult.Success(
                    response = response,
                    commandResults = commandResults
                )
            }
            else -> {
                AgentProcessingResult.Error(
                    message = aiResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                )
            }
        }
    }

    /**
     * Отправляет сообщение без выполнения команд.
     * Полезно для предпросмотра или отладки.
     *
     * @param userMessage текст сообщения
     * @return Result с AgentResponse
     */
    suspend fun sendMessageOnly(userMessage: String): Result<AgentResponse> {
        return chatAIAdapter.sendMessage(userMessage)
    }

    /**
     * Выполняет команды из готового ответа.
     * Полезно для отложенного выполнения.
     *
     * @param response ответ AI с командами
     * @return список результатов выполнения
     */
    suspend fun executeResponseCommands(response: AgentResponse): List<CommandResult> {
        return appControlAdapter.executeCommands(response)
    }

    /**
     * Очищает историю чата.
     */
    suspend fun clearChatHistory() {
        chatAIAdapter.clearHistory()
    }

    /**
     * Получает статистику.
     */
    suspend fun getStats(): AgentStats {
        return AgentStats(
            messageCount = chatAIAdapter.getMessageCount()
        )
    }
}

/**
 * Результат полной обработки сообщения.
 */
sealed class AgentProcessingResult {

    /**
     * Успешная обработка.
     */
    data class Success(
        val response: AgentResponse,
        val commandResults: List<CommandResult>
    ) : AgentProcessingResult() {

        /** Текст ответа для отображения в чате */
        val responseText: String get() = response.responseText

        /** Количество добавленных продуктов */
        val foodCount: Int get() = response.foodEntries.size

        /** Количество выполненных команд */
        val commandCount: Int get() = commandResults.size

        /** Все команды выполнены успешно */
        val allCommandsSuccessful: Boolean
            get() = commandResults.all { it is CommandResult.Success }

        /** Ошибки выполнения команд */
        val commandErrors: List<CommandResult.Error>
            get() = commandResults.filterIsInstance<CommandResult.Error>()
    }

    /**
     * Ошибка обработки.
     */
    data class Error(
        val message: String
    ) : AgentProcessingResult()
}

/**
 * Статистика агента.
 */
data class AgentStats(
    val messageCount: Int
)
