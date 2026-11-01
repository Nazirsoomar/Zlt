package com.example.data.model

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + (1..1000).random(),
    val isUser: Boolean,
    val text: String,
    val suggestedCommand: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
