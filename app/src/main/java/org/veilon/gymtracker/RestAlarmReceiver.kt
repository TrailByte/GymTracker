package org.veilon.gymtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.veilon.gymtracker.ui.UserPreferences

class RestAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_KIND = "kind"
        const val KIND_BUZZ = "buzz"     // one of the 3-2-1 countdown buzzes
        const val KIND_FINISH = "finish" // rest is over: sound + double buzz
    }

    override fun onReceive(context: Context, intent: Intent?) {
        // Dedup with the in-app timer: it clears restEndsAt when it fires.
        // If rest is no longer active, the in-app path already handled it → stay silent.
        val endsAt = runBlocking {
            UserPreferences.restEndsAt(context).first()
        } ?: return

        when (intent?.getStringExtra(EXTRA_KIND)) {
            KIND_BUZZ -> {
                // Only buzz if we're genuinely still counting down
                if (System.currentTimeMillis() < endsAt) RestAlerts.buzz(context)
            }
            KIND_FINISH -> {
                RestAlerts.finishAlert(context)
                runBlocking { UserPreferences.setRestEndsAt(context, null) }
                RestTimerService.stop(context)
            }
        }
    }
}