package com.example.vitanlyapp.ui.screen.main

import android.os.Build
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
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
 * @param bottomPadding Отступ снизу (для safe area)
 * @param onHintClick Callback при клике на подсказку (для автозаполнения поля ввода)
 * @param modifier Модификатор
 */
@Composable
fun BottomTileContent(
    messages: List<ChatMessage>,
    isLoading: Boolean = false,
    isCollapsed: Boolean = false,
    bottomPadding: Dp = 0.dp,
    imeBottomPadding: Dp = 0.dp,
    onHintClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    TileBackground()

    val listState = rememberLazyListState()

    // Контент чата — только когда раскрыто
    if (!isCollapsed) {
        if (messages.isEmpty()) {
            // Пустой чат — показываем карусель подсказок
            // Подсказки поднимаются вверх когда клавиатура открыта (imeBottomPadding)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(bottom = DesignTokens.chatInputBlockHeight + 32.dp + bottomPadding + imeBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                ChatHintsCarousel(
                    onHintClick = onHintClick
                )
            }
        } else {
            // Есть сообщения — показываем список
            // Используем reverseLayout = true — стандартный паттерн для чатов:
            // последние сообщения автоматически видны внизу
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
                        bottom = DesignTokens.chatInputBlockHeight + 32.dp + bottomPadding + imeBottomPadding
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
                        key = { msg -> "${msg.role}-${msg.text.hashCode()}" }
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
    isLoading: Boolean = false,
    isCollapsed: Boolean = false,
    isFullyExpanded: Boolean = false,
    allowAutoIme: Boolean = true,
    bottomPadding: Dp = 0.dp,
    onExpandRequest: () -> Unit = {},
    prefillText: String = "",
    onPrefillConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    var inputText by remember { mutableStateOf("") }
    
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
            .padding(bottom = (10.dp + bottomPadding).coerceAtLeast(24.dp))
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
