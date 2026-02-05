package com.example.vitanlyapp.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import dev.chrisbanes.haze.rememberHazeState

// Плавное замедление в конце: cubic-bezier(0.22, 0.61, 0.36, 1)
private val smoothEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

private val tileAnimationSpec = tween<Float>(
    durationMillis = DesignTokens.tileTransitionDurationMs,
    easing = smoothEasing
)

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onResetData: () -> Unit = {}
) {
    val activeTile by viewModel.activeTile.collectAsStateWithLifecycle()
    val kbjuData by viewModel.kbjuData.collectAsStateWithLifecycle()
    val currentWeight by viewModel.currentWeight.collectAsStateWithLifecycle()
    val activityCoefficient by viewModel.activityCoefficient.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val todayEntries by viewModel.todayEntries.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val scheme = when (themeMode) {
        ThemeMode.CLASSIC -> AppColorSchemes.Classic
        ThemeMode.WARM_DARK -> AppColorSchemes.WarmDark
    }
    
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
        animationSpec = tileAnimationSpec
    )
    val weightMiddle by animateFloatAsState(
        targetValue = when {
            activeTile == TilePosition.MIDDLE -> DesignTokens.tileWeightExpanded
            activeTile != null -> DesignTokens.tileWeightCollapsed
            else -> DesignTokens.tileWeightIdleTopMiddle  // idle = 1f
        },
        animationSpec = tileAnimationSpec
    )
    val weightBottom by animateFloatAsState(
        targetValue = when {
            activeTile == TilePosition.BOTTOM -> DesignTokens.tileWeightExpanded
            activeTile != null -> DesignTokens.tileWeightCollapsed
            else -> DesignTokens.tileWeightIdleBottom  // idle = 0.18f
        },
        animationSpec = tileAnimationSpec
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
            val safeBottom = WindowInsets.safeDrawing.getBottom(density)
            val isExpandedLayout = maxWidth >= DesignTokens.expandedLayoutBreakpoint

            // Отслеживание клавиатуры
            val imeBottom = WindowInsets.ime.getBottom(density)
            val isKeyboardVisible = imeBottom > 0

            Box(modifier = Modifier.fillMaxSize()) {
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
                        todayEntries = todayEntries,
                        userProfile = userProfile,
                        onEntryClick = { selectedEntry = it },
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
                        kcalStat = kcalStat,
                        macroStats = macroStats,
                        currentWeight = currentWeight,
                        activityCoefficient = activityCoefficient,
                        chatMessages = chatMessages,
                        chatLoading = chatLoading,
                        todayEntries = todayEntries,
                        userProfile = userProfile,
                        onEntryClick = { selectedEntry = it },
                        viewModel = viewModel
                    )
                }

                // Кнопки управления — правый верхний угол (только при развёрнутой верхней плитке)
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    AnimatedVisibility(
                        visible = activeTile == TilePosition.TOP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Кнопка сброса данных
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { showResetDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Сбросить данные",
                                    tint = scheme.textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Кнопка переключения темы
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { viewModel.toggleTheme() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Переключить тему",
                                    tint = scheme.textColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
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
    kcalStat: KbjuBarStat,
    macroStats: List<KbjuBarStat>,
    currentWeight: Float,
    activityCoefficient: Float,
    chatMessages: List<ChatMessage>,
    chatLoading: Boolean,
    todayEntries: List<DayEntry>,
    userProfile: UserProfile?,
    onEntryClick: (DayEntry) -> Unit,
    viewModel: MainViewModel
) {
    val density = LocalDensity.current
    
    // HazeState для glassmorphism плашек веса/активности в верхней плитке
    val kbjuHazeState = rememberHazeState()

    // Когда клавиатура видна и чат раскрыт — плитка чата перекрывает остальные
    val chatFullScreen = isKeyboardVisible && activeTile == TilePosition.BOTTOM
    
    // Чат раскрыт (не свёрнут)
    val chatExpanded = activeTile == TilePosition.BOTTOM || chatFullScreen
    
    // Высота нижней плитки: фиксированная в idle, fullscreen при раскрытии
    // Включает safe area снизу чтобы уходить за край экрана
    val safeBottomDp = with(density) { safeBottomPx.toDp() }
    val safeTopDp = with(density) { safeTopPx.toDp() }
    
    // Минимальная высота плитки чата (включая safe area снизу)
    val chatMinHeight = DesignTokens.chatTileMinHeight + safeBottomDp

    Box(modifier = Modifier.fillMaxSize()) {
        // Слой 1: верхние плитки (TOP и MIDDLE) — скрыты когда чат раскрыт
        if (!chatExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DesignTokens.screenPadding)
                    .padding(top = safeTopDp + DesignTokens.screenPadding)
                    .padding(bottom = chatMinHeight)
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
                    KbjuTileContent(
                        kcalStat = kcalStat,
                        macroStats = macroStats,
                        activeTile = activeTile,
                        userProfile = userProfile,
                        hazeState = kbjuHazeState
                    )
                }

                Tile(
                    position = TilePosition.MIDDLE,
                    isExpanded = activeTile == TilePosition.MIDDLE,
                    isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                    onClick = { viewModel.onTileClick(TilePosition.MIDDLE) },
                    modifier = Modifier.weight(weightMiddle)
                ) {
                    InputTileContent(
                        entries = todayEntries,
                        isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                        onEntryClick = onEntryClick
                    )
                }
            }
        }

        // Слой 2: плитка чата — edge-to-edge, уходит за нижний край экрана
        // При раскрытии — на весь экран (fullscreen), иначе — фиксированная минимальная высота
        Tile(
            position = TilePosition.BOTTOM,
            isExpanded = chatExpanded,
            isCollapsed = false, // Нижняя плитка никогда не "схлопывается" визуально
            onClick = { viewModel.onTileClick(TilePosition.BOTTOM) },
            shape = if (chatExpanded) RoundedCornerShape(0.dp) else bottomTileShape,
            edgeToEdge = true,
            modifier = if (chatExpanded) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(chatMinHeight)
            }
        ) {
            BottomTileContent(
                messages = chatMessages,
                onSendMessage = viewModel::sendChatMessage,
                isLoading = chatLoading,
                isCollapsed = !chatExpanded,
                bottomPadding = safeBottomDp,
                onExpandRequest = { viewModel.onTileClick(TilePosition.BOTTOM) }
            )
        }
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
    todayEntries: List<DayEntry>,
    userProfile: UserProfile?,
    onEntryClick: (DayEntry) -> Unit,
    viewModel: MainViewModel
) {
    // HazeState для glassmorphism плашек веса/активности в верхней плитке
    val kbjuHazeState = rememberHazeState()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(DesignTokens.expandedLayoutLeftPanelWeight)) {
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
                KbjuTileContent(
                    kcalStat = kcalStat,
                    macroStats = macroStats,
                    activeTile = activeTile,
                    userProfile = userProfile,
                    hazeState = kbjuHazeState
                )
            }

            Tile(
                position = TilePosition.MIDDLE,
                isExpanded = activeTile == TilePosition.MIDDLE,
                isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                onClick = { viewModel.onTileClick(TilePosition.MIDDLE) },
                modifier = Modifier.weight(weightMiddle)
            ) {
                InputTileContent(
                    entries = todayEntries,
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
                    onSendMessage = viewModel::sendChatMessage,
                    isLoading = chatLoading
                )
            }
        }
    }
}
