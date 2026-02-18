package com.example.vitanlyapp.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.example.vitanlyapp.ui.design.CustomShapes
import com.example.vitanlyapp.ui.design.shapedSurface
import kotlin.math.roundToInt

private val trackHeight = 12.dp
private val thumbSize = 24.dp
private val plankHeight = 72.dp
private val labelFontSize = 11.sp

/**
 * Ползунок выбора физической активности.
 * 5 дискретных уровней, градиентный трек, возвращает коэффициент для формулы TDEE.
 *
 * @param value Коэффициент активности (1.2 .. 1.9)
 * @param onValueChange Колбэк с новым коэффициентом
 * @param embedded true — только контент без внешней обёртки (для встраивания в блок)
 * @param showTitle показывать ли подпись «активность» сверху (false когда заголовок у родителя)
 */
@Composable
fun ActivitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    showTitle: Boolean = true
) {
    val scheme = LocalAppColorScheme.current
    val level = remember(value) { ActivityLevel.fromCoefficient(value) }
    var trackWidthPx by remember { mutableFloatStateOf(400f) }  // fallback до onSizeChanged, иначе usableWidth <= 0
    val density = LocalDensity.current

    fun updateFromPosition(xPx: Float) {
        val thumbPx = with(density) { thumbSize.toPx() }
        val usableWidth = trackWidthPx - thumbPx
        val thumbRadius = thumbPx / 2
        if (usableWidth > 0) {
            val rawFraction = ((xPx - thumbRadius) / usableWidth).coerceIn(0f, 1f)
            val index = (rawFraction * (ActivityLevel.levels.size - 1)).roundToInt()
                .coerceIn(0, ActivityLevel.levels.size - 1)
            onValueChange(ActivityLevel.levels[index].coefficient)
        }
    }

    val fraction = (ActivityLevel.levels.indexOf(level).toFloat() / (ActivityLevel.levels.size - 1))
        .coerceIn(0f, 1f)
    val thumbOffsetPx by animateFloatAsState(
        targetValue = fraction * (trackWidthPx - with(density) { thumbSize.toPx() }),
        animationSpec = tween(DesignTokens.animationNormal),
        label = "thumbOffset"
    )

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showTitle) {
                Text(
                    text = "активность",
                    fontSize = DesignTokens.plankFontSizeLabel,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Thin,
                    fontStyle = FontStyle.Normal,
                    color = scheme.textColor,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight + thumbSize)
                    .onSizeChanged { trackWidthPx = it.width.toFloat() }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> updateFromPosition(offset.x) },
                            onDrag = { change, _ ->
                                change.consume()
                                updateFromPosition(change.position.x)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { offset -> updateFromPosition(offset.x) })
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    scheme.shapeGradientNeutralStart,
                                    scheme.shapeGradientAccentEnd
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                        .size(thumbSize)
                        .align(Alignment.CenterStart)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(scheme.surfaceInput)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = level.label,
                fontSize = labelFontSize,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin,
                color = DesignTokens.textColor
            )
        }
    }

    if (embedded) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* перехват клика */ }
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(plankHeight)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* перехват клика */ }
                .shapedSurface(
                    shape = CustomShapes.rounded(cornerRadius = DesignTokens.plankCornerRadius),
                    elevation = 0.dp,
                    backgroundColor = scheme.plankBackground
                )
                .padding(horizontal = DesignTokens.tilePadding * 2, vertical = 12.dp)
        ) {
            content()
        }
    }
}
