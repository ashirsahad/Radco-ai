package com.accountpro.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "accountpro_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val loggedInUserId: Flow<String?> = context.dataStore.data.map { it[Keys.LOGGED_IN_USER_ID] }
    val languageCode: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE_CODE] ?: "en" }
    val currencyCode: Flow<String> = context.dataStore.data.map { it[Keys.CURRENCY_CODE] ?: "USD" }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: false }

    suspend fun setLoggedInUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(Keys.LOGGED_IN_USER_ID)
            else prefs[Keys.LOGGED_IN_USER_ID] = userId
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }
}
