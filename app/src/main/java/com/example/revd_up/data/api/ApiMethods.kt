package com.example.revd_up.data.api

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*

// Define the API Base URL. 10.0.2.2 points to the host machine's localhost (your Go backend).
const val API_BASE_URL = "http://10.0.2.2:8080"

/**
 * Singleton Ktor HTTP Client configured for JSON content negotiation and logging.
 * This client is shared across all API service classes.
 */
val KtorClient = HttpClient(Android) {
    // 1. Install Content Negotiation for JSON
    install(ContentNegotiation) {
        json(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }

    // 2. Install the Logging plugin
    install(Logging) {
        // Route all Ktor logs to Android's Logcat
        logger = object : Logger {
            override fun log(message: String) {
                Log.d("KtorClient", message)
            }
        }
        // Log everything: request, response headers, and bodies
        level = LogLevel.ALL
    }

    // You would typically add an interceptor here for sending the Authorization token
    // For now, we keep it simple until we introduce the Repository layer.
}