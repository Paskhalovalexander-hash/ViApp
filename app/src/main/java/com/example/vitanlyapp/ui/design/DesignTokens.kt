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
 *
 * Примечание:
 * - `DesignTokens` — константы размеров/цветов/анимаций (глобальные “ручки” дизайна).
 * - Цвета конкретной темы (Classic/WarmDark/MatteDark) живут в `AppColorScheme` и доступны через `LocalAppColorScheme`.
 *   В UI часто используется смесь: размеры из `DesignTokens`, а “тематические” цвета/градиенты из `LocalAppColorScheme`.
 */
object DesignTokens {

    // ==================== ЦВЕТА ====================

    // Основные
    val screenBackground: Color = Color(0x256A624D) // Базовый цвет фона экрана (Classic), часто переопределяется темой
    val textColor: Color = Color(0xFF171717)        // Базовый цвет текста (Classic), часто переопределяется темой

    // Поверхности
    val surfaceMain: Color = Color(0xDFFFF2E1)      // Базовая “поверхность” (Classic): фон плиток/панелей по умолчанию
    val surfaceInput: Color = Color(0xE6E6DFD2)     // Базовая “поверхность” для input/пузырей (Classic)

    // Границы
    val borderSoft: Color = Color(0x948A8A8A)       // Мягкая граница (Classic): деликатные разделители/обводки
    val borderLight: Color = Color(0x4D8A8A8A)      // Лёгкая граница (Classic): тонкие разделители/линии
    val borderGradientTop: Color = Color(0x66FFFFFF) // Верх градиентной обводки плиток (Tile.kt)
    val borderGradientBottom: Color = Color(0x26000000) // Низ градиентной обводки плиток (Tile.kt)

    // Legacy aliases
    val tileBackground: Color = surfaceMain // Legacy: фон плитки (используй `tileBackgroundColor`/scheme где возможно)
    val tileContent: Color = textColor      // Legacy: цвет контента в плитке (используй `LocalAppColorScheme`)

    // ==================== КБЖУ-БАРЫ ====================

    // Градиенты (слева направо)
    // Белок (синий): #9cc7f0 → #7fb3e6
    val barProteinStart: Color = Color(0xFF9CC7F0) // Цвет старта градиента белка (Classic); также удобен для текста “Б”
    val barProteinEnd: Color = Color(0xFF7FB3E6)   // Цвет конца градиента белка (Classic)
    val barProteinBrush: Brush = Brush.horizontalGradient(listOf(barProteinStart, barProteinEnd)) // Градиент заливки бара белка

    // Жиры (жёлтый): #f2d29b → #e6bb6f
    val barFatStart: Color = Color(0xFFF2D29B) // Цвет старта градиента жиров (Classic); также удобен для текста “Ж”
    val barFatEnd: Color = Color(0xFFE6BB6F)   // Цвет конца градиента жиров (Classic)
    val barFatBrush: Brush = Brush.horizontalGradient(listOf(barFatStart, barFatEnd)) // Градиент заливки бара жиров

    // Углеводы (фиолетовый): #d6c4f2 → #c1a8eb
    val barCarbsStart: Color = Color(0xFFD6C4F2) // Цвет старта градиента углеводов (Classic); также удобен для текста “У”
    val barCarbsEnd: Color = Color(0xFFC1A8EB)   // Цвет конца градиента углеводов (Classic)
    val barCarbsBrush: Brush = Brush.horizontalGradient(listOf(barCarbsStart, barCarbsEnd)) // Градиент заливки бара углеводов

    // Фон и переполнение
    val barBackground: Color = Color(0xFFE6E0D6) // Фон “пустого” бара (Classic)
    val barOverflow: Color = Color(0x99D64545)   // Цвет переполнения бара (когда % > 100)

    // Legacy aliases
    val barFill: Color = barProteinStart // Legacy: основной цвет заливки (используй `barProteinBrush`/scheme)

    // Размеры баров
    val barHeight: Dp = 32.dp          // Высота одного бара КБЖУ
    val barSpacing: Dp = 16.dp         // Отступ между барами
    val barCornerRadius: Dp = 16.dp    // Скругление баров
    val barBorderWidth: Dp = 0.5.dp    // Толщина обводки бара
    val barTextPaddingStart: Dp = 16.dp // Отступ текста внутри бара слева
    val barTopPadding: Dp = 20.dp      // Верхний отступ блока баров в плитке (обычно 2 × tilePadding)

    // ==================== ВОЛНА КАЛОРИЙ ====================

    val waveColor: Color = Color(0x99A0DCDC)        // Цвет “волны” калорий (Classic)
    val waveColorTop: Color = waveColor             // Верхний цвет градиента волны
    val waveColorBottom: Color = waveColor.copy(alpha = 0.4f) // Нижний цвет градиента волны
    const val waveAmplitudeFraction: Float = 0.018f // Амплитуда волны (доля высоты)
    const val waveOscillationDurationMs: Int = 5000 // Период анимации волны (мс)

    // Фон плиток — единый для всех
    val tileBackgroundColor: Color = surfaceMain // Legacy-фон плиток; в темах смотри `LocalAppColorScheme.tileBackgroundColor`

    // ==================== РАЗМЕРЫ И ОТСТУПЫ ====================

    // Экран
    val screenPadding: Dp = 10.dp // Общий внешний padding экрана (MainScreen)
    val tileSpacing: Dp = 10.dp   // Базовый отступ между плитками/блоками (MainScreen)

    // Высота зоны кнопок вверху (удаление данных, смена темы)
    val topBarButtonsHeight: Dp = 48.dp // Высота зоны иконок в top-плитке (кнопки: reset/theme/test)

    // Адаптивный макет (фолд раскрыт): ширина в dp для переключения на двухпанельный режим
    val expandedLayoutBreakpoint: Dp = 600.dp           // Порог ширины для двухпанельного режима (ExpandedLayout)
    const val expandedLayoutLeftPanelWeight: Float = 0.4f  // Вес левой панели (TOP+MIDDLE)
    const val expandedLayoutRightPanelWeight: Float = 0.6f // Вес правой панели (BOTTOM чат)

    // Плитки
    val tilePadding: Dp = 10.dp          // Внутренний padding плиток (контент часто использует это значение)
    val tileCollapsedHeight: Dp = 180.dp // Базовая высота “свернутой” плитки (если используется)
    val tileCornerRadius: Dp = 24.dp     // Скругление плиток (Tile.kt)
    val tileBorderWidth: Dp = 1.dp       // Толщина обводки плитки (Tile.kt)
    val tileBorderColor: Color = borderLight // Legacy цвет обводки (в реальности часто градиент в Tile.kt)

    // Legacy
    val blockSpacing: Dp = tileSpacing // Legacy alias для расстояний между блоками

    // ==================== ШРИФТЫ ====================

    val fontFamily: FontFamily = FontFamily.SansSerif // Базовый шрифт (редко используется напрямую)

    val fontFamilyCaloriePercent: FontFamily = FontFamily( // Шрифт для крупных процентов калорий (верхняя плитка)
        Font(R.font.barlow_semi_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    val fontFamilyPlank: FontFamily = FontFamily( // Основной “плоский/тонкий” шрифт интерфейса (плитки/карточки/чат)
        Font(R.font.barlow_semi_condensed_thin, FontWeight.Thin, FontStyle.Normal)
    )

    val fontWeightNormal: FontWeight = FontWeight.W500  // Базовый “обычный” вес для заголовков/текста
    val fontWeightHeading: FontWeight = FontWeight.W600 // Усиленный вес для заголовков

    val fontSizeChat = 14.sp     // Размер текста сообщений чата
    val fontSizeInput = 14.sp    // Размер текста ввода
    val fontSizeButton = 18.sp   // Размер текста кнопок (если текстовые)
    val fontSizeLabel = 11.sp    // Размер мелких подписей/лейблов
    val fontSizeHeading = 14.sp  // Размер заголовков в плитках

    const val lineHeightMultiplier: Float = 1.4f // Множитель lineHeight для текста (если используется)

    // ==================== АНИМАЦИИ ====================

    // Длительности (в миллисекундах)
    const val animationFast: Int = 300                // Быстрая анимация (общая)
    const val animationNormal: Int = 400              // Нормальная анимация (общая)
    const val animationSlow: Int = 500                // Медленная анимация (общая)
    const val tileTransitionDurationMs: Int = 400     // Анимация expand/collapse плиток (MainScreen/Tile)
    const val inputTransitionDurationMs: Int = 350    // Анимации input/переходов ввода
    const val bubbleAppearDurationMs: Int = 200       // Появление bubble в чате
    const val caloriePercentAppearDelayMs: Int = 500  // Задержка перед появлением процента калорий
    const val caloriePercentCountDurationMs: Int = 1100 // Длительность count-up процентов калорий
    const val caloriePercentFadeOutDurationMs: Int = 300 // Затухание процента калорий

    // ==================== МНОЖИТЕЛИ (expand/collapse) ====================

    const val expandedPaddingMultiplier: Float = 1.5f     // Множитель padding в expanded режиме (если применяется)
    const val expandedBarHeightMultiplier: Float = 1.15f  // Множитель высоты баров в expanded
    const val expandedBarGapMultiplier: Float = 1.2f      // Множитель расстояний между барами в expanded
    const val collapsedBarHeightMultiplier: Float = 0.85f // Множитель высоты баров в collapsed
    const val collapsedBarGapMultiplier: Float = 0.88f    // Множитель расстояний между барами в collapsed
    const val tileScaleCollapsed: Float = 0.96f           // Scale плитки при collapsed (Tile.kt)
    const val tileScaleExpanded: Float = 1.02f            // Scale плитки при expanded (Tile.kt)
    const val barsCollapsedScale: Float = 0.92f           // Scale баров при collapsed (если применяется)
    val barsCollapsedOffsetY: Dp = 24.dp                  // Сдвиг баров по Y при collapsed (если применяется)

    // Плашка «текущий вес»
    val plankHeight: Dp = 54.dp                 // Высота плашки “текущий вес” (верхняя плитка)
    val plankWheelPickerHeight: Dp = 132.dp     // Высота wheel picker внутри плашки веса
    val plankWheelPickerWidth: Dp = 80.dp       // Ширина wheel picker внутри плашки веса
    val plankPadding: Dp = 96.dp                // Вертикальный отступ/компоновка плашки веса (layout-ручка)
    val plankCornerRadius: Dp = 12.dp           // Скругление плашки веса
    val plankBackground: Color = Color(0xBFE0B8AC) // Фон плашки веса/внутренних элементов (Classic)
    val plankFontSizeLabel = 20.sp              // Размер подписи “кг”/лейбла в плашке веса
    val plankFontSizeNumber = 40.sp             // Размер числа веса в плашке
    const val tileWeightExpanded: Float = 3f    // Вес плитки (Row weight) в expanded режиме (MainScreen)
    const val tileWeightCollapsed: Float = 0.8f // Вес плитки в collapsed режиме (MainScreen)
    const val tileAlphaCollapsed: Float = 0.85f // Альфа “свернутых” плиток (MainScreen/Tile)

    // Веса для idle-состояния (ни одна плитка не активна)
    const val tileWeightIdleTopMiddle: Float = 1f  // Idle: TOP и MIDDLE равные (MainScreen)
    const val tileWeightIdleBottom: Float = 0.18f  // Idle: BOTTOM минимальный (MainScreen)

    // ==================== TOP ↔ MIDDLE (CompactLayout): доли высоты 20/50/80 ====================

    // Доли свободной высоты (от статус-бара до верха свёрнутого чата); в сумме 1.0 на пару
    const val topMiddleRatioCollapsed: Float = 0.20f   // Доля свободной высоты: свёрнуто
    const val topMiddleRatioBalanced: Float = 0.50f     // Доля свободной высоты: среднее (50/50)
    const val topMiddleRatioExpanded: Float = 0.80f     // Доля свободной высоты: развёрнуто

    // Ручка между TOP и MIDDLE: невидимая, только hitbox (оверлей, не в потоке)
    val topMiddleHandleHitHeightDp: Dp = 54.dp         // Высота зоны захвата между TOP и MIDDLE (dp)

    // Порог при отпускании ручки (0..1): progress < threshold -> липнет к TOP expanded; >= threshold -> MIDDLE expanded
    const val topMiddleHandleSnapToExpandedThreshold: Float = 0.65f

    // Длительность магнита ручки при отпускании (плавнее за счёт чуть большей длительности; easing = smoothEasing как у tile)
    const val topMiddleHandleSnapDurationMs: Int = 170

    // Длительность «остаточной» анимации после snap (scale/elevation плиток): 2/3 от tileTransitionDurationMs
    const val topMiddlePostSnapDurationMs: Int = 267  // 400 * 2/3

    // Прогресс (0..1), с которого начинается «бамп» scale/elevation TOP/MIDDLE — чтобы не было второй фазы после snap
    const val topMiddleBumpStartProgress: Float = 0.55f  // С 55% драга бамп идёт вместе с snap

    // Скорость (px/s), выше которой свайп считается быстрым (направление задаёт snap)
    const val topMiddleHandleFlingVelocityThresholdPxPerSec: Float = 800f

    // Фейд и сдвиг плашек «вес»/«активность» — симметрично при сворачивании и разворачивании TOP
    const val topTilePlanksFadeStartProgress: Float = 0.10f  // Начало фейда/появления (0 = TOP раскрыт)
    const val topTilePlanksFadeEndProgress: Float = 0.50f     // Конец фейда: альфа 0 уже в среднем состоянии; появление от среднего к развёрнутому
    const val topTilePlanksMinAlpha: Float = 0f              // Альфа при свёрнутом TOP (при развороте симметрично до 1f)
    const val topTilePlanksInteractionAlphaThreshold: Float = 0.01f // Ниже этого порога плашки не принимают касания (среднее/свёрнутое состояние)
    val topTilePlanksSlideUpDp: Dp = 28.dp                    // Смещение вверх при сворачивании / вниз при разворачивании (одно окно прогресса)

    // Кнопки управления (сброс/тема/тест): фейд и сдвиг вверх при сворачивании TOP; видны только в развёрнутом состоянии (как плашки)
    const val topTileControlsFadeStartProgress: Float = 0.10f  // Начало фейда
    const val topTileControlsFadeEndProgress: Float = 0.50f   // Конец фейда: альфа 0 уже в среднем состоянии
    val topTileControlsSlideUpDp: Dp = 24.dp                   // Смещение вверх при свёрнутом TOP

    // Бары БЖУ: отступ сверху по progress (бары остаются видимыми и плавно поднимаются)
    val topTileBarsTopInsetExpandedDp: Dp = 48.dp   // Отступ сверху для блока БЖУ в развёрнутом состоянии (место под кнопки)
    val topTileBarsTopInsetCollapsedDp: Dp = 8.dp    // Отступ сверху для блока БЖУ в свёрнутом состоянии

    // Высота минимальной плитки чата (только строка ввода)
    val chatTileMinHeight: Dp = 92.dp // Минимальная высота нижней плитки чата (включая input)

    // Нижний отступ списка продуктов в средней плитке, чтобы последний элемент не заходил под плашку дат
    val middleTileDayIndicatorClearanceDp: Dp = 48.dp

    // Доля экрана для раскрытой плитки чата (2/3 экрана)
    const val chatTileExpandedFraction: Float = 0.66f // Доля экрана для раскрытого чата (если используется)

    // Форма нижней плитки — скругление только сверху, снизу уходит за экран
    val bottomTileCornerRadius: Dp = 24.dp // Скругление верхних углов нижней плитки (TilePosition.BOTTOM)

    // ==================== ТЕНИ ====================

    // Значения теней для плиток (elevation)
    val tileElevationNormal: Dp = 6.dp   // Elevation плитки в обычном состоянии (Tile.kt)
    val tileElevationExpanded: Dp = 8.dp // Elevation плитки в expanded состоянии (Tile.kt)

    // Значения теней для полей ввода
    val inputElevation: Dp = 2.dp // Elevation для input-полей (если используется)

    // ==================== ЧАТ: ПУЗЫРИ И БЛОК ОТПРАВКИ ====================
    val chatBubbleUserBackground: Color = Color(0xFFE8C4B8) // Фон пузыря пользователя (Classic)
    val chatBubbleAssistantBackground: Color = surfaceInput   // Фон пузыря ассистента (Classic)
    val chatInputBlockHeight: Dp = 54.dp                      // Высота блока ввода чата (ChatInputBlock)
    val chatInputBlockCornerRadius: Dp = 18.dp                // Скругление блока ввода чата
    val chatInputBlockBackground: Color = Color(0xBFE0B8AC)    // Фон блока ввода чата (Classic)

    // ==================== КАСТОМНЫЕ ФОРМЫ ====================

    // TabbedShape — форма с выступом ("ушком")
    val tabbedShapeTabWidth: Dp = 80.dp          // Ширина “ушка” TabbedShape
    val tabbedShapeTabExtensionTop: Dp = 60.dp   // Выступ ушка вверх
    val tabbedShapeTabExtensionBottom: Dp = 60.dp // Выступ ушка вниз
    val tabbedShapeTabOffsetX: Dp = 80.dp        // Смещение ушка по X
    val tabbedShapeCornerRadius: Dp = 24.dp      // Скругление основного контейнера TabbedShape
    val tabbedShapeTabCornerRadius: Dp = 20.dp   // Скругление самого ушка

    // Компактная версия TabbedShape
    val tabbedShapeCompactTabWidth: Dp = 60.dp        // Ширина “ушка” (compact)
    val tabbedShapeCompactTabExtension: Dp = 40.dp    // Выступ ушка (compact)
    val tabbedShapeCompactTabOffsetX: Dp = 60.dp      // Смещение ушка по X (compact)
    val tabbedShapeCompactCornerRadius: Dp = 20.dp    // Скругление контейнера (compact)
    val tabbedShapeCompactTabCornerRadius: Dp = 16.dp // Скругление ушка (compact)

    // Градиенты для форм

    // Светлый градиент (как на изображении — серо-белый)
    val shapeGradientLightTop: Color = Color(0xFFFFFFFF)     // Верх светлого градиента форм
    val shapeGradientLightBottom: Color = Color(0xFFD9D9D9)  // Низ светлого градиента форм
    val shapeGradientLight: Brush = Brush.verticalGradient( // Градиент для светлых фигур/форм (CustomShapes)
        listOf(shapeGradientLightTop, shapeGradientLightBottom)
    )

    // Кремовый градиент (в стиле приложения)
    val shapeGradientCreamTop: Color = Color(0xFFFFF8F0)     // Верх кремового градиента форм
    val shapeGradientCreamBottom: Color = Color(0xFFE6DFD2)  // Низ кремового градиента форм
    val shapeGradientCream: Brush = Brush.verticalGradient( // Градиент для “кремовых” фигур/форм (CustomShapes)
        listOf(shapeGradientCreamTop, shapeGradientCreamBottom)
    )

    // Нейтральный градиент
    val shapeGradientNeutralTop: Color = Color(0xFFF5F5F5)    // Верх нейтрального градиента форм
    val shapeGradientNeutralBottom: Color = Color(0xFFE0E0E0) // Низ нейтрального градиента форм
    val shapeGradientNeutral: Brush = Brush.verticalGradient( // Градиент для нейтральных фигур/форм (CustomShapes)
        listOf(shapeGradientNeutralTop, shapeGradientNeutralBottom)
    )

    // Акцентный градиент (бирюзовый)
    val shapeGradientAccentTop: Color = Color(0xFFB8E8E8)    // Верх акцентного градиента форм
    val shapeGradientAccentBottom: Color = Color(0xFF8DCFCF) // Низ акцентного градиента форм
    val shapeGradientAccent: Brush = Brush.verticalGradient( // Градиент для акцентных фигур/форм (CustomShapes)
        listOf(shapeGradientAccentTop, shapeGradientAccentBottom)
    )

    // ==================== GLASSMORPHISM ====================

    // Радиусы размытия (backdrop blur)
    val blurRadius: Dp = 20.dp              // Стандартный blur radius (glass поверхностей)
    val blurRadiusLight: Dp = 12.dp         // Лёгкий blur radius (малые glass элементы)
    val blurRadiusStrong: Dp = 32.dp        // Сильный blur radius (модалки/оверлеи)

    // Прозрачность и тонировка
    const val glassTintAlpha: Float = 0.72f // Базовая альфа tint для glass поверхностей (Tile.kt, DayIndicator и т.п.)
    const val glassNoise: Float = 0.03f     // “Зерно”/шум для glass (0..1)

    // Насыщенность цветов под стеклом
    const val glassSaturationBoost: Float = 1.2f // (Если используется) усиление насыщенности под стеклом

    // Global dithering (anti-banding)
    const val globalDitherNoiseAlpha: Float = 0.012f   // very subtle
    const val globalDitherNoiseDensity: Float = 0.10f  // fine grain

    // ==================== EXPANDED OVERLAY (фон за раскрытой карточкой) ====================

    val expandedOverlayBlurRadius: Dp = 120.dp
    // Затемнение фона за карточкой (отдельный слой затемнения).
    const val expandedOverlayScrimAlpha: Float = 0.45f
    // Tint в blur-слое на фоне (ниже = белее/светлее восприятие; для тёмных тем — уменьшать).
    const val expandedOverlayBlurTintAlpha: Float = 0.15f
    const val expandedOverlayTintAlpha: Float = 0.1f         // Legacy: альфа затемнения заднего фона
    const val expandedOverlayNoiseFactor: Float = 0.00f       // Зерно/шум overlay
    const val expandedOverlayVignetteAlpha: Float = 0f        // Альфа виньетки (затемнение краёв)

    // ==================== EXPANDED FOOD CARD (модальная карточка продукта) ====================

    val expandedFoodCardCornerRadius: Dp = tileCornerRadius   // Скругление углов карточки
    const val expandedFoodCardWidthFraction: Float = 0.92f    // Ширина карточки (доля экрана)
    val expandedFoodCardGlassBlurRadius: Dp = blurRadiusStrong // Радиус размытия внутри карточки (haze)
    // Альфа tint внутри hazeEffect карточки (ниже = белее текст/иконки; для тёмных тем — уменьшать).
    const val expandedFoodCardGlassTintAlpha: Float = 0.65f
    val expandedFoodCardGlassNoise: Float = glassNoise        // Шум стекла expanded карточки
    // Альфа подложки под стеклом (выше = светлее карточка; для тёмных тем — увеличивать).
    const val expandedFoodCardBaseAlpha: Float = 0.60f
    const val expandedFoodCardGlassOverlayAlpha: Float = 0.10f // Доп. overlay для стекла
    const val expandedFoodCardBorderAlpha: Float = 1f         // Альфа обводки карточки
    const val expandedFoodCardVignetteAlpha: Float = 0.0f     // Внутренняя виньетка (если нужна)

    // Шрифты текста в expanded карточке
    val expandedFoodCardTitleFontSize = 18.sp   // Заголовок (название продукта)
    val expandedFoodCardMetaFontSize = 16.sp    // Мета (вес и т.п.)
    val expandedFoodCardBodyFontSize = 14.sp    // Основной текст (описание)

    // Цвета текста (явно белый для тёмных тем; без лишней альфы).
    val expandedFoodCardTitleTextColor: Color = Color.White   // Цвет заголовка
    val expandedFoodCardMetaTextColor: Color = Color.White    // Цвет мета-текста
    val expandedFoodCardBodyTextColor: Color = Color.White   // Цвет основного текста
    const val expandedFoodCardBodyTextAlpha: Float = 1f       // Альфа основного текста (1f = без затемнения)

    // Тень текста (читаемость на haze)
    val expandedFoodCardTextShadowColor: Color = Color.Black
    const val expandedFoodCardTextShadowAlpha: Float = 0.25f
    val expandedFoodCardTextShadowBlur: Dp = 4.dp
    val expandedFoodCardTextShadowOffsetY: Dp = 1.dp

    // Градиент иконок действий (центр → край; BlendMode.SrcIn)
    val expandedFoodCardFgCenterColor: Color = Color.White    // Цвет центра (белый)
    val expandedFoodCardFgEdgeColor: Color = Color(0xFFB8B8B8) // Цвет края (светло-серый)
    const val expandedFoodCardFgCenterAlpha: Float = 1f
    const val expandedFoodCardFgEdgeAlpha: Float = 0.85f

    // ==================== FOOD ENTRY CARDS ====================

    val foodEntryCardHeightCollapsed: Dp = 45.dp             // Высота карточки продукта в ленте (collapsed)
    val foodEntryCardHeightExpanded: Dp = 320.dp             // Целевая высота expanded карточки (если используется в overlay)
    val foodEntryCardCornerRadius: Dp = 20.dp                // Скругление карточки продукта (collapsed)
    val foodEntryCardInnerPadding: Dp = 16.dp                // Внутренние отступы контента карточки продукта
    val foodEntryCardTextRowsSpacing: Dp = -1.dp             // Отступ между 1-й и 2-й строкой текста в карточке

    // ----- Emoji в свёрнутой карточке продукта (список в ленте) -----
    val collapsedFoodEntryEmojiSizeSp = 64.sp                 // Размер emoji (sp); expanded по умолчанию в 3× больше
    const val collapsedFoodEntryEmojiAlpha: Float = 0.3f     // Альфа emoji (яркость под стеклом)
    val collapsedFoodEntryEmojiOffsetXDp: Dp = 0.dp          // Сдвиг emoji по X (положительное — вправо)
    val collapsedFoodEntryEmojiOffsetYDp: Dp = -10.dp        // Сдвиг emoji по Y (положительное — вниз)
    val collapsedFoodEntryEmojiNudgeYDp: Dp = 8.dp           // Тонкая подстройка Y (компенсация метрик шрифта emoji)
    const val collapsedFoodEntryEmojiRotationDeg: Float = 0f // Поворот emoji (градусы)
    val collapsedFoodEntryEmojiTintColor: Color = Color.Black // Цвет tint для приглушения emoji (если TintAlpha > 0)
    const val collapsedFoodEntryEmojiTintAlpha: Float = 0f   // Сила tint (0 = выключено)

    // ----- Emoji в раскрытой карточке продукта (оверлей-модалка); размер по умолчанию 3× collapsed -----
    val expandedFoodEntryEmojiSizeSp = 192.sp                 // Размер emoji (sp); явно 3× collapsed (64 → 192)
    const val expandedFoodEntryEmojiAlpha: Float = 0.35f      // Альфа emoji под стеклом
    val expandedFoodEntryEmojiOffsetXDp: Dp = 0.dp           // Сдвиг emoji по X (тонкая подстройка)
    val expandedFoodEntryEmojiOffsetYDp: Dp = 0.dp           // Сдвиг emoji по Y (тонкая подстройка)
    val expandedFoodEntryEmojiNudgeYDp: Dp = 0.dp             // Компенсация метрик шрифта emoji по Y (в overlay обычно 0)
    const val expandedFoodEntryEmojiRotationDeg: Float = 0f   // Поворот emoji (градусы)
    val expandedFoodEntryEmojiPaddingTopDp: Dp = 12.dp         // Отступ сверху от края контента (позиция top-end)
    val expandedFoodEntryEmojiPaddingEndDp: Dp = 12.dp        // Отступ справа от края контента (позиция top-end)
    val foodEntryCardGlassBlurRadius: Dp = blurRadiusLight    // Blur стекла collapsed карточки
    const val foodEntryCardGlassTintAlpha: Float = 0.72f      // Базовая альфа стекла collapsed карточки (если используется)
    const val foodEntryCardGlassTintAlphaOverride: Float = 0.2f // Альфа tint стекла collapsed карточки (ручная настройка)
    const val foodEntryCardGlassDebugAlpha: Float = 0.16f     // Debug/ручная альфа стекла (collapsed/overlay, если подключено)
    const val foodEntryCardScrimAlpha: Float = 0.4f           // Альфа scrim при раскрытии карточки (если используется)
    val foodEntryCardActionIconSize: Dp = 26.dp               // Размер иконок действий в expanded карточке
    val foodEntryCardActionRowHeight: Dp = 48.dp              // Высота ряда иконок действий в expanded карточке

    // ==================== KCAL CHART (график калорий в верхней плитке) ====================

    // Анимация
    const val chartSettleDurationMs: Int = 300                // Длительность settle-анимации графика
    const val chartFlingVelocityThreshold: Float = 500f       // Порог скорости для fling (dp/s)

    // Цвета графика
    val chartAccentColor: Color = Color(0xFFCC7A3A)           // Основной акцентный цвет графика (оранжевый)
    val chartLineColor: Color = chartAccentColor              // Цвет линии графика
    const val chartFillAlphaTop: Float = 0.45f                // Альфа верха градиента заливки
    const val chartFillAlphaBottom: Float = 0.05f             // Альфа низа градиента заливки
    const val chartSelectionLineAlpha: Float = 0.6f           // Альфа вертикальной линии выбора

    // Размеры графика
    val chartHeight: Dp = 140.dp                              // Высота области графика
    val chartPointRadius: Dp = 3.dp                           // Радиус точки дня
    val chartSelectedPointRadius: Dp = 5.dp                   // Радиус выбранной точки
    val chartLineStrokeWidth: Dp = 2.dp                       // Толщина линии графика
    val chartVerticalPadding: Dp = 6.dp                       // Вертикальный padding графика
    val chartLabelBottomPadding: Dp = 10.dp                   // Отступ снизу для подписей дней

    // Tooltip
    val chartTooltipPadding: Dp = 6.dp                        // Padding внутри tooltip
    val chartTooltipCornerRadius: Dp = 6.dp                   // Скругление tooltip
    const val chartTooltipBackgroundAlpha: Float = 0.9f       // Альфа фона tooltip
}
