package com.example.revd_up

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.revd_up.ui.theme.REVD_UPTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import android.util.Log
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch

// --------------------------------------------------------------------------------
// 1. DATA MODELS AND KTOR CLIENT
// --------------------------------------------------------------------------------

// Define the API Base URL. 10.0.2.2 points to the host machine's localhost (your Go backend).
const val API_BASE_URL = "http://10.0.2.2:8080"

// Data class for sending login credentials
@Serializable
data class LoginRequest(val username: String, val password: String)

// Data class for receiving API response (customize this to match your Go backend's JSON structure)
@Serializable
data class AuthResponse(val success: Boolean, val token: String? = null, val message: String? = null)

// Ktor HTTP Client setup with logging
val httpClient = HttpClient(Android) {
    // 1. Install Content Negotiation for JSON
    install(ContentNegotiation) {
        json(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        })
    }

    // 2. Install the Logging plugin
    install(Logging) {
        // Use Android's Log class for output
        logger = object : Logger {
            override fun log(message: String) {
                Log.d("KtorClient", message)
            }
        }
        // Set the desired logging level
        level = LogLevel.ALL // This logs headers, body, and all details
    }
}

/**
 * Function to call the real Go backend Login endpoint using Ktor.
 */
suspend fun realLoginApi(username: String, password: String): AuthResponse {
    return try {
        val response: AuthResponse = httpClient.post("$API_BASE_URL/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body()
        response
    } catch (e: Exception) {
        // Handle network errors, parsing errors, etc.
        AuthResponse(false, message = "Network error: ${e.message}")
    }
}

/**
 * Function to call the real Go backend Sign Up endpoint using Ktor.
 * For simplicity, it reuses the same data models.
 */
suspend fun realSignUpApi(email: String, password: String): AuthResponse {
    return try {
        val response: AuthResponse = httpClient.post("$API_BASE_URL/api/signup") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password)) // Assuming Go backend uses email/password structure for signup
        }.body()
        response
    } catch (e: Exception) {
        AuthResponse(false, message = "Network error during signup: ${e.message}")
    }
}


// --------------------------------------------------------------------------------
// 2. MAIN ACTIVITY AND NAVIGATION
// --------------------------------------------------------------------------------

sealed class Screen {
    object Onboarding : Screen()
    object Auth : Screen()
}

class MainActivity : ComponentActivity() {
    // ... inside MainActivity class
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            REVD_UPTheme {var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }

                @OptIn(ExperimentalAnimationApi::class) // <-- ADD THIS ANNOTATION HERE
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() with
                                slideOutHorizontally { width -> -width } + fadeOut()
                    }, label = ""
                ) { screen ->
                    when (screen) {
                        Screen.Onboarding -> OnboardingScreen(
                            onFinish = { currentScreen = Screen.Auth }
                        )
                        Screen.Auth -> AuthScreen(
                            onLoginSuccess = { Toast.makeText(this@MainActivity, "Welcome to the Dashboard!", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        }
    }

}

// --------------------------------------------------------------------------------
// 3. ONBOARDING SCREENS (SCROLLABLE WALKTHROUGH)
// --------------------------------------------------------------------------------

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalPagerApi::class) // Annotation to use Accompanist Pager
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage(
                "Welcome to REVD_UP!",
                "Track your fitness goals, log your workouts, and connect with friends.",
                icon = {
                    Icon(
                        rememberVectorPainter(Icons.Filled.Person),
                        contentDescription = "Welcome Icon",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ),
            OnboardingPage(
                "Customize Your Journey",
                "Personalized workout plans and nutrition advice tailored just for you.",
                icon = {
                    Icon(
                        rememberVectorPainter(Icons.Filled.Lock),
                        contentDescription = "Customize Icon",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            ),
            OnboardingPage(
                "Community Challenges",
                "Join competitive challenges and keep yourself motivated with our global community.",
                icon = {
                    Icon(
                        rememberVectorPainter(Icons.Filled.Person),
                        contentDescription = "Community Icon",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            )
        )
    }
    // Use the official rememberPagerState from the library
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope() // Scope for launching animations

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            count = pages.size, // The parameter is 'count'
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(color, shape = RoundedCornerShape(50))
                        .size(10.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                Button(onClick = {
                    // Use a coroutine to animate the scroll
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (pagerState.currentPage < pages.size - 1) {
                Button(onClick = {
                    // Use a coroutine to animate the scroll
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Get Started!")
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        page.icon()
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = page.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

// --------------------------------------------------------------------------------
// 4. AUTHENTICATION SCREENS (LOGIN & SIGNUP - NOW USING REAL API)
// --------------------------------------------------------------------------------

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var authMode by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (authMode == 0) "Welcome Back" else "Create Account",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(48.dp))

            if (authMode == 0) {
                LoginContent(onLoginSuccess)
            } else {
                SignUpContent(onSignUpSuccess = { authMode = 0 })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (authMode == 0) "Don't have an account? Sign Up" else "Already have an account? Log In",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { authMode = if (authMode == 0) 1 else 0 }
            )
        }
    }
}

@Composable
fun LoginContent(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Username / Email") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "User Icon") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock Icon") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = {
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Please enter credentials.", Toast.LENGTH_SHORT).show()
                return@Button
            }

            isLoading = true
            scope.launch {
                val response = realLoginApi(username, password) // *** REAL API CALL ***
                isLoading = false

                if (response.success) {
                    Toast.makeText(context, "Logged in: ${response.token}", Toast.LENGTH_LONG).show()
                    onLoginSuccess()
                } else {
                    Toast.makeText(context, "Login failed: ${response.message}", Toast.LENGTH_LONG).show()
                }
            }
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Log In")
        }
    }
}

// You will also need a SignUpContent composable, which I have added for completeness
@Composable
fun SignUpContent(onSignUpSuccess: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "User Icon") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock Icon") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = {
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@Button
            }

            isLoading = true
            scope.launch {
                val response = realSignUpApi(email, password) // *** REAL API CALL ***
                isLoading = false

                if (response.success) {
                    Toast.makeText(context, "Sign up successful! Please log in.", Toast.LENGTH_LONG).show()
                    onSignUpSuccess()
                } else {
                    Toast.makeText(context, "Sign up failed: ${response.message}", Toast.LENGTH_LONG).show()
                }
            }
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Sign Up")
        }
    }
}

