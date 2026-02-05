package com.example.vitanlyapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Таблица сообщений чата.
 * Хранит историю переписки пользователя с AI-агентом.
 *
 * Индекс по времени создания для сортировки.
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["createdAt"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Роль отправителя (USER/ASSISTANT) */
    val role: MessageRole,

    /** Текст сообщения */
    val text: String,

    /** Время создания сообщения (timestamp) */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Роль отправителя сообщения.
 */
enum class MessageRole {
    /** Сообщение от пользователя */
    USER,

    /** Ответ AI-агента */
    ASSISTANT
}
