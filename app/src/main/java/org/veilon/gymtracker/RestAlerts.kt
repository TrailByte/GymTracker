package org.veilon.gymtracker

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object RestAlerts {
    private fun vibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun buzz(context: Context, ms: Long = 120) {
        vibrator(context).vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun finishAlert(context: Context) {
        // double vibration
        val pattern = longArrayOf(0, 200, 120, 200)
        vibrator(context).vibrate(VibrationEffect.createWaveform(pattern, -1))
        // sound
        try {
            val mp = MediaPlayer.create(context, R.raw.rest_done)
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
        } catch (_: Exception) { }
    }
}