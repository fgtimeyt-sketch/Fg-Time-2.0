package com.example.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "displayName" to displayName,
            "username" to username,
            "bio" to bio,
            "photoUrl" to photoUrl,
            "followersCount" to followersCount,
            "followingCount" to followingCount,
            "postsCount" to postsCount,
            "createdAt" to createdAt
        )
    }
}
