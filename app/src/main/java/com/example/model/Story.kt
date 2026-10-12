package com.example.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Story(
    val storyId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val mediaUrl: String = "",
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "storyId" to storyId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorPhotoUrl" to authorPhotoUrl,
            "mediaUrl" to mediaUrl,
            "caption" to caption,
            "createdAt" to createdAt,
            "expiresAt" to expiresAt
        )
    }
}
