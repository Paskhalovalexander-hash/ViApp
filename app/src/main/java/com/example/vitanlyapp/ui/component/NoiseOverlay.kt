package com.example.vitanlyapp.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/**
 * Накладывает очень мелкую текстуру шума поверх фона.
 * Создаёт матовый эффект на чёрном фоне.
 *
 * @param noiseAlpha Прозрачность шума (0.0 - 1.0). По умолчанию 0.03 для едва заметного эффекта.
 * @param noiseDensity Плотность точек шума (0.0 - 1.0). По умолчанию 0.15.
 */
@Composable
fun NoiseOverlay(
    modifier: Modifier = Modifier,
    noiseAlpha: Float = 0.03f,
    noiseDensity: Float = 0.15f
) {
    // Генерируем шум один раз и запоминаем
    val noisePoints = remember(noiseDensity) {
        generateNoisePoints(density = noiseDensity)
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawNoise(noisePoints, noiseAlpha)
    }
}

/**
 * Генерирует точки шума с заданной плотностью.
 * Использует псевдослучайные координаты в диапазоне [0, 1].
 */
private fun generateNoisePoints(
    density: Float,
    seed: Long = 42L
): List<Pair<Float, Float>> {
    val random = Random(seed)
    // Количество точек зависит от плотности (примерно 10000 * density)
    val count = (10000 * density).toInt().coerceIn(500, 15000)
    
    return List(count) {
        random.nextFloat() to random.nextFloat()
    }
}

/**
 * Рисует точки шума на Canvas.
 */
private fun DrawScope.drawNoise(
    points: List<Pair<Float, Float>>,
    alpha: Float
) {
    val noiseColor = Color.White.copy(alpha = alpha)
    val pointSize = 1f  // Очень мелкие точки
    
    points.forEach { (xFraction, yFraction) ->
        drawCircle(
            color = noiseColor,
            radius = pointSize,
            center = androidx.compose.ui.geometry.Offset(
                x = xFraction * size.width,
                y = yFraction * size.height
            )
        )
    }
}
