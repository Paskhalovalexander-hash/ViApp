package com.example.vitanlyapp.ui.screen.main

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.domain.model.ChatMessage
import com.example.vitanlyapp.domain.model.ChatRole
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

@Composable
fun BottomTileContent(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    isLoading: Boolean = false,
    isCollapsed: Boolean = false,
    bottomPadding: Dp = 0.dp,
    onExpandRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    TileBackground()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Фокус и клавиатура — автоматически при раскрытии
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Автоматический фокус на поле ввода когда плитка раскрывается
    LaunchedEffect(isCollapsed) {
        if (!isCollapsed) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // HazeState для размытия сообщений чата за блоком ввода
    val chatHazeState = rememberHazeState()
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Параметры blur
    val blurRadius = DesignTokens.blurRadiusLight
    val glassTint = scheme.glassTint
    val glassNoise = DesignTokens.glassNoise
    val glassTintAlpha = DesignTokens.glassTintAlpha

    val inputShape = RoundedCornerShape(DesignTokens.chatInputBlockCornerRadius)
    val sendButtonShape = RoundedCornerShape(DesignTokens.chatInputBlockHeight / 2)

    // Градиентная обводка как у плиток
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            scheme.borderGradientTop,
            scheme.borderGradientBottom
        )
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isCollapsed) Alignment.Center else Alignment.TopStart
    ) {
        // Контент чата — источник для размытия (скрыт когда свёрнуто)
        if (!isCollapsed) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(chatHazeState)
                    .padding(horizontal = DesignTokens.tilePadding, vertical = 4.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = DesignTokens.chatInputBlockHeight + 20.dp + bottomPadding // 10dp отступ от строки ввода
                    ),
                    // Сообщения прижаты к низу — новые внизу, старые поднимаются вверх
                    verticalArrangement = Arrangement.Bottom
                ) {
                    items(
                        items = messages,
                        key = { msg -> "${msg.role}-${msg.text.hashCode()}" }
                    ) { message ->
                        ChatBubble(message = message)
                    }
                }
            }
        }

        // Блок ввода с glassmorphism — поверх списка сообщений (или по центру когда свёрнуто)
        // Кнопка отправки встроена внутрь поля ввода справа
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isCollapsed) {
                        Modifier
                            .padding(horizontal = DesignTokens.tilePadding)
                            .padding(bottom = 10.dp + bottomPadding)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = DesignTokens.tilePadding)
                            // Гарантированный отступ от навигации Android
                            .padding(bottom = (10.dp + bottomPadding).coerceAtLeast(24.dp))
                    }
                )
                .height(DesignTokens.chatInputBlockHeight)
                .clip(inputShape)
                .then(
                    if (supportsBlur && !isCollapsed) {
                        Modifier.hazeEffect(state = chatHazeState) {
                            this.blurRadius = blurRadius
                            tints = listOf(HazeTint(glassTint.copy(alpha = glassTintAlpha)))
                            noiseFactor = glassNoise
                        }
                    } else {
                        Modifier.background(scheme.chatInputBlockBackground, inputShape)
                    }
                )
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
                    enabled = !isCollapsed, // Отключаем ввод когда свёрнуто
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

                // Кнопка отправки — встроена внутрь поля ввода справа
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
}
