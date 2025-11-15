package com.example.revd_up

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.revd_up.data.api.RevdUpService
import com.example.revd_up.data.api.RevdUpPostService

import com.example.revd_up.data.api.AuthResponse
import com.example.revd_up.data.store.AuthDataStore
import com.example.revd_up.presentation.views.common.DashboardScreen
import com.example.revd_up.presentation.views.common.OnboardingScreen
import com.example.revd_up.ui.theme.REVD_UPTheme
// Import the new screen files
import com.example.revd_up.presentation.views.auth.AuthScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


// --------------------------------------------------------------------------------
// 1. CONSTANTS AND NAVIGATION DEFINITIONS
// --------------------------------------------------------------------------------

private const val TAG = "REVD_UP_LOG"

// User roles reflecting the views directory structure (customer, merchant, admin)
enum class UserType { CUSTOMER, ADMIN, UNKNOWN, VERIFIED_MECHANIC }

sealed class Screen {
    object Onboarding : Screen()
    object Auth : Screen()
    data class Dashboard(val userType: UserType) : Screen()
}

class MainActivity : ComponentActivity() {

    // Add this client definition
    private val client by lazy {
        HttpClient(Android) {
            // For JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true // This is helpful for API changes
                })
            }
            // For logging network requests and responses
            install(Logging) {
                level = LogLevel.BODY
            }
        }
    }

    private val authDataStore by lazy { AuthDataStore(applicationContext) }
    private val revdUpService by lazy { RevdUpService(client) }
    private val revdUpPostService by lazy { RevdUpPostService.create() }
    private lateinit var googleSignInClient: GoogleSignInClient

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started.")

        // --- Google Sign-In setup (unchanged) ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.google_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            REVD_UPTheme {

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }
                var isCheckingToken by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                // Central navigation helper
                val navigateToScreen: (Screen) -> Unit = { screen ->
                    currentScreen = screen
                    if (screen is Screen.Dashboard) {
                        Toast.makeText(
                            this@MainActivity,
                            "Welcome! Loading ${screen.userType} Dashboard.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // 🔥 Load saved token + user role when app launches
                LaunchedEffect(Unit) {
                    try {
                        val token = authDataStore.authTokenFlow.first()
                        val roleString = authDataStore.userRoleFlow.first()

                        if (token != null && !token.token.isNullOrBlank()) {
                            val userRole = when (roleString) {
                                "CUSTOMER" -> UserType.CUSTOMER
                                "ADMIN" -> UserType.ADMIN
                                "VERIFIED_MECHANIC" -> UserType.VERIFIED_MECHANIC
                                else -> UserType.UNKNOWN
                            }
                            Log.d(TAG, "Restored session with role: $userRole")
                            currentScreen = Screen.Dashboard(userRole)
                        } else {
                            currentScreen = Screen.Onboarding
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring session: ${e.message}")
                        currentScreen = Screen.Onboarding
                    } finally {
                        isCheckingToken = false
                    }
                }

                // --- Google Sign-In Handler ---
                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    scope.launch {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        val idToken = try {
                            task.getResult(ApiException::class.java).idToken
                        } catch (e: ApiException) {
                            Log.w(TAG, "Google Sign-In failed: ${e.statusCode}")
                            "mock-google-id-token-${UUID.randomUUID()}"
                        }

                        if (idToken != null) {
                            val response = try {
                                revdUpService.googleLogin(idToken)
                            } catch (e: Exception) {
                                AuthResponse(false, "Network error during Google login.")
                            }

                            if (response.success == true && response.token != null) {
                                // Save token and role
                                authDataStore.saveAuthToken(response.token)
                                val userRole = "CUSTOMER" // TODO: Replace with role from response
                                authDataStore.saveUserRole(userRole)
                                navigateToScreen(Screen.Dashboard(UserType.valueOf(userRole)))
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    response.message ?: "Google Sign-In failed.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                val onGoogleLoginClicked: () -> Unit = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }

                // --- UI ROUTER ---
                if (isCheckingToken) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() with
                                    slideOutHorizontally { -it } + fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.Onboarding -> OnboardingScreen(
                                onFinish = { navigateToScreen(Screen.Auth) }
                            )
                            Screen.Auth -> AuthScreen(
                                service = revdUpService,
                                dataStore = authDataStore,
                                onLoginSuccess = {
                                    scope.launch {
                                        // Mock user role for demo (replace with backend role)
                                        val userRole = "CUSTOMER"
                                        authDataStore.saveUserRole(userRole)
                                        navigateToScreen(Screen.Dashboard(UserType.valueOf(userRole)))
                                    }
                                },
                                onGoogleLoginClicked = onGoogleLoginClicked
                            )
                            is Screen.Dashboard -> DashboardScreen(
                                dataStore = authDataStore,
                                userType = screen.userType,
                                postService = revdUpPostService,
                                onLogout = {
                                    scope.launch {
                                        authDataStore.clearAuthToken()
                                        navigateToScreen(Screen.Auth)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
