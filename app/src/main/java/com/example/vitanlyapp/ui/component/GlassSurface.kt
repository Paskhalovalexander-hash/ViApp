package com.example.vitanlyapp.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Поверхность с эффектом glassmorphism (размытие фона).
 *
 * На Android 12+ использует Haze для backdrop blur.
 * На более старых версиях — fallback к полупрозрачному фону.
 *
 * @param hazeState Состояние Haze (должно быть создано на уровне экрана и привязано к фону через hazeSource())
 * @param modifier Модификатор
 * @param shape Форма поверхности
 * @param blurRadius Радиус размытия
 * @param tintColor Цвет тонировки (по умолчанию из темы)
 * @param fallbackColor Цвет для fallback на старых Android (по умолчанию из темы)
 * @param showBorder Показывать градиентную границу
 * @param content Контент внутри поверхности
 */
@Composable
fun GlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DesignTokens.tileCornerRadius),
    blurRadius: Dp = DesignTokens.blurRadius,
    tintColor: Color = LocalAppColorScheme.current.glassTint,
    fallbackColor: Color = LocalAppColorScheme.current.glassBackgroundColor,
    showBorder: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = LocalAppColorScheme.current
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Градиентная граница (macOS-стиль)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            scheme.borderGradientTop,
            scheme.borderGradientBottom
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (supportsBlur) {
                    Modifier.hazeEffect(state = hazeState) {
                        this.blurRadius = blurRadius
                        tints = listOf(HazeTint(tintColor.copy(alpha = DesignTokens.glassTintAlpha)))
                        noiseFactor = DesignTokens.glassNoise
                    }
                } else {
                    // Fallback для Android < 12: полупрозрачный фон
                    Modifier.background(fallbackColor, shape)
                }
            )
            .then(
                if (showBorder) {
                    Modifier.border(DesignTokens.tileBorderWidth, gradientBorder, shape)
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * Модификатор для применения glassmorphism эффекта к любому элементу.
 *
 * @param hazeState Состояние Haze
 * @param shape Форма элемента
 * @param blurRadius Радиус размытия
 * @param tintColor Цвет тонировки
 */
@Composable
fun Modifier.glassmorphism(
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(DesignTokens.tileCornerRadius),
    blurRadius: Dp = DesignTokens.blurRadius,
    tintColor: Color = LocalAppColorScheme.current.glassTint,
    fallbackColor: Color = LocalAppColorScheme.current.glassBackgroundColor
): Modifier {
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    return this
        .clip(shape)
        .then(
            if (supportsBlur) {
                Modifier.hazeEffect(state = hazeState) {
                    this.blurRadius = blurRadius
                    tints = listOf(HazeTint(tintColor.copy(alpha = DesignTokens.glassTintAlpha)))
                    noiseFactor = DesignTokens.glassNoise
                }
            } else {
                Modifier.background(fallbackColor, shape)
            }
        )
}
