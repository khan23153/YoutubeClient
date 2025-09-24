package com.metrotube.client.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

/**
 * Authentication Manager following Metrolist approach
 * Handles WebView login, token extraction, and session management
 */
class AuthenticationManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "metrotube_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_VISITOR_DATA = "visitor_data"
        private const val KEY_DATA_SYNC_ID = "data_sync_id"
        private const val KEY_COOKIES = "youtube_cookies"
        private const val KEY_ACCOUNT_NAME = "account_name"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_CHANNEL_HANDLE = "channel_handle"
        private const val KEY_ADVANCED_TOKEN = "advanced_token"
        private const val TAG = "AuthManager"
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_AUTHENTICATED, false)
    }

    /**
     * Set authentication status
     */
    fun setAuthenticated(authenticated: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply()
        Log.d(TAG, "Authentication status set to: \$authenticated")
    }

    /**
     * Save visitor data extracted from YouTube
     */
    fun saveVisitorData(visitorData: String) {
        sharedPreferences.edit().putString(KEY_VISITOR_DATA, visitorData).apply()
        Log.d(TAG, "Visitor data saved: \$visitorData")
    }

    /**
     * Get visitor data
     */
    fun getVisitorData(): String? {
        return sharedPreferences.getString(KEY_VISITOR_DATA, null)
    }

    /**
     * Save data sync ID extracted from YouTube
     */
    fun saveDataSyncId(dataSyncId: String) {
        sharedPreferences.edit().putString(KEY_DATA_SYNC_ID, dataSyncId).apply()
        Log.d(TAG, "DataSync ID saved: \$dataSyncId")
    }

    /**
     * Get data sync ID
     */
    fun getDataSyncId(): String? {
        return sharedPreferences.getString(KEY_DATA_SYNC_ID, null)
    }

    /**
     * Save cookies from WebView
     */
    fun saveCookies(cookies: String) {
        sharedPreferences.edit().putString(KEY_COOKIES, cookies).apply()
        Log.d(TAG, "Cookies saved, length: \${cookies.length}")
    }

    /**
     * Get cookies for API requests
     */
    fun getCookies(): String? {
        return sharedPreferences.getString(KEY_COOKIES, null)
    }

    /**
     * Save account information
     */
    fun saveAccountInfo(name: String?, email: String?, channelHandle: String?) {
        val editor = sharedPreferences.edit()
        name?.let { editor.putString(KEY_ACCOUNT_NAME, it) }
        email?.let { editor.putString(KEY_ACCOUNT_EMAIL, it) }
        channelHandle?.let { editor.putString(KEY_CHANNEL_HANDLE, it) }
        editor.apply()
        Log.d(TAG, "Account info saved - Name: \$name, Email: \$email")
    }

    /**
     * Get account name
     */
    fun getAccountName(): String? {
        return sharedPreferences.getString(KEY_ACCOUNT_NAME, null)
    }

    /**
     * Get account email
     */
    fun getAccountEmail(): String? {
        return sharedPreferences.getString(KEY_ACCOUNT_EMAIL, null)
    }

    /**
     * Get channel handle
     */
    fun getChannelHandle(): String? {
        return sharedPreferences.getString(KEY_CHANNEL_HANDLE, null)
    }

    /**
     * Save advanced token for power users
     */
    fun saveAdvancedToken(token: String) {
        sharedPreferences.edit().putString(KEY_ADVANCED_TOKEN, token).apply()
        setAuthenticated(true)
        Log.d(TAG, "Advanced token saved")
    }

    /**
     * Get advanced token
     */
    fun getAdvancedToken(): String? {
        return sharedPreferences.getString(KEY_ADVANCED_TOKEN, null)
    }

    /**
     * Check if using advanced token authentication
     */
    fun isUsingAdvancedToken(): Boolean {
        return getAdvancedToken() != null
    }

    /**
     * Logout - clear all authentication data
     */
    fun logout() {
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "User logged out, all data cleared")
    }

    /**
     * Get authentication headers for InnerTube API requests
     */
    fun getAuthHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        // Add cookies
        getCookies()?.let { cookies ->
            headers["Cookie"] = cookies
        }

        // Add visitor data
        getVisitorData()?.let { visitorData ->
            headers["X-Goog-Visitor-Id"] = visitorData
        }

        // Add standard headers
        headers["User-Agent"] = getUserAgent()
        headers["Accept-Language"] = "en-US,en;q=0.9"
        headers["Content-Type"] = "application/json"

        return headers
    }

    private fun getUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    /**
     * Validate if current authentication is still valid
     */
    fun validateAuth(): Boolean {
        return when {
            isUsingAdvancedToken() -> getAdvancedToken() != null
            else -> getCookies() != null && getVisitorData() != null
        }
    }
}