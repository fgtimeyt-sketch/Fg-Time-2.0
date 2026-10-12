package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.FgTimeRepository
import com.example.model.Conversation
import com.example.model.UserProfile
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.SignInScreen
import com.example.ui.chat.ChatThreadScreen
import com.example.ui.chat.DirectMessagesScreen
import com.example.ui.explore.ExploreScreen
import com.example.ui.feed.FeedViewModel
import com.example.ui.feed.HomeScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.reels.ReelsScreen
import com.example.ui.theme.FgDarkGreen
import com.example.ui.theme.FgPrimaryGreen
import com.example.ui.theme.FgSoftGreen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_explore"),
    REELS("Reels", Icons.Filled.PlayCircleOutline, Icons.Outlined.PlayCircleOutline, "tab_reels"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@Composable
fun FgTimeApp() {
    val context = LocalContext.current
    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
        try {
            val options = com.google.firebase.FirebaseOptions.Builder()
                .setApplicationId("1:455868092391:android:dcd190d84dc93436a1b742")
                .setApiKey("AIzaSyCGaaIQ5zP964WPLRxIjbl_axwZOFJZzm0")
                .setProjectId("gen-lang-client-0188920817")
                .setStorageBucket("gen-lang-client-0188920817.firebasestorage.app")
                .setGcmSenderId("455868092391")
                .build()
            com.google.firebase.FirebaseApp.initializeApp(context.applicationContext, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val databaseId = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        "ai-studio-android-fgtime-d265212e-e44f-4a28-bea0-583c93ab7511"
    }

    val db = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
    val repository = remember(db) { FgTimeRepository(db) }

    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(repository) }
        }
    )

    val currentUser by authViewModel.currentUser.collectAsState()

    // MANDATORY AUTH GATE: Never instantiate feature ViewModels while currentUser == null
    if (currentUser == null) {
        SignInScreen(authViewModel = authViewModel)
    } else {
        val user = currentUser!!
        FgTimeMainScaffold(
            currentUserId = user.uid,
            repository = repository,
            authViewModel = authViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FgTimeMainScaffold(
    currentUserId: String,
    repository: FgTimeRepository,
    authViewModel: AuthViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var inDirectMessages by remember { mutableStateOf(false) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    val myProfile by repository.observeUserProfile(currentUserId).collectAsState(initial = null)

    // Keyed ViewModel for feed & stories
    val feedViewModel: FeedViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer { FeedViewModel(repository, currentUserId) }
        }
    )

    // Handle back button when inside sub-screens
    if (activeConversation != null) {
        BackHandler { activeConversation = null }
        ChatThreadScreen(
            conversation = activeConversation!!,
            repository = repository,
            currentUserId = currentUserId,
            onBack = { activeConversation = null }
        )
        return
    }

    if (inDirectMessages) {
        BackHandler { inDirectMessages = false }
        DirectMessagesScreen(
            repository = repository,
            currentUserId = currentUserId,
            onBack = { inDirectMessages = false },
            onSelectConversation = { convo -> activeConversation = convo }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fg_leaf),
                            contentDescription = "FG Logo",
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FG TIME",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = FgDarkGreen,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { inDirectMessages = true },
                        modifier = Modifier.testTag("direct_messages_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Direct Messages",
                            tint = FgDarkGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FgDarkGreen,
                            selectedTextColor = FgDarkGreen,
                            indicatorColor = FgSoftGreen,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        feedViewModel = feedViewModel,
                        onNavigateToChat = { inDirectMessages = true }
                    )
                }
                MainTab.EXPLORE -> {
                    ExploreScreen(
                        repository = repository,
                        currentUserId = currentUserId,
                        onStartChat = { targetUser ->
                            coroutineScope.launch {
                                val current = myProfile ?: UserProfile(userId = currentUserId, displayName = "Me")
                                val convoId = repository.getOrCreateConversation(targetUser, current)
                                activeConversation = Conversation(
                                    conversationId = convoId,
                                    participantIds = listOf(currentUserId, targetUser.userId),
                                    participantNames = mapOf(
                                        currentUserId to current.displayName,
                                        targetUser.userId to targetUser.displayName
                                    ),
                                    participantPhotos = mapOf(
                                        currentUserId to current.photoUrl,
                                        targetUser.userId to targetUser.photoUrl
                                    )
                                )
                            }
                        }
                    )
                }
                MainTab.REELS -> {
                    ReelsScreen(
                        repository = repository,
                        currentUserId = currentUserId,
                        onOpenComments = { post -> feedViewModel.openComments(post) }
                    )
                }
                MainTab.PROFILE -> {
                    ProfileScreen(
                        repository = repository,
                        currentUserId = currentUserId,
                        onSignOut = { authViewModel.signOut() }
                    )
                }
            }
        }
    }
}
