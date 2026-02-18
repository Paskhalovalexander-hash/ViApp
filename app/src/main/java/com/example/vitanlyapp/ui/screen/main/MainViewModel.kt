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
import com.example.vitanlyapp.domain.repository.DayKcalPoint
import com.example.vitanlyapp.domain.repository.KbjuRepository
import com.example.vitanlyapp.domain.repository.ThemeRepository
import com.example.vitanlyapp.domain.repository.UserProfile
import com.example.vitanlyapp.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import java.util.LinkedHashMap

/** UI/domain-neutral wrapper: entries tied to their date (atomic pair). */
data class DayEntriesForDate(val date: String, val entries: List<DayEntry>)

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.MATTE_DARK)

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

    /** Эффективная дата экрана: selectedDate ?: today. */
    private val effectiveCenterDate: StateFlow<String> = combine(
        _selectedDate,
        dayEntryRepository.getCurrentDateFlow()
    ) { sel, today -> sel ?: today }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now().format(dateFormatter))

    private val entriesCacheLock = Any()
    private val entriesCache = LinkedHashMap<String, List<DayEntry>>(60, 0.75f, true)
    private val _entriesCacheSnapshot = MutableStateFlow<Map<String, List<DayEntry>>>(emptyMap())
    val entriesCacheSnapshot: StateFlow<Map<String, List<DayEntry>>> = _entriesCacheSnapshot.asStateFlow()

    fun getCachedEntries(date: String): List<DayEntry>? = synchronized(entriesCacheLock) { entriesCache[date] }

    private suspend fun warmCache(dates: List<String>) {
        val toLoad = dates.filter { synchronized(entriesCacheLock) { it !in entriesCache } }
        toLoad.forEach { date ->
            val list = dayEntryRepository.getEntriesForDate(date)
            synchronized(entriesCacheLock) {
                entriesCache[date] = list
                while (entriesCache.size > 60) {
                    entriesCache.remove(entriesCache.keys.first())
                }
                _entriesCacheSnapshot.value = HashMap(entriesCache)
            }
        }
    }

    /** Предзагрузка записей по датам (для плавного переключения дней свайпом). */
    fun requestPreloadDates(dates: List<String>) {
        viewModelScope.launch { warmCache(dates) }
    }

    /** Пользователь уже выбирал дату свайпом — не перезаписывать на today при первом загрузке. */
    private val _hasUserSelectedDate = MutableStateFlow(false)

    /**
     * Список всех календарных дней от самой ранней даты в БД до сегодня включительно.
     * Если БД пуста — только [today]. Хронологический порядок: старые → новые (сегодня последний).
     */
    val availableDates: StateFlow<List<String>> = combine(
        dayEntryRepository.getCurrentDateFlow(),
        dayEntryRepository.getAllDatesFlow()
    ) { todayStr, dbDates ->
        buildAvailableDates(dbDates, todayStr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(LocalDate.now().format(dateFormatter)))

    private fun buildAvailableDates(dbDates: List<String>, todayStr: String): List<String> {
        val today = LocalDate.parse(todayStr, dateFormatter)
        if (dbDates.isEmpty()) return listOf(todayStr)
        val parsed = dbDates.mapNotNull { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }
        val minDate = parsed.minOrNull() ?: return listOf(todayStr)
        val start = if (minDate.isAfter(today)) today else minDate
        val result = mutableListOf<String>()
        var d = start
        while (!d.isAfter(today)) {
            result.add(d.format(dateFormatter))
            d = d.plusDays(1)
        }
        return result
    }

    /** Записи о еде за выбранный день — атомарная пара (date, entries). Для средней плитки. */
    val selectedDayEntriesState: StateFlow<DayEntriesForDate> = effectiveCenterDate
        .flatMapLatest { date ->
            dayEntryRepository.getEntriesForDateFlow(date).map { DayEntriesForDate(date, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayEntriesForDate(LocalDate.now().format(dateFormatter), emptyList()))

    /** Статистика КБЖУ за выбранный день. Единый источник даты: effectiveCenterDate. */
    val kbjuData: StateFlow<KBJUData> = effectiveCenterDate
        .flatMapLatest { targetDate ->
            kbjuRepository.getKbjuForDateFlow(targetDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KBJUData.default())

    // ══════════════════════════════════════════════════════════════════════════
    // Kcal chart data for the top tile graph
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Kcal history for the chart: 21-day window centered on effectiveCenterDate.
     * Includes dates with 0 kcal to ensure continuous chart line.
     */
    val kcalHistory: StateFlow<List<DayKcalPoint>> = effectiveCenterDate
        .flatMapLatest { centerDateStr ->
            val center = LocalDate.parse(centerDateStr, dateFormatter)
            val start = center.minusDays(10)
            val end = center.plusDays(10)
            val startStr = start.format(dateFormatter)
            val endStr = end.format(dateFormatter)
            
            // Generate all dates in range for continuous chart
            val allDates = (0..20).map { start.plusDays(it.toLong()).format(dateFormatter) }
            
            dayEntryRepository.getKcalPerDayFlow(startStr, endStr).map { rawList ->
                val byDate = rawList.associateBy { it.date }
                allDates.map { d -> DayKcalPoint(d, byDate[d]?.kcal ?: 0) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Global maximum daily kcal across entire database.
     * Used for stable Y-axis scaling in the chart.
     */
    val globalMaxKcal: StateFlow<Int> = dayEntryRepository.getGlobalMaxDailyKcalFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    /** Выбирает день для отображения на средней плитке. Сбрасывает превью. */
    fun selectDay(date: String) {
        _hasUserSelectedDate.value = true
        _selectedDate.value = date
    }

    init {
        viewModelScope.launch {
            var hasInitializedDate = false
            availableDates.collect { dates ->
                if (dates.isEmpty()) return@collect
                if (!hasInitializedDate) {
                    hasInitializedDate = true
                    if (!_hasUserSelectedDate.value) {
                        _selectedDate.value = dates.last()
                    }
                }
                val sel = _selectedDate.value
                if (sel != null && sel !in dates) {
                    _selectedDate.value = dates.last()
                }
            }
        }
        viewModelScope.launch {
            effectiveCenterDate.collect { center ->
                val c = LocalDate.parse(center, dateFormatter)
                val windowDates = (0..6).map { c.minusDays(3).plusDays(it.toLong()).format(dateFormatter) }
                warmCache(windowDates)
            }
        }
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

    /**
     * Явно задать активную плитку (например, после snap разделителя TOP↔MIDDLE).
     * @param position TOP, MIDDLE, BOTTOM или null (сбалансировано / ни одна не активна).
     */
    fun setActiveTile(position: TilePosition?) {
        _activeTile.value = position
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
            _chatMessages.value = _chatMessages.value + ChatMessage(UUID.randomUUID().toString(), ChatRole.USER, trimmed)
            _chatLoading.value = true

            // Обрабатываем через оркестратор
            when (val result = agentOrchestrator.processMessage(trimmed)) {
                is OrchestratorResult.Success -> {
                    // Формируем ответ для отображения
                    val responseText = buildResponseText(result)
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        UUID.randomUUID().toString(),
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
     * Заполняет БД тестовыми данными о еде за последние 7 дней.
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
            for (daysAgo in 0..6) {
                val date = today.minusDays(daysAgo.toLong()).format(dateFormatter)
                // Выбираем 3-5 случайных блюд для каждого дня
                val dayFoods = testFoods.shuffled().take((3..5).random())
                dayEntryRepository.addEntriesForDate(date, dayFoods)
            }

            _hasUserSelectedDate.value = true
            val todayStr = today.format(dateFormatter)
            if (_selectedDate.value == todayStr) {
                _selectedDate.value = null
            }
            _selectedDate.value = todayStr

            // Добавляем сообщение в чат
            _chatMessages.value = _chatMessages.value + ChatMessage(
                UUID.randomUUID().toString(),
                ChatRole.ASSISTANT,
                "🧪 Добавлены тестовые данные за 7 дней. Попробуйте свайпы на средней плитке!"
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
            _chatMessages.value = _chatMessages.value + ChatMessage(UUID.randomUUID().toString(), ChatRole.ASSISTANT, it)
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
