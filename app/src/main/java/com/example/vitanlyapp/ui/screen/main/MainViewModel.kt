package com.example.vitanlyapp.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.AgentCommand
import com.example.vitanlyapp.domain.model.ChatMessage
import com.example.vitanlyapp.domain.model.ChatRole
import com.example.vitanlyapp.domain.model.FoodEntry
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
                ThemeMode.WARM_DARK -> ThemeMode.MATTE_DARK
                ThemeMode.MATTE_DARK -> ThemeMode.CLASSIC
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

    // ══════════════════════════════════════════════════════════════════════════
    // Выбранный день — единый источник правды для списка продуктов и статистики
    // ══════════════════════════════════════════════════════════════════════════

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Выбранная дата. null = сегодня. От selectedDate зависят selectedDayEntries и kbjuData. */
    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    /** Список доступных дат с записями (включая сегодня). Старые даты первыми, сегодня последним. */
    val availableDates: StateFlow<List<String>> = dayEntryRepository.getAllDatesFlow()
        .map { dates ->
            val today = LocalDate.now().format(dateFormatter)
            (listOf(today) + dates).distinct().sorted() // Старые слева, новые справа
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(LocalDate.now().format(dateFormatter)))

    /** Записи о еде за выбранный день для отображения в средней плитке. */
    val selectedDayEntries: StateFlow<List<DayEntry>> = _selectedDate
        .flatMapLatest { date ->
            val targetDate = date ?: LocalDate.now().format(dateFormatter)
            dayEntryRepository.getEntriesForDateFlow(targetDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Статистика КБЖУ за выбранный день. Зависит от selectedDate. */
    val kbjuData: StateFlow<KBJUData> = _selectedDate
        .flatMapLatest { date ->
            val targetDate = date ?: LocalDate.now().format(dateFormatter)
            kbjuRepository.getKbjuForDateFlow(targetDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KBJUData.default())

    /** Выбирает день для отображения на средней плитке. */
    fun selectDay(date: String) {
        _selectedDate.value = date
    }

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
    // Тестовые данные
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Заполняет БД тестовыми данными о еде за последние 5 дней.
     * Используется для тестирования навигации по дням.
     */
    fun populateTestData() {
        viewModelScope.launch {
            val testFoods = listOf(
                // Завтраки
                FoodEntry("Яичница", 150, 220, 14f, 17f, 1f, "🍳"),
                FoodEntry("Творог 5%", 200, 210, 34f, 10f, 6f, "🧀"),
                FoodEntry("Овсянка", 250, 230, 8f, 5f, 40f, "🥣"),
                FoodEntry("Омлет", 180, 260, 18f, 20f, 2f, "🍳"),
                // Обеды
                FoodEntry("Куриная грудка", 200, 330, 62f, 7f, 0f, "🍗"),
                FoodEntry("Гречка", 150, 200, 8f, 2f, 40f, "🍚"),
                FoodEntry("Рис", 180, 230, 5f, 1f, 50f, "🍚"),
                FoodEntry("Говядина тушёная", 180, 290, 38f, 15f, 0f, "🥩"),
                // Ужины
                FoodEntry("Рыба запечённая", 180, 200, 36f, 6f, 0f, "🐟"),
                FoodEntry("Салат овощной", 200, 80, 2f, 5f, 8f, "🥗"),
                FoodEntry("Сёмга на пару", 150, 250, 30f, 14f, 0f, "🐟"),
                // Перекусы
                FoodEntry("Банан", 120, 107, 1f, 0f, 27f, "🍌"),
                FoodEntry("Яблоко", 180, 94, 0f, 0f, 25f, "🍎"),
                FoodEntry("Орехи", 30, 180, 5f, 16f, 5f, "🥜"),
                FoodEntry("Йогурт", 150, 90, 5f, 3f, 12f, "🥛")
            )

            val today = LocalDate.now()
            for (daysAgo in 0..4) {
                val date = today.minusDays(daysAgo.toLong()).format(dateFormatter)
                // Выбираем 3-5 случайных блюд для каждого дня
                val dayFoods = testFoods.shuffled().take((3..5).random())
                dayEntryRepository.addEntriesForDate(date, dayFoods)
            }

            // Добавляем сообщение в чат
            _chatMessages.value = _chatMessages.value + ChatMessage(
                ChatRole.ASSISTANT,
                "🧪 Добавлены тестовые данные за 5 дней. Попробуйте свайпы на средней плитке!"
            )
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
