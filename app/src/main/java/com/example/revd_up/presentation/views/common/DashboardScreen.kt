package com.example.revd_up.presentation.views.common

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.revd_up.UserType
import com.example.revd_up.data.store.AuthDataStore
// *** FIX: Correct the import to use the singular form ***
import com.example.revd_up.data.api.RevdUpPostService
import com.example.revd_up.presentation.navigation.*
import com.example.revd_up.presentation.views.admin.AdminDashboard
import com.example.revd_up.presentation.views.customer.CustomerFeedScreen
import com.example.revd_up.presentation.views.mechanic.VerifiedMechanicDashboard
import kotlinx.coroutines.launch

private const val TAG = "REVD_UP_DASHBOARD"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dataStore: AuthDataStore,
    userType: UserType,
    postService: RevdUpPostService,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    // 1. Determine navigation items and start destination based on role
    val (navItems, startDestination) = when (userType) {
        UserType.CUSTOMER -> CustomerNavItems to BottomNavItem.CustomerFeed.route
        UserType.ADMIN -> AdminNavItems to BottomNavItem.AdminDashboard.route
        UserType.VERIFIED_MECHANIC -> VerifiedMechanicNavItems to BottomNavItem.MechanicJobs.route
        UserType.UNKNOWN -> emptyList<BottomNavItem>() to ""
    }

    if (userType == UserType.UNKNOWN) {
        UnknownUserScreen(dataStore = dataStore, onLogout = onLogout)
        return
    }

    // 2. Main Scaffold (Shell)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REV'D UP (${userType.name})") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            Log.d(TAG, "Logout initiated from TopAppBar.")
                            dataStore.clearAuthToken()
                            dataStore.clearUserRole()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Log Out")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavBar(navController = navController, items = navItems)
        }
    ) { paddingValues ->
        // 3. Navigation Host for Bottom Nav (Page Switching)
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Customer Routes
            composable(BottomNavItem.CustomerFeed.route) {
                CustomerFeedScreen(postService = postService)
            }
            composable(BottomNavItem.CustomerProfile.route) {
                //CustomerProfileScreen() // Logout is in TopAppBar
            }

            // Admin Routes
            composable(BottomNavItem.AdminDashboard.route) {
                AdminDashboard(dataStore = dataStore, onLogout = onLogout)
            }
            composable(BottomNavItem.AdminUsers.route) {
                //AdminUsersScreen()
            }

            // Verified Mechanic Routes
            composable(BottomNavItem.MechanicJobs.route) {
                VerifiedMechanicDashboard(dataStore = dataStore, onLogout = onLogout)
            }
            composable(BottomNavItem.MechanicTools.route) {
                VerifiedMechanicDashboard(dataStore = dataStore, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun UnknownUserScreen(
    dataStore: AuthDataStore,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Error: Unknown User Role",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your assigned role is unknown. Please log out and try again.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                scope.launch {
                    Log.d(TAG, "Logout from UnknownUserScreen.")
                    dataStore.clearAuthToken()
                    dataStore.clearUserRole()
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Out")
        }
    }
}