package org.veilon.gymtracker.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPreferences {
    private val USE_LBS: Preferences.Key<Boolean> = booleanPreferencesKey("use_lbs")
    private val REST_SECONDS: Preferences.Key<Int> = intPreferencesKey("rest_seconds")

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
}