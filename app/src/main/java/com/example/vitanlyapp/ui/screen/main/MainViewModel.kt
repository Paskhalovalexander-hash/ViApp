package com.example.vitanlyapp.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.AgentCommand
import com.example.vitanlyapp.domain.model.ChatMessage
import com.example.vitanlyapp.domain.model.ChatRole
import com.example.vitanlyapp.domain.model.KBJUData
import com.example.vitanlyapp.domain.model.KbjuBarStat
import com.example.vitanlyapp.domain.model.ThemeMode
import com.example.vitanlyapp.domain.model.TilePosition
import com.example.vitanlyapp.domain.model.toBarStats
import com.example.vitanlyapp.domain.orchestrator.AgentOrchestrator
import com.example.vitanlyapp.domain.orchestrator.CommandExecutionResult
import com.example.vitanlyapp.domain.orchestrator.OrchestratorResult
import com.example.vitanlyapp.domain.orchestrator.UiActionType
import com.example.vitanlyapp.domain.repository.DayEntry
import com.example.vitanlyapp.domain.repository.DayEntryRepository
import com.example.vitanlyapp.domain.repository.KbjuRepository
import com.example.vitanlyapp.domain.repository.ThemeRepository
import com.example.vitanlyapp.domain.repository.UserProfile
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val kbjuRepository: KbjuRepository,
    private val agentOrchestrator: AgentOrchestrator,
    private val themeRepository: ThemeRepository,
    private val userProfileRepository: UserProfileRepository,
    private val dayEntryRepository: DayEntryRepository
) : ViewModel() {

    init {
        // Подписываемся на изменения профиля для синхронизации веса и активности
        viewModelScope.launch {
            userProfileRepository.ensureProfileExists()
            userProfileRepository.getProfileFlow().collect { profile ->
                profile?.let { syncFromProfile(it) }
            }
        }
    }

    /**
     * Синхронизирует локальное состояние с профилем пользователя.
     */
    private fun syncFromProfile(profile: UserProfile) {
        _currentWeight.value = profile.weight
        _activityCoefficient.value = profile.activityLevel.coefficient
    }

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.CLASSIC)

    fun toggleTheme() {
        viewModelScope.launch {
            val next = when (themeMode.value) {
                ThemeMode.CLASSIC -> ThemeMode.WARM_DARK
                ThemeMode.WARM_DARK -> ThemeMode.CLASSIC
            }
            themeRepository.setThemeMode(next)
        }
    }

    private val _activeTile = MutableStateFlow<TilePosition?>(null)
    val activeTile: StateFlow<TilePosition?> = _activeTile.asStateFlow()

    private val _currentWeight = MutableStateFlow(95.6f)
    val currentWeight: StateFlow<Float> = _currentWeight.asStateFlow()

    private val _activityCoefficient = MutableStateFlow(ActivityLevel.default.coefficient)
    val activityCoefficient: StateFlow<Float> = _activityCoefficient.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    val kbjuData: StateFlow<KBJUData> = kbjuRepository.getKbju()

    /** Записи о еде за сегодня для отображения в средней плитке. Обновляется при смене дня. */
    val todayEntries: StateFlow<List<DayEntry>> = dayEntryRepository
        .getCurrentDateFlow()
        .flatMapLatest { date -> dayEntryRepository.getEntriesForDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Профиль пользователя для отображения параметров в верхней плитке */
    val userProfile: StateFlow<UserProfile?> = userProfileRepository.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateWeight(kg: Float) {
        val validWeight = kg.coerceIn(30f, 200f)
        _currentWeight.value = validWeight
        viewModelScope.launch {
            userProfileRepository.updateWeight(validWeight)
        }
    }

    fun updateActivityCoefficient(coeff: Float) {
        val activityLevel = ActivityLevel.fromCoefficient(coeff)
        _activityCoefficient.value = activityLevel.coefficient
        viewModelScope.launch {
            userProfileRepository.updateActivityLevel(activityLevel)
        }
    }

    /**
     * Переключение активной плитки по клику:
     * если кликнули по той же — сброс в null, иначе — новая активная.
     */
    fun onTileClick(position: TilePosition) {
        _activeTile.value = if (_activeTile.value == position) null else position
    }

    fun updateKbju(data: KBJUData) {
        viewModelScope.launch {
            kbjuRepository.updateKbju(data)
        }
    }

    /** Статистика баров КБЖУ (проценты и overflow) для отображения в верхней плитке. */
    fun getBarStats(data: KBJUData): List<KbjuBarStat> = data.toBarStats()

    /**
     * Отправляет сообщение через AI-оркестратор.
     * Оркестратор автоматически:
     * - Отправляет сообщение в DeepSeek API
     * - Парсит ответ и извлекает food_entries и commands
     * - Добавляет еду в БД через FoodParsingAdapter
     * - Выполняет команды через AppControlAdapter
     */
    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            // Добавляем сообщение пользователя
            _chatMessages.value = _chatMessages.value + ChatMessage(ChatRole.USER, trimmed)
            _chatLoading.value = true

            // Обрабатываем через оркестратор
            when (val result = agentOrchestrator.processMessage(trimmed)) {
                is OrchestratorResult.Success -> {
                    // Формируем ответ для отображения
                    val responseText = buildResponseText(result)
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        ChatRole.ASSISTANT,
                        responseText
                    )
                }
                is OrchestratorResult.Error -> {
                    // Не показываем ошибки пользователю — retry логика в ChatAIAdapter
                    // должна справиться с временными сбоями. Если все попытки исчерпаны,
                    // пользователь просто не получит ответ (лучше чем показывать ошибку).
                }
            }

            _chatLoading.value = false
        }
    }

    /**
     * Формирует текст ответа для отображения в чате.
     * Добавляет информацию о добавленных продуктах и выполненных командах.
     */
    private fun buildResponseText(result: OrchestratorResult.Success): String {
        val builder = StringBuilder(result.responseText)

        // Информация о добавленных продуктах
        if (result.addedFoodCount > 0) {
            builder.append("\n\n📝 Добавлено: ${result.addedFoodCount} ${formatFoodCount(result.addedFoodCount)}")
            builder.append(" (+${result.totalAddedKcal} ккал)")
        }

        // Информация об ошибках команд (если есть)
        if (result.commandErrors.isNotEmpty()) {
            builder.append("\n\n⚠️ Не выполнено:")
            result.commandErrors.forEach { error ->
                builder.append("\n• ${error.commandName}: ${error.error}")
            }
        }

        return builder.toString()
    }

    /**
     * Склонение слова "продукт" в зависимости от числа.
     */
    private fun formatFoodCount(count: Int): String {
        val lastDigit = count % 10
        val lastTwoDigits = count % 100

        return when {
            lastTwoDigits in 11..19 -> "продуктов"
            lastDigit == 1 -> "продукт"
            lastDigit in 2..4 -> "продукта"
            else -> "продуктов"
        }
    }

    /**
     * Очищает историю чата.
     */
    fun clearChatHistory() {
        viewModelScope.launch {
            agentOrchestrator.clearChatHistory()
            _chatMessages.value = emptyList()
        }
    }

    /**
     * Сбрасывает все данные пользователя.
     * После вызова нужно перейти на экран онбординга.
     */
    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            userProfileRepository.clearAllData()
            _chatMessages.value = emptyList()
            onComplete()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Действия с продуктами (UI → Orchestrator → AppControlAdapter)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Удаляет запись о еде по ID.
     */
    fun deleteEntry(entry: DayEntry) {
        viewModelScope.launch {
            val result = agentOrchestrator.executeDirectCommand(
                AgentCommand.DeleteFoodById(entry.id)
            )
            addSystemMessage(result)
        }
    }

    /**
     * Повторяет запись о еде (создаёт копию в текущей сессии).
     */
    fun repeatEntry(entry: DayEntry) {
        viewModelScope.launch {
            val result = agentOrchestrator.executeDirectCommand(
                AgentCommand.RepeatFood(entry.id)
            )
            addSystemMessage(result)
        }
    }

    /**
     * Изменяет вес записи о еде (с пропорциональным пересчётом КБЖУ).
     */
    fun updateEntryWeight(entry: DayEntry, newWeight: Int) {
        viewModelScope.launch {
            val result = agentOrchestrator.executeDirectCommand(
                AgentCommand.UpdateFoodWeight(entry.id, newWeight)
            )
            addSystemMessage(result)
        }
    }

    /**
     * Добавляет системное сообщение в чат с результатом выполнения команды.
     */
    private fun addSystemMessage(result: CommandExecutionResult) {
        val message = when (result) {
            is CommandExecutionResult.Success -> result.message
            is CommandExecutionResult.Error -> "Ошибка: ${result.error}"
            is CommandExecutionResult.Skipped -> result.reason
            is CommandExecutionResult.UiAction -> {
                // Обрабатываем UI-действия
                handleUiAction(result.action)
                null // Не добавляем сообщение, т.к. действие уже выполнено
            }
        }
        message?.let {
            _chatMessages.value = _chatMessages.value + ChatMessage(ChatRole.ASSISTANT, it)
        }
    }

    /**
     * Обрабатывает UI-действия от агента.
     */
    private fun handleUiAction(action: UiActionType) {
        when (action) {
            is UiActionType.OpenTile -> {
                _activeTile.value = action.position
            }
            is UiActionType.CloseTile -> {
                _activeTile.value = null
            }
            is UiActionType.ResetAllData -> {
                // Сброс данных уже выполнен в AppControlAdapter
                // ViewModel получит callback через resetAllData()
            }
        }
    }
}
