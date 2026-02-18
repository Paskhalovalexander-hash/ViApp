package com.example.vitanlyapp.data.repository

import com.example.vitanlyapp.data.local.dao.ChatMessageDao
import com.example.vitanlyapp.data.local.entity.ChatMessageEntity
import com.example.vitanlyapp.data.local.entity.MessageRole
import com.example.vitanlyapp.domain.repository.ChatMessageDomain
import com.example.vitanlyapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория сообщений чата.
 * Использует Room DAO для хранения данных.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    override fun getMessagesFlow(): Flow<List<ChatMessageDomain>> {
        return chatMessageDao.getAllMessagesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLastMessages(limit: Int): List<ChatMessageDomain> {
        // DAO возвращает в обратном порядке (новые первые), переворачиваем
        return chatMessageDao.getLastMessages(limit)
            .reversed()
            .map { it.toDomain() }
    }

    override suspend fun addUserMessage(text: String) {
        val entity = ChatMessageEntity(
            role = MessageRole.USER,
            text = text
        )
        chatMessageDao.insert(entity)
    }

    override suspend fun addAssistantMessage(text: String) {
        val entity = ChatMessageEntity(
            role = MessageRole.ASSISTANT,
            text = text
        )
        chatMessageDao.insert(entity)
    }

    override suspend fun clearHistory() {
        chatMessageDao.clearAll()
    }

    override suspend fun getMessageCount(): Int {
        return chatMessageDao.getMessageCount()
    }

    /**
     * Конвертирует Entity в Domain модель.
     */
    private fun ChatMessageEntity.toDomain() = ChatMessageDomain(
        id = id,
        isUser = role == MessageRole.USER,
        text = text,
        createdAt = createdAt
    )
}
