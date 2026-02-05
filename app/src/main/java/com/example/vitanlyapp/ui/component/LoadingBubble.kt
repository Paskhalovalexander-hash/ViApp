package com.example.vitanlyapp.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.ui.design.DesignTokens

/**
 * Цветовая схема для анимации загрузки.
 * Аналогична HintColorScheme из ChatHintsCarousel.
 */
private data class LoadingColorScheme(
    val centerColor: Color,
    val edgeColor: Color
)

/**
 * Палитра цветов для индикатора загрузки.
 * Используем те же цвета, что и в подсказках чата.
 */
private val loadingColorSchemes = listOf(
    // Терракотовый/коричневый
    LoadingColorScheme(
        centerColor = Color(0xFFB07860),
        edgeColor = Color(0xFF5C3D2E)
    ),
    // Синий/индиго
    LoadingColorScheme(
        centerColor = Color(0xFF6B7FA8),
        edgeColor = Color(0xFF2E3D5C)
    ),
    // Зелёный/изумрудный
    LoadingColorScheme(
        centerColor = Color(0xFF6BA08B),
        edgeColor = Color(0xFF2E5C4A)
    ),
    // Фиолетовый
    LoadingColorScheme(
        centerColor = Color(0xFF8B6BA0),
        edgeColor = Color(0xFF4A2E5C)
    ),
    // Тёплый оранжевый
    LoadingColorScheme(
        centerColor = Color(0xFFA08B6B),
        edgeColor = Color(0xFF5C4A2E)
    ),
    // Розовый/малиновый
    LoadingColorScheme(
        centerColor = Color(0xFFA06B8B),
        edgeColor = Color(0xFF5C2E4A)
    )
)

/** Размер круга индикатора загрузки */
private val loadingCircleSize = 36.dp

/** Длительность одного цикла анимации (перелив через все цвета) */
private const val ANIMATION_DURATION_MS = 6000

/**
 * Индикатор загрузки в виде круга с переливающимися цветами.
 * Отображается вместо сообщения ассистента пока идёт запрос к LLM.
 * Использует те же цвета, что и карусель подсказок.
 *
 * @param modifier Модификатор
 */
@Composable
fun LoadingBubble(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingColors")
    
    // Анимируем progress от 0 до 1 и обратно бесконечно
    val colorProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = loadingColorSchemes.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = ANIMATION_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorProgress"
    )
    
    // Вычисляем текущие цвета на основе progress
    val currentIndex = colorProgress.toInt() % loadingColorSchemes.size
    val nextIndex = (currentIndex + 1) % loadingColorSchemes.size
    val fraction = colorProgress - colorProgress.toInt()
    
    val currentScheme = loadingColorSchemes[currentIndex]
    val nextScheme = loadingColorSchemes[nextIndex]
    
    val centerColor = lerp(currentScheme.centerColor, nextScheme.centerColor, fraction)
    val edgeColor = lerp(currentScheme.edgeColor, nextScheme.edgeColor, fraction)
    
    // Радиальный градиент от центра к краям
    val radialGradient = Brush.radialGradient(
        colors = listOf(centerColor, edgeColor),
        center = Offset(0.5f, 0.5f),
        radius = 100f
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.tilePadding, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(loadingCircleSize)
                .clip(CircleShape)
                .background(radialGradient)
        )
    }
}
