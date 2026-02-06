package com.example.vitanlyapp.ui.design

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.vitanlyapp.R

/**
 * Дизайн-токены приложения VitanlyApp.
 * Цвета, отступы, радиусы, тени, анимации.
 */
object DesignTokens {

    // ==================== ЦВЕТА ====================

    // Основные
    val screenBackground: Color = Color(0x256A624D) // #6a624d25 полупрозрачный бежевый
    val textColor: Color = Color(0xFF171717)        // #171717 почти чёрный

    // Поверхности
    val surfaceMain: Color = Color(0xDFFFF2E1)      // rgba(255, 242, 225, 0.8745) кремовый
    val surfaceInput: Color = Color(0xE6E6DFD2)     // rgba(230, 223, 210, 0.9) бежевый

    // Границы
    val borderSoft: Color = Color(0x948A8A8A)       // rgba(138, 138, 138, 0.58)
    val borderLight: Color = Color(0x4D8A8A8A)      // rgba(138, 138, 138, 0.3)
    val borderGradientTop: Color = Color(0x66FFFFFF) // rgba(255, 255, 255, 0.4)
    val borderGradientBottom: Color = Color(0x26000000) // rgba(0, 0, 0, 0.15)

    // Legacy aliases
    val tileBackground: Color = surfaceMain
    val tileContent: Color = textColor

    // ==================== КБЖУ-БАРЫ ====================

    // Градиенты (слева направо)
    // Белок (синий): #9cc7f0 → #7fb3e6
    val barProteinStart: Color = Color(0xFF9CC7F0)
    val barProteinEnd: Color = Color(0xFF7FB3E6)
    val barProteinBrush: Brush = Brush.horizontalGradient(listOf(barProteinStart, barProteinEnd))

    // Жиры (жёлтый): #f2d29b → #e6bb6f
    val barFatStart: Color = Color(0xFFF2D29B)
    val barFatEnd: Color = Color(0xFFE6BB6F)
    val barFatBrush: Brush = Brush.horizontalGradient(listOf(barFatStart, barFatEnd))

    // Углеводы (фиолетовый): #d6c4f2 → #c1a8eb
    val barCarbsStart: Color = Color(0xFFD6C4F2)
    val barCarbsEnd: Color = Color(0xFFC1A8EB)
    val barCarbsBrush: Brush = Brush.horizontalGradient(listOf(barCarbsStart, barCarbsEnd))

    // Фон и переполнение
    val barBackground: Color = Color(0xFFE6E0D6)    // #e6e0d6
    val barOverflow: Color = Color(0x99D64545)      // #d64545 с прозрачностью 0.6

    // Legacy aliases
    val barFill: Color = barProteinStart

    // Размеры баров
    val barHeight: Dp = 32.dp
    val barSpacing: Dp = 16.dp
    val barCornerRadius: Dp = 16.dp
    val barBorderWidth: Dp = 0.5.dp
    val barTextPaddingStart: Dp = 16.dp
    val barTopPadding: Dp = 20.dp  // 2 × tilePadding

    // ==================== ВОЛНА КАЛОРИЙ ====================

    val waveColor: Color = Color(0x99A0DCDC)        // rgba(160, 220, 220, 0.6) бирюзовый
    val waveColorTop: Color = waveColor
    val waveColorBottom: Color = waveColor.copy(alpha = 0.4f)
    const val waveAmplitudeFraction: Float = 0.018f
    const val waveOscillationDurationMs: Int = 5000

    // Фон плиток — единый для всех
    val tileBackgroundColor: Color = surfaceMain

    // ==================== РАЗМЕРЫ И ОТСТУПЫ ====================

    // Экран
    val screenPadding: Dp = 10.dp
    val tileSpacing: Dp = 10.dp

    // Высота зоны кнопок вверху (удаление данных, смена темы)
    val topBarButtonsHeight: Dp = 48.dp  // 8.dp padding + 40.dp иконка

    // Адаптивный макет (фолд раскрыт): ширина в dp для переключения на двухпанельный режим
    val expandedLayoutBreakpoint: Dp = 600.dp
    const val expandedLayoutLeftPanelWeight: Float = 0.4f
    const val expandedLayoutRightPanelWeight: Float = 0.6f

    // Плитки
    val tilePadding: Dp = 10.dp
    val tileCollapsedHeight: Dp = 180.dp
    val tileCornerRadius: Dp = 24.dp
    val tileBorderWidth: Dp = 1.dp
    val tileBorderColor: Color = borderLight

    // Legacy
    val blockSpacing: Dp = tileSpacing

    // ==================== ШРИФТЫ ====================

    val fontFamily: FontFamily = FontFamily.SansSerif  // Arial/Helvetica equivalent

    val fontFamilyCaloriePercent: FontFamily = FontFamily(
        Font(R.font.barlow_semi_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    val fontFamilyPlank: FontFamily = FontFamily(
        Font(R.font.barlow_semi_condensed_thin, FontWeight.Thin, FontStyle.Normal)
    )

    val fontWeightNormal: FontWeight = FontWeight.W500
    val fontWeightHeading: FontWeight = FontWeight.W600

    val fontSizeChat = 14.sp
    val fontSizeInput = 14.sp
    val fontSizeButton = 18.sp
    val fontSizeLabel = 11.sp
    val fontSizeHeading = 14.sp

    const val lineHeightMultiplier: Float = 1.4f

    // ==================== АНИМАЦИИ ====================

    // Длительности (в миллисекундах)
    const val animationFast: Int = 300
    const val animationNormal: Int = 400
    const val animationSlow: Int = 500
    const val tileTransitionDurationMs: Int = 400
    const val inputTransitionDurationMs: Int = 350
    const val bubbleAppearDurationMs: Int = 200
    const val caloriePercentAppearDelayMs: Int = 500
    const val caloriePercentCountDurationMs: Int = 1100
    const val caloriePercentFadeOutDurationMs: Int = 300

    // ==================== МНОЖИТЕЛИ (expand/collapse) ====================

    const val expandedPaddingMultiplier: Float = 1.5f
    const val expandedBarHeightMultiplier: Float = 1.15f
    const val expandedBarGapMultiplier: Float = 1.2f
    const val collapsedBarHeightMultiplier: Float = 0.85f
    const val collapsedBarGapMultiplier: Float = 0.88f
    const val tileScaleCollapsed: Float = 0.96f
    const val tileScaleExpanded: Float = 1.02f
    const val barsCollapsedScale: Float = 0.92f
    val barsCollapsedOffsetY: Dp = 24.dp

    // Плашка «текущий вес»
    val plankHeight: Dp = 54.dp
    val plankWheelPickerHeight: Dp = 132.dp
    val plankWheelPickerWidth: Dp = 80.dp
    val plankPadding: Dp = 96.dp
    val plankCornerRadius: Dp = 12.dp
    val plankBackground: Color = Color(0xBFE0B8AC)
    val plankFontSizeLabel = 20.sp
    val plankFontSizeNumber = 40.sp
    const val tileWeightExpanded: Float = 3f
    const val tileWeightCollapsed: Float = 0.8f
    const val tileAlphaCollapsed: Float = 0.85f

    // Веса для idle-состояния (ни одна плитка не активна)
    const val tileWeightIdleTopMiddle: Float = 1f  // TOP и MIDDLE равные
    const val tileWeightIdleBottom: Float = 0.18f  // BOTTOM минимальный

    // Высота минимальной плитки чата (только строка ввода)
    val chatTileMinHeight: Dp = 92.dp
    
    // Доля экрана для раскрытой плитки чата (2/3 экрана)
    const val chatTileExpandedFraction: Float = 0.66f

    // Форма нижней плитки — скругление только сверху, снизу уходит за экран
    val bottomTileCornerRadius: Dp = 24.dp

    // ==================== ТЕНИ ====================

    // Значения теней для плиток (elevation)
    val tileElevationNormal: Dp = 6.dp
    val tileElevationExpanded: Dp = 8.dp

    // Значения теней для полей ввода
    val inputElevation: Dp = 2.dp

    // ==================== ЧАТ: ПУЗЫРИ И БЛОК ОТПРАВКИ ====================
    val chatBubbleUserBackground: Color = Color(0xFFE8C4B8)
    val chatBubbleAssistantBackground: Color = surfaceInput
    val chatInputBlockHeight: Dp = 54.dp
    val chatInputBlockCornerRadius: Dp = 18.dp
    val chatInputBlockBackground: Color = Color(0xBFE0B8AC)

    // ==================== КАСТОМНЫЕ ФОРМЫ ====================

    // TabbedShape — форма с выступом ("ушком")
    val tabbedShapeTabWidth: Dp = 80.dp
    val tabbedShapeTabExtensionTop: Dp = 60.dp
    val tabbedShapeTabExtensionBottom: Dp = 60.dp
    val tabbedShapeTabOffsetX: Dp = 80.dp
    val tabbedShapeCornerRadius: Dp = 24.dp
    val tabbedShapeTabCornerRadius: Dp = 20.dp

    // Компактная версия TabbedShape
    val tabbedShapeCompactTabWidth: Dp = 60.dp
    val tabbedShapeCompactTabExtension: Dp = 40.dp
    val tabbedShapeCompactTabOffsetX: Dp = 60.dp
    val tabbedShapeCompactCornerRadius: Dp = 20.dp
    val tabbedShapeCompactTabCornerRadius: Dp = 16.dp

    // Градиенты для форм

    // Светлый градиент (как на изображении — серо-белый)
    val shapeGradientLightTop: Color = Color(0xFFFFFFFF)     // белый
    val shapeGradientLightBottom: Color = Color(0xFFD9D9D9)  // светло-серый
    val shapeGradientLight: Brush = Brush.verticalGradient(
        listOf(shapeGradientLightTop, shapeGradientLightBottom)
    )

    // Кремовый градиент (в стиле приложения)
    val shapeGradientCreamTop: Color = Color(0xFFFFF8F0)     // светло-кремовый
    val shapeGradientCreamBottom: Color = Color(0xFFE6DFD2)  // бежевый
    val shapeGradientCream: Brush = Brush.verticalGradient(
        listOf(shapeGradientCreamTop, shapeGradientCreamBottom)
    )

    // Нейтральный градиент
    val shapeGradientNeutralTop: Color = Color(0xFFF5F5F5)
    val shapeGradientNeutralBottom: Color = Color(0xFFE0E0E0)
    val shapeGradientNeutral: Brush = Brush.verticalGradient(
        listOf(shapeGradientNeutralTop, shapeGradientNeutralBottom)
    )

    // Акцентный градиент (бирюзовый)
    val shapeGradientAccentTop: Color = Color(0xFFB8E8E8)
    val shapeGradientAccentBottom: Color = Color(0xFF8DCFCF)
    val shapeGradientAccent: Brush = Brush.verticalGradient(
        listOf(shapeGradientAccentTop, shapeGradientAccentBottom)
    )

    // ==================== GLASSMORPHISM ====================

    // Радиусы размытия (backdrop blur)
    val blurRadius: Dp = 20.dp              // Стандартное размытие для плиток
    val blurRadiusLight: Dp = 12.dp         // Лёгкое размытие для мелких элементов
    val blurRadiusStrong: Dp = 32.dp        // Сильное размытие для модальных окон

    // Прозрачность и тонировка
    const val glassTintAlpha: Float = 0.72f // Прозрачность тонировки поверхности
    const val glassNoise: Float = 0.03f     // Текстура "зернистости" (0-1)

    // Насыщенность цветов под стеклом
    const val glassSaturationBoost: Float = 1.2f
}
