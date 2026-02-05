package com.example.vitanlyapp.domain.orchestrator

import com.example.vitanlyapp.domain.model.AgentCommand
import com.example.vitanlyapp.domain.model.AgentResponse

/**
 * Оркестратор AI-агента — фасад для координации всех адаптеров Agent Layer.
 *
 * Порядок обработки сообщения:
 * 1. Отправка в ChatAIAdapter → получение AgentResponse
 * 2. Если есть food_entries → FoodParsingAdapter → добавление записей
 * 3. Если есть commands → AppControlAdapter → выполнение команд
 * 4. Возврат результата в ViewModel
 *
 * @see OrchestratorResult
 */
interface AgentOrchestrator {

    /**
     * Обрабатывает сообщение пользователя полностью:
     * 1. Отправляет в AI и получает ответ
     * 2. Обрабатывает food_entries через FoodParsingAdapter
     * 3. Выполняет команды через AppControlAdapter
     * 4. Возвращает результат обработки
     *
     * @param userMessage текст сообщения пользователя
     * @return результат обработки
     */
    suspend fun processMessage(userMessage: String): OrchestratorResult

    /**
     * Отправляет сообщение без выполнения команд.
     * Полезно для предпросмотра или отладки.
     *
     * @param userMessage текст сообщения
     * @return Result с AgentResponse
     */
    suspend fun sendMessageOnly(userMessage: String): Result<AgentResponse>

    /**
     * Выполняет команды из готового ответа.
     * Полезно для отложенного выполнения.
     *
     * @param response ответ AI с командами
     * @return список результатов выполнения
     */
    suspend fun executeResponseCommands(response: AgentResponse): List<CommandExecutionResult>

    /**
     * Выполняет команду напрямую (без AI).
     * Используется для UI-действий с продуктами.
     *
     * @param command команда для выполнения
     * @return результат выполнения
     */
    suspend fun executeDirectCommand(command: AgentCommand): CommandExecutionResult

    /**
     * Очищает историю чата.
     */
    suspend fun clearChatHistory()

    /**
     * Получает статистику агента.
     */
    suspend fun getStats(): AgentStats
}

/**
 * Результат полной обработки сообщения оркестратором.
 */
sealed class OrchestratorResult {

    /**
     * Успешная обработка.
     *
     * @property response оригинальный ответ AI
     * @property foodProcessingResult результат обработки продуктов
     * @property commandResults результаты выполнения команд
     */
    data class Success(
        val response: AgentResponse,
        val foodProcessingResult: FoodProcessingStatus,
        val commandResults: List<CommandExecutionResult>
    ) : OrchestratorResult() {

        /** Текст ответа для отображения в чате */
        val responseText: String get() = response.responseText

        /** Количество добавленных продуктов */
        val addedFoodCount: Int
            get() = when (foodProcessingResult) {
                is FoodProcessingStatus.Added -> foodProcessingResult.count
                else -> 0
            }

        /** Количество выполненных команд */
        val executedCommandCount: Int get() = commandResults.size

        /** Все команды выполнены успешно */
        val allCommandsSuccessful: Boolean
            get() = commandResults.all { it is CommandExecutionResult.Success }

        /** Ошибки выполнения команд */
        val commandErrors: List<CommandExecutionResult.Error>
            get() = commandResults.filterIsInstance<CommandExecutionResult.Error>()

        /** Суммарные калории добавленных продуктов */
        val totalAddedKcal: Int
            get() = when (foodProcessingResult) {
                is FoodProcessingStatus.Added -> foodProcessingResult.totalKcal
                else -> 0
            }
    }

    /**
     * Ошибка обработки.
     *
     * @property message описание ошибки
     * @property exception исключение (если есть)
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : OrchestratorResult()
}

/**
 * Статус обработки продуктов.
 */
sealed class FoodProcessingStatus {
    /** Продукты не были переданы */
    data object None : FoodProcessingStatus()

    /** Продукты успешно добавлены */
    data class Added(
        val count: Int,
        val totalKcal: Int
    ) : FoodProcessingStatus()

    /** Ошибка при добавлении продуктов */
    data class Error(val message: String) : FoodProcessingStatus()
}

/**
 * Результат выполнения одной команды.
 */
sealed class CommandExecutionResult {
    abstract val commandName: String

    /** Команда выполнена успешно */
    data class Success(
        override val commandName: String,
        val message: String
    ) : CommandExecutionResult()

    /** Команда пропущена */
    data class Skipped(
        override val commandName: String,
        val reason: String
    ) : CommandExecutionResult()

    /** Ошибка при выполнении */
    data class Error(
        override val commandName: String,
        val error: String
    ) : CommandExecutionResult()
}

/**
 * Статистика агента.
 */
data class AgentStats(
    /** Количество сообщений в истории */
    val messageCount: Int,
    /** Количество добавленных продуктов за сессию */
    val totalFoodEntriesAdded: Int = 0,
    /** Количество выполненных команд за сессию */
    val totalCommandsExecuted: Int = 0
)
