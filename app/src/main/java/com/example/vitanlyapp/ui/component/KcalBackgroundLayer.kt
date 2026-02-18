package com.example.vitanlyapp.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme

private val kcalAnimationSpec = tween<Float>(
    durationMillis = DesignTokens.tileTransitionDurationMs,
    easing = FastOutSlowInEasing
)

/**
 * Задний слой верхней плитки: индикатор калорий на всю высоту.
 * Один трек, одна ось (снизу вверх). До 100% — зелёный fill; при overflow красный поверх,
 * суммарная высота зелёного и красного вписывается в 100% плитки.
 */
@Composable
fun KcalBackgroundLayer(
    percent: Float,
    overflow: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    // totalFraction = value/max (может быть > 1 при overflow)
    val totalFraction = (percent / 100f).coerceAtLeast(0f)

    // Без overflow: один зелёный fill до value/max. При overflow: зелёный и красный масштабируем в 100%
    val (greenHeightFraction, redHeightFraction) = if (totalFraction <= 1f || totalFraction <= 0f) {
        totalFraction.coerceIn(0f, 1f) to 0f
    } else {
        val green = 1f / totalFraction
        val red = (totalFraction - 1f) / totalFraction
        green to red
    }

    val animatedGreen by animateFloatAsState(
        targetValue = greenHeightFraction,
        animationSpec = kcalAnimationSpec
    )
    val animatedRed by animateFloatAsState(
        targetValue = redHeightFraction,
        animationSpec = kcalAnimationSpec
    )
    val redFillTarget = if (redHeightFraction > 0f) 1f else 0f
    val animatedRedFill by animateFloatAsState(
        targetValue = redFillTarget,
        animationSpec = kcalAnimationSpec
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.barBackground)
        )
        if (redHeightFraction <= 0f) {
            // Без overflow: один зелёный fill до value/max
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedGreen)
                    .background(scheme.barFill)
            )
        } else {
            // При overflow: Column снизу вверх — зелёный и красный в сумме 100% высоты
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedGreen + animatedRed),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (animatedRed > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedRed)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(animatedRedFill)
                                .background(scheme.barOverflow)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedGreen)
                        .background(scheme.barFill)
                )
            }
        }
    }
}
