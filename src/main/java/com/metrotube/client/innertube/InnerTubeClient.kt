package com.metrotube.client.innertube

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.metrotube.client.auth.AuthenticationManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * InnerTube API Client for YouTube Music
 * Based on Metrolist's approach using YouTube's internal API
 */
class InnerTubeClient(private val authManager: AuthenticationManager) {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response
        }
        .build()

    private val gson = Gson()

    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"
        private const val WEB_REMIX_CLIENT_NAME = "WEB_REMIX"
        private const val WEB_REMIX_CLIENT_VERSION = "1.20250310.01.00"
        private const val WEB_REMIX_CLIENT_ID = "67"
    }

    /**
     * YouTube client configuration (WEB_REMIX like Metrolist)
     */
    private fun getClientConfig(): JsonObject {
        val context = JsonObject()
        val client = JsonObject()

        client.addProperty("clientName", WEB_REMIX_CLIENT_NAME)
        client.addProperty("clientVersion", WEB_REMIX_CLIENT_VERSION)
        client.addProperty("clientId", WEB_REMIX_CLIENT_ID)
        client.addProperty("userAgent", getUserAgent())

        // Add visitor data if available
        authManager.getVisitorData()?.let { visitorData ->
            client.addProperty("visitorData", visitorData)
        }

        context.add("client", client)
        return context
    }

    /**
     * Get home page content (personalized)
     */
    fun getHome(): String? {
        val payload = JsonObject()
        payload.add("context", getClientConfig())

        return makeRequest("browse", payload.toString())
    }

    /**
     * Search for content
     */
    fun search(query: String): String? {
        val payload = JsonObject()
        payload.add("context", getClientConfig())
        payload.addProperty("query", query)

        return makeRequest("search", payload.toString())
    }

    /**
     * Get personalized recommendations
     */
    fun getRecommendations(): String? {
        val payload = JsonObject()
        payload.add("context", getClientConfig())

        // Add continuation for recommendations
        val browseEndpoint = JsonObject()
        browseEndpoint.addProperty("browseId", "FEmusic_home")
        payload.add("browseEndpoint", browseEndpoint)

        return makeRequest("browse", payload.toString())
    }

    /**
     * Get user's library content
     */
    fun getLibrary(): String? {
        val payload = JsonObject()
        payload.add("context", getClientConfig())

        val browseEndpoint = JsonObject()
        browseEndpoint.addProperty("browseId", "FEmusic_library_landing")
        payload.add("browseEndpoint", browseEndpoint)

        return makeRequest("browse", payload.toString())
    }

    /**
     * Get playlist content
     */
    fun getPlaylist(playlistId: String): String? {
        val payload = JsonObject()
        payload.add("context", getClientConfig())
        payload.addProperty("browseId", "VL\$playlistId")

        return makeRequest("browse", payload.toString())
    }

    /**
     * Get video/song details
     */
    fun getVideoDetails(videoId: String): String? {
        val payload = JsonObject()
        payload.add("context", getClientConfig())
        payload.addProperty("videoId", videoId)

        return makeRequest("player", payload.toString())
    }

    /**
     * Get next/related content
     */
    fun getNext(videoId: String): String? {
        val payload = JsonObject()
        payload.add("context", getClientConfig())
        payload.addProperty("videoId", videoId)

        return makeRequest("next", payload.toString())
    }

    /**
     * Make authenticated request to InnerTube API
     */
    private fun makeRequest(endpoint: String, jsonPayload: String): String? {
        return try {
            val url = "\$BASE_URL/\$endpoint?key=AIzaSyC9XL3ZjWddXya6X74dJoCTL-WGJF9ZnMlY"

            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

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
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}