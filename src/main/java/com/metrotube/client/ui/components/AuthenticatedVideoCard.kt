package com.metrotube.client.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.metrotube.client.data.Video

@Composable
fun AuthenticatedVideoCard(
    video: Video,
    isAuthenticated: Boolean,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)? = null,
    onSubscribeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Thumbnail with enhanced overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl.ifEmpty { "https://via.placeholder.com/320x180" },
                    contentDescription = video.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Duration overlay
                video.duration?.let { duration ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = duration,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Authentication status indicators
                if (isAuthenticated) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        if (video.isLiked) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Liked",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (video.isInWatchLater) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.WatchLater,
                                contentDescription = "Watch Later",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Video info with enhanced actions
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = video.channelName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row {
                            video.viewCount?.let { views ->
                                Text(
                                    text = views,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            video.publishedTime?.let { time ->
                                Text(
                                    text = " • " + time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Action menu
                    Box {
                        IconButton(
                            onClick = { showMenu = !showMenu }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More actions"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isAuthenticated && onLikeClick != null) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(if (video.isLiked) "Unlike" else "Like") 
                                    },
                                    onClick = { 
                                        onLikeClick()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (video.isLiked) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }

                            if (isAuthenticated) {
                                DropdownMenuItem(
                                    text = { Text("Add to Watch Later") },
                                    onClick = { showMenu = false },
                                    leadingIcon = {
                                        Icon(Icons.Default.WatchLater, contentDescription = null)
                                    }
                                )

                                if (onSubscribeClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Subscribe") },
                                        onClick = { 
                                            onSubscribeClick()
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        }
                                    )
                                }
                            }

                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = { showMenu = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}