package com.example.revd_up.data.store

import android.content.ContentValues.TAG
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.revd_up.data.store.PreferencesKeys.AUTH_TOKEN
import com.example.revd_up.data.store.PreferencesKeys.USER_ROLE
import com.example.revd_up.domain.model.AuthToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

class AuthDataStore(private val context: Context) {

    val authTokenFlow: Flow<AuthToken?> = context.dataStore.data
        .map { preferences ->
            preferences[AUTH_TOKEN]?.let { token ->
                AuthToken(token)
            }
        }

    suspend fun saveAuthToken(token: String) {
        Log.d(TAG, "Saving token to DataStore: $token")
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
        Log.d(TAG, "Token saved successfully")
    }

    suspend fun saveUserRole(role: String) {
        Log.d(TAG, "Saving user role: $role")
        context.dataStore.edit { preferences ->
            preferences[USER_ROLE] = role
        }
        Log.d(TAG, "User role saved successfully")
    }

    val userRoleFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ROLE]
        }

    suspend fun clearUserRole() {
        Log.d(TAG, "Clearing user role from DataStore")
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ROLE)
        }
        Log.d(TAG, "User role cleared successfully")
    }

    suspend fun clearAuthToken() {
        Log.d(TAG, "Clearing auth token and user role")
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
        }
        clearUserRole() // ✅ Clears role automatically on logout
        Log.d(TAG, "Auth token and role cleared successfully")
    }
}
