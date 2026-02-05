package com.example.vitanlyapp.data.agent

import com.example.vitanlyapp.domain.model.AgentCommand
import com.example.vitanlyapp.domain.model.AgentResponse
import com.example.vitanlyapp.domain.model.FoodEntry
import com.example.vitanlyapp.domain.orchestrator.UiActionType
import com.example.vitanlyapp.domain.repository.ChatRepository
import com.example.vitanlyapp.domain.repository.DayEntryRepository
import com.example.vitanlyapp.domain.repository.ThemeRepository
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Адаптер для выполнения команд AI-агента в приложении.
 *
 * Обрабатывает все типы AgentCommand:
 * - Команды профиля: set_weight, set_height, set_age, set_gender, set_activity
 * - Команды целей: set_goal, set_target_weight, set_tempo
 * - Команды еды: add_food, delete_food, delete_meal, clear_day, delete_day
 * - Команды приложения: set_theme, clear_chat, open_tile, close_tile
 * - Команды данных: reset_profile, reset_all_data
 *
 * @see AgentCommand
 */
@Singleton
class AppControlAdapter @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val dayEntryRepository: DayEntryRepository,
    private val themeRepository: ThemeRepository,
    private val chatRepository: ChatRepository,
    private val foodParsingAdapter: FoodParsingAdapter
) {

    /**
     * Выполняет все команды из ответа AI.
     *
     * @param response ответ от AI с командами
     * @return список результатов выполнения команд
     */
    suspend fun executeCommands(response: AgentResponse): List<CommandResult> {
        val results = mutableListOf<CommandResult>()

        for (command in response.commands) {
            val result = executeCommand(command, response.foodEntries)
            results.add(result)
        }

        return results
    }

    /**
     * Выполняет одну команду.
     *
     * @param command команда для выполнения
     * @param foodEntries записи о еде (для команды add_food)
     * @return результат выполнения
     */
    suspend fun executeCommand(
        command: AgentCommand,
        foodEntries: List<FoodEntry> = emptyList()
    ): CommandResult {
        return try {
            when (command) {
                // ══════════════════════════════════════════════════════════════════
                // Команды профиля
                // ══════════════════════════════════════════════════════════════════

                is AgentCommand.SetWeight -> {
                    userProfileRepository.updateWeight(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Вес установлен: ${command.value} кг"
                    )
                }

                is AgentCommand.SetHeight -> {
                    userProfileRepository.updateHeight(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Рост установлен: ${command.value} см"
                    )
                }

                is AgentCommand.SetAge -> {
                    userProfileRepository.updateAge(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Возраст установлен: ${command.value} лет"
                    )
                }

                is AgentCommand.SetGender -> {
                    userProfileRepository.updateGender(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Пол установлен: ${command.value.label}"
                    )
                }

                is AgentCommand.SetActivity -> {
                    userProfileRepository.updateActivityLevel(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Уровень активности: ${command.value.label}"
                    )
                }

                // ══════════════════════════════════════════════════════════════════
                // Команды целей
                // ══════════════════════════════════════════════════════════════════

                is AgentCommand.SetGoal -> {
                    userProfileRepository.updateGoal(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Цель установлена: ${command.value.label}"
                    )
                }

                is AgentCommand.SetTargetWeight -> {
                    userProfileRepository.updateTargetWeight(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Целевой вес: ${command.value} кг"
                    )
                }

                is AgentCommand.SetTempo -> {
                    userProfileRepository.updateTempo(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Темп: ${command.value} кг/неделя"
                    )
                }

                // ══════════════════════════════════════════════════════════════════
                // Команды еды
                // ══════════════════════════════════════════════════════════════════

                is AgentCommand.AddFood -> {
                    if (foodEntries.isEmpty()) {
                        CommandResult.Skipped(
                            command = command,
                            reason = "Нет записей для добавления"
                        )
                    } else {
                        dayEntryRepository.addEntries(foodEntries)
                        val totalKcal = foodEntries.sumOf { it.kcal }
                        CommandResult.Success(
                            command = command,
                            message = "Добавлено ${foodEntries.size} записей (+$totalKcal ккал)"
                        )
                    }
                }

                is AgentCommand.DeleteFood -> {
                    dayEntryRepository.deleteByName(command.name)
                    CommandResult.Success(
                        command = command,
                        message = "Удалено: ${command.name}"
                    )
                }

                is AgentCommand.DeleteMeal -> {
                    dayEntryRepository.deleteByMealSessionId(command.session_id)
                    CommandResult.Success(
                        command = command,
                        message = "Приём пищи удалён"
                    )
                }

                is AgentCommand.ClearDay -> {
                    dayEntryRepository.clearToday()
                    CommandResult.Success(
                        command = command,
                        message = "Записи за сегодня очищены"
                    )
                }

                // ══════════════════════════════════════════════════════════════════
                // Команды прямого управления (UI-действия)
                // ══════════════════════════════════════════════════════════════════

                is AgentCommand.DeleteFoodById -> {
                    val entry = dayEntryRepository.getEntryById(command.id)
                    if (entry != null) {
                        dayEntryRepository.deleteEntry(command.id)
                        CommandResult.Success(
                            command = command,
                            message = "Удалено: ${entry.emoji} ${entry.name} (${entry.weightGrams}г, ${entry.kcal} ккал)"
                        )
                    } else {
                        CommandResult.Error(
                            command = command,
                            error = "Запись не найдена"
                        )
                    }
                }

                is AgentCommand.RepeatFood -> {
                    val newEntry = dayEntryRepository.repeatEntry(command.id)
                    if (newEntry != null) {
                        CommandResult.Success(
                            command = command,
                            message = "Повторено: ${newEntry.emoji} ${newEntry.name} (+${newEntry.kcal} ккал)"
                        )
                    } else {
                        CommandResult.Error(
                            command = command,
                            error = "Запись не найдена"
                        )
                    }
                }

                is AgentCommand.UpdateFoodWeight -> {
                    val updated = dayEntryRepository.updateEntryWeight(command.id, command.newWeightGrams)
                    if (updated != null) {
                        CommandResult.Success(
                            command = command,
                            message = "Изменён вес: ${updated.emoji} ${updated.name} → ${updated.weightGrams}г (${updated.kcal} ккал)"
                        )
                    } else {
                        CommandResult.Error(
                            command = command,
                            error = "Запись не найдена"
                        )
                    }
                }

                // ══════════════════════════════════════════════════════════════════
                // Команды управления приложением
                // ══════════════════════════════════════════════════════════════════

                is AgentCommand.SetTheme -> {
                    themeRepository.setThemeMode(command.value)
                    CommandResult.Success(
                        command = command,
                        message = "Тема изменена: ${command.value.name.lowercase()}"
                    )
                }

                is AgentCommand.DeleteDay -> {
                    val count = dayEntryRepository.clearDay(command.date)
                    if (count > 0) {
                        CommandResult.Success(
                            command = command,
                            message = "Удалено $count записей за ${command.date}"
                        )
                    } else {
                        CommandResult.Success(
                            command = command,
                            message = "Записей за ${command.date} не найдено"
                        )
                    }
                }

                is AgentCommand.ClearChat -> {
                    chatRepository.clearHistory()
                    CommandResult.Success(
                        command = command,
                        message = "История чата очищена"
                    )
                }

                is AgentCommand.OpenTile -> {
                    // UI-команда: возвращаем результат с указанием позиции
                    // ViewModel обрабатывает это и открывает плитку
                    CommandResult.UiAction(
                        command = command,
                        action = UiActionType.OpenTile(command.position)
                    )
                }

                is AgentCommand.CloseTile -> {
                    // UI-команда: ViewModel закрывает текущую плитку
                    CommandResult.UiAction(
                        command = command,
                        action = UiActionType.CloseTile
                    )
                }

                // ══════════════════════════════════════════════════════════════════
                // Команды сброса данных
                // ══════════════════════════════════════════════════════════════════

                is AgentCommand.ResetProfile -> {
                    userProfileRepository.deleteProfile()
                    userProfileRepository.ensureProfileExists()
                    CommandResult.Success(
                        command = command,
                        message = "Профиль сброшен к значениям по умолчанию"
                    )
                }

                is AgentCommand.ResetAllData -> {
                    // UI-команда: требует перехода на онбординг после сброса
                    userProfileRepository.clearAllData()
                    CommandResult.UiAction(
                        command = command,
                        action = UiActionType.ResetAllData
                    )
                }
            }
        } catch (e: Exception) {
            CommandResult.Error(
                command = command,
                error = e.message ?: "Неизвестная ошибка"
            )
        }
    }

    /**
     * Проверяет, содержит ли ответ команды определённого типа.
     */
    fun hasCommandOfType(response: AgentResponse, type: Class<out AgentCommand>): Boolean {
        return response.commands.any { type.isInstance(it) }
    }

    /**
     * Подсчитывает количество команд в ответе.
     */
    fun getCommandCount(response: AgentResponse): Int = response.commands.size

    /**
     * Группирует команды по категориям.
     */
    fun categorizeCommands(commands: List<AgentCommand>): CommandCategories {
        val profile = mutableListOf<AgentCommand>()
        val goals = mutableListOf<AgentCommand>()
        val food = mutableListOf<AgentCommand>()
        val app = mutableListOf<AgentCommand>()
        val data = mutableListOf<AgentCommand>()

        for (command in commands) {
            when (command) {
                is AgentCommand.SetWeight,
                is AgentCommand.SetHeight,
                is AgentCommand.SetAge,
                is AgentCommand.SetGender,
                is AgentCommand.SetActivity -> profile.add(command)

                is AgentCommand.SetGoal,
                is AgentCommand.SetTargetWeight,
                is AgentCommand.SetTempo -> goals.add(command)

                is AgentCommand.AddFood,
                is AgentCommand.DeleteFood,
                is AgentCommand.DeleteMeal,
                is AgentCommand.ClearDay,
                is AgentCommand.DeleteDay,
                is AgentCommand.DeleteFoodById,
                is AgentCommand.RepeatFood,
                is AgentCommand.UpdateFoodWeight -> food.add(command)

                is AgentCommand.SetTheme,
                is AgentCommand.ClearChat,
                is AgentCommand.OpenTile,
                is AgentCommand.CloseTile -> app.add(command)

                is AgentCommand.ResetProfile,
                is AgentCommand.ResetAllData -> data.add(command)
            }
        }

        return CommandCategories(profile, goals, food, app, data)
    }
}

/**
 * Результат выполнения команды.
 */
sealed class CommandResult {
    abstract val command: AgentCommand

    /** Команда выполнена успешно */
    data class Success(
        override val command: AgentCommand,
        val message: String
    ) : CommandResult()

    /** Команда пропущена */
    data class Skipped(
        override val command: AgentCommand,
        val reason: String
    ) : CommandResult()

    /** Ошибка при выполнении */
    data class Error(
        override val command: AgentCommand,
        val error: String
    ) : CommandResult()

    /** Команда требует UI-действия (обрабатывается ViewModel) */
    data class UiAction(
        override val command: AgentCommand,
        val action: UiActionType
    ) : CommandResult()
}

/**
 * Категории команд.
 */
data class CommandCategories(
    val profileCommands: List<AgentCommand>,
    val goalCommands: List<AgentCommand>,
    val foodCommands: List<AgentCommand>,
    val appCommands: List<AgentCommand> = emptyList(),
    val dataCommands: List<AgentCommand> = emptyList()
) {
    val totalCount: Int get() = profileCommands.size + goalCommands.size + foodCommands.size + appCommands.size + dataCommands.size
    val isEmpty: Boolean get() = totalCount == 0
}
