package com.example.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val caption: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val isLikedByMe: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "postId" to postId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "authorPhotoUrl" to authorPhotoUrl,
            "caption" to caption,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "likesCount" to likesCount,
            "commentsCount" to commentsCount,
            "createdAt" to createdAt
        )
    }
}
