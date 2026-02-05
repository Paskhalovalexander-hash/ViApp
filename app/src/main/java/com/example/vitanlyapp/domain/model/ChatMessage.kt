package com.example.vitanlyapp.domain.model

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val text: String)
