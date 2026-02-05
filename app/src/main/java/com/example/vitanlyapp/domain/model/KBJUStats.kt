package com.example.vitanlyapp.domain.model

/**
 * Статистика по одному показателю КБЖУ для отображения в барах:
 * процент от максимума, флаг overflow (превышение > 100%), текущее и максимальное значения.
 */
data class KbjuBarStat(
    val percent: Float,
    val overflow: Boolean,
    val current: Int,
    val max: Int
)

/**
 * Расчёт процента: current / max. Деление на ноль даёт 0%.
 * При current > max — percent > 100%, overflow = true.
 */
fun percentOrZero(current: Int, max: Int): Float {
    if (max <= 0) return 0f
    return (current.toFloat() / max * 100f).coerceAtLeast(0f)
}

/**
 * Расчёт статистики для одного показателя: процент и флаг overflow.
 */
fun barStat(current: Int, max: Int): KbjuBarStat {
    val p = percentOrZero(current, max)
    return KbjuBarStat(percent = p, overflow = p > 100f, current = current, max = max)
}

/**
 * Статистика баров по всем четырём показателям КБЖУ.
 * UI получает проценты и overflow без знания источника данных.
 */
fun KBJUData.toBarStats(): List<KbjuBarStat> = listOf(
    barStat(currentCalories, maxCalories),
    barStat(currentProtein, maxProtein),
    barStat(currentFat, maxFat),
    barStat(currentCarbs, maxCarbs)
)
