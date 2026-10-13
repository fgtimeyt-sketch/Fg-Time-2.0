package com.example.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FgTimeRepository
import com.example.model.Post
import com.example.model.PostComment
import com.example.model.Story
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: FgTimeRepository,
    val currentUserId: String
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _activePostForComments = MutableStateFlow<Post?>(null)
    val activePostForComments: StateFlow<Post?> = _activePostForComments.asStateFlow()

    private val _postComments = MutableStateFlow<List<PostComment>>(emptyList())
    val postComments: StateFlow<List<PostComment>> = _postComments.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    init {
        loadUserProfile()
        loadFeed()
        loadStories()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            repository.observeUserProfile(currentUserId).collect { profile ->
                _userProfile.value = profile
            }
        }
    }

    private fun loadFeed() {
        viewModelScope.launch {
            combine(
                repository.observeFeedPosts(),
                repository.observeMyLikedPostIds(currentUserId)
            ) { allPosts, likedPostIds ->
                allPosts.map { post ->
                    post.copy(isLikedByMe = likedPostIds.contains(post.postId))
                }
            }.collect { postsWithLikes ->
                _posts.value = postsWithLikes
            }
        }
    }

    private fun loadStories() {
        viewModelScope.launch {
            repository.observeStories().collect { activeStories ->
                _stories.value = activeStories
            }
        }
    }

    fun toggleLike(post: Post, currentlyLiked: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleLike(post.postId, currentlyLiked)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun openComments(post: Post) {
        _activePostForComments.value = post
        viewModelScope.launch {
            repository.observeComments(post.postId).collect { comments ->
                _postComments.value = comments
            }
        }
    }

    fun closeComments() {
        _activePostForComments.value = null
        _postComments.value = emptyList()
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val profile = _userProfile.value ?: UserProfile(userId = currentUserId, displayName = "Me")
                repository.addComment(postId, text.trim(), profile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createPost(caption: String, mediaUrl: String, onComplete: () -> Unit) {
        if (caption.isBlank() && mediaUrl.isBlank()) return
        _isPosting.value = true
        viewModelScope.launch {
            try {
                val profile = _userProfile.value ?: UserProfile(userId = currentUserId, displayName = "Me")
                repository.createPost(caption = caption.trim(), mediaUrl = mediaUrl, authorProfile = profile)
                _isPosting.value = false
                onComplete()
            } catch (e: Exception) {
                _isPosting.value = false
                e.printStackTrace()
            }
        }
    }

    fun createStory(mediaUrl: String, caption: String, onComplete: () -> Unit) {
        if (mediaUrl.isBlank()) return
        viewModelScope.launch {
            try {
                val profile = _userProfile.value ?: UserProfile(userId = currentUserId, displayName = "Me")
                repository.createStory(mediaUrl, caption.trim(), profile)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                repository.deletePost(postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
