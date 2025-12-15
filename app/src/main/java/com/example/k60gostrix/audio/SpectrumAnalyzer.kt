package com.example.k60gostrix.audio

import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper

class SpectrumAnalyzer {
    private val audioSessionId = 0

    private var visualizer: Visualizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start(onMagnitudes: (FloatArray) -> Unit) {
        if (visualizer != null) return

        val v = runCatching { Visualizer(audioSessionId) }.getOrNull() ?: return
        visualizer = v

        v.captureSize = Visualizer.getCaptureSizeRange()[1]

        v.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer,
                    waveform: ByteArray,
                    samplingRate: Int
                ) = Unit

                override fun onFftDataCapture(
                    visualizer: Visualizer,
                    fft: ByteArray,
                    samplingRate: Int
                ) {
                    val magnitudes = fftToMagnitudes(fft)
                    mainHandler.post { onMagnitudes(magnitudes) }
                }
            },
            Visualizer.getMaxCaptureRate(),
            false,
            true
        )

        v.enabled = true
    }

    fun stop() {
        val v = visualizer ?: return
        runCatching { v.enabled = false }
        runCatching { v.release() }
        visualizer = null
    }

    private fun fftToMagnitudes(fft: ByteArray): FloatArray {
        val n = fft.size / 2
        val out = FloatArray(n)
        var i = 0
        var outIndex = 0
        while (i + 1 < fft.size && outIndex < n) {
            val re = fft[i].toInt().toFloat()
            val im = fft[i + 1].toInt().toFloat()
            out[outIndex] = kotlin.math.sqrt(re * re + im * im)
            i += 2
            outIndex += 1
        }
        return out
    }
}
