package com.example.vitanlyapp.ui.design

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Форма с вертикальным выступом ("ушком") справа.
 * Прямоугольник с T-образным выступом, выходящим за верхнюю и нижнюю границы.
 *
 * @param tabWidth Ширина выступа
 * @param tabExtensionTop Насколько выступ выходит вверх за основную форму
 * @param tabExtensionBottom Насколько выступ выходит вниз за основную форму
 * @param tabOffsetX Отступ выступа от правого края (0 = вплотную к правому краю)
 * @param cornerRadius Радиус скругления основных углов
 * @param tabCornerRadius Радиус скругления углов выступа
 */
class TabbedShape(
    private val tabWidth: Dp = 80.dp,
    private val tabExtensionTop: Dp = 60.dp,
    private val tabExtensionBottom: Dp = 60.dp,
    private val tabOffsetX: Dp = 80.dp,
    private val cornerRadius: Dp = 24.dp,
    private val tabCornerRadius: Dp = 20.dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        with(density) {
            val tabWidthPx = tabWidth.toPx()
            val tabExtTopPx = tabExtensionTop.toPx()
            val tabExtBottomPx = tabExtensionBottom.toPx()
            val tabOffsetXPx = tabOffsetX.toPx()
            val cornerPx = cornerRadius.toPx()
            val tabCornerPx = tabCornerRadius.toPx()

            // Основные координаты
            val mainLeft = 0f
            val mainTop = tabExtTopPx  // Верх основного прямоугольника (ниже выступа)
            val mainRight = size.width
            val mainBottom = size.height - tabExtBottomPx  // Низ основного прямоугольника

            // Координаты выступа
            val tabLeft = mainRight - tabOffsetXPx - tabWidthPx
            val tabRight = mainRight - tabOffsetXPx
            val tabTop = 0f
            val tabBottom = size.height

            // Начинаем с левого верхнего угла основного прямоугольника
            path.moveTo(mainLeft + cornerPx, mainTop)

            // Верхний край до начала выступа
            path.lineTo(tabLeft - tabCornerPx, mainTop)

            // Внутренний угол перед выступом сверху (вогнутый угол)
            path.quadraticTo(
                tabLeft, mainTop,
                tabLeft, mainTop - tabCornerPx
            )

            // Левый край выступа вверх
            path.lineTo(tabLeft, tabTop + tabCornerPx)

            // Верхний левый угол выступа
            path.quadraticTo(
                tabLeft, tabTop,
                tabLeft + tabCornerPx, tabTop
            )

            // Верхний край выступа
            path.lineTo(tabRight - tabCornerPx, tabTop)

            // Верхний правый угол выступа
            path.quadraticTo(
                tabRight, tabTop,
                tabRight, tabTop + tabCornerPx
            )

            // Правый край выступа вниз до основного прямоугольника
            path.lineTo(tabRight, mainTop - tabCornerPx)

            // Внутренний угол после выступа сверху
            path.quadraticTo(
                tabRight, mainTop,
                tabRight + tabCornerPx, mainTop
            )

            // Верхний край до правого угла
            path.lineTo(mainRight - cornerPx, mainTop)

            // Верхний правый угол основного прямоугольника
            path.quadraticTo(
                mainRight, mainTop,
                mainRight, mainTop + cornerPx
            )

            // Правый край вниз
            path.lineTo(mainRight, mainBottom - cornerPx)

            // Нижний правый угол основного прямоугольника
            path.quadraticTo(
                mainRight, mainBottom,
                mainRight - cornerPx, mainBottom
            )

            // Нижний край до выступа
            path.lineTo(tabRight + tabCornerPx, mainBottom)

            // Внутренний угол перед выступом снизу
            path.quadraticTo(
                tabRight, mainBottom,
                tabRight, mainBottom + tabCornerPx
            )

            // Правый край выступа вниз
            path.lineTo(tabRight, tabBottom - tabCornerPx)

            // Нижний правый угол выступа
            path.quadraticTo(
                tabRight, tabBottom,
                tabRight - tabCornerPx, tabBottom
            )

            // Нижний край выступа
            path.lineTo(tabLeft + tabCornerPx, tabBottom)

            // Нижний левый угол выступа
            path.quadraticTo(
                tabLeft, tabBottom,
                tabLeft, tabBottom - tabCornerPx
            )

            // Левый край выступа вверх до основного прямоугольника
            path.lineTo(tabLeft, mainBottom + tabCornerPx)

            // Внутренний угол после выступа снизу
            path.quadraticTo(
                tabLeft, mainBottom,
                tabLeft - tabCornerPx, mainBottom
            )

            // Нижний край до левого угла
            path.lineTo(mainLeft + cornerPx, mainBottom)

            // Нижний левый угол
            path.quadraticTo(
                mainLeft, mainBottom,
                mainLeft, mainBottom - cornerPx
            )

            // Левый край вверх
            path.lineTo(mainLeft, mainTop + cornerPx)

            // Верхний левый угол
            path.quadraticTo(
                mainLeft, mainTop,
                mainLeft + cornerPx, mainTop
            )

            path.close()
        }

        return Outline.Generic(path)
    }
}

/**
 * Простая форма со скругленными углами (для использования в системе форм).
 * Это обёртка над стандартным RoundedCornerShape для единообразия API.
 */
class SimpleRoundedShape(
    private val cornerRadius: Dp = 24.dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        with(density) {
            val cornerPx = cornerRadius.toPx()

            path.moveTo(cornerPx, 0f)
            path.lineTo(size.width - cornerPx, 0f)
            path.quadraticTo(size.width, 0f, size.width, cornerPx)
            path.lineTo(size.width, size.height - cornerPx)
            path.quadraticTo(size.width, size.height, size.width - cornerPx, size.height)
            path.lineTo(cornerPx, size.height)
            path.quadraticTo(0f, size.height, 0f, size.height - cornerPx)
            path.lineTo(0f, cornerPx)
            path.quadraticTo(0f, 0f, cornerPx, 0f)
            path.close()
        }

        return Outline.Generic(path)
    }
}

/**
 * Предустановленные формы для удобного использования.
 */
object CustomShapes {
    /**
     * Форма по умолчанию с выступом справа.
     */
    val TabbedDefault: Shape = TabbedShape()

    /**
     * Компактная форма с небольшим выступом.
     */
    val TabbedCompact: Shape = TabbedShape(
        tabWidth = 60.dp,
        tabExtensionTop = 40.dp,
        tabExtensionBottom = 40.dp,
        tabOffsetX = 60.dp,
        cornerRadius = 20.dp,
        tabCornerRadius = 16.dp
    )

    /**
     * Создать форму с выступом с кастомными параметрами.
     */
    fun tabbed(
        tabWidth: Dp = 80.dp,
        tabExtensionTop: Dp = 60.dp,
        tabExtensionBottom: Dp = 60.dp,
        tabOffsetX: Dp = 80.dp,
        cornerRadius: Dp = 24.dp,
        tabCornerRadius: Dp = 20.dp
    ): Shape = TabbedShape(
        tabWidth = tabWidth,
        tabExtensionTop = tabExtensionTop,
        tabExtensionBottom = tabExtensionBottom,
        tabOffsetX = tabOffsetX,
        cornerRadius = cornerRadius,
        tabCornerRadius = tabCornerRadius
    )

    /**
     * Простая скруглённая форма.
     */
    fun rounded(cornerRadius: Dp = 24.dp): Shape = SimpleRoundedShape(cornerRadius)
}
