@file:Suppress("DEPRECATION")

package com.example.k60gostrix.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

class GlobalAudioEffectsController {
    private val audioSessionId = 0

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var dynamics: DynamicsProcessing? = null
    private var loudness: LoudnessEnhancer? = null
    @Suppress("DEPRECATION")
    private var virtualizer: Virtualizer? = null

    val hasEqualizer: Boolean
        get() = equalizer != null

    val hasBassBoost: Boolean
        get() = bassBoost != null

    val hasDynamicsProcessing: Boolean
        get() = dynamics != null

    val hasLoudnessEnhancer: Boolean
        get() = loudness != null

    val hasVirtualizer: Boolean
        get() = virtualizer != null

    fun init() {
        if (equalizer != null || bassBoost != null || dynamics != null || loudness != null || virtualizer != null) return

        equalizer = runCatching {
            Equalizer(0, audioSessionId).apply { enabled = true }
        }.getOrNull()

        bassBoost = runCatching {
            BassBoost(0, audioSessionId).apply { enabled = true }
        }.getOrNull()

        dynamics = runCatching {
            DynamicsProcessing(
                0,
                audioSessionId,
                DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1,
                    true,
                    1,
                    true,
                    1,
                    true,
                    1,
                    true
                ).build()
            ).apply { enabled = true }
        }.getOrNull()

        loudness = runCatching {
            LoudnessEnhancer(audioSessionId).apply { enabled = true }
        }.getOrNull()

        @Suppress("DEPRECATION")
        run {
            virtualizer = runCatching {
                Virtualizer(0, audioSessionId).apply { enabled = true }
            }.getOrNull()
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        dynamics?.release()
        dynamics = null
        loudness?.release()
        loudness = null
        virtualizer?.release()
        virtualizer = null
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        dynamics?.enabled = enabled
        loudness?.enabled = enabled
        virtualizer?.enabled = enabled
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        runCatching { eq.enabled = enabled }
    }

    fun getEqualizerEnabled(): Boolean {
        val eq = equalizer ?: return false
        return runCatching { eq.enabled }.getOrDefault(false)
    }

    fun getEqBandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 0

    fun getEqBandLevelRange(): Pair<Short, Short>? {
        val r = equalizer?.bandLevelRange ?: return null
        return r[0] to r[1]
    }

    fun getEqCenterFreqMilliHz(band: Int): Int? {
        val eq = equalizer ?: return null
        val b = band.toShort()
        return runCatching { eq.getCenterFreq(b).toInt() }.getOrNull()
    }

    fun getEqBandLevel(band: Int): Short? {
        val eq = equalizer ?: return null
        val b = band.toShort()
        return runCatching { eq.getBandLevel(b) }.getOrNull()
    }

    fun setEqBandLevel(band: Int, level: Short) {
        val eq = equalizer ?: return
        val b = band.toShort()
        runCatching { eq.setBandLevel(b, level) }
    }

    fun setBassStrength(strength: Short) {
        val bb = bassBoost ?: return
        runCatching { bb.setStrength(strength) }
    }

    fun getBassStrength(): Short = bassBoost?.roundedStrength ?: 0

    fun setLimiterEnabled(enabled: Boolean) {
        val dp = dynamics ?: return
        runCatching {
            val limiter = dp.getLimiterByChannelIndex(0)
            limiter.setEnabled(enabled)
            dp.setLimiterByChannelIndex(0, limiter)
        }
    }

    fun getLimiterEnabled(): Boolean {
        val dp = dynamics ?: return false
        return runCatching<Boolean> { dp.getLimiterByChannelIndex(0).isEnabled }.getOrDefault(false)
    }

    fun setLimiterPostGainDb(db: Float) {
        val dp = dynamics ?: return
        runCatching {
            val limiter = dp.getLimiterByChannelIndex(0)
            limiter.setPostGain(db)
            dp.setLimiterByChannelIndex(0, limiter)
        }
    }

    fun getLimiterPostGainDb(): Float {
        val dp = dynamics ?: return 0f
        return runCatching<Float> { dp.getLimiterByChannelIndex(0).postGain }.getOrDefault(0f)
    }

    fun setPreampDb(db: Float) {
        setLimiterPostGainDb(db)
    }

    fun getPreampDb(): Float {
        return getLimiterPostGainDb()
    }

    fun setLoudnessGainMilliBels(mb: Int) {
        val le = loudness ?: return
        runCatching { le.setTargetGain(mb) }
    }

    fun getLoudnessGainMilliBels(): Int {
        val le = loudness ?: return 0
        return runCatching { le.targetGain.toInt() }.getOrDefault(0)
    }

    fun setVirtualizerStrength(strength: Short) {
        val v = virtualizer ?: return
        @Suppress("DEPRECATION")
        runCatching { v.setStrength(strength) }
    }

    fun getVirtualizerStrength(): Short {
        val v = virtualizer ?: return 0
        @Suppress("DEPRECATION")
        return v.roundedStrength
    }
}
