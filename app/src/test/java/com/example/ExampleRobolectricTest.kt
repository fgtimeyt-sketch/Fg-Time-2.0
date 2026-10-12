package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Post
import com.example.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `launch MainActivity`() {
        Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FG TIME", appName)
    }

    @Test
    fun `verify user profile and post data models`() {
        val profile = UserProfile(
            userId = "user_123",
            displayName = "Creator One",
            username = "creator1",
            bio = "Building on FG TIME",
            photoUrl = "https://example.com/avatar.jpg"
        )
        assertEquals("user_123", profile.userId)
        assertEquals("creator1", profile.username)

        val post = Post(
            postId = "post_abc",
            authorId = profile.userId,
            authorName = profile.displayName,
            authorUsername = profile.username,
            caption = "Growing ideas 🌱",
            mediaUrl = "data:image/jpeg;base64,abc"
        )
        assertEquals("post_abc", post.postId)
        assertEquals("Growing ideas 🌱", post.caption)
        assertNotNull(post.toMap()["createdAt"])
    }
}
