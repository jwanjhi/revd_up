package com.example.revd_up.data.api

import android.content.ContentValues.TAG
import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi // <-- Added import for the OptIn marker
import java.util.UUID

/**
 * Data class for sending login credentials (DTO: Data Transfer Object).
 */
@Serializable
data class LoginRequest(val username: String, val password: String)

/**
 * Data class for receiving API response (DTO: Data Transfer Object).
 * This represents the raw network response.
 */
@Serializable
data class AuthResponse(val success: Boolean, val token: String? = null, val message: String? = null)

/**
 * Dedicated class to house all network-related functions for authentication.
 * This moves the API logic out of MainActivity.
 */
@OptIn(InternalSerializationApi::class) // <-- Added OptIn annotation to suppress the warning
class RevdUpService {

    /**
     * Calls the backend Login endpoint.
     */
    suspend fun login(username: String, password: String): AuthResponse {
        return try {
            val response: AuthResponse = KtorClient.post("$API_BASE_URL/auth/google/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }.body()
            response
        } catch (e: Exception) {
            Log.e("RevdUpService", "Login failed", e)
            AuthResponse(false, message = "Network error: ${e.message}")
        }
    }

    /**
     * Calls the backend Sign Up endpoint.
     */
    suspend fun signUp(email: String, password: String): AuthResponse {
        return try {
            val response: AuthResponse = KtorClient.post("$API_BASE_URL/api/signup") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }.body()
            response
        } catch (e: Exception) {
            Log.e("RevdUpService", "Sign up failed", e)
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

}