package com.example.vitanlyapp.ui.screen.main

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import android.app.Activity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.vitanlyapp.ui.component.NoiseOverlay
import com.example.vitanlyapp.ui.component.Tile
import com.example.vitanlyapp.ui.design.AppColorSchemes
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.example.vitanlyapp.ui.update.UpdateDialog
import com.example.vitanlyapp.ui.update.UpdateViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Плавное замедление в конце: cubic-bezier(0.22, 0.61, 0.36, 1)
private val smoothEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

private fun <T> tileAnimationSpec() = tween<T>(
    durationMillis = DesignTokens.tileTransitionDurationMs,
    easing = smoothEasing
)

// Одна быстрая анимация магнита при отпускании ручки TOP/MIDDLE (без последующих переходов)
private fun <T> splitterSnapSpec() = tween<T>(
    durationMillis = DesignTokens.topMiddleHandleSnapDurationMs,
    easing = smoothEasing
)

// Анимация settle для графика калорий и пейджера дней
private fun <T> chartSettleSpec() = tween<T>(
    durationMillis = DesignTokens.chartSettleDurationMs,
    easing = smoothEasing
)

/**
 * Shared animation state for day navigation.
 * Both the middle tile (day pager) and top tile (kcal chart) read this same Animatable
 * to ensure frame-perfect synchronization during swipes.
 */
class DayNavigationState(
    val position: Animatable<Float, *>,
    val dateCount: Int,
    private val coroutineScope: CoroutineScope
) {
    /** Current settled day index (rounded from fractional position) */
    val currentDayIndex: Int get() = position.value.roundToInt().coerceIn(0, (dateCount - 1).coerceAtLeast(0))
    
    /** Fractional offset from current day (-0.5 to 0.5 when between days) */
    val offsetFraction: Float get() = position.value - currentDayIndex
    
    /** Animate to a specific day index with smooth easing */
    suspend fun animateToDay(index: Int) {
        val targetIndex = index.coerceIn(0, (dateCount - 1).coerceAtLeast(0))
        position.animateTo(
            targetIndex.toFloat(),
            chartSettleSpec()
        )
    }
    
    /** Immediately snap to a fractional position (during drag). Uses Main.immediate so it runs in the same frame. */
    fun snapTo(fractionalIndex: Float) {
        val clamped = fractionalIndex.coerceIn(0f, (dateCount - 1).coerceAtLeast(0).toFloat())
        coroutineScope.launch(Dispatchers.Main.immediate) { position.snapTo(clamped) }
    }
    
    /** Animate with velocity-based fling (velocity in pages/s). Higher velocity = shorter duration for snappier feel. */
    suspend fun settleWithVelocity(velocityPagesPerSec: Float) {
        val currentPos = position.value
        val currentIndex = currentPos.roundToInt()
        val flingThresholdPagesPerSec = 0.5f

        val targetIndex = when {
            velocityPagesPerSec < -flingThresholdPagesPerSec -> (currentIndex + 1).coerceAtMost((dateCount - 1).coerceAtLeast(0))
            velocityPagesPerSec > flingThresholdPagesPerSec -> (currentIndex - 1).coerceAtLeast(0)
            else -> currentPos.roundToInt().coerceIn(0, (dateCount - 1).coerceAtLeast(0))
        }

        val absVel = kotlin.math.abs(velocityPagesPerSec)
        val durationMs = (220 - kotlin.math.min(140f, absVel * 80f).toInt()).coerceIn(80, 220)
        position.animateTo(
            targetIndex.toFloat(),
            tween(durationMillis = durationMs, easing = smoothEasing)
        )
    }
}

/**
 * Remember a DayNavigationState that stays synchronized with the available dates.
 */
@Composable
fun rememberDayNavigationState(
    initialIndex: Int,
    dateCount: Int
): DayNavigationState {
    val coroutineScope = rememberCoroutineScope()
    val position = remember { Animatable(initialIndex.toFloat()) }
    
    // Update position when external index changes (e.g., from ViewModel)
    LaunchedEffect(initialIndex, dateCount) {
        if (dateCount > 0 && position.value.roundToInt() != initialIndex) {
            position.snapTo(initialIndex.toFloat())
        }
    }
    
    return remember(dateCount, coroutineScope) {
        DayNavigationState(position, dateCount, coroutineScope)
    }
}

@Composable
@Suppress("UnusedBoxWithConstraintsScope")
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
    val entriesCache by viewModel.entriesCacheSnapshot.collectAsStateWithLifecycle()

    // Навигация по дням на средней плитке (selectedDate — источник правды)
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDayEntriesState by viewModel.selectedDayEntriesState.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableDates.collectAsStateWithLifecycle()
    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val selectedDateIndex = availableDates.indexOf(selectedDate ?: todayStr).coerceIn(0, (availableDates.size - 1).coerceAtLeast(0))

    // Kcal chart data
    val kcalHistory by viewModel.kcalHistory.collectAsStateWithLifecycle()
    val globalMaxKcal by viewModel.globalMaxKcal.collectAsStateWithLifecycle()

    // Shared day navigation state for sync between middle tile (pager) and top tile (chart)
    val dayNavigationState = rememberDayNavigationState(
        initialIndex = selectedDateIndex,
        dateCount = availableDates.size
    )

    val onDaySettled: (Int) -> Unit = { newPage ->
        if (newPage in availableDates.indices) {
            val date = availableDates[newPage]
            KcalTraceLog.traceIfChanged("SETTLED", "page=$newPage date=$date")
            viewModel.selectDay(date)
        }
    }

    // Предзагрузка записей для текущей и соседних страниц (моментальный переход при свайпе)
    LaunchedEffect(dayNavigationState.currentDayIndex, availableDates) {
        val center = dayNavigationState.currentDayIndex
        availableDates.getOrNull(center)?.let { centerDate ->
            if (selectedDate != centerDate) {
                viewModel.selectDay(centerDate)
            }
        }
        val start = (center - 2).coerceAtLeast(0)
        val end = (center + 2).coerceAtMost((availableDates.size - 1).coerceAtLeast(0))
        val datesToPreload = (start..end).mapNotNull { availableDates.getOrNull(it) }
        if (datesToPreload.isNotEmpty()) viewModel.requestPreloadDates(datesToPreload)
    }
    
    LaunchedEffect(selectedDate, selectedDateIndex) {
        KcalTraceLog.traceIfChanged("MAIN_SYNC", "selectedDate=$selectedDate,selectedDateIndex=$selectedDateIndex")
    }

    val scheme = when (themeMode) {
        ThemeMode.CLASSIC -> AppColorSchemes.Classic
        ThemeMode.WARM_DARK -> AppColorSchemes.WarmDark
        ThemeMode.MATTE_DARK -> AppColorSchemes.MatteDark
    }

    // Проверка обновлений при запуске
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates()
    }

    // Диалог обновления
    UpdateDialog(viewModel = updateViewModel)
    
    // Жест "назад": если клавиатура открыта — только закрыть её; иначе — свернуть плитку
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler(enabled = activeTile != null) {
        if (activeTile == TilePosition.BOTTOM && isKeyboardVisible) {
            keyboardController?.hide()
        } else {
            viewModel.onTileClick(activeTile!!)
        }
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

    // Full-screen expanding card overlay
    var overlayMounted by remember { mutableStateOf(false) }
    var overlayEntry by remember { mutableStateOf<DayEntry?>(null) }

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

    val overlayHazeState = rememberHazeState()

    CompositionLocalProvider(LocalAppColorScheme provides scheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val safeTop = WindowInsets.safeDrawing.getTop(density)
                val isExpandedLayout = maxWidth >= DesignTokens.expandedLayoutBreakpoint
                val imeBottom = WindowInsets.ime.getBottom(density)
                val isKeyboardVisible = imeBottom > 0
                val safeBottom = WindowInsets.navigationBars.getBottom(density)
                Box(modifier = Modifier.fillMaxSize().hazeSource(overlayHazeState)) {
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
                    )
                    if (themeMode == ThemeMode.MATTE_DARK) {
                        NoiseOverlay(
                            noiseAlpha = 0.025f,
                            noiseDensity = 0.12f
                        )
                    }
                    NoiseOverlay(
                        noiseAlpha = DesignTokens.globalDitherNoiseAlpha,
                        noiseDensity = DesignTokens.globalDitherNoiseDensity
                    )
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
                            selectedDate = selectedDate,
                            selectedDayEntriesState = selectedDayEntriesState,
                            availableDates = availableDates,
                            entriesCache = entriesCache,
                            userProfile = userProfile,
                            dayNavigationState = dayNavigationState,
                            kcalHistory = kcalHistory,
                            globalMaxKcal = globalMaxKcal,
                            onDaySettled = onDaySettled,
                            onEntryClick = { selectedEntry = it },
                            onEntryExpandRequest = { entry, _ ->
                                overlayEntry = entry
                                overlayMounted = true
                            },
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
                            selectedDate = selectedDate,
                            selectedDayEntriesState = selectedDayEntriesState,
                            availableDates = availableDates,
                            entriesCache = entriesCache,
                            userProfile = userProfile,
                            dayNavigationState = dayNavigationState,
                            kcalHistory = kcalHistory,
                            globalMaxKcal = globalMaxKcal,
                            onDaySettled = onDaySettled,
                            onEntryClick = { selectedEntry = it },
                            onEntryExpandRequest = { entry, _ ->
                                overlayEntry = entry
                                overlayMounted = true
                            },
                            onShowResetDialog = { showResetDialog = true },
                            onToggleTheme = { viewModel.toggleTheme() },
                            viewModel = viewModel
                        )
                    }
                }

                if (overlayMounted && overlayEntry != null) {
                    ExpandingCardOverlay(
                        entry = overlayEntry!!,
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                        overlayHazeState = overlayHazeState,
                        onFullyDismissed = {
                            overlayEntry = null
                            overlayMounted = false
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandingCardOverlay(
    entry: DayEntry,
    maxWidth: Dp,
    maxHeight: Dp,
    overlayHazeState: HazeState,
    onFullyDismissed: () -> Unit,
    viewModel: MainViewModel
) {
    val scheme = LocalAppColorScheme.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val cardShape = RoundedCornerShape(DesignTokens.expandedFoodCardCornerRadius)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(scheme.borderGradientTop, scheme.borderGradientBottom)
    )

    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween<Float>(durationMillis = 220, easing = LinearEasing))
    }

    val dismissSpec = tween<Float>(durationMillis = 220, easing = LinearEasing)
    val requestDismiss: () -> Unit = {
        scope.launch {
            progress.animateTo(0f, dismissSpec)
            onFullyDismissed()
        }
    }

    BackHandler(onBack = requestDismiss)

    val p = progress.value
    val cardAlpha = p
    val cardScale = 0.92f + (1f - 0.92f) * p

    val blurP = p
    val blurRadiusDp = DesignTokens.expandedOverlayBlurRadius * blurP
    val tintAlpha = DesignTokens.expandedOverlayBlurTintAlpha * blurP
    val noiseP = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val noise = DesignTokens.expandedOverlayNoiseFactor * (noiseP * noiseP)

    val cardWidth = maxWidth * DesignTokens.expandedFoodCardWidthFraction
    val cardHeight = DesignTokens.foodEntryCardHeightExpanded

    var showWeightDialog by androidx.compose.runtime.remember { mutableStateOf(false) }
    var weightText by androidx.compose.runtime.remember(entry) {
        mutableStateOf(entry.weightGrams.toString())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (supportsBlur) {
                        Modifier.hazeEffect(state = overlayHazeState) {
                            blurRadius = blurRadiusDp
                            tints = listOf(HazeTint(scheme.glassTint.copy(alpha = tintAlpha)))
                            noiseFactor = noise
                        }
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { requestDismiss() }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(cardWidth, cardHeight)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .clip(cardShape)
                    .border(DesignTokens.tileBorderWidth, gradientBorder, cardShape)
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (scheme.tileTopMiddleBackgroundBrush != null) {
                            Modifier
                                .graphicsLayer { alpha = DesignTokens.expandedFoodCardBaseAlpha }
                                .background(scheme.tileTopMiddleBackgroundBrush!!, cardShape)
                        } else {
                            Modifier.background(scheme.tileBackgroundColor.copy(alpha = DesignTokens.expandedFoodCardBaseAlpha), cardShape)
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .hazeSource(hazeState)
                ) {
                    androidx.compose.material3.Text(
                        text = entry.emoji,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = DesignTokens.expandedFoodEntryEmojiSizeSp,
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                            lineHeight = DesignTokens.expandedFoodEntryEmojiSizeSp
                        ),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = DesignTokens.expandedFoodEntryEmojiPaddingTopDp,
                                end = DesignTokens.expandedFoodEntryEmojiPaddingEndDp
                            )
                            .offset(
                                x = DesignTokens.expandedFoodEntryEmojiOffsetXDp,
                                y = DesignTokens.expandedFoodEntryEmojiOffsetYDp
                            )
                            .graphicsLayer {
                                alpha = DesignTokens.expandedFoodEntryEmojiAlpha
                                rotationZ = DesignTokens.expandedFoodEntryEmojiRotationDeg
                                translationY = with(density) { DesignTokens.expandedFoodEntryEmojiNudgeYDp.toPx() }
                            }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .then(
                            if (supportsBlur) {
                                Modifier.hazeEffect(state = hazeState) {
                                    blurRadius = DesignTokens.expandedFoodCardGlassBlurRadius
                                    tints = listOf(HazeTint(scheme.glassTint.copy(alpha = DesignTokens.expandedFoodCardGlassTintAlpha)))
                                    noiseFactor = DesignTokens.expandedFoodCardGlassNoise
                                }
                            } else {
                                Modifier.background(
                                    scheme.tileBackgroundColor.copy(alpha = 0.75f),
                                    cardShape
                                )
                            }
                        )
                        .padding(DesignTokens.foodEntryCardInnerPadding)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Text(
                                text = entry.name,
                                fontFamily = DesignTokens.fontFamilyPlank,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                fontSize = DesignTokens.expandedFoodCardTitleFontSize,
                                color = DesignTokens.expandedFoodCardTitleTextColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = DesignTokens.expandedFoodCardTextShadowColor.copy(alpha = DesignTokens.expandedFoodCardTextShadowAlpha),
                                        offset = Offset(0f, with(density) { DesignTokens.expandedFoodCardTextShadowOffsetY.toPx() }),
                                        blurRadius = with(density) { DesignTokens.expandedFoodCardTextShadowBlur.toPx() }
                                    )
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.Text(
                                text = "${entry.weightGrams}г",
                                fontFamily = DesignTokens.fontFamilyPlank,
                                fontSize = DesignTokens.expandedFoodCardMetaFontSize,
                                color = DesignTokens.expandedFoodCardMetaTextColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = DesignTokens.expandedFoodCardTextShadowColor.copy(alpha = DesignTokens.expandedFoodCardTextShadowAlpha),
                                        offset = Offset(0f, with(density) { DesignTokens.expandedFoodCardTextShadowOffsetY.toPx() }),
                                        blurRadius = with(density) { DesignTokens.expandedFoodCardTextShadowBlur.toPx() }
                                    )
                                )
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(scheme.textColor.copy(alpha = 0.08f))
                        ) {
                            androidx.compose.material3.Text(
                                text = "краткая информация / полезность-вред",
                                fontFamily = DesignTokens.fontFamilyPlank,
                                fontSize = DesignTokens.expandedFoodCardBodyFontSize,
                                color = DesignTokens.expandedFoodCardBodyTextColor.copy(alpha = DesignTokens.expandedFoodCardBodyTextAlpha),
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = DesignTokens.expandedFoodCardTextShadowColor.copy(alpha = DesignTokens.expandedFoodCardTextShadowAlpha),
                                        offset = Offset(0f, with(density) { DesignTokens.expandedFoodCardTextShadowOffsetY.toPx() }),
                                        blurRadius = with(density) { DesignTokens.expandedFoodCardTextShadowBlur.toPx() }
                                    )
                                ),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(8.dp)
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DesignTokens.foodEntryCardActionRowHeight),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(DesignTokens.foodEntryCardActionIconSize)
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithCache {
                                        val brush = Brush.radialGradient(
                                            colors = listOf(
                                                DesignTokens.expandedFoodCardFgCenterColor.copy(alpha = DesignTokens.expandedFoodCardFgCenterAlpha),
                                                DesignTokens.expandedFoodCardFgEdgeColor.copy(alpha = DesignTokens.expandedFoodCardFgEdgeAlpha)
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.minDimension * 0.9f
                                        )
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(brush, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                                    .clickable {
                                        viewModel.deleteEntry(entry)
                                        requestDismiss()
                                    }
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Повторить",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(DesignTokens.foodEntryCardActionIconSize)
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithCache {
                                        val brush = Brush.radialGradient(
                                            colors = listOf(
                                                DesignTokens.expandedFoodCardFgCenterColor.copy(alpha = DesignTokens.expandedFoodCardFgCenterAlpha),
                                                DesignTokens.expandedFoodCardFgEdgeColor.copy(alpha = DesignTokens.expandedFoodCardFgEdgeAlpha)
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.minDimension * 0.9f
                                        )
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(brush, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                                    .clickable {
                                        viewModel.repeatEntry(entry)
                                        requestDismiss()
                                    }
                            )
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = "Изменить вес",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(DesignTokens.foodEntryCardActionIconSize)
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithCache {
                                        val brush = Brush.radialGradient(
                                            colors = listOf(
                                                DesignTokens.expandedFoodCardFgCenterColor.copy(alpha = DesignTokens.expandedFoodCardFgCenterAlpha),
                                                DesignTokens.expandedFoodCardFgEdgeColor.copy(alpha = DesignTokens.expandedFoodCardFgEdgeAlpha)
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.minDimension * 0.9f
                                        )
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(brush, blendMode = BlendMode.SrcIn)
                                        }
                                    }
                                    .clickable { showWeightDialog = true }
                            )
                        }
                    }
                }
            }
            }
        }

        if (showWeightDialog) {
            AlertDialog(
                onDismissRequest = { showWeightDialog = false },
                title = {
                    Text(text = "Изменить вес", fontFamily = DesignTokens.fontFamilyPlank)
                },
                text = {
                    androidx.compose.foundation.layout.Column {
                        Text(
                            text = "${entry.emoji} ${entry.name}",
                            fontFamily = DesignTokens.fontFamilyPlank,
                            fontSize = 14.sp,
                            color = scheme.textColor.copy(alpha = 0.7f)
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) weightText = it },
                            label = { Text("Вес (г)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            weightText.toIntOrNull()?.takeIf { it > 0 }?.let { grams ->
                                viewModel.updateEntryWeight(entry, grams)
                                showWeightDialog = false
                                requestDismiss()
                            }
                        }
                    ) {
                        Text("Сохранить", fontFamily = DesignTokens.fontFamilyPlank)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWeightDialog = false }) {
                        Text("Отмена", fontFamily = DesignTokens.fontFamilyPlank)
                    }
                }
            )
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
@Suppress("UnusedBoxWithConstraintsScope")
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
    selectedDate: String?,
    selectedDayEntriesState: DayEntriesForDate,
    availableDates: List<String>,
    entriesCache: Map<String, List<DayEntry>>,
    userProfile: UserProfile?,
    dayNavigationState: DayNavigationState?,
    kcalHistory: List<com.example.vitanlyapp.domain.repository.DayKcalPoint>,
    globalMaxKcal: Int,
    onDaySettled: (Int) -> Unit,
    onEntryClick: (DayEntry) -> Unit,
    onEntryExpandRequest: (DayEntry, Rect) -> Unit,
    onShowResetDialog: () -> Unit,
    onToggleTheme: () -> Unit,
    viewModel: MainViewModel
) {
    val density = LocalDensity.current
    val scheme = LocalAppColorScheme.current

    // HazeState для glassmorphism плашек веса/активности в верхней плитке
    val kbjuHazeState = rememberHazeState()

    // Текст для автозаполнения поля ввода (из подсказки)
    var chatPrefillText by remember { mutableStateOf("") }

    // Когда клавиатура видна и чат раскрыт — плитка чата перекрывает остальные
    val chatFullScreen = isKeyboardVisible && activeTile == TilePosition.BOTTOM
    
    // Высота нижней плитки: фиксированная в idle, fullscreen при раскрытии
    // Включает safe area снизу чтобы уходить за край экрана
    val safeBottomDp = with(density) { safeBottomPx.toDp() }
    val safeTopDp = with(density) { safeTopPx.toDp() }
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    // Для позиционирования контента/инпута используем НЕ сумму, а max(navBars, ime),
    // иначе на некоторых девайсах/клавиатурах navBars уже входит в ime и всё улетает вверх.
    val systemBottomInsetDp = with(density) { maxOf(safeBottomPx, imeBottomPx).toDp() }
    
    // Минимальная высота плитки чата (включая safe area снизу)
    val chatMinHeight = DesignTokens.chatTileMinHeight + safeBottomDp

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenHeightPx = with(density) { screenHeight.toPx() }
        val chatMinHeightPx = with(density) { chatMinHeight.toPx() }
        
        // Высота раскрытой плитки чата: 3/4 экрана
        val chatExpandedHeightPx = screenHeightPx * 0.75f
        
        // Диапазон перетаскивания
        val maxDragOffset = (chatExpandedHeightPx - chatMinHeightPx).coerceAtLeast(1f)
        
        // Анимируемый offset для свайпа (0 = свёрнуто, maxDragOffset = раскрыто)
        val dragOffset = remember { Animatable(0f) }
        
        // Флаг: плитка полностью раскрыта (для управления клавиатурой)
        val isFullyExpanded = dragOffset.value >= maxDragOffset * 0.95f
        
        // Скрыли IME в текущем жесте — не даём ChatInputBlock повторно показать клавиатуру
        var imeHiddenThisGesture by remember { mutableStateOf(false) }
        // Состояние плитки до начала драга — для гистерезиса snap (40% / 80%)
        var wasExpandedAtDragStart by remember { mutableStateOf(false) }
        
        // Состояние для отслеживания перетаскивания
        val draggableState = rememberDraggableState { delta ->
            coroutineScope.launch {
                val newOffset = (dragOffset.value - delta).coerceIn(0f, maxDragOffset)
                dragOffset.snapTo(newOffset)
            }
        }
        
        // Синхронизация: когда ViewModel меняет activeTile → анимируем offset
        LaunchedEffect(activeTile) {
            val targetOffset = if (activeTile == TilePosition.BOTTOM) maxDragOffset else 0f
            dragOffset.animateTo(targetOffset, tileAnimationSpec())
        }
        
        // Текущая высота плитки чата на основе offset
        val chatTileHeight = with(density) {
            (chatMinHeightPx + dragOffset.value).toDp()
        }
        
        // Чат раскрыт — определяем по offset (больше 30% = раскрыт)
        val chatExpanded = dragOffset.value > maxDragOffset * 0.3f || chatFullScreen
        // Контент (LazyColumn) скрываем раньше — чтобы не тупило при сворачивании
        val showChatContent = dragOffset.value > maxDragOffset * 0.7f
        
        // Прогресс раскрытия (0..1) для анимации скругления углов
        val expansionProgress = if (maxDragOffset > 0f) {
            (dragOffset.value / maxDragOffset).coerceIn(0f, 1f)
        } else 0f
        
        // Анимация скругления углов (от 24dp до 0dp) на основе progress
        val cornerRadius = DesignTokens.bottomTileCornerRadius * (1f - expansionProgress)
        
        val animatedShape = RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        
        // Оптимизация: верхние плитки активны только когда чат почти свёрнут
        // Порог — когда высота плитки чата меньше минимальной + 20% от разницы
        val expansionThreshold = chatMinHeight + (screenHeight * DesignTokens.chatTileExpandedFraction - chatMinHeight) * 0.15f
        val topTilesActive = chatTileHeight < expansionThreshold

        // ----- TOP/MIDDLE: свободная высота только от свёрнутого чата (20/50/80) -----
        val freeHeightDp = (maxHeight - safeTopDp - DesignTokens.screenPadding - chatMinHeight - DesignTokens.tileSpacing - DesignTokens.tileSpacing).coerceAtLeast(0.dp)
        val freeHeightPx = with(density) { freeHeightDp.toPx() }.coerceAtLeast(0f)
        val handleHitHeightPx = with(density) { DesignTokens.topMiddleHandleHitHeightDp.toPx() }
        val maxSplitterOffsetPx = (freeHeightPx - handleHitHeightPx).coerceAtLeast(1f)

        val splitterOffset = remember { Animatable(0f) }
        var lastHandleSnappedOffsetPx by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(maxSplitterOffsetPx) {
            if (maxSplitterOffsetPx > 0f && splitterOffset.value == 0f && lastHandleSnappedOffsetPx == 0f) {
                splitterOffset.snapTo(0f)
            }
        }

        val splitterProgress = if (maxSplitterOffsetPx > 0f) (splitterOffset.value / maxSplitterOffsetPx).coerceIn(0f, 1f) else 0f
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        val topRatioFromProgress = lerp(DesignTokens.topMiddleRatioExpanded, DesignTokens.topMiddleRatioCollapsed, splitterProgress)

        var isSplitterDragging by remember { mutableStateOf(false) }

        val splitterDraggableState = rememberDraggableState { delta ->
            coroutineScope.launch {
                splitterOffset.snapTo((splitterOffset.value - delta).coerceIn(0f, maxSplitterOffsetPx))
            }
        }

        val onSplitterDragStarted: suspend (CoroutineScope, Offset) -> Unit = { _, _ ->
            isSplitterDragging = true
        }

        val onSplitterDragStopped: suspend (CoroutineScope, Float) -> Unit = { scope, velocity ->
            scope.launch {
                val progress = if (maxSplitterOffsetPx > 0f) (splitterOffset.value / maxSplitterOffsetPx).coerceIn(0f, 1f) else 0f
                val thresholdVel = DesignTokens.topMiddleHandleFlingVelocityThresholdPxPerSec
                val targetOffset = if (kotlin.math.abs(velocity) >= thresholdVel) {
                    if (velocity < 0f) maxSplitterOffsetPx else 0f
                } else {
                    if (progress < DesignTokens.topMiddleHandleSnapToExpandedThreshold) 0f else maxSplitterOffsetPx
                }
                lastHandleSnappedOffsetPx = targetOffset
                splitterOffset.animateTo(targetOffset, splitterSnapSpec())
                isSplitterDragging = false
                if (targetOffset <= 2f) viewModel.setActiveTile(TilePosition.TOP)
                else viewModel.setActiveTile(TilePosition.MIDDLE)
            }
        }

        val balancedOffsetPx = maxSplitterOffsetPx * 0.5f
        LaunchedEffect(activeTile, maxSplitterOffsetPx, isSplitterDragging) {
            if (!isSplitterDragging) {
                val targetOffset = when (activeTile) {
                    TilePosition.TOP -> 0f
                    TilePosition.MIDDLE -> maxSplitterOffsetPx
                    TilePosition.BOTTOM, null -> balancedOffsetPx.coerceIn(0f, maxSplitterOffsetPx)
                }
                if (kotlin.math.abs(splitterOffset.value - targetOffset) > 1f) {
                    splitterOffset.animateTo(targetOffset, tileAnimationSpec())
                }
            }
        }

        val topRatio = topRatioFromProgress
        val middleRatio = 1f - topRatio
        val topHeightDp = with(density) { (freeHeightPx * topRatio).toDp() }
        val middleHeightDp = with(density) { (freeHeightPx * middleRatio).toDp() }

        val topCollapseProgress = splitterProgress

        // Планки вес/активность: alpha = 1 - smoothstep(fadeStart..fadeEnd, progress), симметрично для drag и tap
        val plankFadeT = when {
            topCollapseProgress <= DesignTokens.topTilePlanksFadeStartProgress -> 0f
            topCollapseProgress >= DesignTokens.topTilePlanksFadeEndProgress -> 1f
            else -> {
                val t = (topCollapseProgress - DesignTokens.topTilePlanksFadeStartProgress) / (DesignTokens.topTilePlanksFadeEndProgress - DesignTokens.topTilePlanksFadeStartProgress)
                t * t * (3 - 2 * t) // smoothstep
            }
        }
        val plankFadeAlpha = 1f - plankFadeT * (1f - DesignTokens.topTilePlanksMinAlpha)
        val plankSlideUpPx = with(density) { DesignTokens.topTilePlanksSlideUpDp.toPx() }
        val plankTranslationY = -plankSlideUpPx * plankFadeT
        val plankHitHeightDp = (180f * (1 - plankFadeT)).dp

        val controlsFadeAlpha = when {
            topCollapseProgress <= DesignTokens.topTileControlsFadeStartProgress -> 1f
            topCollapseProgress >= DesignTokens.topTileControlsFadeEndProgress -> 0f
            else -> lerp(
                1f,
                0f,
                (topCollapseProgress - DesignTokens.topTileControlsFadeStartProgress) / (DesignTokens.topTileControlsFadeEndProgress - DesignTokens.topTileControlsFadeStartProgress)
            )
        }
        val controlsSlideUpPx = with(density) { DesignTokens.topTileControlsSlideUpDp.toPx() }
        val controlsTranslationY = -lerp(0f, controlsSlideUpPx, when {
            topCollapseProgress <= DesignTokens.topTileControlsFadeStartProgress -> 0f
            topCollapseProgress >= DesignTokens.topTileControlsFadeEndProgress -> 1f
            else -> (topCollapseProgress - DesignTokens.topTileControlsFadeStartProgress) / (DesignTokens.topTileControlsFadeEndProgress - DesignTokens.topTileControlsFadeStartProgress)
        })

        val splitterOverlayOffsetYDp = with(density) {
            (topHeightDp.toPx() - handleHitHeightPx / 2).coerceIn(0f, (freeHeightPx - handleHitHeightPx).coerceAtLeast(0f)).toDp()
        }

        // Слой 1: верхние плитки (TOP и MIDDLE) — только две плитки, без разделителя в потоке
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.screenPadding)
                .padding(top = safeTopDp + DesignTokens.screenPadding)
                .padding(bottom = chatMinHeight + DesignTokens.tileSpacing)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.tileSpacing)
            ) {
            Tile(
                position = TilePosition.TOP,
                isExpanded = activeTile == TilePosition.TOP,
                isCollapsed = activeTile != null && activeTile != TilePosition.TOP,
                onClick = { viewModel.onTileClick(TilePosition.TOP) },
                modifier = Modifier.fillMaxWidth().height(topHeightDp),
                backgroundBrush = scheme.tileTopMiddleBackgroundBrush,
                overflowContent = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(plankHitHeightDp)
                                .graphicsLayer {
                                    alpha = plankFadeAlpha
                                    translationY = plankTranslationY
                                }
                                .pointerInteropFilter { _ ->
                                    plankFadeAlpha < DesignTokens.topTilePlanksInteractionAlphaThreshold
                                }
                        ) {
                            KbjuTileWheelOverflowContent(
                                isExpanded = activeTile == TilePosition.TOP,
                                currentWeight = currentWeight,
                                onWeightChange = viewModel::updateWeight,
                                activityCoefficient = activityCoefficient,
                                onActivityChange = viewModel::updateActivityCoefficient,
                                hazeState = kbjuHazeState,
                                topCollapseProgress = topCollapseProgress
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .graphicsLayer {
                                    alpha = controlsFadeAlpha
                                    translationY = controlsTranslationY
                                }
                                .then(
                                    if (controlsFadeAlpha < DesignTokens.topTilePlanksInteractionAlphaThreshold)
                                        Modifier.pointerInteropFilter { true }
                                    else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                                        tint = LocalAppColorScheme.current.textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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
                                        tint = LocalAppColorScheme.current.textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { viewModel.populateTestData() },
                                                onLongPress = { KcalTraceLog.dumpToLogcat() }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = "Тестовые данные",
                                        tint = LocalAppColorScheme.current.textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                tileTransitionDurationMs = DesignTokens.topMiddlePostSnapDurationMs,
                collapseProgress = topCollapseProgress,
                tapToExpandOverlay = plankFadeAlpha < DesignTokens.topTilePlanksInteractionAlphaThreshold
            ) {
                // Top tile content: Kcal chart + KBJU bars
                KbjuTileContentWithChart(
                    kcalStat = kcalStat,
                    macroStats = macroStats,
                    activeTile = activeTile,
                    userProfile = userProfile,
                    selectedDate = selectedDate,
                    hazeState = kbjuHazeState,
                    isActive = topTilesActive,
                    topCollapseProgress = topCollapseProgress,
                    kcalHistory = kcalHistory,
                    availableDates = availableDates,
                    dayNavigationState = dayNavigationState,
                    globalMaxKcal = globalMaxKcal
                )
            }

            Tile(
                position = TilePosition.MIDDLE,
                isExpanded = activeTile == TilePosition.MIDDLE,
                isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                onClick = { viewModel.onTileClick(TilePosition.MIDDLE) },
                modifier = Modifier.fillMaxWidth().height(middleHeightDp),
                backgroundBrush = scheme.tileTopMiddleBackgroundBrush,
                tileTransitionDurationMs = DesignTokens.topMiddlePostSnapDurationMs,
                collapseProgress = topCollapseProgress
            ) {
                InputTileContent(
                    entries = selectedDayEntriesState.entries,
                    entriesDate = selectedDayEntriesState.date,
                    availableDates = availableDates,
                    dayNavigationState = dayNavigationState,
                    getCachedEntries = { date -> entriesCache[date] },
                    onDaySettled = onDaySettled,
                    isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                    onEntryClick = onEntryClick,
                    onEntryExpandRequest = onEntryExpandRequest,
                    isActive = topTilesActive
                )
            }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = splitterOverlayOffsetYDp)
                    .fillMaxWidth()
                    .height(DesignTokens.topMiddleHandleHitHeightDp)
                    .draggable(
                        state = splitterDraggableState,
                        orientation = Orientation.Vertical,
                        onDragStarted = onSplitterDragStarted,
                        onDragStopped = onSplitterDragStopped
                    )
            )
        }

        // Слой 2: ручка 48dp + плитка. Драг за ручку — палец управляет высотой (snapTo)
        val onDragStarted: suspend (CoroutineScope, Offset) -> Unit = { _, _ ->
            wasExpandedAtDragStart = dragOffset.value > maxDragOffset * 0.5f
            keyboardController?.hide()
            imeHiddenThisGesture = true
        }
        val onDragStopped: suspend (CoroutineScope, Float) -> Unit = { scope, _ ->
            imeHiddenThisGesture = false
            scope.launch {
                val expandThreshold = maxDragOffset * 0.4f   // 40% — дотянул до 40% → развернуть
                val collapseThreshold = maxDragOffset * 0.8f  // 80% — опустил ниже 80% → свернуть
                val current = dragOffset.value
                val targetOffset = when {
                    current >= collapseThreshold -> maxDragOffset
                    current < expandThreshold -> 0f
                    else -> if (wasExpandedAtDragStart) 0f else maxDragOffset
                }
                dragOffset.animateTo(targetOffset, tileAnimationSpec())
                if (targetOffset == maxDragOffset && activeTile != TilePosition.BOTTOM) {
                    viewModel.onTileClick(TilePosition.BOTTOM)
                } else if (targetOffset == 0f && activeTile == TilePosition.BOTTOM) {
                    viewModel.onTileClick(TilePosition.BOTTOM)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(chatTileHeight)
        ) {
            Tile(
                position = TilePosition.BOTTOM,
                isExpanded = chatExpanded,
                isCollapsed = false,
                onClick = { viewModel.onTileClick(TilePosition.BOTTOM) },
                shape = animatedShape,
                edgeToEdge = true,
                modifier = Modifier.fillMaxSize()
            ) {
                BottomTileContent(
                    messages = chatMessages,
                    isLoading = chatLoading,
                    isCollapsed = !chatExpanded,
                    showContent = showChatContent,
                    systemBottomInset = systemBottomInsetDp,
                    onHintClick = { hint -> chatPrefillText = hint }
                )
            }
            // Зона захвата: -30dp от верха плитки, высота 60dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-30).dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStarted = onDragStarted,
                        onDragStopped = onDragStopped
                    )
            )
        }
        
        // Слой 3: блок ввода — всегда внизу экрана (с учётом IME/navBars insets)
        ChatInputBlock(
            onSendMessage = viewModel::sendChatMessage,
            isLoading = chatLoading,
            isCollapsed = !chatExpanded,
            isFullyExpanded = isFullyExpanded,
            allowAutoIme = !imeHiddenThisGesture,
            onExpandRequest = { viewModel.onTileClick(TilePosition.BOTTOM) },
            prefillText = chatPrefillText,
            onPrefillConsumed = { chatPrefillText = "" },
            modifier = Modifier
                .padding(bottom = systemBottomInsetDp)
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
    selectedDate: String?,
    selectedDayEntriesState: DayEntriesForDate,
    availableDates: List<String>,
    entriesCache: Map<String, List<DayEntry>>,
    userProfile: UserProfile?,
    dayNavigationState: DayNavigationState?,
    kcalHistory: List<com.example.vitanlyapp.domain.repository.DayKcalPoint>,
    globalMaxKcal: Int,
    onDaySettled: (Int) -> Unit,
    onEntryClick: (DayEntry) -> Unit,
    onEntryExpandRequest: (DayEntry, Rect) -> Unit,
    onShowResetDialog: () -> Unit,
    onToggleTheme: () -> Unit,
    viewModel: MainViewModel
) {
    val scheme = LocalAppColorScheme.current
    val density = LocalDensity.current
    val systemBottomInsetDp = with(density) {
        maxOf(
            WindowInsets.navigationBars.getBottom(this),
            WindowInsets.ime.getBottom(this)
        ).toDp()
    }
    
    // HazeState для glassmorphism плашек веса/активности в верхней плитке
    val kbjuHazeState = rememberHazeState()
    
    // Один прогресс для TOP collapse (0 = развёрнут, 1 = свёрнут) — без отдельного AnimatedVisibility
    val topCollapseProgress by animateFloatAsState(
        targetValue = if (activeTile == TilePosition.TOP) 0f else 1f,
        animationSpec = tileAnimationSpec()
    )
    fun lerpExpanded(a: Float, b: Float, t: Float) = a + (b - a) * t
    val controlsFadeAlphaExpanded = when {
        topCollapseProgress <= DesignTokens.topTileControlsFadeStartProgress -> 1f
        topCollapseProgress >= DesignTokens.topTileControlsFadeEndProgress -> 0f
        else -> lerpExpanded(
            1f,
            0f,
            (topCollapseProgress - DesignTokens.topTileControlsFadeStartProgress) / (DesignTokens.topTileControlsFadeEndProgress - DesignTokens.topTileControlsFadeStartProgress)
        )
    }
    val controlsSlideUpPxExpanded = with(density) { DesignTokens.topTileControlsSlideUpDp.toPx() }
    val controlsTranslationYExpanded = -lerpExpanded(0f, controlsSlideUpPxExpanded, when {
        topCollapseProgress <= DesignTokens.topTileControlsFadeStartProgress -> 0f
        topCollapseProgress >= DesignTokens.topTileControlsFadeEndProgress -> 1f
        else -> (topCollapseProgress - DesignTokens.topTileControlsFadeStartProgress) / (DesignTokens.topTileControlsFadeEndProgress - DesignTokens.topTileControlsFadeStartProgress)
    })
    // Планки вес/активность: тот же smoothstep по progress (симметрично при tap)
    val plankFadeTExpanded = when {
        topCollapseProgress <= DesignTokens.topTilePlanksFadeStartProgress -> 0f
        topCollapseProgress >= DesignTokens.topTilePlanksFadeEndProgress -> 1f
        else -> {
            val t = (topCollapseProgress - DesignTokens.topTilePlanksFadeStartProgress) / (DesignTokens.topTilePlanksFadeEndProgress - DesignTokens.topTilePlanksFadeStartProgress)
            t * t * (3 - 2 * t)
        }
    }
    val plankFadeAlphaExpanded = 1f - plankFadeTExpanded * (1f - DesignTokens.topTilePlanksMinAlpha)
    val plankSlideUpPxExpanded = with(density) { DesignTokens.topTilePlanksSlideUpDp.toPx() }
    val plankTranslationYExpanded = -plankSlideUpPxExpanded * plankFadeTExpanded
    val plankHitHeightDpExpanded = (180f * (1 - plankFadeTExpanded)).dp

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
                backgroundBrush = scheme.tileTopMiddleBackgroundBrush,
                overflowContent = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(plankHitHeightDpExpanded)
                                .graphicsLayer {
                                    alpha = plankFadeAlphaExpanded
                                    translationY = plankTranslationYExpanded
                                }
                                .pointerInteropFilter { _ ->
                                    plankFadeAlphaExpanded < DesignTokens.topTilePlanksInteractionAlphaThreshold
                                }
                        ) {
                            KbjuTileWheelOverflowContent(
                                isExpanded = activeTile == TilePosition.TOP,
                                currentWeight = currentWeight,
                                onWeightChange = viewModel::updateWeight,
                                activityCoefficient = activityCoefficient,
                                onActivityChange = viewModel::updateActivityCoefficient,
                                hazeState = kbjuHazeState,
                                topCollapseProgress = topCollapseProgress
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .graphicsLayer {
                                    alpha = controlsFadeAlphaExpanded
                                    translationY = controlsTranslationYExpanded
                                }
                                .then(
                                    if (controlsFadeAlphaExpanded < DesignTokens.topTilePlanksInteractionAlphaThreshold)
                                        Modifier.pointerInteropFilter { true }
                                    else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { viewModel.populateTestData() },
                                                onLongPress = { KcalTraceLog.dumpToLogcat() }
                                            )
                                        },
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
                },
                tapToExpandOverlay = plankFadeAlphaExpanded < DesignTokens.topTilePlanksInteractionAlphaThreshold
            ) {
                // Top tile content: Kcal chart + KBJU bars
                KbjuTileContentWithChart(
                    kcalStat = kcalStat,
                    macroStats = macroStats,
                    activeTile = activeTile,
                    userProfile = userProfile,
                    selectedDate = selectedDate,
                    hazeState = kbjuHazeState,
                    isActive = true,
                    topCollapseProgress = topCollapseProgress,
                    kcalHistory = kcalHistory,
                    availableDates = availableDates,
                    dayNavigationState = dayNavigationState,
                    globalMaxKcal = globalMaxKcal
                )
            }

            Tile(
                position = TilePosition.MIDDLE,
                isExpanded = activeTile == TilePosition.MIDDLE,
                isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                onClick = { viewModel.onTileClick(TilePosition.MIDDLE) },
                modifier = Modifier.weight(weightMiddle),
                backgroundBrush = scheme.tileTopMiddleBackgroundBrush
            ) {
                InputTileContent(
                    entries = selectedDayEntriesState.entries,
                    entriesDate = selectedDayEntriesState.date,
                    availableDates = availableDates,
                    dayNavigationState = dayNavigationState,
                    getCachedEntries = { date -> entriesCache[date] },
                    onDaySettled = onDaySettled,
                    isCollapsed = activeTile != null && activeTile != TilePosition.MIDDLE,
                    onEntryClick = onEntryClick,
                    onEntryExpandRequest = onEntryExpandRequest
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
                    systemBottomInset = systemBottomInsetDp,
                    onHintClick = { hint -> chatPrefillText = hint }
                )
            }
            
            // Блок ввода поверх плитки чата
            ChatInputBlock(
                onSendMessage = viewModel::sendChatMessage,
                isLoading = chatLoading,
                isCollapsed = false,
                isFullyExpanded = true, // В ExpandedLayout чат всегда раскрыт
                prefillText = chatPrefillText,
                onPrefillConsumed = { chatPrefillText = "" },
                modifier = Modifier
                    .padding(bottom = systemBottomInsetDp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}
