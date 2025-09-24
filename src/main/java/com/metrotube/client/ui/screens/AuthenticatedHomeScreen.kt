package com.metrotube.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.metrotube.client.ui.components.AuthenticatedVideoCard
import com.metrotube.client.ui.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedHomeScreen(
    viewModel: AuthenticatedHomeViewModel
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Enhanced Top App Bar with user info
        TopAppBar(
            title = { 
                Text(
                    if (isAuthenticated) "MetroTube (Authenticated)" 
                    else "MetroTube"
                )
            },
            actions = {
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }

                if (isAuthenticated && userProfile != null) {
                    // User avatar/menu
                    Box {
                        IconButton(onClick = { showUserMenu = !showUserMenu }) {
                            AsyncImage(
                                model = userProfile!!.avatarUrl ?: "",
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                fallback = {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "User",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(userProfile!!.name ?: "User")
                                        Text(
                                            userProfile!!.email ?: "",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = { showUserMenu = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { showUserMenu = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = { 
                                    viewModel.logout()
                                    showUserMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                                }
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { viewModel.refreshAuth() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        )

        // Authentication status indicator
        if (!isAuthenticated) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Using public mode - Login for personalized content",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Search Bar
        if (isSearchActive) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { 
                    viewModel.searchVideos(searchQuery)
                    isSearchActive = false
                },
                onActiveChange = { isSearchActive = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {}
        }

        // Enhanced Tab Row with auth-aware tabs
        TabRow(
            selectedTabIndex = selectedTab
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.setSelectedTab(0) },
                text = { Text(if (isAuthenticated) "For You" else "Home") },
                icon = { Icon(Icons.Default.Home, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.setSelectedTab(1) },
                text = { Text("Discover") },
                icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { viewModel.setSelectedTab(2) },
                text = { Text(if (isAuthenticated) "Library" else "Trending") },
                icon = { 
                    Icon(
                        if (isAuthenticated) Icons.Default.LibraryMusic else Icons.Default.Subscriptions,
                        contentDescription = null
                    )
                },
                enabled = !isAuthenticated || selectedTab == 2 // Disable if not authenticated, unless already selected
            )
        }

        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadHomeContent() }) {
                            Text("Retry")
                        }
                    }
                }
                videos.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No videos found")
                        if (!isAuthenticated) {
                            Text(
                                "Login for personalized recommendations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos) { video ->
                            AuthenticatedVideoCard(
                                video = video,
                                isAuthenticated = isAuthenticated,
                                onClick = { 
                                    // TODO: Navigate to video player
                                },
                                onLikeClick = if (isAuthenticated) {
                                    { viewModel.toggleLike(video.id) }
                                } else null,
                                onSubscribeClick = if (isAuthenticated) {
                                    { viewModel.toggleSubscription(video.channelId ?: "") }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}