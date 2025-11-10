package com.example.revd_up.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.revd_up.data.store.PreferencesKeys.AUTH_TOKEN
import com.example.revd_up.domain.model.AuthToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Global DataStore instance definition
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

/**
 * Handles persistent storage of authentication data (e.g., JWT tokens).
 * This replaces the need for keeping tokens in memory and provides auto-login capability.
 * * @param context The Android Context, typically ApplicationContext.
 */
class AuthDataStore(private val context: Context) {

    /**
     * Reads the authentication token from DataStore as a Flow.
     * @return Flow of AuthToken or null if not present.
     */
    val authTokenFlow: Flow<AuthToken?> = context.dataStore.data
        .map { preferences ->
            preferences[AUTH_TOKEN]?.let { token ->
                AuthToken(token)
            }
        }

    /**
     * Saves the authentication token to DataStore.
     */
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }

    /**
     * Clears all authentication preferences (used for logout).
     */
    suspend fun clearAuthToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
            preferences.remove(PreferencesKeys.USER_ROLE) // Also clear role on logout
        }
    }
}