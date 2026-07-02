package org.veilon.gymtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.runBlocking
import org.veilon.gymtracker.ui.UserPreferences

class RestAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_KIND = "kind"
        const val KIND_FINISH = "finish" // rest is over: sound + double buzz
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_KIND) != KIND_FINISH) return

        // The alarm is the SOLE alerter. Skip/finish/cancel already cancel this alarm,
        // so if it fires at all, it SHOULD alert — no dedup guard (that was suppressing it).
        // goAsync keeps the receiver alive so the async sound/vibration can run.
        val pending = goAsync()
        try {
            RestAlerts.finishAlert(context)
            runBlocking { UserPreferences.setRestEndsAt(context, null) }
            RestTimerService.stop(context)
        } catch (_: Exception) {
        } finally {
            Handler(Looper.getMainLooper()).postDelayed({ pending.finish() }, 4000)
        }
    }
}
