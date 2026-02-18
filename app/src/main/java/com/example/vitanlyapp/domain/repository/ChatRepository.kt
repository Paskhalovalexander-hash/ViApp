package com.example.vitanlyapp.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий сообщений чата.
 * Domain-интерфейс без привязки к Room/Android.
 */
interface ChatRepository {

    /**
     * Получить все сообщения как Flow (реактивно).
     * Сортировка: старые в начале, новые в конце.
     */
    fun getMessagesFlow(): Flow<List<ChatMessageDomain>>

    /**
     * Получить последние N сообщений для контекста AI.
     */
    suspend fun getLastMessages(limit: Int): List<ChatMessageDomain>

    /**
     * Добавить сообщение пользователя.
     */
    suspend fun addUserMessage(text: String)

    /**
     * Добавить ответ ассистента.
     */
    suspend fun addAssistantMessage(text: String)

    /**
     * Очистить историю чата.
     */
    suspend fun clearHistory()

    /**
     * Получить количество сообщений.
     */
    suspend fun getMessageCount(): Int
}

/**
 * Domain-модель сообщения чата.
 * Отделена от Entity для чистой архитектуры.
 */
data class ChatMessageDomain(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val createdAt: Long
)
