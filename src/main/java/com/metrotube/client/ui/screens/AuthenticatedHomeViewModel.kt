package com.metrotube.client.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.metrotube.client.data.Video
import com.metrotube.client.data.UserProfile
import com.metrotube.client.network.AuthenticatedNetworkUtils
import com.metrotube.client.extractor.AuthenticatedYouTubeExtractor
import com.metrotube.client.auth.AuthenticationManager

/**
 * Home view model with authentication support
 * Provides personalized content when user is logged in
 */
class AuthenticatedHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthenticationManager(application)
    private val networkUtils = AuthenticatedNetworkUtils(authManager)
    private val extractor = AuthenticatedYouTubeExtractor()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        checkAuthenticationStatus()
        loadHomeContent()
    }

    private fun checkAuthenticationStatus() {
        _isAuthenticated.value = authManager.isAuthenticated()

        if (authManager.isAuthenticated()) {
            _userProfile.value = UserProfile(
                name = authManager.getAccountName(),
                email = authManager.getAccountEmail(), 
                channelHandle = authManager.getChannelHandle(),
                isAuthenticated = true
            )
        }
    }

    /**
     * Load personalized home content (if authenticated) or public content
     */
    fun loadHomeContent() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null

            try {
                val response = if (authManager.isAuthenticated()) {
                    // Load personalized content using InnerTube API
                    networkUtils.getPersonalizedHome()
                } else {
                    // Load public content
                    networkUtils.getYouTubeHomePage()
                }

                if (response != null) {
                    val extractedVideos = if (authManager.isAuthenticated()) {
                        extractor.extractFromInnerTubeResponse(response)
                    } else {
                        extractor.extractHomepageVideos(response)
                    }
                    _videos.value = extractedVideos
                } else {
                    _error.value = "Failed to load content"
                }
            } catch (e: Exception) {
                _error.value = "Error: " + e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load user's personalized recommendations
     */
    fun loadRecommendations() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null

            try {
                val response = networkUtils.getRecommendations()

                if (response != null) {
                    val extractedVideos = if (authManager.isAuthenticated()) {
                        extractor.extractFromInnerTubeResponse(response)
                    } else {
                        extractor.extractHomepageVideos(response)
                    }
                    _videos.value = extractedVideos
                } else {
                    _error.value = "Failed to load recommendations"
                }
            } catch (e: Exception) {
                _error.value = "Error: " + e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load user's library (subscriptions, playlists, etc.)
     */
    fun loadUserLibrary() {
        if (!authManager.isAuthenticated()) {
            _error.value = "Login required for library access"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null

            try {
                val response = networkUtils.getUserLibrary()

                if (response != null) {
                    val extractedVideos = extractor.extractFromInnerTubeResponse(response)
                    _videos.value = extractedVideos
                } else {
                    _error.value = "Failed to load library"
                }
            } catch (e: Exception) {
                _error.value = "Error: " + e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Search with personalized results
     */
    fun searchVideos(query: String) {
        if (query.isBlank()) {
            loadHomeContent()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null

            try {
                val response = networkUtils.searchWithAuth(query)

                if (response != null) {
                    val extractedVideos = if (authManager.isAuthenticated()) {
                        extractor.extractSearchFromInnerTube(response)
                    } else {
                        extractor.extractSearchVideos(response)
                    }
                    _videos.value = extractedVideos
                } else {
                    _error.value = "Failed to search"
                }
            } catch (e: Exception) {
                _error.value = "Error: " + e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle tab selection with auth-aware content loading
     */
    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        when (tabIndex) {
            0 -> loadHomeContent()
            1 -> loadRecommendations()
            2 -> loadUserLibrary()
        }
    }

    /**
     * Like/unlike a video (authenticated users only)
     */
    fun toggleLike(videoId: String) {
        if (!authManager.isAuthenticated()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO: Implement like/unlike API call
                // This would use InnerTube API to like/unlike
            } catch (e: Exception) {
                _error.value = "Failed to update like status"
            }
        }
    }

    /**
     * Subscribe/unsubscribe from channel (authenticated users only)
     */
    fun toggleSubscription(channelId: String) {
        if (!authManager.isAuthenticated()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO: Implement subscribe/unsubscribe API call
                // This would use InnerTube API to manage subscriptions
            } catch (e: Exception) {
                _error.value = "Failed to update subscription"
            }
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        authManager.logout()
        _isAuthenticated.value = false
        _userProfile.value = null
        loadHomeContent() // Reload with public content
    }

    /**
     * Refresh authentication status
     */
    fun refreshAuth() {
        checkAuthenticationStatus()
        loadHomeContent()
    }
}