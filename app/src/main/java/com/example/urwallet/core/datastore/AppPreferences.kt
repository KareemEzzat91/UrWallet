package com.example.urwallet.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.urwallet.core.common.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
 *
 * 3. Notifications & Local Reminders:
 *    Preferences for daily reminders, reminder time, budget alerts, and goal milestones.
 *    Also tracks deterministic delivered alert keys to enforce strict threshold crossing idempotency.
 */
class AppPreferences(
    private val dataStore: DataStore<Preferences>
) {

    constructor(context: Context) : this(context.dataStore)

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        private val KEY_FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        private val KEY_PIN_LOCKOUT_UNTIL_TIMESTAMP = longPreferencesKey("pin_lockout_until_timestamp")

        // Notification Preferences
        private val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("is_daily_reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val KEY_BUDGET_ALERTS_ENABLED = booleanPreferencesKey("is_budget_alerts_enabled")
        private val KEY_GOAL_ALERTS_ENABLED = booleanPreferencesKey("is_goal_alerts_enabled")
        private val KEY_DELIVERED_ALERT_KEYS = stringSetPreferencesKey("delivered_alert_keys")

        // Financial Event Detection Preferences
        private val KEY_SMS_DETECTION_ENABLED = booleanPreferencesKey("is_sms_detection_enabled")
        private val KEY_SMS_LAST_SCAN_TIMESTAMP = longPreferencesKey("sms_last_scan_timestamp")

        // Language Preference
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val appLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_APP_LANGUAGE] ?: Constants.DEFAULT_LANGUAGE
    }

    suspend fun setAppLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = languageCode
        }
    }

    val isSmsDetectionEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SMS_DETECTION_ENABLED] ?: false
    }

    suspend fun setSmsDetectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SMS_DETECTION_ENABLED] = enabled
        }
    }

    val lastSmsScanTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_SMS_LAST_SCAN_TIMESTAMP] ?: 0L
    }

    suspend fun setLastSmsScanTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_SMS_LAST_SCAN_TIMESTAMP] = timestamp
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    val currencySymbol: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_SYMBOL] ?: Constants.DEFAULT_CURRENCY_SYMBOL
    }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    val currencyCode: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_CODE] ?: Constants.DEFAULT_CURRENCY_CODE
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_CODE] = code
        }
    }

    val isAppLockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_APP_LOCK_ENABLED] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    val pinSalt: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_PIN_SALT]
    }

    val pinHash: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_PIN_HASH]
    }

    suspend fun savePin(salt: String, hash: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PIN_SALT] = salt
            preferences[KEY_PIN_HASH] = hash
            preferences[KEY_APP_LOCK_ENABLED] = true
        }
    }

    suspend fun clearPin() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_PIN_SALT)
            preferences.remove(KEY_PIN_HASH)
            preferences[KEY_APP_LOCK_ENABLED] = false
        }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    val failedPinAttempts: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_FAILED_PIN_ATTEMPTS] ?: 0
    }

    suspend fun incrementFailedPinAttempts(): Int {
        var newCount = 1
        dataStore.edit { preferences ->
            val current = preferences[KEY_FAILED_PIN_ATTEMPTS] ?: 0
            newCount = current + 1
            preferences[KEY_FAILED_PIN_ATTEMPTS] = newCount
        }
        return newCount
    }

    suspend fun resetFailedPinAttempts() {
        dataStore.edit { preferences ->
            preferences[KEY_FAILED_PIN_ATTEMPTS] = 0
            preferences[KEY_PIN_LOCKOUT_UNTIL_TIMESTAMP] = 0L
        }
    }

    val pinLockoutUntil: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_PIN_LOCKOUT_UNTIL_TIMESTAMP] ?: 0L
    }

    suspend fun setPinLockoutUntil(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_PIN_LOCKOUT_UNTIL_TIMESTAMP] = timestamp
        }
    }

    // --- Phase 12 Notification Preferences ---

    val isDailyReminderEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_REMINDER_ENABLED] ?: true
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_ENABLED] = enabled
        }
    }

    val reminderHour: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_REMINDER_HOUR] ?: 21
    }

    val reminderMinute: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_REMINDER_MINUTE] ?: 0
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_REMINDER_HOUR] = hour
            preferences[KEY_REMINDER_MINUTE] = minute
        }
    }

    val isBudgetAlertsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BUDGET_ALERTS_ENABLED] ?: true
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BUDGET_ALERTS_ENABLED] = enabled
        }
    }

    val isGoalAlertsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_GOAL_ALERTS_ENABLED] ?: true
    }

    suspend fun setGoalAlertsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_GOAL_ALERTS_ENABLED] = enabled
        }
    }

    val deliveredAlertKeys: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_DELIVERED_ALERT_KEYS] ?: emptySet()
    }

    suspend fun markAlertDelivered(key: String) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_DELIVERED_ALERT_KEYS] ?: emptySet()
            preferences[KEY_DELIVERED_ALERT_KEYS] = current + key
        }
    }

    suspend fun isAlertDelivered(key: String): Boolean {
        return deliveredAlertKeys.first().contains(key)
    }
}
