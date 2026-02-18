package com.example.vitanlyapp.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import kotlin.math.PI
import kotlin.math.sin

// Плавная кривая: cubic-bezier(0.22, 1, 0.36, 1)
private val smoothEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private val waveLevelAnimationSpec = tween<Float>(
    durationMillis = DesignTokens.tileTransitionDurationMs,
    easing = smoothEasing
)

/**
 * Волна как индикатор калорий: заполнение снизу вверх.
 * Цвет: бирюзовый полупрозрачный rgba(160, 220, 220, 0.6)
 * 
 * @param animateWave Включить анимацию колебания волны. При false волна статична.
 *                    Используется для отложенного запуска тяжелых анимаций при старте.
 */
@Composable
fun KbjuWave(
    percent: Float,
    overflow: Boolean,
    modifier: Modifier = Modifier,
    animateWave: Boolean = true
) {
    val scheme = LocalAppColorScheme.current
    val fillFraction = (percent / 100f).coerceIn(0f, 1f)
    val overflowFraction = (percent / 100f - 1f).coerceIn(0f, Float.MAX_VALUE)
    val overflowCapped = overflowFraction.coerceAtMost(1f)

    val greenFraction = if (overflowCapped <= 0f) fillFraction else 1f
    val overflowDisplay = if (overflowCapped <= 0f) 0f else overflowCapped

    val levelYFraction by animateFloatAsState(
        targetValue = 1f - greenFraction,
        animationSpec = waveLevelAnimationSpec
    )
    val overflowLevelFraction by animateFloatAsState(
        targetValue = overflowDisplay,
        animationSpec = waveLevelAnimationSpec
    )

    // Циклические колебания волны (отключается для оптимизации запуска)
    val amplitudePhase = remember { Animatable(0f) }
    LaunchedEffect(animateWave) {
        if (!animateWave) {
            amplitudePhase.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            amplitudePhase.animateTo(
                targetValue = (2 * PI).toFloat(),
                animationSpec = tween(
                    durationMillis = DesignTokens.waveOscillationDurationMs,
                    easing = LinearEasing
                )
            )
            amplitudePhase.snapTo(0f)
        }
    }
    val ampPhase = amplitudePhase.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Бирюзовая волна калорий
        val levelY = h * levelYFraction
        drawWavePath(
            width = w,
            height = h,
            levelY = levelY,
            amplitudePhase = ampPhase,
            brush = Brush.verticalGradient(
                colors = listOf(
                    scheme.waveColor,
                    scheme.waveColor.copy(alpha = 0.4f)
                ),
                startY = 0f,
                endY = h
            )
        )

        // 2. При overflow — красная волна снизу
        if (overflowLevelFraction > 0f) {
            val redLevelY = h * (1f - overflowLevelFraction)
            drawWavePath(
                width = w,
                height = h,
                levelY = redLevelY,
                amplitudePhase = ampPhase,
                brush = Brush.verticalGradient(
                    colors = listOf(scheme.barOverflow, scheme.barOverflow),
                    startY = 0f,
                    endY = h
                ),
                fromTop = false
            )
        }
    }
}

/**
 * Волна: 1 период синуса на всю ширину.
 */
private fun DrawScope.drawWavePath(
    width: Float,
    height: Float,
    levelY: Float,
    amplitudePhase: Float,
    brush: Brush,
    fromTop: Boolean = false
) {
    val L = width
    val amplitude = L * DesignTokens.waveAmplitudeFraction
    val twoPi = (2 * PI).toFloat()
    val step = (L / 250f).coerceAtLeast(1f)

    fun waveY(x: Float): Float =
        levelY + amplitude * sin(twoPi * x / L) * sin(amplitudePhase)

    val path = Path()
    if (fromTop) {
        path.moveTo(0f, 0f)
        path.lineTo(L, 0f)
        var x = L
        while (x >= 0f) {
            path.lineTo(x, waveY(x).coerceIn(0f, height))
            x -= step
        }
        path.close()
    } else {
        path.moveTo(0f, height)
        path.lineTo(L, height)
        var x = L
        while (x >= 0f) {
            path.lineTo(x, waveY(x).coerceIn(0f, height))
            x -= step
        }
        path.lineTo(0f, waveY(0f).coerceIn(0f, height))
        path.close()
    }
    drawPath(path, brush = brush)
}
