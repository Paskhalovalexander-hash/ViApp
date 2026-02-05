package com.example.vitanlyapp.ui.screen.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.UserGoal
import com.example.vitanlyapp.ui.design.AppColorScheme
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.shapedSurface
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.FWheelPickerFocusVertical
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Контент страницы онбординга в зависимости от шага.
 */
@Composable
fun OnboardingPage(
    step: OnboardingStep,
    state: OnboardingState,
    onBirthYearChange: (Int) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onHeightChange: (Int) -> Unit,
    onWeightChange: (Float) -> Unit,
    onActivityChange: (ActivityLevel) -> Unit,
    onGoalChange: (UserGoal) -> Unit,
    scheme: AppColorScheme
) {
    when (step) {
        OnboardingStep.BIRTH_YEAR -> BirthYearPage(
            birthYear = state.birthYear,
            onBirthYearChange = onBirthYearChange,
            scheme = scheme
        )
        OnboardingStep.GENDER -> GenderPage(
            selectedGender = state.gender,
            onGenderChange = onGenderChange,
            scheme = scheme
        )
        OnboardingStep.HEIGHT -> HeightPage(
            height = state.height,
            onHeightChange = onHeightChange,
            scheme = scheme
        )
        OnboardingStep.WEIGHT -> WeightPage(
            weight = state.weight,
            onWeightChange = onWeightChange,
            scheme = scheme
        )
        OnboardingStep.ACTIVITY -> ActivityPage(
            activityLevel = state.activityLevel,
            onActivityChange = onActivityChange,
            scheme = scheme
        )
        OnboardingStep.GOAL -> GoalPage(
            selectedGoal = state.goal,
            onGoalChange = onGoalChange,
            scheme = scheme
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Страница 1: Год рождения
// ══════════════════════════════════════════════════════════════════════════

private val yearRange = (1920..LocalDate.now().year - 10).toList()

private val yearMin = 1920
private val yearMax = LocalDate.now().year - 10

@Composable
private fun BirthYearPage(
    birthYear: Int,
    onBirthYearChange: (Int) -> Unit,
    scheme: AppColorScheme
) {
    PageContainer(
        title = "Укажите год рождения",
        subtitle = "Это нужно для расчёта вашей нормы калорий",
        scheme = scheme
    ) {
        val scope = rememberCoroutineScope()
        var showInputDialog by remember { mutableStateOf(false) }
        val initialIndex = yearRange.indexOf(birthYear).coerceIn(0, yearRange.lastIndex)
        val pickerState = rememberFWheelPickerState(initialIndex = initialIndex)

        LaunchedEffect(pickerState) {
            snapshotFlow { pickerState.currentIndex }
                .collect { idx ->
                    val year = yearRange.getOrNull(idx) ?: return@collect
                    if (year != birthYear) {
                        onBirthYearChange(year)
                    }
                }
        }

        if (showInputDialog) {
            BirthYearInputDialog(
                currentValue = birthYear,
                scheme = scheme,
                onDismiss = { showInputDialog = false },
                onConfirm = { newYear ->
                    onBirthYearChange(newYear)
                    val idx = yearRange.indexOf(newYear).coerceIn(0, yearRange.lastIndex)
                    scope.launch { pickerState.animateScrollToIndex(idx) }
                    showInputDialog = false
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showInputDialog = true },
            contentAlignment = Alignment.Center
        ) {
            FVerticalWheelPicker(
                modifier = Modifier
                    .width(150.dp)
                    .height(200.dp),
                count = yearRange.size,
                state = pickerState,
                key = { yearRange[it] },
                itemHeight = 50.dp,
                unfocusedCount = 2,
                focus = {
                    FWheelPickerFocusVertical(
                        dividerSize = 1.dp,
                        dividerColor = scheme.borderSoft
                    )
                }
            ) { index ->
                val year = yearRange[index]
                val isSelected = index == pickerState.currentIndexSnapshot
                val targetColor = if (isSelected) scheme.textColor else scheme.borderSoft
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(300),
                    label = "year_color"
                )

                Text(
                    text = year.toString(),
                    fontSize = if (isSelected) 32.sp else 24.sp,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Thin,
                    color = animatedColor
                )
            }
        }
    }
}

@Composable
private fun BirthYearInputDialog(
    currentValue: Int,
    scheme: AppColorScheme,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputText by remember(currentValue) { mutableStateOf(currentValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Введите год рождения",
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shapedSurface(
                        shape = RoundedCornerShape(DesignTokens.plankCornerRadius),
                        elevation = 0.dp,
                        backgroundColor = scheme.surfaceInput
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { s ->
                        val filtered = s.filter { it.isDigit() }
                        if (filtered.length <= 4) inputText = filtered
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = scheme.textColor,
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontWeight = FontWeight.Thin,
                        fontSize = 18.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = inputText.toIntOrNull()
                    ?.coerceIn(yearMin, yearMax) ?: return@TextButton
                onConfirm(parsed)
            }) {
                Text("OK", fontFamily = DesignTokens.fontFamilyPlank)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = DesignTokens.fontFamilyPlank)
            }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════
// Страница 2: Пол
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun GenderPage(
    selectedGender: Gender,
    onGenderChange: (Gender) -> Unit,
    scheme: AppColorScheme
) {
    PageContainer(
        title = "Укажите ваш пол",
        subtitle = "Формула расчёта отличается для мужчин и женщин",
        scheme = scheme
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GenderCard(
                gender = Gender.MALE,
                isSelected = selectedGender == Gender.MALE,
                onClick = { onGenderChange(Gender.MALE) },
                icon = Icons.Default.Male,
                scheme = scheme,
                modifier = Modifier.weight(1f)
            )
            GenderCard(
                gender = Gender.FEMALE,
                isSelected = selectedGender == Gender.FEMALE,
                onClick = { onGenderChange(Gender.FEMALE) },
                icon = Icons.Default.Female,
                scheme = scheme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GenderCard(
    gender: Gender,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    scheme: AppColorScheme,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            if (isSelected) scheme.textColor.copy(alpha = 0.5f) else scheme.borderGradientTop,
            if (isSelected) scheme.textColor.copy(alpha = 0.3f) else scheme.borderGradientBottom
        )
    )
    val backgroundColor = if (isSelected) scheme.shapeGradientAccent else scheme.shapeGradientCream

    Box(
        modifier = modifier
            .height(160.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(2.dp, gradientBorder, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = gender.label,
                tint = scheme.textColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = gender.label,
                fontSize = 20.sp,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Normal,
                color = scheme.textColor
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Страница 3: Рост
// ══════════════════════════════════════════════════════════════════════════

private val heightRange = (100..250).toList()

private const val heightMin = 100
private const val heightMax = 250

@Composable
private fun HeightPage(
    height: Int,
    onHeightChange: (Int) -> Unit,
    scheme: AppColorScheme
) {
    PageContainer(
        title = "Укажите ваш рост",
        subtitle = "Рост в сантиметрах",
        scheme = scheme
    ) {
        val scope = rememberCoroutineScope()
        var showInputDialog by remember { mutableStateOf(false) }
        val initialIndex = heightRange.indexOf(height).coerceIn(0, heightRange.lastIndex)
        val pickerState = rememberFWheelPickerState(initialIndex = initialIndex)

        LaunchedEffect(pickerState) {
            snapshotFlow { pickerState.currentIndex }
                .collect { idx ->
                    val h = heightRange.getOrNull(idx) ?: return@collect
                    if (h != height) {
                        onHeightChange(h)
                    }
                }
        }

        if (showInputDialog) {
            HeightInputDialog(
                currentValue = height,
                scheme = scheme,
                onDismiss = { showInputDialog = false },
                onConfirm = { newHeight ->
                    onHeightChange(newHeight)
                    val idx = heightRange.indexOf(newHeight).coerceIn(0, heightRange.lastIndex)
                    scope.launch { pickerState.animateScrollToIndex(idx) }
                    showInputDialog = false
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showInputDialog = true },
            contentAlignment = Alignment.Center
        ) {
            FVerticalWheelPicker(
                modifier = Modifier
                    .width(150.dp)
                    .height(200.dp),
                count = heightRange.size,
                state = pickerState,
                key = { heightRange[it] },
                itemHeight = 50.dp,
                unfocusedCount = 2,
                focus = {
                    FWheelPickerFocusVertical(
                        dividerSize = 1.dp,
                        dividerColor = scheme.borderSoft
                    )
                }
            ) { index ->
                val h = heightRange[index]
                val isSelected = index == pickerState.currentIndexSnapshot
                val targetColor = if (isSelected) scheme.textColor else scheme.borderSoft
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(300),
                    label = "height_color"
                )

                Text(
                    text = "$h см",
                    fontSize = if (isSelected) 32.sp else 24.sp,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Thin,
                    color = animatedColor
                )
            }
        }
    }
}

@Composable
private fun HeightInputDialog(
    currentValue: Int,
    scheme: AppColorScheme,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputText by remember(currentValue) { mutableStateOf(currentValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Введите рост",
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shapedSurface(
                        shape = RoundedCornerShape(DesignTokens.plankCornerRadius),
                        elevation = 0.dp,
                        backgroundColor = scheme.surfaceInput
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { s ->
                        val filtered = s.filter { it.isDigit() }
                        if (filtered.length <= 3) inputText = filtered
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = scheme.textColor,
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontWeight = FontWeight.Thin,
                        fontSize = 18.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = inputText.toIntOrNull()
                    ?.coerceIn(heightMin, heightMax) ?: return@TextButton
                onConfirm(parsed)
            }) {
                Text("OK", fontFamily = DesignTokens.fontFamilyPlank)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = DesignTokens.fontFamilyPlank)
            }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════
// Страница 4: Вес
// ══════════════════════════════════════════════════════════════════════════

private val weightValues: List<Float> = run {
    val list = mutableListOf<Float>()
    var v = 30f
    while (v <= 200f) {
        list.add(v)
        v += 0.5f
    }
    list
}

private const val weightMin = 30f
private const val weightMax = 200f

@Composable
private fun WeightPage(
    weight: Float,
    onWeightChange: (Float) -> Unit,
    scheme: AppColorScheme
) {
    PageContainer(
        title = "Укажите ваш вес",
        subtitle = "Текущий вес в килограммах",
        scheme = scheme
    ) {
        val scope = rememberCoroutineScope()
        var showInputDialog by remember { mutableStateOf(false) }
        val initialIndex = weightValues.indexOfFirst { kotlin.math.abs(it - weight) < 0.3f }
            .coerceIn(0, weightValues.lastIndex)
        val pickerState = rememberFWheelPickerState(initialIndex = initialIndex)

        LaunchedEffect(pickerState) {
            snapshotFlow { pickerState.currentIndex }
                .collect { idx ->
                    val w = weightValues.getOrNull(idx) ?: return@collect
                    if (kotlin.math.abs(w - weight) > 0.1f) {
                        onWeightChange(w)
                    }
                }
        }

        if (showInputDialog) {
            WeightInputDialog(
                currentValue = weight,
                scheme = scheme,
                onDismiss = { showInputDialog = false },
                onConfirm = { newWeight ->
                    onWeightChange(newWeight)
                    val idx = weightValues.indexOfFirst { kotlin.math.abs(it - newWeight) < 0.3f }
                        .coerceIn(0, weightValues.lastIndex)
                    scope.launch { pickerState.animateScrollToIndex(idx) }
                    showInputDialog = false
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showInputDialog = true },
            contentAlignment = Alignment.Center
        ) {
            FVerticalWheelPicker(
                modifier = Modifier
                    .width(150.dp)
                    .height(200.dp),
                count = weightValues.size,
                state = pickerState,
                key = { weightValues[it].toString() },
                itemHeight = 50.dp,
                unfocusedCount = 2,
                focus = {
                    FWheelPickerFocusVertical(
                        dividerSize = 1.dp,
                        dividerColor = scheme.borderSoft
                    )
                }
            ) { index ->
                val w = weightValues[index]
                val isSelected = index == pickerState.currentIndexSnapshot
                val targetColor = if (isSelected) scheme.textColor else scheme.borderSoft
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(300),
                    label = "weight_color"
                )

                Text(
                    text = "%.1f кг".format(w).replace('.', ','),
                    fontSize = if (isSelected) 32.sp else 24.sp,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Thin,
                    color = animatedColor
                )
            }
        }
    }
}

@Composable
private fun WeightInputDialog(
    currentValue: Float,
    scheme: AppColorScheme,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var inputText by remember(currentValue) {
        mutableStateOf("%.1f".format(currentValue).replace('.', ','))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Введите вес",
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shapedSurface(
                        shape = RoundedCornerShape(DesignTokens.plankCornerRadius),
                        elevation = 0.dp,
                        backgroundColor = scheme.surfaceInput
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { s ->
                        val filtered = s.filter { it.isDigit() || it == ',' || it == '.' }
                        if (filtered.length <= 5) inputText = filtered
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = scheme.textColor,
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontWeight = FontWeight.Thin,
                        fontSize = 18.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = inputText.replace(',', '.').toFloatOrNull()
                    ?.coerceIn(weightMin, weightMax) ?: return@TextButton
                val closest = weightValues.minByOrNull { kotlin.math.abs(it - parsed) } ?: parsed
                onConfirm(closest)
            }) {
                Text("OK", fontFamily = DesignTokens.fontFamilyPlank)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = DesignTokens.fontFamilyPlank)
            }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════
// Страница 5: Уровень активности
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun ActivityPage(
    activityLevel: ActivityLevel,
    onActivityChange: (ActivityLevel) -> Unit,
    scheme: AppColorScheme
) {
    PageContainer(
        title = "Ваш уровень активности",
        subtitle = "Насколько активен ваш образ жизни",
        scheme = scheme
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActivityLevel.entries.forEach { level ->
                ActivityCard(
                    level = level,
                    isSelected = activityLevel == level,
                    onClick = { onActivityChange(level) },
                    scheme = scheme
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    level: ActivityLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
    scheme: AppColorScheme
) {
    val shape = RoundedCornerShape(12.dp)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            if (isSelected) scheme.textColor.copy(alpha = 0.5f) else scheme.borderGradientTop,
            if (isSelected) scheme.textColor.copy(alpha = 0.3f) else scheme.borderGradientBottom
        )
    )
    val backgroundColor = if (isSelected) scheme.shapeGradientAccent else scheme.shapeGradientCream

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, gradientBorder, shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = level.label,
            fontSize = 18.sp,
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Thin,
            color = scheme.textColor
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Страница 6: Цель
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun GoalPage(
    selectedGoal: UserGoal,
    onGoalChange: (UserGoal) -> Unit,
    scheme: AppColorScheme
) {
    PageContainer(
        title = "Какая у вас цель?",
        subtitle = "Выберите основную цель",
        scheme = scheme
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserGoal.entries.forEach { goal ->
                GoalCard(
                    goal = goal,
                    isSelected = selectedGoal == goal,
                    onClick = { onGoalChange(goal) },
                    scheme = scheme
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: UserGoal,
    isSelected: Boolean,
    onClick: () -> Unit,
    scheme: AppColorScheme
) {
    val shape = RoundedCornerShape(16.dp)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            if (isSelected) scheme.textColor.copy(alpha = 0.5f) else scheme.borderGradientTop,
            if (isSelected) scheme.textColor.copy(alpha = 0.3f) else scheme.borderGradientBottom
        )
    )
    val backgroundColor = if (isSelected) scheme.shapeGradientAccent else scheme.shapeGradientCream

    val (emoji, description) = when (goal) {
        UserGoal.LOSE -> "📉" to "Дефицит калорий для похудения"
        UserGoal.GAIN -> "📈" to "Профицит калорий для набора массы"
        UserGoal.MAINTAIN -> "⚖️" to "Поддержание текущего веса"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(1.5.dp, gradientBorder, shape)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = goal.label,
                    fontSize = 20.sp,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Normal,
                    color = scheme.textColor
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Thin,
                    color = scheme.textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Общий контейнер страницы
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun PageContainer(
    title: String,
    subtitle: String,
    scheme: AppColorScheme,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Normal,
            color = scheme.textColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 16.sp,
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            color = scheme.textColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        content()
    }
}
