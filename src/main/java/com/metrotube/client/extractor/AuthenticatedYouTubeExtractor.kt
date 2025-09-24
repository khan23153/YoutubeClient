package com.metrotube.client.extractor

import com.metrotube.client.data.Video
import com.metrotube.client.data.StreamUrl
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern

/**
 * Enhanced YouTube extractor with InnerTube API response support
 * Handles both HTML scraping and structured JSON responses
 */
class AuthenticatedYouTubeExtractor {

    private val gson = Gson()

    /**
     * Extract videos from InnerTube API response (JSON)
     */
    fun extractFromInnerTubeResponse(jsonResponse: String): List<Video> {
        return try {
            val jsonElement = JsonParser.parseString(jsonResponse)
            val videos = mutableListOf<Video>()

            // Parse InnerTube response structure
            if (jsonElement.isJsonObject) {
                val responseObj = jsonElement.asJsonObject

                // Look for contents in various possible locations
                val contents = responseObj.get("contents")
                if (contents != null && contents.isJsonObject) {
                    extractVideosFromContents(contents.asJsonObject, videos)
                }

                // Also check for continuation items
                val continuationContents = responseObj.get("continuationContents")
                if (continuationContents != null && continuationContents.isJsonObject) {
                    extractVideosFromContents(continuationContents.asJsonObject, videos)
                }
            }

            videos.distinctBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to mock data for development
            getMockAuthenticatedVideos()
        }
    }

    /**
     * Extract search results from InnerTube search response
     */
    fun extractSearchFromInnerTube(jsonResponse: String): List<Video> {
        return try {
            val jsonElement = JsonParser.parseString(jsonResponse)
            val videos = mutableListOf<Video>()

            if (jsonElement.isJsonObject) {
                val responseObj = jsonElement.asJsonObject
                val contents = responseObj.get("contents")

                if (contents != null && contents.isJsonObject) {
                    // Navigate through search response structure
                    val searchResults = contents.asJsonObject
                        .get("twoColumnSearchResultsRenderer")?.asJsonObject
                        ?.get("primaryContents")?.asJsonObject
                        ?.get("sectionListRenderer")?.asJsonObject
                        ?.get("contents")?.asJsonArray

                    searchResults?.forEach { item ->
                        if (item.isJsonObject) {
                            extractVideoFromSearchItem(item.asJsonObject, videos)
                        }
                    }
                }
            }

            videos.distinctBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            getMockSearchVideos()
        }
    }

    private fun extractVideosFromContents(contents: JsonObject, videos: MutableList<Video>) {
        // This is a simplified extraction - real implementation would be more complex
        // Look for video renderers in the response
        traverseJsonForVideoRenderers(contents, videos)
    }

    private fun traverseJsonForVideoRenderers(json: JsonObject, videos: MutableList<Video>) {
        json.entrySet().forEach { entry ->
            when {
                entry.key.contains("videoRenderer") && entry.value.isJsonObject -> {
                    extractVideoFromRenderer(entry.value.asJsonObject)?.let { videos.add(it) }
                }
                entry.key.contains("compactVideoRenderer") && entry.value.isJsonObject -> {
                    extractVideoFromRenderer(entry.value.asJsonObject)?.let { videos.add(it) }
                }
                entry.value.isJsonObject -> {
                    traverseJsonForVideoRenderers(entry.value.asJsonObject, videos)
                }
                entry.value.isJsonArray -> {
                    entry.value.asJsonArray.forEach { item ->
                        if (item.isJsonObject) {
                            traverseJsonForVideoRenderers(item.asJsonObject, videos)
                        }
                    }
                }
            }
        }
    }

    private fun extractVideoFromRenderer(renderer: JsonObject): Video? {
        return try {
            val videoId = renderer.get("videoId")?.asString ?: return null

            val title = renderer.get("title")?.asJsonObject
                ?.get("runs")?.asJsonArray?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: "Unknown Title"

            val ownerText = renderer.get("ownerText")?.asJsonObject
                ?.get("runs")?.asJsonArray?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: "Unknown Channel"

            val thumbnails = renderer.get("thumbnail")?.asJsonObject
                ?.get("thumbnails")?.asJsonArray
            val thumbnailUrl = thumbnails?.get(thumbnails.size() - 1)?.asJsonObject
                ?.get("url")?.asString ?: ""

            val lengthText = renderer.get("lengthText")?.asJsonObject
                ?.get("simpleText")?.asString

            val viewCountText = renderer.get("viewCountText")?.asJsonObject
                ?.get("simpleText")?.asString

            val publishedTimeText = renderer.get("publishedTimeText")?.asJsonObject
                ?.get("simpleText")?.asString

            Video(
                id = videoId,
                title = title,
                channelName = ownerText,
                thumbnailUrl = thumbnailUrl,
                duration = lengthText,
                viewCount = viewCountText,
                publishedTime = publishedTimeText,
                videoUrl = "https://www.youtube.com/watch?v=" + videoId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractVideoFromSearchItem(item: JsonObject, videos: MutableList<Video>) {
        // Handle search-specific video renderer structure
        val videoRenderer = item.get("videoRenderer")
        if (videoRenderer != null && videoRenderer.isJsonObject) {
            extractVideoFromRenderer(videoRenderer.asJsonObject)?.let { videos.add(it) }
        }
    }

    // Fallback HTML extraction methods (same as before)
    fun extractHomepageVideos(html: String): List<Video> {
        return try {
            val document: Document = Jsoup.parse(html)
            val videos = mutableListOf<Video>()

            val videoElements = document.select(
                "div[class*='ytd-rich-item-renderer'], " +
                "div[class*='ytd-video-renderer'], " +
                "div[class*='ytd-compact-video-renderer']"
            )

            for (element in videoElements) {
                extractVideoFromElement(element)?.let { video ->
                    videos.add(video)
                }
            }

            videos.distinctBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            getMockAuthenticatedVideos()
        }
    }

    fun extractSearchVideos(html: String): List<Video> {
        return try {
            val document: Document = Jsoup.parse(html)
            val videos = mutableListOf<Video>()

            val videoElements = document.select("div[class*='ytd-video-renderer']")

            for (element in videoElements) {
                extractVideoFromElement(element)?.let { video ->
                    videos.add(video)
                }
            }

            if (videos.isEmpty()) {
                getMockSearchVideos()
            } else {
                videos
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getMockSearchVideos()
        }
    }

    private fun extractVideoFromElement(element: Element): Video? {
        return try {
            val linkElement = element.selectFirst("a[href*='/watch?v=']")
            val href = linkElement?.attr("href") ?: return null
            val videoId = extractVideoId(href) ?: return null

            val titleElement = element.selectFirst("a[title], h3 a, #video-title")
            val title = titleElement?.text()?.trim() ?: "Unknown Title"

            val channelElement = element.selectFirst("a[class*='channel'], .ytd-channel-name a")
            val channelName = channelElement?.text()?.trim() ?: "Unknown Channel"

            val thumbnailElement = element.selectFirst("img")
            var thumbnailUrl = thumbnailElement?.attr("src") ?: thumbnailElement?.attr("data-src") ?: ""

            if (thumbnailUrl.startsWith("//")) {
                thumbnailUrl = "https:" + thumbnailUrl
            }

            val durationElement = element.selectFirst(".ytd-thumbnail-overlay-time-status-renderer")
            val duration = durationElement?.text()?.trim()

            val metadataElements = element.select(".ytd-video-meta-block span")
            var viewCount: String? = null
            var publishedTime: String? = null

            for (meta in metadataElements) {
                val text = meta.text()
                if (text.contains("views") || text.contains("view")) {
                    viewCount = text
                } else if (text.contains("ago")) {
                    publishedTime = text
                }
            }

            Video(
                id = videoId,
                title = title,
                channelName = channelName,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                viewCount = viewCount,
                publishedTime = publishedTime,
                videoUrl = "https://www.youtube.com/watch?v=" + videoId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Pattern.compile("(?:youtube\.com/watch\?v=|/watch\?v=|youtu\.be/)([a-zA-Z0-9_-]{11})"),
            Pattern.compile("/watch\?v=([a-zA-Z0-9_-]{11})"),
            Pattern.compile("v=([a-zA-Z0-9_-]{11})")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    private fun getMockAuthenticatedVideos(): List<Video> {
        return listOf(
            Video(
                id = "auth1",
                title = "Personalized Recommendation 1",
                channelName = "Your Subscribed Channel",
                thumbnailUrl = "https://i.ytimg.com/vi/auth1/mqdefault.jpg",
                duration = "4:32",
                viewCount = "1.5M views",
                publishedTime = "1 day ago",
                videoUrl = "https://www.youtube.com/watch?v=auth1",
                isLiked = true
            ),
            Video(
                id = "auth2", 
                title = "From Your Music Library",
                channelName = "Favorite Artist",
                thumbnailUrl = "https://i.ytimg.com/vi/auth2/mqdefault.jpg",
                duration = "3:45",
                viewCount = "2.1M views",
                publishedTime = "3 days ago",
                videoUrl = "https://www.youtube.com/watch?v=auth2",
                isInWatchLater = true
            )
        )
    }

    private fun getMockSearchVideos(): List<Video> {
        return listOf(
            Video(
                id = "search_auth1",
                title = "Personalized Search Result",
                channelName = "Recommended Channel",
                thumbnailUrl = "https://i.ytimg.com/vi/search_auth1/mqdefault.jpg",
                duration = "5:20",
                viewCount = "800K views",
                publishedTime = "2 days ago",
                videoUrl = "https://www.youtube.com/watch?v=search_auth1"
            )
        )
    }
}