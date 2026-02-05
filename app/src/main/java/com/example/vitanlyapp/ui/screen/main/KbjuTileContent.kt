package com.example.vitanlyapp.ui.screen.main

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.delay

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

@Composable
fun KbjuTileContent(
    kcalStat: KbjuBarStat,
    macroStats: List<KbjuBarStat>,
    activeTile: TilePosition?,
    userProfile: UserProfile?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val scheme = LocalAppColorScheme.current
    
    // Отложенный запуск анимации волны для оптимизации первого рендера
    var waveAnimationEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(WAVE_ANIMATION_DELAY_MS)
        waveAnimationEnabled = true
    }
    
    // Фон
    TileBackground()

    // Волна заполняет всю область — источник для размытия плашек
    KbjuWave(
        percent = kcalStat.percent,
        overflow = kcalStat.overflow,
        animateWave = waveAnimationEnabled,
        modifier = Modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
    )

    // Слои (снизу вверх): волна → цифры → бары
    var showCaloriePercent by remember { mutableStateOf(false) }
    LaunchedEffect(activeTile) {
        if (activeTile != null) {
            showCaloriePercent = false
        } else {
            delay(DesignTokens.caloriePercentAppearDelayMs.toLong())
            showCaloriePercent = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
        val barsTopPadding by animateDpAsState(
            targetValue = if (isExpanded) DesignTokens.topBarButtonsHeight else 0.dp,
            animationSpec = tween(
                durationMillis = DesignTokens.tileTransitionDurationMs,
                easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.tilePadding)
                .padding(top = barsTopPadding)
        ) {
            KbjuBars(
                stats = macroStats,
                labels = listOf("Белок", "Жиры", "Углеводы"),
                interactionsEnabled = isExpanded,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isExpanded) Modifier.height(barsHeight)
                        else Modifier.fillMaxSize()
                    )
            )
            if (isExpanded && userProfile != null) {
                UserProfileDisplay(
                    profile = userProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DesignTokens.barSpacing)
                )
            }
        }
    }
}

/**
 * Плашки "вес" и "активность" с wheel picker, выезжающие при развёртывании плитки.
 *
 * @param hazeState Состояние Haze для glassmorphism (опционально)
 */
@Composable
fun KbjuTileWheelOverflowContent(
    isExpanded: Boolean,
    currentWeight: Float,
    onWeightChange: (Float) -> Unit,
    activityCoefficient: Float,
    onActivityChange: (Float) -> Unit,
    hazeState: HazeState? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isExpanded) bottomBlockHeight else 0.dp)
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

@Composable
private fun BottomBlock(
    title: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val scheme = LocalAppColorScheme.current
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useBlur = hazeState != null && supportsBlur

    Box(
        modifier = modifier
            .height(bottomBlockHeight)
            .graphicsLayer { clip = false }
    ) {
        val blockShape = RoundedCornerShape(bottomBlockCornerRadius)
        val gradientBorder = Brush.verticalGradient(
            colors = listOf(
                scheme.borderGradientTop,
                scheme.borderGradientBottom
            )
        )

        // Фон плашки: glassmorphism или обычный
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(blockShape)
                .then(
                    if (useBlur && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = DesignTokens.blurRadius
                            tints = listOf(HazeTint(scheme.glassTint.copy(alpha = DesignTokens.glassTintAlpha)))
                            noiseFactor = DesignTokens.glassNoise
                        }
                    } else {
                        Modifier.background(scheme.plankBackground, blockShape)
                    }
                )
                .border(DesignTokens.tileBorderWidth, gradientBorder, blockShape)
                .padding(DesignTokens.tilePadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = title,
                    fontSize = DesignTokens.plankFontSizeLabel,
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
