package com.metrotube.client.data

data class Video(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String? = null,
    val thumbnailUrl: String,
    val duration: String? = null,
    val viewCount: String? = null,
    val publishedTime: String? = null,
    val description: String? = null,
    val videoUrl: String? = null,
    val streamUrls: List<StreamUrl> = emptyList(),
    val isLiked: Boolean = false, // For authenticated users
    val isDisliked: Boolean = false, // For authenticated users
    val isInWatchLater: Boolean = false // For authenticated users
)

data class StreamUrl(
    val quality: String,
    val url: String,
    val format: String = "mp4",
    val itag: Int? = null
)

data class Channel(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val subscriberCount: String? = null,
    val verified: Boolean = false,
    val isSubscribed: Boolean = false // For authenticated users
)

data class Playlist(
    val id: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val videoCount: Int = 0,
    val videos: List<Video> = emptyList(),
    val isOwned: Boolean = false, // For authenticated users
    val privacy: PlaylistPrivacy = PlaylistPrivacy.PUBLIC
)

enum class PlaylistPrivacy {
    PUBLIC, UNLISTED, PRIVATE
}

data class UserProfile(
    val name: String?,
    val email: String?,
    val channelHandle: String?,
    val avatarUrl: String? = null,
    val isAuthenticated: Boolean = false
)

data class AuthenticationData(
    val visitorData: String?,
    val dataSyncId: String?,
    val cookies: String?,
    val advancedToken: String? = null
)

// InnerTube API response models
data class InnerTubeResponse(
    val contents: Any?,
    val header: Any?,
    val microformat: Any?
)

data class BrowseResponse(
    val contents: BrowseContents?,
    val header: Any?,
    val metadata: Any?
)

data class BrowseContents(
    val sectionListRenderer: SectionListRenderer?
)

data class SectionListRenderer(
    val contents: List<Any>?
)