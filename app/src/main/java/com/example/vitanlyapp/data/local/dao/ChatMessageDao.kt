package com.example.vitanlyapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.vitanlyapp.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с сообщениями чата.
 */
@Dao
interface ChatMessageDao {

    /**
     * Получить все сообщения как Flow (реактивно).
     * Сортировка по времени создания (новые в конце).
     */
    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    /**
     * Получить последние N сообщений.
     */
    @Query("SELECT * FROM chat_messages ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLastMessages(limit: Int): List<ChatMessageEntity>

    /**
     * Вставить новое сообщение.
     */
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    /**
     * Вставить несколько сообщений.
     */
    @Insert
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    /**
     * Очистить всю историю чата.
     */
    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()

    /**
     * Получить количество сообщений.
     */
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int
}
