package org.veilon.gymtracker

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RestTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val NOTIF_ID = 4201
        const val EXTRA_ENDS_AT = "ends_at"
        const val ACTION_START = "start_rest"
        const val ACTION_STOP = "stop_rest"

        // Request codes for the four alarms (-3s, -2s, -1s, finish)
        private val REQ_CODES = intArrayOf(7001, 7002, 7003, 7000)

        fun start(context: Context, endsAtMillis: Long) {
            val i = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ENDS_AT, endsAtMillis)
            }
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            val i = Intent(context, RestTimerService::class.java).apply { action = ACTION_STOP }
            context.startService(i)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                cancelAlarms()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, 0L)
                createChannel()
                startForeground(NOTIF_ID, buildNotification(endsAt))
                scheduleAlarms(endsAt)
            }
        }
        return START_STICKY
    }

    private fun alarmPI(kind: String, reqCode: Int): PendingIntent {
        val i = Intent(this, RestAlarmReceiver::class.java).apply {
            putExtra(RestAlarmReceiver.EXTRA_KIND, kind)
        }
        return PendingIntent.getBroadcast(
            this, reqCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarms(endsAt: Long) {
        val am = getSystemService(AlarmManager::class.java)
        cancelAlarms()
        val now = System.currentTimeMillis()

        // Can we schedule exact alarms? (Android 12+ may deny it.)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true

        fun schedule(triggerAt: Long, pi: PendingIntent) {
            if (triggerAt <= now) return
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                // Fallback: inexact but still fires in Doze (may be delayed slightly).
                // The in-app timer is the primary path anyway when the app is open.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }

        // 3-2-1 buzzes
        schedule(endsAt - 3000, alarmPI(RestAlarmReceiver.KIND_BUZZ, REQ_CODES[0]))
        schedule(endsAt - 2000, alarmPI(RestAlarmReceiver.KIND_BUZZ, REQ_CODES[1]))
        schedule(endsAt - 1000, alarmPI(RestAlarmReceiver.KIND_BUZZ, REQ_CODES[2]))
        // finish
        schedule(endsAt.coerceAtLeast(now + 1), alarmPI(RestAlarmReceiver.KIND_FINISH, REQ_CODES[3]))
    }

    private fun cancelAlarms() {
        val am = getSystemService(AlarmManager::class.java)
        REQ_CODES.forEachIndexed { idx, code ->
            val kind = if (idx == 3) RestAlarmReceiver.KIND_FINISH else RestAlarmReceiver.KIND_BUZZ
            am.cancel(alarmPI(kind, code))
        }
    }

    private fun buildNotification(endsAt: Long): Notification {
        val tapIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Rest")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endsAt)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .setSilent(true)
            .build()
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Rest Timer", NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableVibration(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }
}
