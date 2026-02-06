package com.example.vitanlyapp.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitanlyapp.domain.model.ChatMessage
import com.example.vitanlyapp.domain.model.KBJUData
import com.example.vitanlyapp.domain.model.KbjuBarStat
import com.example.vitanlyapp.domain.model.ThemeMode
import com.example.vitanlyapp.domain.model.TilePosition
import com.example.vitanlyapp.domain.repository.DayEntry
import com.example.vitanlyapp.domain.repository.UserProfile
import com.example.vitanlyapp.ui.component.Tile
import com.example.vitanlyapp.ui.design.AppColorSchemes
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.example.vitanlyapp.ui.update.UpdateDialog
import com.example.vitanlyapp.ui.update.UpdateViewModel
import dev.chrisbanes.haze.rememberHazeState

// Плавное замедление в конце: cubic-bezier(0.22, 0.61, 0.36, 1)
private val smoothEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

private fun <T> tileAnimationSpec() = tween<T>(
    durationMillis = DesignTokens.tileTransitionDurationMs,
    easing = smoothEasing
)

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
    onResetData: () -> Unit = {}
) {
    val activeTile by viewModel.activeTile.collectAsStateWithLifecycle()
    val kbjuData by viewModel.kbjuData.collectAsStateWithLifecycle()
    val currentWeight by viewModel.currentWeight.collectAsStateWithLifecycle()
    val activityCoefficient by viewModel.activityCoefficient.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    // Навигация по дням на средней плитке
    val selectedDayEntries by viewModel.selectedDayEntries.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableDates.collectAsStateWithLifecycle()

    val scheme = when (themeMode) {
        ThemeMode.CLASSIC -> AppColorSchemes.Classic
        ThemeMode.WARM_DARK -> AppColorSchemes.WarmDark
    }

    // Проверка обновлений при запуске
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates()
    }

    // Диалог обновления
    UpdateDialog(viewModel = updateViewModel)
    
    // Жест "назад" сворачивает плитку вместо закрытия приложения
    BackHandler(enabled = activeTile != null) {
        viewModel.onTileClick(activeTile!!) // Повторный клик сворачивает плитку
    }

    // Делаем иконки строки состояния читаемыми: тёмные на светлом фоне (Classic), светлые на тёмном (WarmDark)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = (themeMode == ThemeMode.CLASSIC)
    }

    // Диалог подтверждения сброса данных
    var showResetDialog by remember { mutableStateOf(false) }

    // Диалог действий с продуктом
    var selectedEntry by remember { mutableStateOf<DayEntry?>(null) }

    selectedEntry?.let { entry ->
        FoodEntryActionDialog(
            entry = entry,
            onAction = { action ->
                when (action) {
                    is FoodEntryAction.Delete -> viewModel.deleteEntry(entry)
                    is FoodEntryAction.Repeat -> viewModel.repeatEntry(entry)
                    is FoodEntryAction.UpdateWeight -> viewModel.updateEntryWeight(entry, action.newWeight)
                }
            },
            onDismiss = { selectedEntry = null }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Сбросить все данные?",
                    fontFamily = DesignTokens.fontFamilyPlank
                )
            },
            text = {
                Text(
                    text = "Это удалит профиль, записи о еде и историю чата. Вам нужно будет пройти настройку заново.",
                    fontFamily = DesignTokens.fontFamilyPlank
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetAllData { onResetData() }
                    }
                ) {
                    Text("Удалить", fontFamily = DesignTokens.fontFamilyPlank)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена", fontFamily = DesignTokens.fontFamilyPlank)
                }
            }
        )
    }

    // Новая логика весов: в idle TOP и MIDDLE равные и большие, BOTTOM минимальный
    val weightTop by animateFloatAsState(
        targetValue = when {
            activeTile == TilePosition.TOP -> DesignTokens.tileWeightExpanded
            activeTile != null -> DesignTokens.tileWeightCollapsed
            else -> DesignTokens.tileWeightIdleTopMiddle  // idle = 1f
        },
        animationSpec = tileAnimationSpec()
    )
    val weightMiddle by animateFloatAsState(
        targetValue = when {
            activeTile == TilePosition.MIDDLE -> DesignTokens.tileWeightExpanded
            activeTile != null -> DesignTokens.tileWeightCollapsed
            else -> DesignTokens.tileWeightIdleTopMiddle  // idle = 1f
        },
        animationSpec = tileAnimationSpec()
    )
    val weightBottom by animateFloatAsState(
        targetValue = when {
            activeTile == TilePosition.BOTTOM -> DesignTokens.tileWeightExpanded
            activeTile != null -> DesignTokens.tileWeightCollapsed
            else -> DesignTokens.tileWeightIdleBottom  // idle = 0.18f
        },
        animationSpec = tileAnimationSpec()
    )

    // Статистика КБЖУ
    val barStats = viewModel.getBarStats(kbjuData)
    val kcalStat = barStats.firstOrNull() ?: KbjuBarStat(0f, false, 0, 0)
    val macroStats = barStats.drop(1)

    CompositionLocalProvider(LocalAppColorScheme provides scheme) {
        // Фон на весь экран (включая строку состояния), чтобы иконки были читаемы в обеих темах
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (scheme.screenBackgroundBrush != null) {
                        Modifier.background(scheme.screenBackgroundBrush!!)
                    } else {
                        Modifier.background(scheme.screenBackground)
                    }
                )
        ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            // Safe area insets — применяем только к верхним плиткам, нижняя игнорирует
            val density = LocalDensity.current
            val safeTop = WindowInsets.safeDrawing.getTop(density)
            val isExpandedLayout = maxWidth >= DesignTokens.expandedLayoutBreakpoint

            // Отслеживание клавиатуры
            val imeBottom = WindowInsets.ime.getBottom(density)
            val isKeyboardVisible = imeBottom > 0
            
            // Для нижнего safe area используем navigationBars (не safeDrawing!)
            // safeDrawing на Huawei возвращает неправильные значения
            val safeBottom = WindowInsets.navigationBars.getBottom(density)

            if (isExpandedLayout) {
                ExpandedLayout(
                    weightTop = weightTop,
                    weightMiddle = weightMiddle,
                    activeTile = activeTile,
                    kcalStat = kcalStat,
                    macroStats = macroStats,
                    currentWeight = currentWeight,
                    activityCoefficient = activityCoefficient,
                    chatMessages = chatMessages,
                    chatLoading = chatLoading,
                    selectedDayEntries = selectedDayEntries,
                    availableDates = availableDates,
                    userProfile = userProfile,
                    onEntryClick = { selectedEntry = it },
                    onShowResetDialog = { showResetDialog = true },
                    onToggleTheme = { viewModel.toggleTheme() },
                    viewModel = viewModel
                )
            } else {
                CompactLayout(
                    weightTop = weightTop,
                    weightMiddle = weightMiddle,
                    activeTile = activeTile,
                    isKeyboardVisible = isKeyboardVisible,
                    safeTopPx = safeTop,
                    safeBottomPx = safeBottom,
                    imeBottomPx = imeBottom,
                    kcalStat = kcalStat,
                    macroStats = macroStats,
                    currentWeight = currentWeight,
                    activityCoefficient = activityCoefficient,
                    chatMessages = chatMessages,
                    chatLoading = chatLoading,
                    selectedDayEntries = selectedDayEntries,
                    availableDates = availableDates,
                    userProfile = userProfile,
                    onEntryClick = { selectedEntry = it },
                    onShowResetDialog = { showResetDialog = true },
                    onToggleTheme = { viewModel.toggleTheme() },
                    viewModel = viewModel
                )
            }
        }
        }
    }
}

// Форма нижней плитки — скругление только сверху
private val bottomTileShape = RoundedCornerShape(
    topStart = DesignTokens.bottomTileCornerRadius,
    topEnd = DesignTokens.bottomTileCornerRadius,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

@Composable
private fun CompactLayout(
    weightTop: Float,
    weightMiddle: Float,
    activeTile: TilePosition?,
    isKeyboardVisible: Boolean,
    safeTopPx: Int,
    safeBottomPx: Int,
    imeBottomPx: Int,
    kcalStat: KbjuBarStat,
    macroStats: List<KbjuBarStat>,
    currentWeight: Float,
    activityCoefficient: Float,
    chatMessages: List<ChatMessage>,
    chatLoading: Boolean,
    selectedDayEntries: List<DayEntry>,
    availableDates: List<String>,
    userProfile: UserProfile?,
    onEntryClick: (DayEntry) -> Unit,
    onShowResetDialog: () -> Unit,
    onToggleTheme: () -> Unit,
    viewModel: MainViewModel
) {
    val density = LocalDensity.current
    
    // HazeState для glassmorphism плашек веса/активности в верхней плитке
    val kbjuHazeState = rememberHazeState()
    
    // Текст для автозаполнения поля ввода (из подсказки)
    var chatPrefillText by remember { mutableStateOf("") }

    // Когда клавиатура видна и чат раскрыт — плитка чата перекрывает остальные
    val chatFullScreen = isKeyboardVisible && activeTile == TilePosition.BOTTOM
    
    // Чат раскрыт (не свёрнут)
    val chatExpanded = activeTile == TilePosition.BOTTOM || chatFullScreen
    
    // Высота нижней плитки: фиксированная в idle, fullscreen при раскрытии
    // Включает safe area снизу чтобы уходить за край экрана
    val safeBottomDp = with(density) { safeBottomPx.toDp() }
    val safeTopDp = with(density) { safeTopPx.toDp() }
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    
    // Минимальная высота плитки чата (включая safe area снизу)
    val chatMinHeight = DesignTokens.chatTileMinHeight + safeBottomDp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        
        // Анимация высоты плитки чата
        val chatTileHeight by animateDpAsState(
            targetValue = if (chatExpanded) screenHeight else chatMinHeight,
            animationSpec = tileAnimationSpec(),
            label = "chatTileHeight"
        )
        
        // Анимация скругления углов (от 24dp до 0dp)
        val cornerRadius by animateDpAsState(
            targetValue = if (chatExpanded) 0.dp else DesignTokens.bottomTileCornerRadius,
            animationSpec = tileAnimationSpec(),
            label = "cornerRadius"
        )
        
        val animatedShape = RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        
        // Оптимизация: верхние плитки активны только когда чат почти свёрнут
        // Порог — когда высота плитки чата меньше минимальной + 20% от разницы
        val expansionThreshold = chatMinHeight + (screenHeight - chatMinHeight) * 0.15f
        val topTilesActive = chatTileHeight < expansionThreshold
        
        // Слой 1: верхние плитки (TOP и MIDDLE) — всегда отображаются на заднем плане
        // Плитка чата плавно перекрывает их при разворачивании
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.screenPadding)
                .padding(top = safeTopDp + DesignTokens.screenPadding)
                .padding(bottom = chatMinHeight + DesignTokens.tileSpacing),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.tileSpacing)
        ) {
            Tile(
                position = TilePosition.TOP,
                isExpanded = activeTile == TilePosition.TOP,
                isCollapsed = activeTile != null && activeTile != TilePosition.TOP,
                onClick = { viewModel.onTileClick(TilePosition.TOP) },
                modifier = Modifier.weight(weightTop),
                overflowContent = {
                    KbjuTileWheelOverflowContent(
                        isExpanded = activeTile == TilePosition.TOP,
                        currentWeight = currentWeight,
                        onWeightChange = viewModel::updateWeight,
                        activityCoefficient = activityCoefficient,
                        onActivityChange = viewModel::updateActivityCoefficient,
                        hazeState = kbjuHazeState
                    )
                }
            ) {
                val scheme = LocalAppColorScheme.current
                Box(modifier = Modifier.fillMaxSize()) {
                    KbjuTileContent(
                        kcalStat = kcalStat,
                        macroStats = macroStats,
                        activeTile = activeTile,
                        userProfile = userProfile,
                        hazeState = kbjuHazeState,
                        isActive = topTilesActive
                    )
                    
                    // Кнопки управления — правый верхний угол (только при развёрнутой плитке)
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = activeTile == TilePosition.TOP,
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(100))
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Кнопка сброса данных
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onShowResetDialog() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Сбросить данные",
                                        tint = scheme.textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Кнопка переключения темы
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onToggleTheme() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Переключить тему",
                                        tint = scheme.textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Кнопка заполнения тестовых данных
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { viewModel.populateTestData() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = "Тестовые данные",
                                        tint = scheme.textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Tile(
                position = TilePosition.MIDDLE,
                isExpanded = activeTile == TilePosition.MIDDLE,
                isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                onClick = { viewModel.onTileClick(TilePosition.MIDDLE) },
                modifier = Modifier.weight(weightMiddle)
            ) {
                // Индекс выбранного дня (сегодня = последний, т.к. даты отсортированы по возрастанию)
                var selectedDateIndex by remember { mutableStateOf(availableDates.lastIndex.coerceAtLeast(0)) }
                
                // Обновляем индекс когда список дат меняется (например, после загрузки тестовых данных)
                LaunchedEffect(availableDates.size) {
                    if (selectedDateIndex >= availableDates.size || selectedDateIndex == 0) {
                        selectedDateIndex = availableDates.lastIndex.coerceAtLeast(0)
                    }
                }
                
                InputTileContent(
                    entries = selectedDayEntries,
                    availableDates = availableDates,
                    selectedDateIndex = selectedDateIndex,
                    onDaySelected = { index ->
                        selectedDateIndex = index
                        if (index < availableDates.size) {
                            viewModel.selectDay(availableDates[index])
                        }
                    },
                    isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                    onEntryClick = onEntryClick,
                    isActive = topTilesActive
                )
            }
        }

        // Слой 2: плитка чата — edge-to-edge, уходит за нижний край экрана
        // Анимированная высота и скругление при разворачивании/сворачивании
        Tile(
            position = TilePosition.BOTTOM,
            isExpanded = chatExpanded,
            isCollapsed = false, // Нижняя плитка никогда не "схлопывается" визуально
            onClick = { viewModel.onTileClick(TilePosition.BOTTOM) },
            shape = animatedShape,
            edgeToEdge = true,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(chatTileHeight)
        ) {
            BottomTileContent(
                messages = chatMessages,
                isLoading = chatLoading,
                isCollapsed = !chatExpanded,
                bottomPadding = safeBottomDp,
                imeBottomPadding = imeBottomDp,
                onHintClick = { hint -> chatPrefillText = hint }
            )
        }
        
        // Слой 3: блок ввода — всегда внизу экрана
        // imePadding() поднимает блок над клавиатурой
        ChatInputBlock(
            onSendMessage = viewModel::sendChatMessage,
            isLoading = chatLoading,
            isCollapsed = !chatExpanded,
            bottomPadding = safeBottomDp,
            onExpandRequest = { viewModel.onTileClick(TilePosition.BOTTOM) },
            prefillText = chatPrefillText,
            onPrefillConsumed = { chatPrefillText = "" },
            modifier = Modifier
                .imePadding()
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ExpandedLayout(
    weightTop: Float,
    weightMiddle: Float,
    activeTile: TilePosition?,
    kcalStat: KbjuBarStat,
    macroStats: List<KbjuBarStat>,
    currentWeight: Float,
    activityCoefficient: Float,
    chatMessages: List<ChatMessage>,
    chatLoading: Boolean,
    selectedDayEntries: List<DayEntry>,
    availableDates: List<String>,
    userProfile: UserProfile?,
    onEntryClick: (DayEntry) -> Unit,
    onShowResetDialog: () -> Unit,
    onToggleTheme: () -> Unit,
    viewModel: MainViewModel
) {
    // HazeState для glassmorphism плашек веса/активности в верхней плитке
    val kbjuHazeState = rememberHazeState()
    
    // Текст для автозаполнения поля ввода (из подсказки)
    var chatPrefillText by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(DesignTokens.expandedLayoutLeftPanelWeight),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.tileSpacing)
        ) {
            Tile(
                position = TilePosition.TOP,
                isExpanded = activeTile == TilePosition.TOP,
                isCollapsed = activeTile != null && activeTile != TilePosition.TOP,
                onClick = { viewModel.onTileClick(TilePosition.TOP) },
                modifier = Modifier.weight(weightTop),
                overflowContent = {
                    KbjuTileWheelOverflowContent(
                        isExpanded = activeTile == TilePosition.TOP,
                        currentWeight = currentWeight,
                        onWeightChange = viewModel::updateWeight,
                        activityCoefficient = activityCoefficient,
                        onActivityChange = viewModel::updateActivityCoefficient,
                        hazeState = kbjuHazeState
                    )
                }
            ) {
                val scheme = LocalAppColorScheme.current
                Box(modifier = Modifier.fillMaxSize()) {
                    KbjuTileContent(
                        kcalStat = kcalStat,
                        macroStats = macroStats,
                        activeTile = activeTile,
                        userProfile = userProfile,
                        hazeState = kbjuHazeState
                    )
                    
                    // Кнопки управления — правый верхний угол (только при развёрнутой плитке)
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = activeTile == TilePosition.TOP,
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(100))
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Кнопка сброса данных
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onShowResetDialog() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Сбросить данные",
                                        tint = scheme.textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Кнопка переключения темы
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onToggleTheme() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Переключить тему",
                                        tint = scheme.textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Кнопка заполнения тестовых данных
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { viewModel.populateTestData() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = "Тестовые данные",
                                        tint = scheme.textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Tile(
                position = TilePosition.MIDDLE,
                isExpanded = activeTile == TilePosition.MIDDLE,
                isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                onClick = { viewModel.onTileClick(TilePosition.MIDDLE) },
                modifier = Modifier.weight(weightMiddle)
            ) {
                // Индекс выбранного дня (сегодня = последний, т.к. даты отсортированы по возрастанию)
                var selectedDateIndex by remember { mutableStateOf(availableDates.lastIndex.coerceAtLeast(0)) }
                
                // Обновляем индекс когда список дат меняется
                LaunchedEffect(availableDates.size) {
                    if (selectedDateIndex >= availableDates.size || selectedDateIndex == 0) {
                        selectedDateIndex = availableDates.lastIndex.coerceAtLeast(0)
                    }
                }
                
                InputTileContent(
                    entries = selectedDayEntries,
                    availableDates = availableDates,
                    selectedDateIndex = selectedDateIndex,
                    onDaySelected = { index ->
                        selectedDateIndex = index
                        if (index < availableDates.size) {
                            viewModel.selectDay(availableDates[index])
                        }
                    },
                    isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                    onEntryClick = onEntryClick
                )
            }
        }

        Box(modifier = Modifier.weight(DesignTokens.expandedLayoutRightPanelWeight)) {
            Tile(
                position = TilePosition.BOTTOM,
                isExpanded = true,
                isCollapsed = false,
                onClick = { viewModel.onTileClick(TilePosition.BOTTOM) },
                modifier = Modifier.fillMaxSize()
            ) {
                BottomTileContent(
                    messages = chatMessages,
                    isLoading = chatLoading,
                    isCollapsed = false,
                    onHintClick = { hint -> chatPrefillText = hint }
                )
            }
            
            // Блок ввода поверх плитки чата
            ChatInputBlock(
                onSendMessage = viewModel::sendChatMessage,
                isLoading = chatLoading,
                isCollapsed = false,
                prefillText = chatPrefillText,
                onPrefillConsumed = { chatPrefillText = "" },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
