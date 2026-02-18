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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vitanlyapp.domain.model.KBJUData
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
 * 
 * Горизонтальный свайп переключает дни через стандартный HorizontalPager.
 * Вертикальный скролл работает внутри LazyColumn на каждой странице.
 * Синхронизация с графиком калорий через shared DayNavigationState.
 * 
 * Внизу — индикаторы дней с морфингом текста.
 *
 * @param entries список записей о еде (live для selected day)
 * @param entriesDate дата (ISO), которой соответствуют entries
 * @param availableDates список доступных дат (новые первые)
 * @param dayNavigationState shared state для синхронизации с графиком калорий
 * @param getCachedEntries возвращает кэшированные записи по дате или null
 * @param onDaySettled callback при оседании пейджера на странице (только при смене settledPage)
 * @param isCollapsed свёрнута ли плитка (отключает клики по продуктам)
 * @param onEntryClick callback при клике на продукт
 * @param onEntryExpandRequest callback при запросе раскрытия карточки (entry + bounds в window)
 */
@Composable
fun InputTileContent(
    modifier: Modifier = Modifier,
    entries: List<DayEntry> = emptyList(),
    entriesDate: String = "",
    availableDates: List<String> = emptyList(),
    dayNavigationState: DayNavigationState? = null,
    getCachedEntries: (String) -> List<DayEntry>? = { null },
    onDaySettled: (Int) -> Unit = {},
    isCollapsed: Boolean = false,
    onEntryClick: (DayEntry) -> Unit = {},
    onEntryExpandRequest: (DayEntry, Rect) -> Unit = { _, _ -> },
    kbjuData: KBJUData = KBJUData.default(),
    isActive: Boolean = true
) {
    TileBackground()

    val scheme = LocalAppColorScheme.current

    // HazeState для glassmorphism плашки с индикаторами дней
    val hazeState = rememberHazeState()

    // Начальный индекс из dayNavigationState
    val initialPage = dayNavigationState?.currentDayIndex ?: 0

    // Стандартный PagerState для плавных свайпов
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (availableDates.size - 1).coerceAtLeast(0)),
        pageCount = { availableDates.size.coerceAtLeast(1) }
    )

    // Синхронизация pagerState → dayNavigationState (при свайпе пользователем)
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        val fractionalPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
        dayNavigationState?.snapTo(fractionalPosition)
    }

    // Синхронизация dayNavigationState → pagerState (при внешнем изменении, например, тап по графику)
    LaunchedEffect(dayNavigationState?.currentDayIndex) {
        val targetPage = dayNavigationState?.currentDayIndex ?: return@LaunchedEffect
        if (targetPage != pagerState.settledPage && targetPage in 0 until pagerState.pageCount) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Callback при оседании пейджера (смена дня)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                if (settledPage in availableDates.indices) {
                    onDaySettled(settledPage)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Слой 1: Основной контент — HorizontalPager с вертикальным скроллом внутри
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
        ) {
            if (availableDates.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> availableDates.getOrNull(page) ?: page },
                    beyondViewportPageCount = 1 // Предзагрузка соседних страниц для плавности
                ) { page ->
                    val pageDate = availableDates.getOrNull(page) ?: ""
                    val pageEntries = if (pageDate == entriesDate) {
                        entries
                    } else {
                        getCachedEntries(pageDate) ?: emptyList()
                    }
                    
                    key(pageDate) {
                        DayPageContent(
                            entries = pageEntries,
                            isCollapsed = isCollapsed,
                            onEntryClick = onEntryClick,
                            onEntryExpandRequest = onEntryExpandRequest,
                            kbjuData = kbjuData,
                            isCurrentPage = page == pagerState.settledPage
                        )
                    }
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
    onEntryExpandRequest: (DayEntry, Rect) -> Unit,
    kbjuData: KBJUData,
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
                .padding(
                    start = 0.dp,
                    top = DesignTokens.tilePadding,
                    end = 0.dp,
                    bottom = DesignTokens.tilePadding + DesignTokens.middleTileDayIndicatorClearanceDp
                ),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            groupedEntries.forEach { (sessionId, sessionEntries) ->
                item(key = "divider_$sessionId") {
                    MealSessionDivider(timestamp = sessionId)
                }

                items(
                    items = sessionEntries,
                    key = { it.id }
                ) { entry ->
                    var bounds by remember { mutableStateOf<Rect?>(null) }
                    FoodEntryCard(
                        entry = entry,
                        kbjuData = kbjuData,
                        onClick = if (isCollapsed) null else {
                            {
                                bounds?.let { onEntryExpandRequest(entry, it) }
                                    ?: onEntryClick(entry)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                bounds = coords.boundsInWindow()
                            }
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
 * Показывает скользящее окно из 7 дней относительно текущей страницы.
 * Текущий день показан как текст "01.янв" / "Сегодня", остальные — как точки.
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
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val windowSize = 7
    val startIndex = (currentPage - windowSize / 2).coerceIn(0, (dates.size - windowSize).coerceAtLeast(0))
    val endIndexExclusive = (startIndex + windowSize).coerceAtMost(dates.size)
    val window = dates.subList(startIndex, endIndexExclusive)

    val pillShape = RoundedCornerShape(50)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(pillShape)
            .background(scheme.plankBackground.copy(alpha = 0.85f), pillShape)
            .then(
                if (supportsBlur) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = DesignTokens.blurRadius
                        tints = listOf(HazeTint(scheme.glassTint.copy(alpha = DesignTokens.glassTintAlpha)))
                        noiseFactor = DesignTokens.glassNoise
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            window.forEachIndexed { windowIndex, date ->
                val globalIndex = startIndex + windowIndex
                val distanceFromCurrent = (globalIndex - currentPage - currentPageOffset).absoluteValue
                val isSelected = distanceFromCurrent < 0.5f
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
        modifier = Modifier.height(15.dp)
    ) {
        // Точка — видна когда morphProgress близок к 0
        Box(
            modifier = Modifier
                .size(4.5.dp)
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
            fontSize = 9.sp,
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
 * Карточка продукта — фиксированная высота, 2 слоя: emoji сзади, glass спереди.
 * Показывает: название, вес, ккал + Б/Ж/У, % от дневной нормы.
 * Когда onClick != null — кликабельна для открытия меню действий.
 */
@Composable
fun FoodEntryCard(
    entry: DayEntry,
    kbjuData: KBJUData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scheme = LocalAppColorScheme.current
    val hazeState = rememberHazeState()
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val cardShape = RectangleShape

    val emojiNudgeYPx = with(LocalDensity.current) { DesignTokens.collapsedFoodEntryEmojiNudgeYDp.toPx() }
    val pctKcal = if (kbjuData.maxCalories == 0) 0 else (entry.kcal * 100 / kbjuData.maxCalories)
    val pctP = if (kbjuData.maxProtein == 0) 0 else (entry.protein * 100 / kbjuData.maxProtein).toInt()
    val pctF = if (kbjuData.maxFat == 0) 0 else (entry.fat * 100 / kbjuData.maxFat).toInt()
    val pctC = if (kbjuData.maxCarbs == 0) 0 else (entry.carbs * 100 / kbjuData.maxCarbs).toInt()

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .height(DesignTokens.foodEntryCardHeightCollapsed)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Слой 1: задний — большой emoji
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
                .hazeSource(hazeState)
        ) {
            Text(
                text = entry.emoji,
                style = TextStyle(
                    fontSize = DesignTokens.collapsedFoodEntryEmojiSizeSp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = DesignTokens.collapsedFoodEntryEmojiSizeSp
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(0.dp)
                    .offset(
                        x = -DesignTokens.collapsedFoodEntryEmojiOffsetXDp,
                        y = DesignTokens.collapsedFoodEntryEmojiOffsetYDp
                    )
                    .graphicsLayer {
                        alpha = DesignTokens.collapsedFoodEntryEmojiAlpha
                        rotationZ = DesignTokens.collapsedFoodEntryEmojiRotationDeg
                        translationY = emojiNudgeYPx
                    }
                    .drawWithContent {
                        drawContent()
                        if (DesignTokens.collapsedFoodEntryEmojiTintAlpha > 0f) {
                            drawRect(
                                color = DesignTokens.collapsedFoodEntryEmojiTintColor.copy(alpha = DesignTokens.collapsedFoodEntryEmojiTintAlpha),
                                blendMode = BlendMode.Modulate
                            )
                        }
                    }
            )
        }

        // Слой 2: передний — glass контейнер
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
                .then(
                    if (supportsBlur) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = DesignTokens.foodEntryCardGlassBlurRadius
                            tints = listOf(HazeTint(scheme.glassTint.copy(alpha = DesignTokens.foodEntryCardGlassDebugAlpha)))
                            noiseFactor = DesignTokens.glassNoise
                        }
                    } else {
                        Modifier.background(scheme.tileBackgroundColor.copy(alpha = DesignTokens.foodEntryCardGlassDebugAlpha), cardShape)
                    }
                )
                .padding(
                    horizontal = DesignTokens.foodEntryCardInnerPadding,
                    vertical = 6.dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.foodEntryCardTextRowsSpacing, Alignment.CenterVertically),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.name,
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = scheme.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${entry.weightGrams}г",
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontSize = 12.sp,
                        color = scheme.textColor.copy(alpha = 0.6f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("${entry.kcal} ккал · ")
                            withStyle(SpanStyle(color = DesignTokens.barProteinStart)) { append("Б ${entry.protein.toInt()}") }
                            append(" ")
                            withStyle(SpanStyle(color = DesignTokens.barFatStart)) { append("Ж ${entry.fat.toInt()}") }
                            append(" ")
                            withStyle(SpanStyle(color = DesignTokens.barCarbsStart)) { append("У ${entry.carbs.toInt()}") }
                        },
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontSize = 12.sp,
                        color = scheme.textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("ккал $pctKcal% · ")
                            withStyle(SpanStyle(color = DesignTokens.barProteinStart)) { append("Б $pctP%") }
                            append(" ")
                            withStyle(SpanStyle(color = DesignTokens.barFatStart)) { append("Ж $pctF%") }
                            append(" ")
                            withStyle(SpanStyle(color = DesignTokens.barCarbsStart)) { append("У $pctC%") }
                        },
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontSize = 11.sp,
                        color = scheme.textColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
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
            .padding(vertical = 4.dp, horizontal = 4.dp),
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
