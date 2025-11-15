package com.example.revd_up.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.Serializable

/**
 * Generic data models
 */
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(
    val success: Boolean? = null,
    val token: String? = null,
    val message: String? = null,
    val user: UserData? = null
)

@Serializable
data class UserData(
    val id: String? = null,
    val name: String? = null,
    val username: String? = null,
    val email: String? = null,
    val pictureUrl: String? = null,
    val role: String? = null
)

class RevdUpService(client: HttpClient) {

    companion object {
        private const val TAG = "RevdUpService"
        private const val API_BASE_URL = "http://10.0.2.2:8080"
    }

    // 🔐 Your stored token (set this when you log in)
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun clearAuthToken() {
        authToken = null
        clearUserRole() // ✅ invoke here to clear role too
    }

    private fun clearUserRole() {
        Log.d(TAG, "Clearing stored user role and token...")
        // Example: clear from shared preferences or local cache
    }

    // -------------------- LOGIN & SIGNUP --------------------
    suspend fun login(username: String, password: String): AuthResponse {
        return try {
            val response: AuthResponse = KtorClient.post("$API_BASE_URL/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }.body()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            AuthResponse(false, message = "Network error: ${e.message}")
        }
    }

    suspend fun signUp(email: String, password: String): AuthResponse {
        return try {
            val response: AuthResponse = KtorClient.post("$API_BASE_URL/api/signup") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }.body()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            AuthResponse(false, message = "Network error during signup: ${e.message}")
        }
    }

    suspend fun googleLogin(idToken: String): AuthResponse {
        return try {
            val response: AuthResponse = KtorClient.post("$API_BASE_URL/auth/google/verify") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("idToken" to idToken))
            }.body()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Google Login failed", e)
            AuthResponse(false, message = "Network error: ${e.message}")
        }
    }

    // -------------------- GENERIC API METHODS --------------------

    /**
     * Generic POST with optional JSON or multipart body.
     */
    suspend fun doPost(
        endpoint: String,
        body: Any? = null,
        isMultipart: Boolean = false
    ): HttpResponse {
        val url = "$API_BASE_URL/$endpoint"
        return KtorClient.post(url) {
            authToken?.let { header("Authorization", "Bearer $it") }

            // FIX: Change the type check from List<*> to List<PartData>
            if (isMultipart && body is List<*> && body.all { it is PartData }) {
                @Suppress("UNCHECKED_CAST") // This cast is now safe because of the check
                setBody(MultiPartFormDataContent(body as List<PartData>))
            } else if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    /**
     * Generic GET request with optional query parameters or ID.
     */
    suspend fun doGet(
        endpoint: String,
        param: String? = null
    ): HttpResponse {
        val url = buildString {
            append("$API_BASE_URL/$endpoint")
            param?.let { append("/$it") }
        }
        return KtorClient.get(url) {
            authToken?.let { header("Authorization", "Bearer $it") }
        }
    }

    /**
     * Generic PUT request for updating resources.
     */
    suspend fun doPut(
        endpoint: String,
        id: String? = null,
        body: Any? = null
    ): HttpResponse {
        val url = buildString {
            append("$API_BASE_URL/$endpoint")
            id?.let { append("/$it") }
        }
        return KtorClient.put(url) {
            authToken?.let { header("Authorization", "Bearer $it") }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    /**
     * Generic DELETE request for deleting by ID.
     */
    suspend fun doDelete(
        endpoint: String,
        id: String? = null
    ): HttpResponse {
        val url = buildString {
            append("$API_BASE_URL/$endpoint")
            id?.let { append("/$it") }
        }
        return KtorClient.delete(url) {
            authToken?.let { header("Authorization", "Bearer $it") }
        }
    }
}
