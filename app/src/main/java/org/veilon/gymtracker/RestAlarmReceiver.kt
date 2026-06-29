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
        const val KIND_FINISH = "finish" // rest is over: sound + double buzz
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_KIND) != KIND_FINISH) return

        // Guard: only fire if rest is still active (user may have skipped/finished,
        // which clears restEndsAt and cancels this alarm — but this is a belt-and-braces check).
        val endsAt = runBlocking { UserPreferences.restEndsAt(context).first() }
        if (endsAt == null) {
            // Already cleared — just make sure the notification is gone.
            RestTimerService.stop(context)
            return
        }

        RestAlerts.finishAlert(context)
        runBlocking { UserPreferences.setRestEndsAt(context, null) }
        RestTimerService.stop(context)
    }
}
