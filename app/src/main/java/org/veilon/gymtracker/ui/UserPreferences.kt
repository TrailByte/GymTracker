package org.veilon.gymtracker.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPreferences {
    private val USE_LBS: Preferences.Key<Boolean> = booleanPreferencesKey("use_lbs")
    private val REST_SECONDS: Preferences.Key<Int> = intPreferencesKey("rest_seconds")
    private val ACTIVE_SESSION: Preferences.Key<Long> = longPreferencesKey("active_session")
    private val REST_ENDS_AT: Preferences.Key<Long> = longPreferencesKey("rest_ends_at")
    private val WEEKLY_GOAL: Preferences.Key<Int> = intPreferencesKey("weekly_goal")

    fun useLbs(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[USE_LBS] ?: false }

    suspend fun setUseLbs(context: Context, useLbs: Boolean) {
        context.dataStore.edit { it[USE_LBS] = useLbs }
    }

    fun restSeconds(context: Context): Flow<Int> =
        context.dataStore.data.map { it[REST_SECONDS] ?: 90 }

    suspend fun setRestSeconds(context: Context, seconds: Int) {
        context.dataStore.edit { it[REST_SECONDS] = seconds }
    }

    // Active workout session id; null when no workout is in progress
    fun activeSession(context: Context): Flow<Long?> =
        context.dataStore.data.map { prefs ->
            prefs[ACTIVE_SESSION]?.let { if (it <= 0L) null else it }
        }

    suspend fun setActiveSession(context: Context, sessionId: Long?) {
        context.dataStore.edit { prefs ->
            if (sessionId == null) prefs.remove(ACTIVE_SESSION)
            else prefs[ACTIVE_SESSION] = sessionId
        }
    }

    // Timestamp (epoch millis) when the current rest period ends; null = not resting
    fun restEndsAt(context: Context): Flow<Long?> =
        context.dataStore.data.map { prefs ->
            prefs[REST_ENDS_AT]?.let { if (it <= 0L) null else it }
        }

    suspend fun setRestEndsAt(context: Context, endsAt: Long?) {
        context.dataStore.edit { prefs ->
            if (endsAt == null) prefs.remove(REST_ENDS_AT)
            else prefs[REST_ENDS_AT] = endsAt
        }
    }

    fun weeklyGoal(context: Context): Flow<Int> =
        context.dataStore.data.map { it[WEEKLY_GOAL] ?: 3 }

    suspend fun setWeeklyGoal(context: Context, goal: Int) {
        context.dataStore.edit { it[WEEKLY_GOAL] = goal.coerceAtLeast(1) }
    }

    // with the other keys
    private val THEME_MODE: Preferences.Key<String> = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")

    // with the other accessors — values: "system" | "light" | "dark"
    fun themeMode(context: Context): Flow<String> =
        context.dataStore.data.map { it[THEME_MODE] ?: "system" }

    suspend fun setThemeMode(context: Context, mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}