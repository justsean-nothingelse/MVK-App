package com.seanpereyra.mvk.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Tiny sound engine for the randomizer. Tones are synthesized on the fly, so the app ships
 * no audio files. Every sound is a no-op when SFX are turned off in Settings.
 */
class Sfx(private val scope: CoroutineScope) {
    @Volatile var enabled: Boolean = true

    private val sampleRate = 44100

    /** Short dry "tick" as each character locks in. */
    fun tick() = play(freq = 1650.0, ms = 28, volume = 0.35, decay = 60.0)

    /** Brighter "clack" for the quick shuffling frames. */
    fun blip() = play(freq = 900.0 + Math.random() * 500, ms = 22, volume = 0.22, decay = 70.0)

    /** Happy two-note chime when the final code lands. */
    fun win() {
        if (!enabled) return
        scope.launch(Dispatchers.Default) {
            tone(784.0, 90, 0.4, 18.0)   // G5
            tone(1175.0, 150, 0.4, 14.0) // D6
        }
    }

    private fun play(freq: Double, ms: Int, volume: Double, decay: Double) {
        if (!enabled) return
        scope.launch(Dispatchers.Default) { tone(freq, ms, volume, decay) }
    }

    /** Blocking sine tone with an exponential fade so it never clicks. */
    private fun tone(freq: Double, ms: Int, volume: Double, decay: Double) {
        val count = sampleRate * ms / 1000
        val buf = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * decay)
            buf[i] = (sin(2 * PI * freq * t) * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            buf.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        runCatching {
            track.write(buf, 0, buf.size)
            track.setNotificationMarkerPosition(buf.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) { runCatching { t?.release() } }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        }.onFailure { runCatching { track.release() } }
    }
}
