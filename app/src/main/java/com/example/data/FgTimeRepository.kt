package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.Follow
import com.example.model.Post
import com.example.model.PostComment
import com.example.model.Story
import com.example.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FgTimeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Convenience constructor resolving the provisioned database ID
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: error("User must be authenticated")
    }

    // ----------------------------------------------------
    // User Profile
    // ----------------------------------------------------

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)?.let {
                    if (it.userId.isBlank()) it.copy(userId = snapshot.id) else it
                }
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.set(profile.toMap()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun searchUsers(query: String): Flow<List<UserProfile>> = callbackFlow {
        val collectionRef = db.collection("users")
        val listener = collectionRef.limit(30).addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(UserProfile::class.java)?.let {
                    if (it.userId.isBlank()) it.copy(userId = doc.id) else it
                }
            } ?: emptyList()
            val filtered = if (query.isBlank()) {
                users
            } else {
                val q = query.trim().lowercase()
                users.filter {
                    it.username.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q)
                }
            }
            trySend(filtered)
        }
        awaitClose { listener.remove() }
    }

    // ----------------------------------------------------
    // Posts & Feed
    // ----------------------------------------------------

    fun observeFeedPosts(): Flow<List<Post>> = callbackFlow {
        val collectionRef = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "posts")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val posts = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.let {
                    if (it.postId.isBlank()) it.copy(postId = doc.id) else it
                }
            } ?: emptyList()
            trySend(posts)
        }
        awaitClose { listener.remove() }
    }

    fun observeUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val collectionRef = db.collection("posts")
            .whereEqualTo("authorId", userId)
            .limit(50)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "posts")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val posts = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.let {
                    if (it.postId.isBlank()) it.copy(postId = doc.id) else it
                }
            } ?: emptyList()
            val sorted = posts.sortedByDescending { it.createdAt }
            trySend(sorted)
        }
        awaitClose { listener.remove() }
    }

    suspend fun createPost(
        caption: String,
        mediaUrl: String,
        mediaType: String = "image",
        authorProfile: UserProfile
    ) {
        val uid = requireUserId()
        val postId = UUID.randomUUID().toString()
        val docRef = db.collection("posts").document(postId)

        val post = Post(
            postId = postId,
            authorId = uid,
            authorName = authorProfile.displayName.ifBlank { "FG Member" },
            authorUsername = authorProfile.username.ifBlank { "member" },
            authorPhotoUrl = authorProfile.photoUrl,
            caption = caption,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            likesCount = 0L,
            commentsCount = 0L,
            createdAt = System.currentTimeMillis()
        )

        try {
            docRef.set(post.toMap()).await()
            // Increment postsCount on user profile
            val userRef = db.collection("users").document(uid)
            userRef.update("postsCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePost(postId: String) {
        val uid = requireUserId()
        val docRef = db.collection("posts").document(postId)
        try {
            docRef.delete().await()
            val userRef = db.collection("users").document(uid)
            userRef.update("postsCount", FieldValue.increment(-1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // ----------------------------------------------------
    // Likes
    // ----------------------------------------------------

    fun observePostLikes(postId: String): Flow<Set<String>> = callbackFlow {
        val likesRef = db.collection("posts").document(postId).collection("likes")
        val listener = likesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, likesRef.path)
                trySend(emptySet())
                return@addSnapshotListener
            }
            val userIds = snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
            trySend(userIds)
        }
        awaitClose { listener.remove() }
    }

    fun observeMyLikedPostIds(userId: String): Flow<Set<String>> = callbackFlow {
        val likedPostsRef = db.collection("users").document(userId).collection("likedPosts")
        val listener = likedPostsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, likedPostsRef.path)
                trySend(emptySet())
                return@addSnapshotListener
            }
            val ids = snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
            trySend(ids)
        }
        awaitClose { listener.remove() }
    }

    suspend fun toggleLike(postId: String, isLiked: Boolean) {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(uid)
        val userLikedRef = db.collection("users").document(uid).collection("likedPosts").document(postId)

        try {
            if (isLiked) {
                // Unlike
                likeRef.delete().await()
                userLikedRef.delete().await()
                postRef.update("likesCount", FieldValue.increment(-1)).await()
            } else {
                // Like
                val likeData = mapOf(
                    "userId" to uid,
                    "createdAt" to System.currentTimeMillis()
                )
                likeRef.set(likeData).await()
                userLikedRef.set(mapOf("createdAt" to System.currentTimeMillis())).await()
                postRef.update("likesCount", FieldValue.increment(1)).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            throw e
        }
    }

    // ----------------------------------------------------
    // Comments
    // ----------------------------------------------------

    fun observeComments(postId: String): Flow<List<PostComment>> = callbackFlow {
        val commentsRef = db.collection("posts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listener = commentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "posts/$postId/comments")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val comments = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(PostComment::class.java)?.let {
                    if (it.commentId.isBlank()) it.copy(commentId = doc.id) else it
                }
            } ?: emptyList()
            trySend(comments)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, text: String, authorProfile: UserProfile) {
        val uid = requireUserId()
        val commentId = UUID.randomUUID().toString()
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document(commentId)

        val comment = PostComment(
            commentId = commentId,
            postId = postId,
            authorId = uid,
            authorName = authorProfile.displayName.ifBlank { "FG Member" },
            authorPhotoUrl = authorProfile.photoUrl,
            text = text,
            createdAt = System.currentTimeMillis()
        )

        try {
            commentRef.set(comment.toMap()).await()
            postRef.update("commentsCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            throw e
        }
    }

    // ----------------------------------------------------
    // Stories
    // ----------------------------------------------------

    fun observeStories(): Flow<List<Story>> = callbackFlow {
        val now = System.currentTimeMillis()
        val collectionRef = db.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .limit(30)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "stories")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val stories = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Story::class.java)?.let {
                    if (it.storyId.isBlank()) it.copy(storyId = doc.id) else it
                }
            } ?: emptyList()
            trySend(stories.sortedByDescending { it.createdAt })
        }
        awaitClose { listener.remove() }
    }

    suspend fun createStory(mediaUrl: String, caption: String, authorProfile: UserProfile) {
        val uid = requireUserId()
        val storyId = UUID.randomUUID().toString()
        val docRef = db.collection("stories").document(storyId)
        val now = System.currentTimeMillis()

        val story = Story(
            storyId = storyId,
            authorId = uid,
            authorName = authorProfile.displayName.ifBlank { "FG Member" },
            authorPhotoUrl = authorProfile.photoUrl,
            mediaUrl = mediaUrl,
            caption = caption,
            createdAt = now,
            expiresAt = now + (24 * 60 * 60 * 1000L)
        )

        try {
            docRef.set(story.toMap()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // ----------------------------------------------------
    // Follows
    // ----------------------------------------------------

    fun observeFollowing(userId: String): Flow<List<String>> = callbackFlow {
        val collectionRef = db.collection("follows")
            .whereEqualTo("followerId", userId)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "follows")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val follows = snapshot?.toObjects(Follow::class.java) ?: emptyList()
            trySend(follows.map { it.followingId })
        }
        awaitClose { listener.remove() }
    }

    suspend fun toggleFollow(targetUserId: String, isFollowing: Boolean) {
        val uid = requireUserId()
        val followDocId = "${uid}_${targetUserId}"
        val followRef = db.collection("follows").document(followDocId)
        val myUserRef = db.collection("users").document(uid)
        val targetUserRef = db.collection("users").document(targetUserId)

        try {
            if (isFollowing) {
                followRef.delete().await()
                myUserRef.update("followingCount", FieldValue.increment(-1)).await()
                targetUserRef.update("followersCount", FieldValue.increment(-1)).await()
            } else {
                val follow = Follow(
                    followId = followDocId,
                    followerId = uid,
                    followingId = targetUserId,
                    createdAt = System.currentTimeMillis()
                )
                followRef.set(follow.toMap()).await()
                myUserRef.update("followingCount", FieldValue.increment(1)).await()
                targetUserRef.update("followersCount", FieldValue.increment(1)).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            throw e
        }
    }

    // ----------------------------------------------------
    // Direct Messaging & Conversations
    // ----------------------------------------------------

    fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collectionRef = db.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .limit(30)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "conversations")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val convos = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Conversation::class.java)?.let {
                    if (it.conversationId.isBlank()) it.copy(conversationId = doc.id) else it
                }
            } ?: emptyList()
            trySend(convos.sortedByDescending { it.updatedAt })
        }
        awaitClose { listener.remove() }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "conversations/$conversationId/messages")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.let {
                    if (it.messageId.isBlank()) it.copy(messageId = doc.id) else it
                }
            } ?: emptyList()
            trySend(messages)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateConversation(
        targetUser: UserProfile,
        currentUser: UserProfile
    ): String {
        val myUid = requireUserId()
        val targetUid = targetUser.userId
        val conversationId = if (myUid < targetUid) "${myUid}_${targetUid}" else "${targetUid}_${myUid}"
        val docRef = db.collection("conversations").document(conversationId)

        val existing = docRef.get().await()
        if (!existing.exists()) {
            val convo = Conversation(
                conversationId = conversationId,
                participantIds = listOf(myUid, targetUid),
                participantNames = mapOf(
                    myUid to currentUser.displayName.ifBlank { "You" },
                    targetUid to targetUser.displayName.ifBlank { "User" }
                ),
                participantPhotos = mapOf(
                    myUid to currentUser.photoUrl,
                    targetUid to targetUser.photoUrl
                ),
                lastMessage = "Started a conversation",
                lastMessageTimestamp = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(convo.toMap()).await()
        }
        return conversationId
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        mediaUrl: String = "",
        currentUser: UserProfile
    ) {
        val uid = requireUserId()
        val messageId = UUID.randomUUID().toString()
        val convoRef = db.collection("conversations").document(conversationId)
        val messageRef = convoRef.collection("messages").document(messageId)
        val now = System.currentTimeMillis()

        val msg = ChatMessage(
            messageId = messageId,
            conversationId = conversationId,
            senderId = uid,
            senderName = currentUser.displayName.ifBlank { "You" },
            senderPhotoUrl = currentUser.photoUrl,
            text = text,
            mediaUrl = mediaUrl,
            createdAt = now
        )

        try {
            messageRef.set(msg.toMap()).await()
            convoRef.update(
                mapOf(
                    "lastMessage" to (text.ifBlank { "Photo attachment" }),
                    "lastMessageTimestamp" to now,
                    "updatedAt" to now
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messageRef.path)
            throw e
        }
    }
}
