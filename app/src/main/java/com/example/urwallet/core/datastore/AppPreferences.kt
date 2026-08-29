package com.example.urwallet.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.urwallet.core.common.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

/**
 * Persistent preferences backed by Jetpack DataStore.
 *
 * Architectural & Design Notes:
 * 1. Single-Currency MVP:
 *    [currencySymbol] and [currencyCode] are strictly display preferences (default EGP / ج.م).
 *    All financial transactions, budgets, and goals are calculated in a single unit.
 *    No exchange rates or currency conversions exist in the MVP.
 *
 * 2. Security & PIN Authentication:
 *    PIN authentication uses PBKDF2WithHmacSHA256 key derivation / password hashing
 *    with a cryptographically random salt. This is password hashing, NOT encryption.
 *    Only [pinHash] and [pinSalt] are persisted.
 */
class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    val currencySymbol: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_SYMBOL] ?: Constants.DEFAULT_CURRENCY_SYMBOL
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    val currencyCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_CODE] ?: Constants.DEFAULT_CURRENCY_CODE
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_CODE] = code
        }
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_LOCK_ENABLED] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    val pinSalt: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PIN_SALT]
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PIN_HASH]
    }

    suspend fun savePin(salt: String, hash: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PIN_SALT] = salt
            preferences[KEY_PIN_HASH] = hash
            preferences[KEY_APP_LOCK_ENABLED] = true
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PIN_SALT)
            preferences.remove(KEY_PIN_HASH)
            preferences[KEY_APP_LOCK_ENABLED] = false
        }
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }
}
