package com.example.vitanlyapp.ui.screen.main

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vitanlyapp.domain.repository.DayEntry
import com.example.vitanlyapp.ui.component.TileBackground
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

/**
 * Контент средней плитки: список продуктов с навигацией по дням.
 * Свайп влево/вправо переключает дни.
 * Внизу — индикаторы дней с морфингом текста.
 *
 * @param entries список записей о еде за выбранный день
 * @param availableDates список доступных дат (новые первые)
 * @param selectedDateIndex индекс выбранной даты в списке
 * @param onDaySelected callback при смене дня (передаёт индекс)
 * @param isCollapsed свёрнута ли плитка (отключает клики по продуктам)
 * @param onEntryClick callback при клике на продукт
 */
@Composable
fun InputTileContent(
    entries: List<DayEntry> = emptyList(),
    availableDates: List<String> = emptyList(),
    selectedDateIndex: Int = 0,
    onDaySelected: (Int) -> Unit = {},
    isCollapsed: Boolean = false,
    onEntryClick: (DayEntry) -> Unit = {},
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    TileBackground()

    val scheme = LocalAppColorScheme.current
    
    // HazeState для glassmorphism плашки с индикаторами дней
    val hazeState = rememberHazeState()

    // Pager state для свайпов между днями
    val pagerState = rememberPagerState(
        initialPage = selectedDateIndex,
        pageCount = { availableDates.size.coerceAtLeast(1) }
    )

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Синхронизация с внешним selectedDateIndex; зависит от availableDates.size чтобы скроллить к today после загрузки
    LaunchedEffect(selectedDateIndex, availableDates.size) {
        if (pagerState.currentPage != selectedDateIndex && selectedDateIndex < availableDates.size) {
            isProgrammaticScroll = true
            try {
                pagerState.animateScrollToPage(selectedDateIndex)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    // Отслеживаем свайпы: settledPage — после завершения анимации; только user-driven, не programmatic
    LaunchedEffect(pagerState, availableDates.size, selectedDateIndex) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (!isProgrammaticScroll && page in availableDates.indices && page != selectedDateIndex) {
                    onDaySelected(page)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Слой 1: Основной контент — источник для размытия (hazeSource)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
        ) {
            if (availableDates.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    DayPageContent(
                        entries = if (page == pagerState.settledPage) entries else emptyList(),
                        isCollapsed = isCollapsed,
                        onEntryClick = onEntryClick,
                        isCurrentPage = page == pagerState.currentPage
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Добавьте продукты в чате",
                        color = scheme.textColor.copy(alpha = 0.5f),
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Слой 2: Индикаторы дней — glassmorphism pill поверх контента
        if (availableDates.size > 1) {
            DayIndicator(
                dates = availableDates,
                currentPage = pagerState.currentPage,
                currentPageOffset = pagerState.currentPageOffsetFraction,
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * Контент одной страницы (дня) в пейджере.
 */
@Composable
private fun DayPageContent(
    entries: List<DayEntry>,
    isCollapsed: Boolean,
    onEntryClick: (DayEntry) -> Unit,
    isCurrentPage: Boolean
) {
    val scheme = LocalAppColorScheme.current
    val listState = rememberLazyListState()

    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCurrentPage) "Нет записей за этот день" else "",
                color = scheme.textColor.copy(alpha = 0.5f),
                fontFamily = DesignTokens.fontFamilyPlank,
                fontSize = 14.sp
            )
        }
    } else {
        // Группируем по mealSessionId, новые сессии первыми
        val groupedEntries = remember(entries) {
            entries
                .groupBy { it.mealSessionId }
                .toSortedMap(reverseOrder())
        }

        LazyColumn(
            state = listState,
            reverseLayout = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.tilePadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groupedEntries.forEach { (sessionId, sessionEntries) ->
                item(key = "divider_$sessionId") {
                    MealSessionDivider(timestamp = sessionId)
                }

                items(
                    items = sessionEntries,
                    key = { it.id }
                ) { entry ->
                    FoodEntryItem(
                        entry = entry,
                        onClick = if (isCollapsed) null else { { onEntryClick(entry) } }
                    )
                }
            }

            item(key = "day_bottom_divider") {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.borderLight
                )
            }
        }
    }
}

/**
 * Индикаторы дней с морфинг-анимацией в glassmorphism pill-плашке.
 * Текущий день показан как текст "01.янв", остальные — как точки.
 * При свайпе текст плавно морфится в точку и наоборот.
 */
@Composable
private fun DayIndicator(
    dates: List<String>,
    currentPage: Int,
    currentPageOffset: Float,
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    
    // Pill-форма: высота 24dp → радиус 12dp для полукругов на краях
    val pillShape = RoundedCornerShape(50) // 50% = pill с полукругами
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .clip(pillShape)
            .background(scheme.plankBackground.copy(alpha = 0.85f), pillShape) // Видимый полупрозрачный фон
            .then(
                if (supportsBlur) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = DesignTokens.blurRadius
                        tints = listOf(HazeTint(scheme.glassTint.copy(alpha = DesignTokens.glassTintAlpha)))
                        noiseFactor = DesignTokens.glassNoise
                    }
                } else {
                    Modifier // Fallback уже применён через background выше
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dates.forEachIndexed { index, date ->
                // Вычисляем "близость" к текущей странице для морфинга
                val distanceFromCurrent = (index - currentPage - currentPageOffset).absoluteValue
                val isSelected = distanceFromCurrent < 0.5f
                
                // Анимация морфинга: 0 = точка, 1 = текст
                val morphProgress by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "morphProgress"
                )

                MorphingDateItem(
                    date = date,
                    isToday = date == today,
                    morphProgress = morphProgress,
                    textColor = scheme.textColor
                )
            }
        }
    }
}

/**
 * Элемент индикатора с морфингом между точкой и текстом даты.
 *
 * @param morphProgress 0f = точка, 1f = текст даты
 */
@Composable
private fun MorphingDateItem(
    date: String,
    isToday: Boolean,
    morphProgress: Float,
    textColor: androidx.compose.ui.graphics.Color
) {
    val dateText = remember(date, isToday) {
        if (isToday) {
            "Сегодня"
        } else {
            formatDateShort(date)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(20.dp)
    ) {
        // Точка — видна когда morphProgress близок к 0
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(1f - morphProgress)
                .scale(1f - morphProgress * 0.5f)
                .background(
                    color = textColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )

        // Текст даты — видим когда morphProgress близок к 1
        Text(
            text = dateText,
            color = textColor.copy(alpha = 0.6f * morphProgress),
            fontSize = 11.sp,
            fontFamily = DesignTokens.fontFamilyPlank,
            modifier = Modifier
                .graphicsLayer {
                    alpha = morphProgress
                    scaleX = 0.5f + morphProgress * 0.5f
                    scaleY = 0.5f + morphProgress * 0.5f
                }
        )
    }
}

/**
 * Форматирует дату в короткий формат "01.янв".
 */
private fun formatDateShort(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        val day = date.dayOfMonth.toString().padStart(2, '0')
        val month = when (date.monthValue) {
            1 -> "янв"
            2 -> "фев"
            3 -> "мар"
            4 -> "апр"
            5 -> "май"
            6 -> "июн"
            7 -> "июл"
            8 -> "авг"
            9 -> "сен"
            10 -> "окт"
            11 -> "ноя"
            12 -> "дек"
            else -> ""
        }
        "$day.$month"
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Элемент списка — одна запись о продукте.
 * Показывает: emoji, название, калории.
 * Когда onClick != null — кликабелен для открытия меню действий.
 */
@Composable
fun FoodEntryItem(
    entry: DayEntry,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji
        Text(
            text = entry.emoji,
            fontSize = 18.sp
        )

        Spacer(Modifier.width(8.dp))

        // Название продукта
        Text(
            text = entry.name,
            modifier = Modifier.weight(1f),
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = scheme.textColor
        )

        // Калории
        Text(
            text = "${entry.kcal} ккал",
            fontFamily = DesignTokens.fontFamilyPlank,
            fontSize = 12.sp,
            color = scheme.textColor.copy(alpha = 0.6f)
        )
    }
}

/**
 * Разделитель между приёмами пищи с временем.
 */
@Composable
fun MealSessionDivider(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current

    val timeText = remember(timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = scheme.borderLight
        )
        Text(
            text = timeText,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 11.sp,
            color = scheme.textColor.copy(alpha = 0.4f),
            fontFamily = DesignTokens.fontFamilyPlank
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = scheme.borderLight
        )
    }
}

/**
 * Действие с продуктом.
 */
sealed class FoodEntryAction {
    data object Delete : FoodEntryAction()
    data object Repeat : FoodEntryAction()
    data class UpdateWeight(val newWeight: Int) : FoodEntryAction()
}

/**
 * Диалог выбора действия с продуктом.
 */
@Composable
fun FoodEntryActionDialog(
    entry: DayEntry,
    onAction: (FoodEntryAction) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAppColorScheme.current
    var showWeightInput by remember { mutableStateOf(false) }
    var weightText by remember { mutableStateOf(entry.weightGrams.toString()) }

    if (showWeightInput) {
        // Диалог ввода нового веса
        AlertDialog(
            onDismissRequest = { showWeightInput = false },
            title = {
                Text(
                    text = "Изменить вес",
                    fontFamily = DesignTokens.fontFamilyPlank
                )
            },
            text = {
                Column {
                    Text(
                        text = "${entry.emoji} ${entry.name}",
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontSize = 14.sp,
                        color = scheme.textColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { newValue ->
                            // Только цифры
                            if (newValue.all { it.isDigit() }) {
                                weightText = newValue
                            }
                        },
                        label = { Text("Вес (г)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newWeight = weightText.toIntOrNull()
                        if (newWeight != null && newWeight > 0) {
                            onAction(FoodEntryAction.UpdateWeight(newWeight))
                            onDismiss()
                        }
                    }
                ) {
                    Text("Сохранить", fontFamily = DesignTokens.fontFamilyPlank)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightInput = false }) {
                    Text("Отмена", fontFamily = DesignTokens.fontFamilyPlank)
                }
            }
        )
    } else {
        // Основной диалог выбора действия
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scheme.tileBackgroundColor
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Заголовок с информацией о продукте
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = entry.emoji,
                            fontSize = 24.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = entry.name,
                                fontFamily = DesignTokens.fontFamilyPlank,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = scheme.textColor
                            )
                            Text(
                                text = "${entry.weightGrams}г • ${entry.kcal} ккал",
                                fontFamily = DesignTokens.fontFamilyPlank,
                                fontSize = 12.sp,
                                color = scheme.textColor.copy(alpha = 0.6f)
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.borderLight)

                    // Кнопки действий
                    ActionButton(
                        icon = Icons.Default.Delete,
                        text = "Удалить",
                        onClick = {
                            onAction(FoodEntryAction.Delete)
                            onDismiss()
                        }
                    )

                    ActionButton(
                        icon = Icons.Default.ContentCopy,
                        text = "Повторить",
                        onClick = {
                            onAction(FoodEntryAction.Repeat)
                            onDismiss()
                        }
                    )

                    ActionButton(
                        icon = Icons.Default.Scale,
                        text = "Изменить вес",
                        onClick = { showWeightInput = true }
                    )
                }
            }
        }
    }
}

/**
 * Кнопка действия в диалоге.
 */
@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val scheme = LocalAppColorScheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = scheme.textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            fontFamily = DesignTokens.fontFamilyPlank,
            fontSize = 15.sp,
            color = scheme.textColor
        )
    }
}
