package com.example.vitanlyapp.ui.screen.main
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.domain.model.ChatMessage
import com.example.vitanlyapp.domain.model.ChatRole
import com.example.vitanlyapp.ui.component.ChatHintsCarousel
import com.example.vitanlyapp.ui.component.LoadingBubble
import com.example.vitanlyapp.ui.component.TileBackground
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme

@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    val isUser = message.role == ChatRole.USER
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.tilePadding, vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isUser) scheme.chatBubbleUserBackground
                    else scheme.chatBubbleAssistantBackground
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(0.85f),
            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Text(
                text = message.text,
                color = scheme.textColor,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin,
                fontSize = DesignTokens.fontSizeChat
            )
        }
    }
}

/**
 * Контент плитки чата — только список сообщений (без блока ввода).
 * Блок ввода вынесен отдельно в ChatInputBlock для фиксированной позиции.
 * 
 * Когда чат пустой и раскрыт — показывает карусель подсказок.
 * Когда идёт загрузка — показывает LoadingBubble с анимацией.
 *
 * @param messages Список сообщений чата
 * @param isLoading Идёт ли загрузка ответа от LLM
 * @param isCollapsed Свёрнута ли плитка
 * @param showContent Показывать контент (LazyColumn/карусель). false при сворачивании — плитка не тупит
 * @param systemBottomInset Отступ снизу (системные insets, без суммирования IME + navBars)
 * @param onHintClick Callback при клике на подсказку (для автозаполнения поля ввода)
 * @param modifier Модификатор
 */
@Composable
fun BottomTileContent(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isCollapsed: Boolean = false,
    showContent: Boolean = true,
    systemBottomInset: Dp = 0.dp,
    onHintClick: (String) -> Unit = {}
) {
    TileBackground()

    val listState = rememberLazyListState()

    // Контент чата — только когда раскрыто И showContent (скрываем раньше при сворачивании)
    if (!isCollapsed && showContent) {
        if (messages.isEmpty()) {
            // Пустой чат — показываем карусель подсказок
            // Подсказки поднимаются вверх когда клавиатура открыта (systemBottomInset)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(bottom = DesignTokens.chatInputBlockHeight + 24.dp + systemBottomInset),
                contentAlignment = Alignment.Center
            ) {
                ChatHintsCarousel(
                    onHintClick = onHintClick
                )
            }
        } else {
            // Есть сообщения — отступ снизу = высота input + зазор, чтобы последнее сообщение было над input
            val GAP = 24.dp
            val bottomOffset = DesignTokens.chatInputBlockHeight + GAP + systemBottomInset

            LaunchedEffect(messages.size, isLoading, systemBottomInset) {
                listState.scrollToItem(0)
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = DesignTokens.tilePadding, vertical = 4.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = bottomOffset
                    )
                ) {
                    // При reverseLayout первый item отображается внизу
                    // Поэтому сначала loading (будет внизу), потом сообщения в обратном порядке
                    
                    // Индикатор загрузки — внизу списка (первый при reverseLayout)
                    if (isLoading) {
                        item(key = "loading") {
                            LoadingBubble()
                        }
                    }
                    
                    // Сообщения в обратном порядке (последнее сообщение = первый item = внизу)
                    items(
                        items = messages.asReversed(),
                        key = { msg -> msg.id }
                    ) { message ->
                        ChatBubble(message = message)
                    }
                }
            }
        }
    }
}

/**
 * Блок ввода сообщения — вынесен отдельно для фиксированной позиции внизу экрана.
 * Не движется вместе с плиткой чата при анимации.
 *
 * @param onSendMessage Callback при отправке сообщения
 * @param isLoading Идёт ли загрузка
 * @param isCollapsed Свёрнута ли плитка
 * @param isFullyExpanded Полностью ли раскрыта плитка (для показа клавиатуры)
 * @param bottomPadding Отступ снизу (для safe area)
 * @param allowAutoIme Если false — не показывать клавиатуру автоматически (например, при drag вниз)
 * @param onExpandRequest Callback при запросе раскрытия плитки
 * @param prefillText Текст для автозаполнения поля ввода (например, из подсказки)
 * @param onPrefillConsumed Callback после применения prefillText (для сброса)
 * @param modifier Модификатор
 */
@Composable
fun ChatInputBlock(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isCollapsed: Boolean = false,
    isFullyExpanded: Boolean = false,
    allowAutoIme: Boolean = true,
    onExpandRequest: () -> Unit = {},
    prefillText: String = "",
    onPrefillConsumed: () -> Unit = {}
) {
    val scheme = LocalAppColorScheme.current
    var inputText by remember { mutableStateOf("") }
    val density = LocalDensity.current

    // Дополнительный нижний отступ (помимо системных insets, которые задаются снаружи).
    // Сохраняем поведение: на gesture-nav минимум ~24dp, на 3-button/nav-bar обычно ~10dp.
    val navBottomDp = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val extraBottomPadding = maxOf(10.dp, (24.dp - navBottomDp).coerceAtLeast(0.dp))
    
    // Применяем prefillText когда он приходит
    LaunchedEffect(prefillText) {
        if (prefillText.isNotEmpty()) {
            inputText = prefillText
            onPrefillConsumed()
        }
    }
    
    // Фокус и клавиатура — только когда полностью раскрыто
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Автоматический фокус и клавиатура только при ПОЛНОМ раскрытии и если разрешено (не во время drag вниз)
    LaunchedEffect(isFullyExpanded, allowAutoIme) {
        if (isFullyExpanded && allowAutoIme) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val inputShape = RoundedCornerShape(DesignTokens.chatInputBlockCornerRadius)
    val sendButtonShape = RoundedCornerShape(DesignTokens.chatInputBlockHeight / 2)

    // Градиентная обводка как у плиток
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            scheme.borderGradientTop,
            scheme.borderGradientBottom
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.tilePadding)
            .padding(bottom = extraBottomPadding)
            .height(DesignTokens.chatInputBlockHeight)
            .clip(inputShape)
            .background(scheme.chatInputBlockBackground, inputShape)
            .border(DesignTokens.tileBorderWidth, gradientBorder, inputShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Поле ввода
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it.replace("\n", "") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            onSendMessage(text)
                            inputText = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                enabled = !isCollapsed,
                textStyle = TextStyle(
                    color = scheme.textColor,
                    fontFamily = DesignTokens.fontFamilyPlank,
                    fontWeight = FontWeight.Thin,
                    fontSize = DesignTokens.fontSizeInput
                ),
                cursorBrush = SolidColor(scheme.barFatFill),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Сообщение...",
                                color = scheme.textColor.copy(alpha = 0.5f),
                                fontFamily = DesignTokens.fontFamilyPlank,
                                fontWeight = FontWeight.Thin,
                                fontSize = DesignTokens.fontSizeInput
                            )
                        }
                        inner()
                    }
                }
            )

            // Кнопка отправки
            Box(
                modifier = Modifier
                    .size(DesignTokens.chatInputBlockHeight - 12.dp)
                    .clip(sendButtonShape)
                    .background(scheme.chatBubbleUserBackground.copy(alpha = 0.5f))
                    .clickable(enabled = !isLoading && !isCollapsed) {
                        val text = inputText.trim()
                        if (text.isEmpty()) return@clickable
                        onSendMessage(text)
                        inputText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                // заглушка под иконку отправки
            }
        }
        
        // Overlay для перехвата кликов когда плитка свёрнута
        if (isCollapsed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onExpandRequest() }
            )
        }
    }
}
