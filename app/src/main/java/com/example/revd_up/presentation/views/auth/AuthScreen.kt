package com.example.revd_up.presentation.views.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.revd_up.data.api.RevdUpService
import com.example.revd_up.data.store.AuthDataStore
import kotlinx.coroutines.launch

private const val TAG = "REVD_UP_AUTH"

// --------------------------------------------------------------------------------
// 1. TOP-LEVEL AUTH SCREEN (Router between Login and Sign Up)
// --------------------------------------------------------------------------------

@Composable
fun AuthScreen(
    service: RevdUpService,
    dataStore: AuthDataStore,
    onLoginSuccess: () -> Unit,
    onGoogleLoginClicked: () -> Unit // Called when Google button is pressed
) {
    var authMode by remember { mutableStateOf(0) } // 0 = Login, 1 = Sign Up

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

            // Google Login Button (Delegates to MainActivity's launcher)
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
                    onSignUpSuccess = { authMode = 0 } // Switch back to login after successful signup
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

// --------------------------------------------------------------------------------
// 2. LOGIN CONTENT
// --------------------------------------------------------------------------------

@Composable
private fun LoginContent(
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
                val response = service.login(username, password)
                isLoading = false

                if (response.success == true && response.token != null) {
                    dataStore.saveAuthToken(response.token)

                    // 🔹 Save user role (default to "unknown" if null or empty)
                    val role = response.user?.role ?: "unknown"
                    dataStore.saveUserRole(role)

                    onLoginSuccess()
                }
                else {
                    Toast.makeText(context, response.message ?: "Login failed. Check your details.", Toast.LENGTH_LONG).show()
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

// --------------------------------------------------------------------------------
// 3. SIGNUP CONTENT
// --------------------------------------------------------------------------------

@Composable
private fun SignUpContent(
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
                val response = service.signUp(email, password)
                isLoading = false

                if (response.success == true) {
                    Toast.makeText(context, "Sign up successful! Please log in.", Toast.LENGTH_LONG).show()
                    onSignUpSuccess() // Switch to the login view
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