package com.example.revd_up

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountCircle // Import for Google Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// IMPORTING NEWLY CREATED DATA LAYERS
import com.example.revd_up.data.api.RevdUpService
import com.example.revd_up.data.api.AuthResponse // Import AuthResponse
import com.example.revd_up.data.store.AuthDataStore

// ACCOMPANIST PAGER IMPORTS (for OnboardingScreen)
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

// --------------------------------------------------------------------------------
// 1. CONSTANTS AND NAVIGATION
// --------------------------------------------------------------------------------

private const val TAG = "REVD_UP_LOG"

sealed class Screen {
    object Onboarding : Screen()
    object Auth : Screen()
    object Dashboard : Screen()
}

class MainActivity : ComponentActivity() {

    private val authDataStore by lazy {
        Log.d(TAG, "Initializing AuthDataStore.")
        AuthDataStore(applicationContext)
    }
    private val revdUpService by lazy {
        Log.d(TAG, "Initializing RevdUpService.")
        RevdUpService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started.")

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_client_id)) // from strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            REVD_UPTheme {

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }
                var isCheckingToken by remember { mutableStateOf(true) }
                val currentToken by authDataStore.authTokenFlow.collectAsState(initial = null)
                val scope = rememberCoroutineScope()

                val navigateToScreen: (Screen) -> Unit = { screen ->
                    currentScreen = screen
                    Log.d(TAG, "Navigation requested to: $screen")
                    if (screen == Screen.Dashboard) {
                        Toast.makeText(this@MainActivity, "Welcome to the Dashboard!", Toast.LENGTH_SHORT).show()
                    }
                }

                // LaunchedEffect to determine the *initial* screen based on the token
                LaunchedEffect(Unit) {
                    Log.d(TAG, "LaunchedEffect(Unit) triggered: Starting token check.")
                    try {
                        val initialToken = authDataStore.authTokenFlow.first()
                        if (initialToken != null && !initialToken.token.isNullOrBlank()) {
                            currentScreen = Screen.Dashboard
                            Log.d(TAG, "Token found. Navigating to: Dashboard.")
                        } else {
                            currentScreen = Screen.Onboarding
                            Log.d(TAG, "No token found. Navigating to: Onboarding.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching initial token via flow.first(): ${e.message}", e)
                        currentScreen = Screen.Onboarding
                    } finally {
                        isCheckingToken = false
                        Log.d(TAG, "Token check complete.")
                    }
                }

                /**
                 * **IMPLEMENTED:** Simulated Google Login Handler
                 * This mimics the real-world flow of receiving an ID token and sending it to the backend.
                 */
                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account.idToken

                        if (idToken != null) {
                            scope.launch {
                                // Call backend verification
                                val response = revdUpService.googleLogin(idToken)
                                if (response.success && response.token != null) {
                                    authDataStore.saveAuthToken(response.token)
                                    navigateToScreen(Screen.Dashboard)
                                } else {
                                    Toast.makeText(this@MainActivity, response.message ?: "Google Sign-In failed.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: ApiException) {
                        Log.e(TAG, "Google Sign-In failed: ${e.statusCode}")
                        Toast.makeText(this@MainActivity, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                    }
                }

                val onGoogleLoginClicked: () -> Unit = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }



                // Display Loading Indicator if the initial token check is still running
                if (isCheckingToken) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    @OptIn(ExperimentalAnimationApi::class)
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            slideInHorizontally { width -> width } + fadeIn() with
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        }, label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.Onboarding -> OnboardingScreen(
                                onFinish = { navigateToScreen(Screen.Auth) }
                            )
                            Screen.Auth -> AuthScreen(
                                service = revdUpService,
                                dataStore = authDataStore,
                                onLoginSuccess = { navigateToScreen(Screen.Dashboard) },
                                onGoogleLoginClicked = onGoogleLoginClicked // Pass the handler
                            )
                            Screen.Dashboard -> DashboardScreen(
                                dataStore = authDataStore,
                                onLogout = { navigateToScreen(Screen.Auth) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// 3. ONBOARDING SCREENS (Unchanged)
// --------------------------------------------------------------------------------

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage(
                "Welcome to REVD_UP!",
                "Track your fitness goals, log your workouts, and connect with friends.",
                icon = { Icon(rememberVectorPainter(Icons.Filled.Person), contentDescription = "Welcome Icon", modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary) }
            ),
            OnboardingPage(
                "Customize Your Journey",
                "Personalized workout plans and nutrition advice tailored just for you.",
                icon = { Icon(rememberVectorPainter(Icons.Filled.Lock), contentDescription = "Customize Icon", modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.secondary) }
            ),
            OnboardingPage(
                "Community Challenges",
                "Join competitive challenges and keep yourself motivated with our global community.",
                icon = { Icon(rememberVectorPainter(Icons.Filled.Person), contentDescription = "Community Icon", modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.tertiary) }
            )
        )
    }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            count = pages.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // Indicator dots
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

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) { Text("Previous") }
            } else { Spacer(modifier = Modifier.width(1.dp)) }

            if (pagerState.currentPage < pages.size - 1) {
                Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) { Text("Next") }
            } else {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) { Text("Get Started!") }
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
        Text(text = page.title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = page.description, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth(0.8f))
    }
}

// --------------------------------------------------------------------------------
// 4. AUTHENTICATION SCREENS (NOW WITH GOOGLE BUTTON)
// --------------------------------------------------------------------------------

@Composable
fun AuthScreen(
    service: RevdUpService,
    dataStore: AuthDataStore,
    onLoginSuccess: () -> Unit,
    onGoogleLoginClicked: () -> Unit // NEW HANDLER
) {
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

            // **Google Login Button (Placed at the top of the Auth Screen)**
            Button(
                onClick = onGoogleLoginClicked,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4), // Google Blue
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In with Google")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))


            if (authMode == 0) {
                LoginContent(
                    service = service,
                    dataStore = dataStore,
                    onLoginSuccess = onLoginSuccess
                )
            } else {
                SignUpContent(
                    service = service,
                    onSignUpSuccess = { authMode = 0 }
                )
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
fun LoginContent(
    service: RevdUpService,
    dataStore: AuthDataStore,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = username, onValueChange = { username = it },
        label = { Text("Username / Email") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "User Icon") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password, onValueChange = { password = it },
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
                Log.d(TAG, "Attempting Login...")
                val response = service.login(username, password)
                isLoading = false

                if (response.success && response.token != null) {
                    Log.d(TAG, "Login successful. Saving token and navigating.")
                    dataStore.saveAuthToken(response.token)
                    onLoginSuccess()
                } else {
                    Log.d(TAG, "Login failed. Message: ${response.message}")
                    Toast.makeText(context, response.message ?: "Login failed. Check server.", Toast.LENGTH_LONG).show()
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

@Composable
fun SignUpContent(
    service: RevdUpService,
    onSignUpSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = email, onValueChange = { email = it },
        label = { Text("Email") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "User Icon") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password, onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock Icon") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = confirmPassword, onValueChange = { confirmPassword = it },
        label = { Text("Confirm Password") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock Icon") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = {
            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@Button
            }

            isLoading = true
            scope.launch {
                Log.d(TAG, "Attempting Sign Up...")
                val response = service.signUp(email, password)
                isLoading = false

                if (response.success) {
                    Log.d(TAG, "Sign Up successful. Returning to login screen.")
                    Toast.makeText(context, "Sign up successful! Please log in.", Toast.LENGTH_LONG).show()
                    onSignUpSuccess()
                } else {
                    Log.d(TAG, "Sign Up failed. Message: ${response.message}")
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

// --------------------------------------------------------------------------------
// 5. DASHBOARD SCREEN (Placeholder for Logged In State)
// --------------------------------------------------------------------------------

@Composable
fun DashboardScreen(
    dataStore: AuthDataStore,
    onLogout: () -> Unit
) {
    // Collect the token to display it and for logout logic
    val token by dataStore.authTokenFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dashboard", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Status: Logged In",
            color = Color.Green,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display truncated token for confirmation
        Text(
            // Safely access the token string
            text = "Token: ${token?.token?.take(20)}...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    Log.d(TAG, "Logout initiated.")
                    dataStore.clearAuthToken() // Clear the token
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Out")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    REVD_UPTheme {
        // Preview can show the Onboarding screen
        OnboardingScreen(onFinish = {})
    }
}