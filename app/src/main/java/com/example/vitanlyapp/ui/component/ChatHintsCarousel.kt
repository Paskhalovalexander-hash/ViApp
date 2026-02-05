package com.example.vitanlyapp.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitanlyapp.ui.data.ChatHint
import com.example.vitanlyapp.ui.data.chatHints
import com.example.vitanlyapp.ui.design.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Цветовая схема для одной подсказки.
 */
data class HintColorScheme(
    val centerColor: Color,    // Центр радиального градиента (светлее)
    val edgeColor: Color,      // Края радиального градиента (темнее)
    val borderColor: Color     // Цвет обводки
)

/**
 * Палитра цветов для подсказок.
 * Цвета плавно меняются при переключении.
 */
private val hintColorSchemes = listOf(
    // Терракотовый/коричневый
    HintColorScheme(
        centerColor = Color(0xFFB07860),
        edgeColor = Color(0xFF5C3D2E),
        borderColor = Color(0x66D4A574)
    ),
    // Синий/индиго
    HintColorScheme(
        centerColor = Color(0xFF6B7FA8),
        edgeColor = Color(0xFF2E3D5C),
        borderColor = Color(0x6690A4D4)
    ),
    // Зелёный/изумрудный
    HintColorScheme(
        centerColor = Color(0xFF6BA08B),
        edgeColor = Color(0xFF2E5C4A),
        borderColor = Color(0x6690D4B4)
    ),
    // Фиолетовый
    HintColorScheme(
        centerColor = Color(0xFF8B6BA0),
        edgeColor = Color(0xFF4A2E5C),
        borderColor = Color(0x66B490D4)
    ),
    // Тёплый оранжевый
    HintColorScheme(
        centerColor = Color(0xFFA08B6B),
        edgeColor = Color(0xFF5C4A2E),
        borderColor = Color(0x66D4C090)
    ),
    // Розовый/малиновый
    HintColorScheme(
        centerColor = Color(0xFFA06B8B),
        edgeColor = Color(0xFF5C2E4A),
        borderColor = Color(0x66D490B4)
    )
)

private val hintTitleColor = Color.White
private val hintExampleColor = Color.White.copy(alpha = 0.65f)
private val hintIndicatorActive = Color.White.copy(alpha = 0.9f)
private val hintIndicatorInactive = Color.White.copy(alpha = 0.35f)

private val hintBlockHeight = 120.dp
private val hintBlockCornerRadius = 16.dp
private val hintBorderWidth = 1.dp

/** Задержка автопрокрутки подсказок (мс) */
private const val AUTO_SCROLL_DELAY_MS = 5000L

/**
 * Карусель подсказок для пустого чата.
 * Автоматически переключается каждые 5 секунд.
 * Свайп влево/вправо для ручного переключения.
 *
 * @param hints Список подсказок для отображения (по умолчанию из ui/data/ChatHints.kt)
 * @param onHintClick Callback при клике на подсказку (для автозаполнения поля ввода)
 * @param modifier Модификатор
 */
@Composable
fun ChatHintsCarousel(
    hints: List<ChatHint> = chatHints,
    onHintClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { hints.size })

    // Автопрокрутка каждые 5 секунд (исправленная версия)
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_SCROLL_DELAY_MS)
            val nextPage = (pagerState.currentPage + 1) % hints.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 500)
            )
        }
    }

    // Текущий цвет схемы с плавной анимацией
    val currentColorScheme = hintColorSchemes[pagerState.currentPage % hintColorSchemes.size]
    
    val animatedCenterColor by animateColorAsState(
        targetValue = currentColorScheme.centerColor,
        animationSpec = tween(durationMillis = 600),
        label = "centerColor"
    )
    val animatedEdgeColor by animateColorAsState(
        targetValue = currentColorScheme.edgeColor,
        animationSpec = tween(durationMillis = 600),
        label = "edgeColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = currentColorScheme.borderColor,
        animationSpec = tween(durationMillis = 600),
        label = "borderColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth(0.67f) // Не больше 2/3 ширины экрана
            .padding(horizontal = DesignTokens.tilePadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(hintBlockHeight),
            beyondViewportPageCount = 1
        ) { page ->
            HintCard(
                hint = hints[page],
                onClick = { onHintClick(hints[page].example) },
                centerColor = animatedCenterColor,
                edgeColor = animatedEdgeColor,
                borderColor = animatedBorderColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Индикаторы страниц
        PageIndicators(
            pageCount = hints.size,
            currentPage = pagerState.currentPage
        )
    }
}

/**
 * Карточка одной подсказки с радиальным градиентом.
 */
@Composable
private fun HintCard(
    hint: ChatHint,
    onClick: () -> Unit,
    centerColor: Color,
    edgeColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(hintBlockCornerRadius)

    // Радиальный градиент от центра к краям
    val radialGradient = Brush.radialGradient(
        colors = listOf(centerColor, edgeColor),
        center = Offset(0.3f, 0.3f), // Смещение центра градиента влево-вверх
        radius = 800f
    )

    Box(
        modifier = modifier
            .height(hintBlockHeight)
            .clip(cardShape)
            .background(radialGradient)
            .border(hintBorderWidth, borderColor, cardShape)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // Заголовок подсказки (белый, жирный)
            Text(
                text = hint.title,
                fontSize = 16.sp,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Bold,
                color = hintTitleColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Пример запроса (светлее)
            Text(
                text = hint.example,
                fontSize = 14.sp,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Normal,
                color = hintExampleColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Индикаторы страниц (точки).
 */
@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) hintIndicatorActive else hintIndicatorInactive
                    )
            )
        }
    }
}
