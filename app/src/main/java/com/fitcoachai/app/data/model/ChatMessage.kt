package com.fitcoachai.app.data.model

import android.graphics.Bitmap

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val image: Bitmap? = null,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)