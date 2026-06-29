package org.veilon.gymtracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class RestTimerService : Service() {

    private var timer: CountDownTimer? = null

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val NOTIF_ID = 4201
        const val EXTRA_ENDS_AT = "ends_at"
        const val ACTION_START = "start_rest"
        const val ACTION_STOP = "stop_rest"

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
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_START -> {
                val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, 0L)
                startCountdown(endsAt)
            }
        }
        return START_STICKY
    }

    private fun startCountdown(endsAt: Long) {
        createChannel()
        val remainingMs = (endsAt - System.currentTimeMillis()).coerceAtLeast(0L)

        // Show the foreground notification with a system-ticked countdown chronometer
        startForeground(NOTIF_ID, buildNotification(endsAt))

        timer?.cancel()
        var lastBuzzSecond = -1
        timer = object : CountDownTimer(remainingMs, 250) {
            override fun onTick(msLeft: Long) {
                val secLeft = Math.ceil(msLeft / 1000.0).toInt()
                // 3-2-1 buildup: single buzz at each of the last 3 seconds
                if (secLeft in 1..3 && secLeft != lastBuzzSecond) {
                    lastBuzzSecond = secLeft
                    vibrate(120)
                }
            }
            override fun onFinish() {
                // Finish: sound + double vibration
                playSound()
                vibrateDouble()
                stopSelf()
            }
        }.start()
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
            .setWhen(endsAt)  // chronometer counts down to this moment
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .setSilent(true)  // our buzzes/sound are manual; keep the notif itself quiet
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

    private fun vibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VibratorManager::class.java)).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(ms: Long) {
        vibrator().vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateDouble() {
        // pattern: wait 0, buzz 200, pause 120, buzz 200
        val pattern = longArrayOf(0, 200, 120, 200)
        vibrator().vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun playSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.rest_done)
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
        } catch (_: Exception) { /* sound is best-effort */ }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}