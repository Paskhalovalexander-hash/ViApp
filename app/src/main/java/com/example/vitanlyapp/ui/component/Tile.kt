package com.example.vitanlyapp.ui.component

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.domain.model.TilePosition
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// Плавное замедление в конце: cubic-bezier(0.22, 0.61, 0.36, 1)
private val smoothEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

/**
 * Универсальная плитка с единым скруглённым контейнером.
 * 
 * ВАЖНО: shape и clip применяются ТОЛЬКО к корневому контейнеру.
 * Весь контент рисуется ВНУТРИ и автоматически обрезается по форме.
 * Контент сам управляет своими отступами через padding.
 *
 * @param position Позиция плитки
 * @param isExpanded Развёрнута ли плитка
 * @param isCollapsed Свёрнута ли плитка
 * @param onClick Обработчик клика
 * @param modifier Модификатор
 * @param shape Форма плитки (по умолчанию RoundedCornerShape)
 * @param backgroundBrush Градиент фона (если null — используется сплошной цвет)
 * @param backgroundColor Цвет фона (используется если backgroundBrush = null)
 * @param hazeState Состояние Haze для glassmorphism (если null — blur отключён)
 * @param blurRadius Радиус размытия для glassmorphism
 * @param edgeToEdge Без внутреннего padding (для edge-to-edge плиток)
 * @param content Контент плитки
 * @param overflowContent Опциональный контент без clip (для wheel picker overflow)
 * @param tileTransitionDurationMs Длительность анимации scale/elevation (если null — DesignTokens.tileTransitionDurationMs; для TOP/MIDDLE в compact можно передать topMiddlePostSnapDurationMs)
 * @param collapseProgress Прогресс сворачивания TOP (0..1); если передан для TOP/MIDDLE — scale/elevation от него (бамп с topMiddleBumpStartProgress), без отдельной анимации после snap
 * @param tapToExpandOverlay Когда true, поверх контента рисуется прозрачный слой, перехватывающий тапы и вызывающий onClick (для разворота плитки из среднего состояния)
 */
@Composable
@Suppress("UnusedBoxWithConstraintsScope")
fun Tile(
    position: TilePosition,
    isExpanded: Boolean,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DesignTokens.tileCornerRadius),
    backgroundBrush: Brush? = null,
    backgroundColor: Color = LocalAppColorScheme.current.surfaceMain,
    hazeState: HazeState? = null,
    blurRadius: Dp = DesignTokens.blurRadius,
    edgeToEdge: Boolean = false,
    overflowContent: @Composable (() -> Unit)? = null,
    tileTransitionDurationMs: Int? = null,
    collapseProgress: Float? = null,
    tapToExpandOverlay: Boolean = false,
    content: @Composable () -> Unit
) {
    val useProgressDrivenBump = collapseProgress != null && (position == TilePosition.TOP || position == TilePosition.MIDDLE)
    val bumpProgress = if (useProgressDrivenBump) {
        when {
            collapseProgress <= DesignTokens.topMiddleBumpStartProgress -> 0f
            collapseProgress >= 1f -> 1f
            else -> (collapseProgress - DesignTokens.topMiddleBumpStartProgress) / (1f - DesignTokens.topMiddleBumpStartProgress)
        }
    } else 0f

    val durationMs = tileTransitionDurationMs ?: DesignTokens.tileTransitionDurationMs
    val transitionSpec = tween<Float>(durationMillis = durationMs, easing = smoothEasing)
    val scaleByState by animateFloatAsState(
        targetValue = when {
            isExpanded -> DesignTokens.tileScaleExpanded
            isCollapsed -> DesignTokens.tileScaleCollapsed
            else -> 1f
        },
        animationSpec = transitionSpec
    )
    val elevationByState by animateDpAsState(
        targetValue = if (isExpanded) DesignTokens.tileElevationExpanded else DesignTokens.tileElevationNormal,
        animationSpec = tween<Dp>(durationMillis = durationMs, easing = smoothEasing)
    )

    val scale = if (useProgressDrivenBump) {
        when (position) {
            TilePosition.TOP -> DesignTokens.tileScaleExpanded + (DesignTokens.tileScaleCollapsed - DesignTokens.tileScaleExpanded) * bumpProgress
            TilePosition.MIDDLE -> DesignTokens.tileScaleCollapsed + (DesignTokens.tileScaleExpanded - DesignTokens.tileScaleCollapsed) * bumpProgress
            else -> scaleByState
        }
    } else scaleByState
    val elevation = if (useProgressDrivenBump) {
        when (position) {
            TilePosition.TOP -> (DesignTokens.tileElevationExpanded.value + (DesignTokens.tileElevationNormal.value - DesignTokens.tileElevationExpanded.value) * bumpProgress).dp
            TilePosition.MIDDLE -> (DesignTokens.tileElevationNormal.value + (DesignTokens.tileElevationExpanded.value - DesignTokens.tileElevationNormal.value) * bumpProgress).dp
            else -> elevationByState
        }
    } else elevationByState

    val scheme = LocalAppColorScheme.current
    // macOS-градиентная обводка: верх светлый, низ тёмный
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            scheme.borderGradientTop,
            scheme.borderGradientBottom
        )
    )

    // Определяем, можно ли использовать blur (Android 12+)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useBlur = hazeState != null && supportsBlur

    // Цвета для glassmorphism
    val glassTint = scheme.glassTint
    val glassBackground = scheme.glassBackgroundColor

    // Модификатор фона — градиент, сплошной цвет или glassmorphism
    @Suppress("KotlinConstantConditions")
    fun Modifier.tileBackground(): Modifier = when {
        backgroundBrush != null -> this.background(backgroundBrush, shape)
        useBlur -> this.hazeEffect(state = hazeState!!) {
            this.blurRadius = blurRadius
            tints = listOf(HazeTint(glassTint.copy(alpha = DesignTokens.glassTintAlpha)))
            noiseFactor = DesignTokens.glassNoise
        }
        hazeState != null -> this.background(glassBackground, shape) // Fallback для Android < 12
        else -> this.background(backgroundColor, shape)
    }

    // Два слоя: тень рисуется в масштабированном размере (без ресемплинга), контент — scale поверх
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize()
            .then(if (!edgeToEdge) Modifier.padding(DesignTokens.tileSpacing / 2) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        val w = maxWidth
        val h = maxHeight
        Box(Modifier.fillMaxSize()) {
            // Задний слой: только тень по размеру (w*scale, h*scale), не масштабируется — без артефактов
            Box(
                modifier = Modifier
                    .size(w * scale, h * scale)
                    .align(Alignment.Center)
                    .shadow(elevation, shape)
                    .clip(shape)
                    .tileBackground()
            )
            // Передний слой: контент с scale только по Y (ширина полная), фон, бордер (clipped)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1f; scaleY = scale }
                    .clip(shape)
                    .tileBackground()
                    .border(DesignTokens.tileBorderWidth, gradientBorder, shape)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
            // Overflow-контент без clip (wheel picker) — рисуется поверх
            overflowContent?.let { overflow ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = 1f; scaleY = scale; clip = false }
                ) {
                    overflow()
                }
            }
            // Оверлей для тапа «развернуть» в среднем/свёрнутом состоянии (плашки и кнопки не забирают касания)
            if (tapToExpandOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onClick() }
                )
            }
        }
    }
}

/**
 * Общий фон для контента плиток.
 */
@Composable
fun TileBackground() {
    val scheme = LocalAppColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.tileBackgroundColor)
    )
}
