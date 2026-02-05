package com.example.vitanlyapp.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.ui.design.DesignTokens

/**
 * Skeleton-версия главного экрана для отображения во время загрузки.
 * Показывает структуру приложения с shimmer-эффектом.
 */
@Composable
fun MainScreenSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    val shimmerColors = listOf(
        DesignTokens.surfaceMain.copy(alpha = 0.6f),
        DesignTokens.surfaceMain.copy(alpha = 0.9f),
        DesignTokens.surfaceMain.copy(alpha = 0.6f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignTokens.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize()
                .padding(DesignTokens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.tileSpacing)
        ) {
            // Верхняя плитка (КБЖУ) - самая большая при загрузке
            SkeletonTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(DesignTokens.tileWeightExpanded),
                shimmerProgress = shimmerProgress,
                shimmerColors = shimmerColors
            )

            // Средняя плитка (Ввод еды)
            SkeletonTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(DesignTokens.tileWeightCollapsed),
                shimmerProgress = shimmerProgress,
                shimmerColors = shimmerColors
            )

            // Нижняя плитка (Чат)
            SkeletonTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(DesignTokens.tileWeightCollapsed),
                shimmerProgress = shimmerProgress,
                shimmerColors = shimmerColors
            )
        }
    }
}

@Composable
private fun SkeletonTile(
    modifier: Modifier = Modifier,
    shimmerProgress: Float,
    shimmerColors: List<Color>
) {
    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerProgress * 1000f - 500f, 0f),
        end = Offset(shimmerProgress * 1000f, 0f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DesignTokens.tileCornerRadius))
            .background(shimmerBrush)
    )
}
