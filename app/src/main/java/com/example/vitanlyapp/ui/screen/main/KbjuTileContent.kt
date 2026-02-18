package com.example.vitanlyapp.ui.screen.main

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitanlyapp.domain.model.KbjuBarStat
import com.example.vitanlyapp.domain.model.TilePosition
import com.example.vitanlyapp.domain.repository.UserProfile
import com.example.vitanlyapp.ui.component.ActivityWheelPicker
import com.example.vitanlyapp.ui.component.KbjuBars
import com.example.vitanlyapp.ui.component.WeightWheelPicker
import com.example.vitanlyapp.ui.component.KbjuWave
import com.example.vitanlyapp.ui.component.TileBackground
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.example.vitanlyapp.ui.design.shapedSurface
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.example.vitanlyapp.domain.repository.DayKcalPoint
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Контент верхней плитки КБЖУ: волна калорий, процент, бары макронутриентов.
 *
 * @param hazeState Состояние Haze для glassmorphism плашек веса/активности (опционально)
 */
/**
 * Задержка перед запуском тяжелых анимаций (мс).
 * Даёт время на первый рендер экрана.
 */
private const val WAVE_ANIMATION_DELAY_MS = 300L

/**
 * Верхняя плитка с КБЖУ (волна, процент, бары) и графиком калорий.
 * График виден только когда плитка раскрыта — между барами и плашками вес/активность.
 */
@Composable
fun KbjuTileContentWithChart(
    kcalStat: KbjuBarStat,
    macroStats: List<KbjuBarStat>,
    activeTile: TilePosition?,
    userProfile: UserProfile?,
    selectedDate: String?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isActive: Boolean = true,
    topCollapseProgress: Float = 0f,
    kcalHistory: List<DayKcalPoint>,
    availableDates: List<String>,
    dayNavigationState: DayNavigationState?,
    globalMaxKcal: Int
) {
    val scheme = LocalAppColorScheme.current
    val isExpanded = activeTile == TilePosition.TOP

    val barsTopInsetDp = (DesignTokens.topTileBarsTopInsetExpandedDp.value + (DesignTokens.topTileBarsTopInsetCollapsedDp.value - DesignTokens.topTileBarsTopInsetExpandedDp.value) * topCollapseProgress).dp
    
    // Отложенный запуск анимации волны
    var waveAnimationEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            delay(WAVE_ANIMATION_DELAY_MS)
            waveAnimationEnabled = true
        }
    }
    
    // Стек слоёв
    Box(modifier = modifier.fillMaxSize()) {
        // Слой 0: Фон и волна калорий
        Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
            TileBackground()
            KbjuWave(
                percent = kcalStat.percent,
                overflow = kcalStat.overflow,
                animateWave = waveAnimationEnabled && isActive,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
            )
        }

        // Слой 1: Процент калорий (только когда не раскрыто)
        var showCaloriePercent by remember { mutableStateOf(false) }
        LaunchedEffect(activeTile) {
            if (activeTile != null) {
                showCaloriePercent = false
            } else {
                delay(DesignTokens.caloriePercentAppearDelayMs.toLong())
                showCaloriePercent = true
            }
        }

        Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
            // Процент калорий — виден только в неактивном/свёрнутом состоянии
            if (!isExpanded) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                ) {
                    AnimatedVisibility(
                        visible = showCaloriePercent,
                        enter = fadeIn(animationSpec = tween(DesignTokens.caloriePercentFadeOutDurationMs)),
                        exit = fadeOut(animationSpec = tween(0))
                    ) {
                        CaloriePercentDisplay(
                            percent = kcalStat.percent,
                            showCaloriePercent = showCaloriePercent
                        )
                    }
                }
            }
        }

        // Слой 2: КБЖУ бары (сверху плитки)
        Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
            val barsHeight = DesignTokens.barHeight * 5

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DesignTokens.tilePadding)
                    .padding(top = barsTopInsetDp)
            ) {
                KbjuBars(
                    stats = macroStats,
                    labels = listOf("Белок", "Жиры", "Углеводы"),
                    interactionsEnabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isExpanded) Modifier.height(barsHeight)
                            else Modifier.fillMaxSize()
                        )
                )
            }
        }

        // Слой 3: График калорий — виден ТОЛЬКО когда плитка раскрыта
        // Располагается между барами сверху и плашками вес/активность снизу, в блоке как «вес»/«активность»
        if (isExpanded && kcalHistory.isNotEmpty() && availableDates.isNotEmpty()) {
            val barsHeight = DesignTokens.barHeight * 5
            val chartTopOffset = barsTopInsetDp + barsHeight + 8.dp
            val chartBottomPadding = 190.dp // Место для плашек вес/активность

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1.5f)
                    .padding(horizontal = DesignTokens.tilePadding)
                    .padding(top = chartTopOffset, bottom = chartBottomPadding)
            ) {
                ChartBlock(hazeState = hazeState, modifier = Modifier.fillMaxSize()) {
                    KcalAreaChart(
                        kcalHistory = kcalHistory,
                        availableDates = availableDates,
                        dayNavigationState = dayNavigationState,
                        globalMaxKcal = globalMaxKcal,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun KbjuTileContent(
    kcalStat: KbjuBarStat,
    macroStats: List<KbjuBarStat>,
    activeTile: TilePosition?,
    userProfile: UserProfile?,
    selectedDate: String?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isActive: Boolean = true,
    topCollapseProgress: Float = 0f
) {
    val scheme = LocalAppColorScheme.current

    val barsTopInsetDp = (DesignTokens.topTileBarsTopInsetExpandedDp.value + (DesignTokens.topTileBarsTopInsetCollapsedDp.value - DesignTokens.topTileBarsTopInsetExpandedDp.value) * topCollapseProgress).dp
    
    // Отложенный запуск анимации волны для оптимизации первого рендера
    // Анимации приостанавливаются когда плитка перекрыта (isActive = false)
    var waveAnimationEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            delay(WAVE_ANIMATION_DELAY_MS)
            waveAnimationEnabled = true
        }
    }
    
    // Стек: фон и волна сзади (zIndex 0), контент спереди (zIndex 2)
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
            TileBackground()
            KbjuWave(
                percent = kcalStat.percent,
                overflow = kcalStat.overflow,
                animateWave = waveAnimationEnabled && isActive,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
            )
        }

        var showCaloriePercent by remember { mutableStateOf(false) }
        LaunchedEffect(activeTile) {
            if (activeTile != null) {
                showCaloriePercent = false
            } else {
                delay(DesignTokens.caloriePercentAppearDelayMs.toLong())
                showCaloriePercent = true
            }
        }

        Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
        // Процент калорий
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
        ) {
            AnimatedVisibility(
                visible = showCaloriePercent,
                enter = fadeIn(animationSpec = tween(DesignTokens.caloriePercentFadeOutDurationMs)),
                exit = fadeOut(animationSpec = tween(0))
            ) {
                CaloriePercentDisplay(
                    percent = kcalStat.percent,
                    showCaloriePercent = showCaloriePercent
                )
            }
        }

        val isExpanded = activeTile == TilePosition.TOP
        val barsHeight = DesignTokens.barHeight * 5

        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .padding(horizontal = DesignTokens.tilePadding)
                .padding(top = barsTopInsetDp)
        ) {
            KbjuBars(
                stats = macroStats,
                labels = listOf("Белок", "Жиры", "Углеводы"),
                interactionsEnabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isExpanded) Modifier.height(barsHeight)
                        else Modifier.fillMaxSize()
                    )
            )
        }
        }
    }
}

/**
 * Плашки "вес" и "активность" с wheel picker. Видимость и высота по progress (симметричный фейд in/out).
 *
 * @param topCollapseProgress Прогресс сворачивания TOP (0 = раскрыт, 1 = свёрнут); если передан — высота и фейд по smoothstep в одном окне
 * @param hazeState Состояние Haze для glassmorphism (опционально)
 */
@Composable
fun KbjuTileWheelOverflowContent(
    isExpanded: Boolean,
    currentWeight: Float,
    onWeightChange: (Float) -> Unit,
    activityCoefficient: Float,
    onActivityChange: (Float) -> Unit,
    hazeState: HazeState? = null,
    topCollapseProgress: Float? = null
) {
    val plankFadeT = when {
        topCollapseProgress == null -> if (isExpanded) 0f else 1f
        topCollapseProgress <= DesignTokens.topTilePlanksFadeStartProgress -> 0f
        topCollapseProgress >= DesignTokens.topTilePlanksFadeEndProgress -> 1f
        else -> {
            val t = (topCollapseProgress - DesignTokens.topTilePlanksFadeStartProgress) / (DesignTokens.topTilePlanksFadeEndProgress - DesignTokens.topTilePlanksFadeStartProgress)
            t * t * (3 - 2 * t)
        }
    }
    val plankHeight = if (topCollapseProgress != null) bottomBlockHeight * (1 - plankFadeT) else if (isExpanded) bottomBlockHeight else 0.dp
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(plankHeight)
                .graphicsLayer { clip = false }
                .padding(horizontal = DesignTokens.tilePadding)
                .padding(bottom = DesignTokens.tilePadding),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.tilePadding)
        ) {
            BottomBlock(title = "вес", modifier = Modifier.weight(1f), hazeState = hazeState) {
                WeightWheelPicker(
                    value = currentWeight,
                    onValueChange = onWeightChange,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                )
            }
            BottomBlock(title = "активность", modifier = Modifier.weight(1f), hazeState = hazeState) {
                ActivityWheelPicker(
                    value = activityCoefficient,
                    onValueChange = onActivityChange,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                )
            }
        }
    }
}

private val bottomBlockHeight = 180.dp
private val bottomBlockCornerRadius = 16.dp
private val wheelOverflowHeight = 180.dp
private val plankBlurRadius = 60.dp
private const val plankTintAlpha = 0.90f
private const val plankNoiseFactor = 0.12f
private const val plankFallbackTintAlpha = 0.92f

@Composable
private fun BottomBlock(
    title: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val scheme = LocalAppColorScheme.current
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useHazeBlur = hazeState != null && supportsBlur
    val blockShape = RoundedCornerShape(bottomBlockCornerRadius)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            scheme.borderGradientTop,
            scheme.borderGradientBottom
        )
    )

    Box(
        modifier = modifier
            .height(bottomBlockHeight)
            .graphicsLayer { clip = false }
    ) {
        // 1) Background layer: strong backdrop blur + dense tint (haze) or fallback dense tint only
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(blockShape)
                .then(
                    if (useHazeBlur) {
                        Modifier.hazeEffect(state = hazeState!!) {
                            blurRadius = plankBlurRadius
                            tints = listOf(HazeTint(scheme.macroBarTrackColor.copy(alpha = plankTintAlpha)))
                            noiseFactor = plankNoiseFactor
                        }
                    } else {
                        Modifier.background(scheme.macroBarTrackColor.copy(alpha = plankFallbackTintAlpha), blockShape)
                    }
                )
                .border(DesignTokens.tileBorderWidth, gradientBorder, blockShape)
        )
        // 2) Foreground: title + content (no haze/blur, stays sharp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .graphicsLayer { clip = false }
                .padding(DesignTokens.tilePadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = title,
                    fontSize = DesignTokens.fontSizeLabel,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Thin,
                    fontStyle = FontStyle.Normal,
                    color = scheme.textColor.copy(alpha = 0.72f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {}
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(wheelOverflowHeight)
                    .graphicsLayer { clip = false },
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ChartBlock(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val scheme = LocalAppColorScheme.current
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useHazeBlur = hazeState != null && supportsBlur
    val blockShape = RoundedCornerShape(bottomBlockCornerRadius)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            scheme.borderGradientTop,
            scheme.borderGradientBottom
        )
    )

    Box(modifier = modifier.graphicsLayer { clip = false }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(blockShape)
                .then(
                    if (useHazeBlur) {
                        Modifier.hazeEffect(state = hazeState!!) {
                            blurRadius = plankBlurRadius
                            tints = listOf(HazeTint(scheme.macroBarTrackColor.copy(alpha = plankTintAlpha)))
                            noiseFactor = plankNoiseFactor
                        }
                    } else {
                        Modifier.background(scheme.macroBarTrackColor.copy(alpha = plankFallbackTintAlpha), blockShape)
                    }
                )
                .border(DesignTokens.tileBorderWidth, gradientBorder, blockShape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .clip(blockShape)
                .graphicsLayer { clip = true }
        ) {
            content()
        }
    }
}

/**
 * Отображение параметров профиля (кроме веса и активности).
 * Каждый параметр — в отдельной строке.
 */
@Composable
private fun UserProfileDisplay(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    val textColor = scheme.textColor.copy(alpha = 0.7f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Рост: ${profile.height} см",
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            fontSize = 11.sp,
            color = textColor
        )
        Text(
            text = "Возраст: ${profile.age} лет",
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            fontSize = 11.sp,
            color = textColor
        )
        Text(
            text = "Пол: ${profile.gender.label}",
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            fontSize = 11.sp,
            color = textColor
        )
        Text(
            text = "Цель: ${profile.goal.label}",
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            fontSize = 11.sp,
            color = textColor
        )
        Text(
            text = "Целевой вес: ${profile.targetWeight.toInt()} кг",
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            fontSize = 11.sp,
            color = textColor
        )
        Text(
            text = "Темп: ${profile.tempo} кг/нед",
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

@Composable
@Suppress("UnusedBoxWithConstraintsScope")
private fun CaloriePercentDisplay(
    percent: Float,
    showCaloriePercent: Boolean
) {
    val scheme = LocalAppColorScheme.current
    val zonePadding = DesignTokens.barSpacing
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = zonePadding),
        contentAlignment = Alignment.Center
    ) {
        val percentAnimatable = remember { Animatable(1f) }
        LaunchedEffect(showCaloriePercent, percent) {
            if (showCaloriePercent) {
                percentAnimatable.snapTo(1f)
                percentAnimatable.animateTo(
                    targetValue = percent,
                    animationSpec = tween(
                        durationMillis = DesignTokens.caloriePercentCountDurationMs,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
        val fontSize = (maxHeight.value * 0.9f).coerceIn(72f, 140f).sp
        Text(
            text = "${percentAnimatable.value.toInt()}%",
            fontSize = fontSize,
            fontFamily = DesignTokens.fontFamilyCaloriePercent,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = scheme.textColor
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// KCAL AREA CHART - Smooth scrolling calorie history
// ══════════════════════════════════════════════════════════════════════════

/**
 * Displays a 7-day calorie history chart that syncs with the DayNavigationState.
 * The chart smoothly scrolls as days are swiped in the middle tile.
 *
 * @param kcalHistory Full kcal history data (21+ days centered on selected date)
 * @param availableDates List of all available dates
 * @param dayNavigationState Shared animation state for day navigation
 * @param globalMaxKcal Maximum kcal value for Y-axis scaling
 * @param modifier Modifier for layout
 */
@Composable
@Suppress("UnusedBoxWithConstraintsScope")
fun KcalAreaChart(
    kcalHistory: List<DayKcalPoint>,
    availableDates: List<String>,
    dayNavigationState: DayNavigationState?,
    globalMaxKcal: Int,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    val density = LocalDensity.current
    
    // Chart colors from design tokens
    val accentColor = DesignTokens.chartAccentColor
    val lineColor = DesignTokens.chartLineColor
    val fillAlphaTop = DesignTokens.chartFillAlphaTop
    val fillAlphaBottom = DesignTokens.chartFillAlphaBottom
    val selectionLineAlpha = DesignTokens.chartSelectionLineAlpha
    
    // Chart dimensions from tokens
    val pointRadius = with(density) { DesignTokens.chartPointRadius.toPx() }
    val selectedPointRadius = with(density) { DesignTokens.chartSelectedPointRadius.toPx() }
    val lineStrokeWidth = with(density) { DesignTokens.chartLineStrokeWidth.toPx() }
    val verticalPadding = with(density) { DesignTokens.chartVerticalPadding.toPx() }
    val labelBottomPadding = with(density) { DesignTokens.chartLabelBottomPadding.toPx() }
    
    // Current position from shared state
    val currentPosition = dayNavigationState?.position?.value ?: 0f
    val centerIndex = currentPosition.roundToInt().coerceIn(0, (availableDates.size - 1).coerceAtLeast(0))
    
    // Map kcal data to a lookup by date
    val kcalByDate = remember(kcalHistory) {
        kcalHistory.associateBy { it.date }
    }
    
    // Determine the Y-axis max (use globalMaxKcal with some headroom)
    val yAxisMax = (globalMaxKcal * 1.2f).coerceAtLeast(1000f)
    
    // Visible window: 7 days centered on current selection
    val visibleDayCount = 7
    val halfWindow = visibleDayCount / 2
    
    val chartTileBackgroundColor = scheme.macroBarTrackColor.copy(alpha = plankFallbackTintAlpha)

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val chartWidth = with(density) { maxWidth.toPx() }
        val chartHeight = with(density) { maxHeight.toPx() }
        
        val labelAreaHeight = labelBottomPadding + 16f
        val graphTop = verticalPadding
        val graphBottom = chartHeight
        val graphHeight = graphBottom - graphTop
        
        val daySpacing = chartWidth / (visibleDayCount - 1).coerceAtLeast(1)
        val selectionX = chartWidth / 2f
        
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val offsetFraction = currentPosition - centerIndex
            val baseOffset = offsetFraction * daySpacing
            
            val extendedHalfWindow = halfWindow + 2
            val points = mutableListOf<Pair<Float, Float>>()
            
            for (i in -extendedHalfWindow..extendedHalfWindow) {
                val dateIndex = centerIndex + i
                if (dateIndex < 0 || dateIndex >= availableDates.size) continue
                
                val date = availableDates[dateIndex]
                val kcal = kcalByDate[date]?.kcal ?: 0
                
                val x = selectionX + (i * daySpacing) - baseOffset
                val yNormalized = kcal / yAxisMax
                val y = graphTop + graphHeight * (1 - yNormalized)
                
                points.add(Pair(x, y))
            }
            
            val selectedPointY = when {
                points.isEmpty() -> graphBottom
                points.size == 1 -> points[0].second
                else -> {
                    val firstX = points.first().first
                    val lastX = points.last().first
                    var interpY = if (selectionX <= firstX) points.first().second
                        else if (selectionX >= lastX) points.last().second
                        else points.last().second
                    for (j in 0 until points.size - 1) {
                        val x0 = points[j].first
                        val x1 = points[j + 1].first
                        val y0 = points[j].second
                        val y1 = points[j + 1].second
                        val xMin = kotlin.math.min(x0, x1)
                        val xMax = kotlin.math.max(x0, x1)
                        if (selectionX in xMin..xMax) {
                            val dx = x1 - x0
                            val t = if (kotlin.math.abs(dx) < 1e-6f) 1f else (selectionX - x0) / dx
                            interpY = y0 + t * (y1 - y0)
                            break
                        }
                    }
                    interpY
                }
            }
            
            if (points.size >= 2) {
                val areaPath = Path().apply {
                    moveTo(points.first().first, graphBottom)
                    points.forEach { (x, y) -> lineTo(x, y) }
                    lineTo(points.last().first, graphBottom)
                    close()
                }
                
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = fillAlphaTop),
                        accentColor.copy(alpha = fillAlphaBottom)
                    ),
                    startY = graphTop,
                    endY = graphBottom
                )
                
                drawPath(areaPath, fillBrush)
                
                val linePath = Path().apply {
                    points.forEachIndexed { index, (x, y) ->
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                
                drawPath(
                    linePath,
                    lineColor,
                    style = Stroke(width = lineStrokeWidth)
                )
                
                val eraserRadiusOffset = lineStrokeWidth / 2f
                points.forEachIndexed { index, (x, y) ->
                    val distFromCenter = kotlin.math.abs(x - selectionX)
                    val isSelected = distFromCenter < daySpacing / 2
                    val radius = if (isSelected) selectedPointRadius else pointRadius
                    val eraserRadius = radius + eraserRadiusOffset
                    val alpha = if (isSelected) 1f else 0.7f
                    
                    drawCircle(
                        color = chartTileBackgroundColor,
                        radius = eraserRadius,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(x, y),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
            
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            drawLine(
                color = scheme.textColor.copy(alpha = selectionLineAlpha),
                start = Offset(selectionX, selectedPointY),
                end = Offset(selectionX, graphBottom),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )
        }
        
        // Draw date labels below the chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            for (i in -halfWindow..halfWindow) {
                val dateIndex = centerIndex + i
                val dateStr = if (dateIndex in availableDates.indices) {
                    val date = availableDates[dateIndex]
                    if (date == today) "Сегодня" else formatDateForChart(date)
                } else {
                    ""
                }
                
                val distFromCenter = kotlin.math.abs(i.toFloat() + (currentPosition - centerIndex))
                val isNearCenter = distFromCenter < 0.5f
                val alpha = if (isNearCenter) 0.8f else 0.4f
                
                Text(
                    text = dateStr,
                    fontSize = 9.sp,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    color = scheme.textColor.copy(alpha = alpha),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // Tooltip for selected day kcal
        val selectedDate = if (centerIndex in availableDates.indices) availableDates[centerIndex] else null
        val selectedKcal = selectedDate?.let { kcalByDate[it]?.kcal } ?: 0
        
        if (selectedKcal > 0) {
            val yNormalized = selectedKcal / yAxisMax
            val tooltipY = with(density) { 
                (graphTop + graphHeight * (1 - yNormalized)).toDp() - 24.dp 
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = tooltipY.coerceAtLeast(4.dp))
                    .background(
                        scheme.plankBackground.copy(alpha = DesignTokens.chartTooltipBackgroundAlpha),
                        RoundedCornerShape(DesignTokens.chartTooltipCornerRadius)
                    )
                    .padding(horizontal = DesignTokens.chartTooltipPadding, vertical = 2.dp)
            ) {
                Text(
                    text = "$selectedKcal ккал",
                    fontSize = 11.sp,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Medium,
                    color = scheme.textColor
                )
            }
        }
    }
}

/**
 * Formats a date string for display in the chart labels.
 */
private fun formatDateForChart(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        val day = date.dayOfMonth.toString().padStart(2, '0')
        val month = when (date.monthValue) {
            1 -> "янв"
            2 -> "фев"
            3 -> "мар"
            4 -> "апр"
            5 -> "май"
            6 -> "июн"
            7 -> "июл"
            8 -> "авг"
            9 -> "сен"
            10 -> "окт"
            11 -> "ноя"
            12 -> "дек"
            else -> ""
        }
        "$day.$month"
    } catch (e: Exception) {
        ""
    }
}
