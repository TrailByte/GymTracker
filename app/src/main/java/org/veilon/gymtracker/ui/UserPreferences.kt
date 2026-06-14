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

    fun weeklyGoal(context: Context): Flow<Int> =
        context.dataStore.data.map { it[WEEKLY_GOAL] ?: 3 }

    suspend fun setWeeklyGoal(context: Context, goal: Int) {
        context.dataStore.edit { it[WEEKLY_GOAL] = goal.coerceAtLeast(1) }
    }
}