package com.example.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "messageId" to messageId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderName" to senderName,
            "senderPhotoUrl" to senderPhotoUrl,
            "text" to text,
            "mediaUrl" to mediaUrl,
            "createdAt" to createdAt
        )
    }
}
