package com.example.vitanlyapp.ui.design

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LocalAppColorScheme = compositionLocalOf { AppColorSchemes.Classic }

/**
 * Цветовая схема приложения.
 * Classic — светлая. WarmDark — тёмная с холодными серыми и синеватыми тонами (без бордового/красного).
 */
data class AppColorScheme(
    val screenBackground: Color,
    val screenBackgroundBrush: Brush?,
    val textColor: Color,
    val surfaceMain: Color,
    val surfaceInput: Color,
    val borderSoft: Color,
    val borderLight: Color,
    val borderGradientTop: Color,
    val borderGradientBottom: Color,
    val tileBackgroundColor: Color,
    val tileTopMiddleBackgroundBrush: Brush?,  // Градиент для TOP/MIDDLE плиток (null = используется tileBackgroundColor)
    val barProteinBrush: Brush,
    val barFatBrush: Brush,
    val barCarbsBrush: Brush,
    val barFill: Color,
    val barBackground: Color,
    val barOverflow: Color,
    val waveColor: Color,
    val plankBackground: Color,
    val chatBubbleUserBackground: Color,
    val chatBubbleAssistantBackground: Color,
    val chatInputBlockBackground: Color,
    val shapeGradientCream: Brush,
    val shapeGradientNeutral: Brush,
    val shapeGradientAccent: Brush,
    val shapeGradientNeutralStart: Color,
    val shapeGradientAccentEnd: Color,
    // Glassmorphism
    val glassTint: Color,           // Цвет тонировки для blur-поверхностей
    val glassBackgroundColor: Color, // Полупрозрачный фон для glass-элементов
)

object AppColorSchemes {

    val Classic = AppColorScheme(
        screenBackground = Color(0x256A624D),
        screenBackgroundBrush = null,
        textColor = Color(0xFF171717),
        surfaceMain = Color(0xDFFFF2E1),
        surfaceInput = Color(0xE6E6DFD2),
        borderSoft = Color(0x948A8A8A),
        borderLight = Color(0x4D8A8A8A),
        borderGradientTop = Color(0x66FFFFFF),
        borderGradientBottom = Color(0x26000000),
        tileBackgroundColor = Color(0xDFFFF2E1),
        tileTopMiddleBackgroundBrush = null,  // Используется tileBackgroundColor
        barProteinBrush = Brush.horizontalGradient(
            listOf(Color(0xFF9CC7F0), Color(0xFF7FB3E6))
        ),
        barFatBrush = Brush.horizontalGradient(
            listOf(Color(0xFFF2D29B), Color(0xFFE6BB6F))
        ),
        barCarbsBrush = Brush.horizontalGradient(
            listOf(Color(0xFFD6C4F2), Color(0xFFC1A8EB))
        ),
        barFill = Color(0xFF9CC7F0),
        barBackground = Color(0xFFE6E0D6),
        barOverflow = Color(0x99D64545),
        waveColor = Color(0x99A0DCDC),
        plankBackground = Color(0xBFE0B8AC),
        chatBubbleUserBackground = Color(0xFFE8C4B8),
        chatBubbleAssistantBackground = Color(0xE6E6DFD2),
        chatInputBlockBackground = Color(0xBFE0B8AC),
        shapeGradientCream = Brush.verticalGradient(
            listOf(Color(0xFFFFF8F0), Color(0xFFE6DFD2))
        ),
        shapeGradientNeutral = Brush.verticalGradient(
            listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0))
        ),
        shapeGradientAccent = Brush.verticalGradient(
            listOf(Color(0xFFB8E8E8), Color(0xFF8DCFCF))
        ),
        shapeGradientNeutralStart = Color(0xFFF5F5F5),
        shapeGradientAccentEnd = Color(0xFF8DCFCF),
        // Glassmorphism: насыщенный коралловый тон для светлой темы
        glassTint = Color(0xFFE8C4B8),
        glassBackgroundColor = Color(0x99E0B8AC),  // ~60% прозрачности кораллового
    )

    val WarmDark = AppColorScheme(
        screenBackground = Color(0xFF0A0D12),
        screenBackgroundBrush = Brush.verticalGradient(
            listOf(Color(0xFF1A1F28), Color(0xFF0A0D12))
        ),
        textColor = Color(0xFFE8ECF1),
        surfaceMain = Color(0xFF151A22),
        surfaceInput = Color(0xFF1E242E),
        borderSoft = Color(0x994A5568),
        borderLight = Color(0x4D4A5568),
        borderGradientTop = Color(0x664A5568),
        borderGradientBottom = Color(0x80151A22),
        tileBackgroundColor = Color(0xFF151A22),
        tileTopMiddleBackgroundBrush = null,  // Используется tileBackgroundColor
        barProteinBrush = Brush.horizontalGradient(
            listOf(Color(0xFF4A6B8A), Color(0xFF3D5A75))
        ),
        barFatBrush = Brush.horizontalGradient(
            listOf(Color(0xFF5A6B7A), Color(0xFF4A5A68))
        ),
        barCarbsBrush = Brush.horizontalGradient(
            listOf(Color(0xFF6B7B8A), Color(0xFF5A6A78))
        ),
        barFill = Color(0xFF4A6B8A),
        barBackground = Color(0xFF1A1F28),
        barOverflow = Color(0x80605050),
        waveColor = Color(0x994A6B8A),
        plankBackground = Color(0xFF1E242E),
        chatBubbleUserBackground = Color(0xFF3D4F6B),
        chatBubbleAssistantBackground = Color(0xFF1E242E),
        chatInputBlockBackground = Color(0xFF1E242E),
        shapeGradientCream = Brush.verticalGradient(
            listOf(Color(0xFF1E242E), Color(0xFF151A22))
        ),
        shapeGradientNeutral = Brush.verticalGradient(
            listOf(Color(0xFF1E242E), Color(0xFF151A22))
        ),
        shapeGradientAccent = Brush.verticalGradient(
            listOf(Color(0xFF4A6B8A), Color(0xFF2D3A4A))
        ),
        shapeGradientNeutralStart = Color(0xFF2D333B),
        shapeGradientAccentEnd = Color(0xFF3D5A75),
        // Glassmorphism: холодный серо-синий тон для тёмной темы
        glassTint = Color(0xFF1E242E),
        glassBackgroundColor = Color(0x80151A22),  // ~50% прозрачности тёмного
    )

    /**
     * MatteDark — матовая тёмная тема с чёрным фоном и приглушёнными серыми тонами.
     * Фон: чистый чёрный #000000 с текстурой шума.
     * Плитки: градиент от #181B1F до #6C6C6C для TOP/MIDDLE, #1A1918 для BOTTOM.
     * Внутренние элементы (плашки, блок ввода): #41403D (тусклый серый).
     */
    val MatteDark = AppColorScheme(
        screenBackground = Color(0xFF000000),  // Чистый чёрный с текстурой шума
        screenBackgroundBrush = null,  // Шум будет добавлен отдельным слоем
        textColor = Color(0xFFE8E8E8),  // Светлый текст
        surfaceMain = Color(0xFF181B1F),  // Тёмный серый для плиток
        surfaceInput = Color(0xFF252525),  // Чуть светлее для полей ввода
        borderSoft = Color(0x6683807A),  // Приглушённая граница
        borderLight = Color(0x3383807A),  // Лёгкая граница
        borderGradientTop = Color(0x4D6C6C6C),  // Верх градиента границы
        borderGradientBottom = Color(0x33181B1F),  // Низ градиента границы
        tileBackgroundColor = Color(0xFF1A1918),  // Цвет нижней плитки
        tileTopMiddleBackgroundBrush = Brush.verticalGradient(
            listOf(Color(0xFF181B1F), Color(0xFF6C6C6C))  // Градиент для TOP/MIDDLE плиток
        ),
        barProteinBrush = Brush.horizontalGradient(
            listOf(Color(0xFFA06B8B), Color(0xFF5C2E4A))  // Белок: розовый/малиновый (подсказка 6)
        ),
        barFatBrush = Brush.horizontalGradient(
            listOf(Color(0xFFB07860), Color(0xFF5C3D2E))  // Жиры: терракотовый/коричневый (подсказка 1)
        ),
        barCarbsBrush = Brush.horizontalGradient(
            listOf(Color(0xFF6BA08B), Color(0xFF2E5C4A))  // Углеводы: зелёный/изумрудный (подсказка 3)
        ),
        barFill = Color(0xFFA06B8B),
        barBackground = Color(0xFF2A2A2A),  // Фон баров
        barOverflow = Color(0x80804040),  // Переполнение (приглушённый красный)
        waveColor = Color(0x995A5A5A),  // Волна калорий (серая)
        plankBackground = Color(0xFF41403D),  // Внутренние элементы (плашки) — тусклый серый
        chatBubbleUserBackground = Color(0xFF4A4A4A),  // Пузырь пользователя
        chatBubbleAssistantBackground = Color(0xFF2A2A2A),  // Пузырь ассистента
        chatInputBlockBackground = Color(0xFF41403D),  // Блок ввода — тусклый серый
        shapeGradientCream = Brush.verticalGradient(
            listOf(Color(0xFF181B1F), Color(0xFF6C6C6C))  // Градиент TOP/MIDDLE плиток
        ),
        shapeGradientNeutral = Brush.verticalGradient(
            listOf(Color(0xFF2A2A2A), Color(0xFF1A1918))
        ),
        shapeGradientAccent = Brush.verticalGradient(
            listOf(Color(0xFF6C6C6C), Color(0xFF4A4A4A))
        ),
        shapeGradientNeutralStart = Color(0xFF3A3A3A),
        shapeGradientAccentEnd = Color(0xFF5A5A5A),
        // Glassmorphism: тусклый серый тон
        glassTint = Color(0xFF41403D),
        glassBackgroundColor = Color(0xB341403D),  // ~70% прозрачности
    )
}
