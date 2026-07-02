package org.veilon.gymtracker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object RestAlerts {
    private fun vibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun buzz(context: Context, ms: Long = 120) {
        try {
            vibrator(context).vibrate(
                VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } catch (_: Exception) { }
    }

    /** Rest is over: sound + double vibration. Best-effort, never throws. */
    fun finishAlert(context: Context) {
        // --- Vibration (double buzz) ---
        try {
            val vib = vibrator(context)
            if (vib.hasVibrator()) {
                val pattern = longArrayOf(0, 250, 150, 250)
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vib.vibrate(effect)
            }
        } catch (_: Exception) { }

        // --- Sound ---
        try {
            val mp = MediaPlayer()
            val afd = context.resources.openRawResourceFd(R.raw.rest_done)
            if (afd != null) {
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setOnCompletionListener { it.release() }
                mp.prepare()
                mp.start()
            }
        } catch (_: Exception) { }
    }
}
