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

        private const val REQ_FINISH = 7000

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
            ACTION_START -> {
                val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, 0L)
                createChannel()
                startForeground(NOTIF_ID, buildNotification(endsAt))
                scheduleFinishAlarm(endsAt)
            }
            else -> {
                // ACTION_STOP, or a null/unknown redelivered intent:
                // tear down cleanly so we never leave a dangling notification.
                teardown()
                return START_NOT_STICKY
            }
        }
        // Never let Android resurrect this service on its own.
        return START_NOT_STICKY
    }

    private fun teardown() {
        cancelFinishAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun finishPI(): PendingIntent {
        val i = Intent(this, RestAlarmReceiver::class.java).apply {
            putExtra(RestAlarmReceiver.EXTRA_KIND, RestAlarmReceiver.KIND_FINISH)
        }
        return PendingIntent.getBroadcast(
            this, REQ_FINISH, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleFinishAlarm(endsAt: Long) {
        val am = getSystemService(AlarmManager::class.java)
        cancelFinishAlarm()
        val now = System.currentTimeMillis()
        val triggerAt = endsAt.coerceAtLeast(now + 1)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, finishPI())
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, finishPI())
        }
    }

    private fun cancelFinishAlarm() {
        getSystemService(AlarmManager::class.java).cancel(finishPI())
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
