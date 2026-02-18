package com.example.vitanlyapp.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.FWheelPickerFocusVertical
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val WEIGHT_MIN = 30f
private const val WEIGHT_MAX = 200f
private const val WEIGHT_STEP = 0.1f

/**
 * Ленивая инициализация списка весов.
 * Список создается только при первом обращении, а не при загрузке класса.
 */
private val weights: List<Float> by lazy {
    val capacity = ((WEIGHT_MAX - WEIGHT_MIN) / WEIGHT_STEP).toInt() + 1
    buildList(capacity) {
        var v = WEIGHT_MIN
        while (v <= WEIGHT_MAX + 0.01f) {
            add(v)
            v += WEIGHT_STEP
        }
    }
}

private val itemHeightDp = 40.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeightWheelPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    val scope = rememberCoroutineScope()
    
    // Inline editing state
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(TextFieldValue("")) }

    val initialIndex = weights.indexOfFirst { abs(it - value) < 0.06f }.coerceIn(0, weights.lastIndex)
    val state = rememberFWheelPickerState(initialIndex = initialIndex)

    LaunchedEffect(value) {
        val idx = weights.indexOfFirst { abs(it - value) < 0.06f }.coerceIn(0, weights.lastIndex)
        if (state.currentIndex != idx && !state.isScrollInProgress) {
            state.animateScrollToIndex(idx)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.currentIndex }
            .collect { idx ->
                val w = weights.getOrNull(idx) ?: return@collect
                if (abs(w - value) > 0.05f) {
                    onValueChange(w)
                }
            }
    }

    // Confirm edited value
    fun confirmEdit() {
        if (!isEditing) return
        val parsed = editText.text.replace(',', '.').toFloatOrNull()
            ?.coerceIn(WEIGHT_MIN, WEIGHT_MAX)
        if (parsed != null) {
            val closest = weights.minByOrNull { abs(it - parsed) } ?: parsed
            onValueChange(closest)
            val idx = weights.indexOfFirst { abs(it - closest) < 0.06f }.coerceIn(0, weights.lastIndex)
            scope.launch { state.animateScrollToIndex(idx) }
        }
        isEditing = false
    }

    // Enter edit mode
    fun enterEditMode() {
        val currentWeight = weights.getOrNull(state.currentIndex) ?: value
        val text = "%.1f".format(currentWeight).replace('.', ',')
        editText = TextFieldValue(
            text = text,
            selection = TextRange(0, text.length)
        )
        isEditing = true
    }

    Box(
        modifier = modifier
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { /* обычный клик - ничего не делаем, чтобы не мешать скроллу */ },
                onLongClick = { enterEditMode() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEditing) {
            FVerticalWheelPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .graphicsLayer { clip = false },
                count = weights.size,
                state = state,
                key = { weights[it].toString() },
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
                    val weight = weights[index]
                    val isSelected = index == state.currentIndexSnapshot
                    val targetColor = if (isSelected) scheme.textColor else scheme.borderSoft
                    val animatedColor by animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = tween(durationMillis = 350),
                        label = "weight_item_color"
                    )

                    Text(
                        text = "%.1f".format(weight).replace('.', ','),
                        fontSize = DesignTokens.plankFontSizeNumber,
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontWeight = FontWeight.Thin,
                        fontStyle = FontStyle.Normal,
                        color = animatedColor
                    )
                }
            )
        } else {
            // Edit mode - show text field
            EditableWeightField(
                textFieldValue = editText,
                onTextChange = { newValue ->
                    val filtered = newValue.text.filter { it.isDigit() || it == ',' || it == '.' }
                    if (filtered.length <= 5) {
                        editText = newValue.copy(text = filtered)
                    }
                },
                onConfirm = { confirmEdit() },
                textStyle = TextStyle(
                    fontSize = DesignTokens.plankFontSizeNumber,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Thin,
                    fontStyle = FontStyle.Normal,
                    color = scheme.textColor,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun EditableWeightField(
    textFieldValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    onConfirm: () -> Unit,
    textStyle: TextStyle
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var hasFocusedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Small delay to ensure composition is complete
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = onTextChange,
        modifier = Modifier
            .widthIn(min = 60.dp, max = 100.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasFocusedOnce = true
                } else if (hasFocusedOnce) {
                    // Only confirm if we had focus before and now lost it
                    onConfirm()
                }
            },
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                onConfirm()
            }
        ),
        cursorBrush = SolidColor(textStyle.color)
    )
}
