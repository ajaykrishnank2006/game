package com.example.game

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.sin

object GameEffectsManager {

    private val audioExecutor = Executors.newSingleThreadExecutor()

    fun playTone(
        frequency: Double, 
        durationMs: Int, 
        volume: Float = 0.3f, 
        endFrequency: Double? = null
    ) {
        audioExecutor.submit {
            try {
                val sampleRate = 22050
                val numSamples = (durationMs * sampleRate / 1000)
                val samples = ShortArray(numSamples)
                
                var phase = 0.0
                val startF = frequency
                val endF = endFrequency ?: frequency

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val currFreq = startF + (endF - startF) * progress
                    phase += 2.0 * Math.PI * currFreq / sampleRate
                    
                    val value = sin(phase)
                    
                    // Simple linear fade envelope to prevent clicking/popping at start and end
                    val envelope = when {
                        i < numSamples * 0.1 -> i / (numSamples * 0.1)
                        i > numSamples * 0.8 -> (numSamples - i) / (numSamples * 0.2)
                        else -> 1.0
                    }
                    
                    samples[i] = (value * 32767.0 * volume * envelope).toInt().toShort()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    numSamples * 2,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()

                // Schedule release after the sound completes
                Thread {
                    try {
                        Thread.sleep(durationMs.toLong() + 30)
                        audioTrack.stop()
                        audioTrack.release()
                    } catch (ignored: Exception) {}
                }.start()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playSuccess() {
        playTone(660.0, 70, 0.22f)
        Thread {
            try {
                Thread.sleep(80)
                playTone(990.0, 140, 0.22f)
            } catch (ignored: Exception) {}
        }.start()
    }

    fun playMiss() {
        playTone(320.0, 240, 0.28f, endFrequency = 110.0)
    }

    fun playTick() {
        playTone(1800.0, 7, 0.08f)
    }

    fun playStart() {
        playTone(523.25, 60, 0.16f) // C5
        Thread {
            try {
                Thread.sleep(70)
                playTone(659.25, 60, 0.16f) // E5
                Thread.sleep(70)
                playTone(783.99, 120, 0.20f) // G5
            } catch (ignored: Exception) {}
        }.start()
    }

    fun triggerSuccessHaptic(context: Context) {
        vibrate(context, longArrayOf(0, 35))
    }

    fun triggerMissHaptic(context: Context) {
        vibrate(context, longArrayOf(0, 80, 50, 120))
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
