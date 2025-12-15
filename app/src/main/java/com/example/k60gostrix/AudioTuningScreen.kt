package com.example.k60gostrix

import android.Manifest
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.k60gostrix.audio.GlobalAudioEffectsController
import com.example.k60gostrix.audio.SpectrumAnalyzer
import kotlin.math.max

@Composable
fun AudioTuningScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val effects = remember { GlobalAudioEffectsController() }
    val enabled = remember { mutableStateOf(true) }

    val permissionGranted = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted.value = granted
    }

    val spectrum = remember { SpectrumAnalyzer() }
    val magnitudesState = remember { mutableStateOf(FloatArray(0)) }
    val latestOnMagnitudes = rememberUpdatedState<(FloatArray) -> Unit> { mags ->
        magnitudesState.value = mags
    }

    DisposableEffect(Unit) {
        effects.init()
        effects.setEnabled(enabled.value)
        onDispose {
            spectrum.stop()
            effects.release()
        }
    }

    LaunchedEffect(permissionGranted.value) {
        if (permissionGranted.value) {
            spectrum.start { mags -> latestOnMagnitudes.value(mags) }
        } else {
            spectrum.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutputRouteInfo(context)
        HorizontalDivider()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Global DSP")
            Switch(
                checked = enabled.value,
                onCheckedChange = {
                    enabled.value = it
                    effects.setEnabled(it)
                }
            )
        }

        Text(text = "Equalizer: ${effects.hasEqualizer}")
        if (effects.hasEqualizer) {
            EqualizerControls(effects)
        }

        HorizontalDivider()

        Text(text = "BassBoost: ${effects.hasBassBoost}")
        if (effects.hasBassBoost) {
            BassBoostControls(effects)
        }

        HorizontalDivider()

        Text(text = "Limiter (DynamicsProcessing): ${effects.hasDynamicsProcessing}")
        if (effects.hasDynamicsProcessing) {
            LimiterControls(effects)
        }

        HorizontalDivider()

        Text(text = "Analyse spectre (Visualizer)")
        if (!permissionGranted.value) {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(text = "Autoriser microphone (requis pour l'analyse)")
            }
        }
        SpectrumGraph(magnitudesState.value)
    }
}

@Composable
private fun OutputRouteInfo(context: Context) {
    val devicesText = remember {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        devices.joinToString(separator = "\n") { d ->
            val name = d.productName?.toString().orEmpty()
            val type = audioDeviceTypeToString(d.type)
            val addr = d.address.orEmpty()
            "- $type | $name | $addr"
        }.ifBlank { "- (aucun périphérique de sortie détecté)" }
    }

    Text(text = "Sortie audio détectée (diagnostic)")
    Text(text = devicesText)
}

private fun audioDeviceTypeToString(type: Int): String {
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "SPEAKER"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT_A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT_SCO"
        else -> "TYPE_$type"
    }
}

@Composable
private fun EqualizerControls(effects: GlobalAudioEffectsController) {
    val bandCount = remember { mutableIntStateOf(effects.getEqBandCount()) }
    val range = remember { mutableStateOf(effects.getEqBandLevelRange()) }

    val minLevel = range.value?.first ?: (-1500).toShort()
    val maxLevel = range.value?.second ?: (1500).toShort()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (band in 0 until bandCount.intValue) {
            val centerMilliHz = effects.getEqCenterFreqMilliHz(band)
            val labelHz = if (centerMilliHz != null) centerMilliHz / 1000 else 0

            val initial = effects.getEqBandLevel(band)?.toFloat() ?: 0f
            val levelState = remember(band) { mutableFloatStateOf(initial) }

            Text(text = "Band ${band + 1} - ${labelHz} Hz")
            Slider(
                value = levelState.floatValue,
                onValueChange = {
                    levelState.floatValue = it
                    effects.setEqBandLevel(band, it.toInt().toShort())
                },
                valueRange = minLevel.toFloat()..maxLevel.toFloat()
            )
        }
    }
}

@Composable
private fun BassBoostControls(effects: GlobalAudioEffectsController) {
    val strengthState = remember { mutableFloatStateOf(effects.getBassStrength().toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Strength: ${strengthState.floatValue.toInt()} / 1000")
        Slider(
            value = strengthState.floatValue,
            onValueChange = {
                strengthState.floatValue = it
                effects.setBassStrength(it.toInt().toShort())
            },
            valueRange = 0f..1000f
        )
    }
}

@Composable
private fun LimiterControls(effects: GlobalAudioEffectsController) {
    val enabledState = remember { mutableStateOf(effects.getLimiterEnabled()) }
    val postGainState = remember { mutableFloatStateOf(effects.getLimiterPostGainDb()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Enabled")
            Switch(
                checked = enabledState.value,
                onCheckedChange = {
                    enabledState.value = it
                    effects.setLimiterEnabled(it)
                }
            )
        }

        Text(text = "Post gain: ${"%.1f".format(postGainState.floatValue)} dB")
        Slider(
            value = postGainState.floatValue,
            onValueChange = {
                postGainState.floatValue = it
                effects.setLimiterPostGainDb(it)
            },
            valueRange = -12f..12f
        )
    }
}

@Composable
private fun SpectrumGraph(magnitudes: FloatArray) {
    val bars = 48
    val data = if (magnitudes.isNotEmpty()) magnitudes else FloatArray(bars)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val barWidth = size.width / bars
        val maxMag = max(1f, data.maxOrNull() ?: 1f)
        for (i in 0 until bars) {
            val idx = ((i.toFloat() / bars) * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
            val mag = data[idx] / maxMag
            val h = mag.coerceIn(0f, 1f) * size.height
            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = androidx.compose.ui.geometry.Offset(i * barWidth, size.height - h),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, h)
            )
        }
    }
}
