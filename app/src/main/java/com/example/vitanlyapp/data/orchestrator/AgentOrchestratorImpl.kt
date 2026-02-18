package com.example.vitanlyapp.data.orchestrator

import com.example.vitanlyapp.data.agent.AppControlAdapter
import com.example.vitanlyapp.data.agent.ChatAIAdapter
import com.example.vitanlyapp.data.agent.CommandResult
import com.example.vitanlyapp.data.agent.FoodParsingAdapter
import com.example.vitanlyapp.data.agent.FoodProcessingResult
import com.example.vitanlyapp.domain.model.AgentCommand
import com.example.vitanlyapp.domain.model.AgentResponse
import com.example.vitanlyapp.domain.orchestrator.AgentOrchestrator
import com.example.vitanlyapp.domain.orchestrator.AgentStats
import com.example.vitanlyapp.domain.orchestrator.CommandExecutionResult
import com.example.vitanlyapp.domain.orchestrator.FoodProcessingStatus
import com.example.vitanlyapp.domain.orchestrator.OrchestratorResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация оркестратора AI-агента.
 *
 * Координирует все адаптеры Agent Layer:
 * - [ChatAIAdapter] — отправка сообщений в DeepSeek API
 * - [FoodParsingAdapter] — обработка распознанных продуктов
 * - [AppControlAdapter] — выполнение команд
 *
 * Логика обработки сообщения:
 * 1. Отправляем сообщение в AI через ChatAIAdapter
 * 2. Если ответ содержит food_entries → обрабатываем через FoodParsingAdapter
 * 3. Если ответ содержит commands → выполняем через AppControlAdapter
 *    (команда AddFood будет пропущена, т.к. food_entries уже обработаны)
 * 4. Возвращаем агрегированный результат
 *
 * @see AgentOrchestrator
 */
@Singleton
class AgentOrchestratorImpl @Inject constructor(
    private val chatAIAdapter: ChatAIAdapter,
    private val appControlAdapter: AppControlAdapter,
    private val foodParsingAdapter: FoodParsingAdapter
) : AgentOrchestrator {

    // Счётчики для статистики сессии
    private var sessionFoodEntriesAdded = 0
    private var sessionCommandsExecuted = 0

    override suspend fun processMessage(userMessage: String): OrchestratorResult {
        // 1. Отправляем сообщение в AI
        val aiResult = chatAIAdapter.sendMessage(userMessage)

        return when {
            aiResult.isSuccess -> {
                val response = aiResult.getOrThrow()
                processSuccessfulResponse(response)
            }
            else -> {
                OrchestratorResult.Error(
                    message = aiResult.exceptionOrNull()?.message ?: "Неизвестная ошибка AI",
                    exception = aiResult.exceptionOrNull()
                )
            }
        }
    }

    /**
     * Обрабатывает успешный ответ от AI.
     */
    private suspend fun processSuccessfulResponse(response: AgentResponse): OrchestratorResult.Success {
        // 2. Обрабатываем food_entries (если есть)
        val foodStatus = processFoodEntries(response)

        // 3. Выполняем команды (если есть)
        // Фильтруем AddFood, т.к. food_entries уже обработаны выше
        val commandResults = executeCommands(response, skipAddFood = foodStatus is FoodProcessingStatus.Added)

        return OrchestratorResult.Success(
            response = response,
            foodProcessingResult = foodStatus,
            commandResults = commandResults
        )
    }

    /**
     * Обрабатывает food_entries из ответа AI.
     */
    private suspend fun processFoodEntries(response: AgentResponse): FoodProcessingStatus {
        if (response.foodEntries.isEmpty()) {
            return FoodProcessingStatus.None
        }

        val result = foodParsingAdapter.processFoodEntries(response)

        return when (result) {
            is FoodProcessingResult.Success -> {
                sessionFoodEntriesAdded += result.addedCount
                FoodProcessingStatus.Added(
                    count = result.addedCount,
                    totalKcal = result.totalKcal
                )
            }
            is FoodProcessingResult.Empty -> FoodProcessingStatus.None
            is FoodProcessingResult.AllInvalid -> {
                FoodProcessingStatus.Error("Все ${result.count} записей невалидны")
            }
            is FoodProcessingResult.Error -> {
                FoodProcessingStatus.Error(result.message)
            }
        }
    }

    /**
     * Выполняет команды из ответа AI.
     *
     * @param response ответ AI
     * @param skipAddFood пропустить команду AddFood (если food_entries уже обработаны)
     */
    private suspend fun executeCommands(
        response: AgentResponse,
        skipAddFood: Boolean
    ): List<CommandExecutionResult> {
        if (response.commands.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<CommandExecutionResult>()

        for (command in response.commands) {
            // Пропускаем AddFood если продукты уже добавлены через FoodParsingAdapter
            if (skipAddFood && command is AgentCommand.AddFood) {
                results.add(
                    CommandExecutionResult.Skipped(
                        commandName = "add_food",
                        reason = "Продукты уже добавлены через FoodParsingAdapter"
                    )
                )
                continue
            }

            val result = appControlAdapter.executeCommand(command, response.foodEntries)
            results.add(mapCommandResult(result))
            sessionCommandsExecuted++
        }

        return results
    }

    /**
     * Преобразует внутренний CommandResult в публичный CommandExecutionResult.
     */
    private fun mapCommandResult(result: CommandResult): CommandExecutionResult {
        return when (result) {
            is CommandResult.Success -> CommandExecutionResult.Success(
                commandName = result.command.toCommandName(),
                message = result.message
            )
            is CommandResult.Skipped -> CommandExecutionResult.Skipped(
                commandName = result.command.toCommandName(),
                reason = result.reason
            )
            is CommandResult.Error -> CommandExecutionResult.Error(
                commandName = result.command.toCommandName(),
                error = result.error
            )
            is CommandResult.UiAction -> CommandExecutionResult.UiAction(
                commandName = result.command.toCommandName(),
                action = result.action
            )
        }
    }

    override suspend fun sendMessageOnly(userMessage: String): Result<AgentResponse> {
        return chatAIAdapter.sendMessage(userMessage, saveToHistory = false)
    }

    override suspend fun executeResponseCommands(response: AgentResponse): List<CommandExecutionResult> {
        return executeCommands(response, skipAddFood = false)
    }

    override suspend fun executeDirectCommand(command: AgentCommand): CommandExecutionResult {
        val result = appControlAdapter.executeCommand(command)
        sessionCommandsExecuted++
        return mapCommandResult(result)
    }

    override suspend fun clearChatHistory() {
        chatAIAdapter.clearHistory()
        // Сбрасываем счётчики сессии
        sessionFoodEntriesAdded = 0
        sessionCommandsExecuted = 0
    }

    override suspend fun getStats(): AgentStats {
        return AgentStats(
            messageCount = chatAIAdapter.getMessageCount(),
            totalFoodEntriesAdded = sessionFoodEntriesAdded,
            totalCommandsExecuted = sessionCommandsExecuted
        )
    }
}

/**
 * Получает строковое имя команды для логирования/отображения.
 */
private fun AgentCommand.toCommandName(): String = when (this) {
    // Профиль
    is AgentCommand.SetWeight -> "set_weight"
    is AgentCommand.SetHeight -> "set_height"
    is AgentCommand.SetAge -> "set_age"
    is AgentCommand.SetGender -> "set_gender"
    is AgentCommand.SetActivity -> "set_activity"
    // Цели
    is AgentCommand.SetGoal -> "set_goal"
    is AgentCommand.SetTargetWeight -> "set_target_weight"
    is AgentCommand.SetTempo -> "set_tempo"
    // Еда
    is AgentCommand.AddFood -> "add_food"
    is AgentCommand.DeleteFood -> "delete_food"
    is AgentCommand.DeleteMeal -> "delete_meal"
    is AgentCommand.ClearDay -> "clear_day"
    is AgentCommand.DeleteDay -> "delete_day"
    is AgentCommand.DeleteFoodById -> "delete_food_by_id"
    is AgentCommand.RepeatFood -> "repeat_food"
    is AgentCommand.UpdateFoodWeight -> "update_food_weight"
    // Приложение
    is AgentCommand.SetTheme -> "set_theme"
    is AgentCommand.ClearChat -> "clear_chat"
    is AgentCommand.OpenTile -> "open_tile"
    is AgentCommand.CloseTile -> "close_tile"
    // Данные
    is AgentCommand.ResetProfile -> "reset_profile"
    is AgentCommand.ResetAllData -> "reset_all_data"
}
