package com.metrotube.client.network

import com.metrotube.client.auth.AuthenticationManager
import com.metrotube.client.innertube.InnerTubeClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Enhanced Network utilities with authentication support
 * Uses both traditional scraping and InnerTube API calls
 */
class AuthenticatedNetworkUtils(private val authManager: AuthenticationManager) {

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val innerTubeClient = InnerTubeClient(authManager)

    /**
     * Get personalized home page using InnerTube API
     */
    fun getPersonalizedHome(): String? {
        return if (authManager.isAuthenticated()) {
            innerTubeClient.getHome()
        } else {
            // Fallback to public scraping
            getYouTubeHomePage()
        }
    }

    /**
     * Search with personalized results
     */
    fun searchWithAuth(query: String): String? {
        return if (authManager.isAuthenticated()) {
            innerTubeClient.search(query)
        } else {
            // Fallback to public search
            searchYouTube(query)
        }
    }

    /**
     * Get user's library/subscriptions
     */
    fun getUserLibrary(): String? {
        return if (authManager.isAuthenticated()) {
            innerTubeClient.getLibrary()
        } else {
            null // Not available without authentication
        }
    }

    /**
     * Get personalized recommendations
     */
    fun getRecommendations(): String? {
        return if (authManager.isAuthenticated()) {
            innerTubeClient.getRecommendations()
        } else {
            getYouTubeTrending() // Fallback to trending
        }
    }

    /**
     * Get playlist with authentication
     */
    fun getPlaylist(playlistId: String): String? {
        return if (authManager.isAuthenticated()) {
            innerTubeClient.getPlaylist(playlistId)
        } else {
            getPublicPlaylist(playlistId)
        }
    }

    /**
     * Get video details with enhanced info
     */
    fun getVideoDetails(videoId: String): String? {
        return if (authManager.isAuthenticated()) {
            innerTubeClient.getVideoDetails(videoId)
        } else {
            getPublicVideoPage(videoId)
        }
    }

    // Public methods (fallback for non-authenticated users)

    fun getYouTubeHomePage(): String? {
        return makePublicRequest("https://www.youtube.com/")
    }

    fun getYouTubeTrending(): String? {
        return makePublicRequest("https://www.youtube.com/feed/trending")
    }

    fun searchYouTube(query: String): String? {
        val searchUrl = "https://www.youtube.com/results?search_query=" + query
        return makePublicRequest(searchUrl)
    }

    fun getPublicVideoPage(videoId: String): String? {
        val videoUrl = "https://www.youtube.com/watch?v=" + videoId
        return makePublicRequest(videoUrl)
    }

    fun getChannelPage(channelId: String): String? {
        val channelUrl = "https://www.youtube.com/channel/" + channelId
        return makePublicRequest(channelUrl)
    }

    fun getPublicPlaylist(playlistId: String): String? {
        val playlistUrl = "https://www.youtube.com/playlist?list=" + playlistId
        return makePublicRequest(playlistUrl)
    }

    /**
     * Make authenticated request with cookies and headers
     */
    private fun makeAuthenticatedRequest(url: String): String? {
        return try {
            val requestBuilder = Request.Builder()
                .url(url)

            // Add authentication headers
            authManager.getAuthHeaders().forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Make public request (no authentication)
     */
    private fun makePublicRequest(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", getUserAgent())
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}