package org.veilon.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext

    val useLbs = UserPreferences.useLbs(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val restSeconds = UserPreferences.restSeconds(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90)

    fun setUseLbs(value: Boolean) {
        viewModelScope.launch { UserPreferences.setUseLbs(appContext, value) }
    }

    fun setRestSeconds(value: Int) {
        viewModelScope.launch {
            UserPreferences.setRestSeconds(appContext, value.coerceAtLeast(0))
        }
    }
}