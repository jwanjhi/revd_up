package com.example.revd_up.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import com.example.revd_up.domain.model.Post


@Serializable
data class Comment(
    val id: String? = null,
    val userId: String,
    val text: String,
    val createdAt: String? = null
)

@Serializable
data class LikeRequest(val userId: String)

@Serializable
data class CommentRequest(val userId: String, val text: String)


open class RevdUpPostService(private val api: RevdUpService) {

    private val TAG = "RevdUpPostService"

    /**
     * Create a new post.
     */
    suspend fun createPost(caption: String, mediaUrl: String?, tags: List<String>?): Post? {
        val body = mapOf(
            "caption" to caption,
            "mediaUrl" to (mediaUrl ?: ""),
            "tags" to (tags ?: emptyList<String>()),
            "mediaType" to if (mediaUrl?.endsWith(".mp4") == true) "video" else "image"
        )
        return try {
            val response = api.doPost("posts", body)
            if (response.status.value in 200..299) {
                val postWrapper = response.body<Map<String, Post>>()
                postWrapper["post"]
            } else {
                Log.e(TAG, "CreatePost failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "CreatePost exception: ${e.message}", e)
            null
        }
    }

    /**
     * Get all posts (feed).
     */
    suspend fun getAllPosts(): List<Post> {
        return try {
            val response = api.doGet("posts")
            if (response.status.value in 200..299) {
                response.body()
            } else {
                Log.e(TAG, "GetAllPosts failed: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "GetAllPosts exception: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get post by ID.
     */
    suspend fun getPostById(id: String): Post? {
        return try {
            val response = api.doGet("posts", id)
            if (response.status.value in 200..299) {
                response.body()
            } else {
                Log.e(TAG, "GetPostById failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "GetPostById exception: ${e.message}", e)
            null
        }
    }

    /**
     * Update post.
     */
    suspend fun updatePost(id: String, caption: String?, tags: List<String>?, mediaUrl: String?, mediaType: String?): Boolean {
        val body = mapOf(
            "caption" to caption,
            "tags" to tags,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType
        )
        return try {
            val response = api.doPut("posts", id, body)
            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "UpdatePost exception: ${e.message}", e)
            false
        }
    }

    /**
     * Delete post (soft delete).
     */
    suspend fun deletePost(id: String): Boolean {
        return try {
            val response = api.doDelete("posts", id)
            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "DeletePost exception: ${e.message}", e)
            false
        }
    }

    /**
     * Like a post.
     */
    suspend fun likePost(postId: String, userId: String): Boolean {
        val body = LikeRequest(userId)
        return try {
            val response = api.doPost("posts/$postId/like", body)
            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "LikePost exception: ${e.message}", e)
            false
        }
    }

    /**
     * Add comment to a post.
     */
    suspend fun addComment(postId: String, userId: String, text: String): Comment? {
        val body = CommentRequest(userId, text)
        return try {
            val response = api.doPost("posts/$postId/comments", body)
            if (response.status.value in 200..299) {
                val wrapper = response.body<Map<String, Comment>>()
                wrapper["comment"]
            } else {
                Log.e(TAG, "AddComment failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "AddComment exception: ${e.message}", e)
            null
        }
    }

    /**
     * Search posts by caption or tag.
     */
    suspend fun searchPosts(query: String): List<Post> {
        return try {
            val response = api.doGet("posts/search?q=$query")
            if (response.status.value in 200..299) {
                response.body()
            } else {
                Log.e(TAG, "SearchPosts failed: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SearchPosts exception: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        fun create(): RevdUpPostService {
            val client = HttpClient(Android) {
                // Install Ktor plugins inside this block
                install(Logging) {
                    level = LogLevel.ALL
                }
                install(ContentNegotiation) {
                    json()
                }
            }
            // This now correctly returns an instance of the implementation class
            return RevdUpPostServiceImpl(RevdUpService(client))
        }
    }
}

// This is the one, correct definition of the implementation class.
class RevdUpPostServiceImpl(api: RevdUpService) : RevdUpPostService(api) {
    // You can add implementation-specific details here if needed,
    // otherwise, inheriting from RevdUpPostService is sufficient.
}

// THE DUPLICATE CLASS DEFINITION THAT WAS HERE HAS BEEN REMOVED.
