package com.example.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Follow(
    val followId: String = "",
    val followerId: String = "",
    val followingId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "followerId" to followerId,
            "followingId" to followingId,
            "createdAt" to createdAt
        )
    }
}
