package com.example.vitanlyapp.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.FWheelPickerFocusVertical
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import kotlin.math.abs

private val activityBlue = Color(0xFF4A90D9)
private val activityRed = Color(0xFFD94A4A)
private val itemHeightDp = 40.dp

@Composable
fun ActivityWheelPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    val levels = ActivityLevel.levels
    val levelCount = levels.size

    val selectedLevel = ActivityLevel.fromCoefficient(value)
    val initialIndex = levels.indexOf(selectedLevel).coerceIn(0, levelCount - 1)
    val state = rememberFWheelPickerState(initialIndex = initialIndex)

    LaunchedEffect(value) {
        val idx = levels.indexOf(ActivityLevel.fromCoefficient(value)).coerceIn(0, levelCount - 1)
        if (state.currentIndex != idx && !state.isScrollInProgress) {
            state.animateScrollToIndex(idx)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.currentIndex }
            .collect { idx ->
                val level = levels.getOrNull(idx) ?: return@collect
                if (abs(level.coefficient - value) > 0.01f) {
                    onValueChange(level.coefficient)
                }
            }
    }

    Box(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { },
        contentAlignment = Alignment.Center
    ) {
        FVerticalWheelPicker(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .graphicsLayer { clip = false },
            count = levelCount,
            state = state,
            key = { levels[it].name },
            itemHeight = itemHeightDp,
            unfocusedCount = 1,
            focus = {
                FWheelPickerFocusVertical(
                    dividerSize = 0.dp,
                    dividerColor = scheme.borderSoft.copy(alpha = 0f)
                )
            },
            display = { index ->
                val focused = index == state.currentIndexSnapshot
                val showNeighbors = state.isScrollInProgress
                val visible = focused || showNeighbors
                Box(Modifier.alpha(if (visible) 1f else 0f)) {
                    Content(index)
                }
            },
            content = { index ->
                val level = levels[index]
                val isSelected = index == state.currentIndexSnapshot
                val gradientT = if (levelCount > 1) index / (levelCount - 1).toFloat() else 0f
                val gradientColor = lerp(activityBlue, activityRed, gradientT)
                val targetColor = if (isSelected) gradientColor else scheme.borderSoft
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 350),
                    label = "activity_item_color"
                )
                Text(
                    text = level.label,
                    fontSize = DesignTokens.plankFontSizeLabel,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Thin,
                    fontStyle = FontStyle.Normal,
                    color = animatedColor
                )
            }
        )
    }
}
