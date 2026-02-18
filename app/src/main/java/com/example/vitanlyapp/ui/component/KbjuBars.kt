package com.example.vitanlyapp.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.example.vitanlyapp.domain.model.KbjuBarStat
import com.example.vitanlyapp.ui.design.AppColorScheme
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.example.vitanlyapp.ui.design.shapedSurface

// Плавная кривая: cubic-bezier(0.22, 1, 0.36, 1)
private val smoothEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private val barAnimationSpec = tween<Float>(
    durationMillis = DesignTokens.tileTransitionDurationMs,
    easing = smoothEasing
)

/**
 * Список КБЖУ-баров: Белок, Жиры, Углеводы.
 * Каждый бар имеет свой градиент согласно дизайну.
 * Размеры вычисляются динамически на основе доступной высоты.
 *
 * @param interactionsEnabled при false клики по барам не обрабатываются (для свёрнутой плитки)
 */
@Composable
@Suppress("UnusedBoxWithConstraintsScope")
fun KbjuBars(
    stats: List<KbjuBarStat>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    interactionsEnabled: Boolean = true
) {
    val scheme = LocalAppColorScheme.current
    var showValueMax by remember { mutableStateOf(setOf<Int>()) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        val ratio = DesignTokens.barSpacing / DesignTokens.barHeight
        // Размеры из layout — без отдельной анимации, чтобы шли в такт с weight/scale плитки
        val barHeight = (availableHeight / (4 * ratio + 3)).coerceAtMost(DesignTokens.barHeight)
        val spacing = (barHeight * ratio).coerceAtMost(DesignTokens.barSpacing)
        val sizeScale = barHeight / DesignTokens.barHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            stats.forEachIndexed { index, stat ->
                val label = labels.getOrNull(index) ?: ""
                val brush = when (index) {
                    0 -> scheme.barProteinBrush
                    1 -> scheme.barFatBrush
                    2 -> scheme.barCarbsBrush
                    else -> scheme.barProteinBrush
                }
                KbjuBar(
                    stat = stat,
                    label = label,
                    fillBrush = brush,
                    barHeight = barHeight,
                    barCornerRadius = DesignTokens.barCornerRadius * sizeScale,
                    barTextPaddingStart = DesignTokens.barTextPaddingStart * sizeScale,
                    fontSize = 13.sp * sizeScale,
                    scheme = scheme,
                    showValueMax = index in showValueMax,
                    interactionsEnabled = interactionsEnabled,
                    onClick = { showValueMax = if (index in showValueMax) showValueMax - index else showValueMax + index }
                )
            }
        }
    }
}

@Composable
private fun KbjuBar(
    stat: KbjuBarStat,
    label: String,
    fillBrush: Brush,
    barHeight: Dp = DesignTokens.barHeight,
    barCornerRadius: Dp = DesignTokens.barCornerRadius,
    barTextPaddingStart: Dp = DesignTokens.barTextPaddingStart,
    fontSize: androidx.compose.ui.unit.TextUnit = DesignTokens.fontSizeLabel,
    scheme: AppColorScheme,
    showValueMax: Boolean = false,
    interactionsEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    // До 100%: доля = value/max, но не больше 1
    val fillFraction = (stat.percent / 100f).coerceIn(0f, 1f)
    // Свыше 100%: доля красного оверлея = (value - max) / max
    val overflowFraction = (stat.percent / 100f - 1f).coerceIn(0f, Float.MAX_VALUE)
    val overflowCapped = overflowFraction.coerceAtMost(1f)

    val greenFraction = if (overflowCapped <= 0f) fillFraction else 1f
    val overflowDisplay = if (overflowCapped <= 0f) 0f else overflowCapped

    val animatedFill by animateFloatAsState(
        targetValue = greenFraction,
        animationSpec = barAnimationSpec
    )
    val animatedOverflow by animateFloatAsState(
        targetValue = overflowDisplay,
        animationSpec = barAnimationSpec
    )
    val overflowFillTarget = if (overflowDisplay > 0f) 1f else 0f
    val animatedOverflowFill by animateFloatAsState(
        targetValue = overflowFillTarget,
        animationSpec = barAnimationSpec
    )

    val barShape = RoundedCornerShape(barCornerRadius)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .then(
                    if (interactionsEnabled) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .shapedSurface(
                    shape = barShape,
                    elevation = 0.dp,
                    backgroundColor = scheme.barBackground
                )
        ) {
            // 1. Градиентный fill — на всю ширину трека
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(animatedFill)
                    .height(barHeight)
                    .background(fillBrush, barShape)
            )

            // 2. Красный overflow — с начала поверх
            if (animatedOverflow > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(animatedOverflow)
                        .height(barHeight)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(animatedOverflowFill)
                            .height(barHeight)
                            .background(scheme.barOverflow, barShape)
                    )
                }
            }

            // 3. Лейбл слева — текст прямо на фоне бара, как в плашке «текущий вес»
            Text(
                text = label,
                color = scheme.textColor,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = barTextPaddingStart)
            )

            // 4. Процент или N/N max справа — текст прямо на фоне бара
            Text(
                text = if (showValueMax) "${stat.current}/${stat.max}" else "${stat.percent.toInt()}%",
                color = scheme.textColor,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = barTextPaddingStart)
            )
        }
    }
}
