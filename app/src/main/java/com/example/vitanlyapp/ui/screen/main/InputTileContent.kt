package com.example.vitanlyapp.ui.screen.main

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Контент средней плитки: список продуктов за сегодня.
 * Продукты группируются по приёмам пищи (mealSessionId).
 * Самые свежие записи внизу (как в чате).
 *
 * @param entries список записей о еде за сегодня
 * @param isCollapsed свёрнута ли плитка (отключает клики по продуктам)
 * @param onEntryClick callback при клике на продукт
 */
@Composable
fun InputTileContent(
    entries: List<DayEntry> = emptyList(),
    isCollapsed: Boolean = false,
    onEntryClick: (DayEntry) -> Unit = {},
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    TileBackground()

    val listState = rememberLazyListState()

    if (entries.isEmpty()) {
        // Пустое состояние
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val scheme = LocalAppColorScheme.current
            Text(
                text = "Добавьте продукты в чате",
                color = scheme.textColor.copy(alpha = 0.5f),
                fontFamily = DesignTokens.fontFamilyPlank,
                fontSize = 14.sp
            )
        }
    } else {
        // Группируем по mealSessionId, новые сессии первыми (индекс 0 = самые новые)
        val groupedEntries = remember(entries) {
            entries
                .groupBy { it.mealSessionId }
                .toSortedMap(reverseOrder())
        }

        val scheme = LocalAppColorScheme.current

        LazyColumn(
            state = listState,
            reverseLayout = false,
            modifier = modifier
                .fillMaxSize()
                .padding(DesignTokens.tilePadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groupedEntries.forEach { (sessionId, sessionEntries) ->
                // Разделитель с временем
                item(key = "divider_$sessionId") {
                    MealSessionDivider(timestamp = sessionId)
                }

                // Продукты в этой сессии (клики обрабатываются только когда плитка развёрнута)
                items(
                    items = sessionEntries,
                    key = { it.id }
                ) { entry ->
                    FoodEntryItem(
                        entry = entry,
                        onClick = if (isCollapsed) null else run { { onEntryClick(entry) } }
                    )
                }
            }

            // Сплошная черта в конце дня
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
