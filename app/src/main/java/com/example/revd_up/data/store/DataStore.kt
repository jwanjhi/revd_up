package com.example.revd_up.data.store

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Defines the keys used for saving preferences in DataStore.
 */
object PreferencesKeys {
    // Key for storing the JWT or session token.
    val AUTH_TOKEN = stringPreferencesKey("auth_token")

    // Key for storing the user's role (e.g., "admin", "merchant", "customer")
    val USER_ROLE = stringPreferencesKey("user_role")
}