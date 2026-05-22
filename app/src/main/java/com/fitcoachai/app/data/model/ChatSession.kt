package com.fitcoachai.app.data.model

data class ChatSession(
    val id: String = "",
    val title: String = "Nouvelle conversation",
    val lastTimestamp: Long = System.currentTimeMillis()
)
