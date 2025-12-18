package com.example.k60gostrix

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.k60gostrix.audio.GlobalAudioEffectsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AudioTuningScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val effects = remember { GlobalAudioEffectsController() }
    val enabled = remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        effects.init()
        effects.setEnabled(enabled.value)
        onDispose {
            effects.release()
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
            EqualizerControls(context, effects)
        }

        if (effects.hasLoudnessEnhancer) {
            HorizontalDivider()
            LoudnessControls(context, effects)
        }

        if (effects.hasVirtualizer) {
            HorizontalDivider()
            VirtualizerControls(context, effects)
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
private fun EqualizerControls(context: Context, effects: GlobalAudioEffectsController) {
    val bandCount = remember { mutableIntStateOf(effects.getEqBandCount()) }
    val range = remember { mutableStateOf(effects.getEqBandLevelRange()) }

    val minLevel = range.value?.first ?: (-1500).toShort()
    val maxLevel = range.value?.second ?: (1500).toShort()

    fun clampBandLevel(v: Float): Float = v.coerceIn(minLevel.toFloat(), maxLevel.toFloat())

    val prefs = remember {
        context.getSharedPreferences("global_dsp", Context.MODE_PRIVATE)
    }

    val eqEnabled = remember {
        mutableStateOf(prefs.getBoolean("eq_enabled", effects.getEqualizerEnabled()))
    }

    val presetMenuExpanded = remember { mutableStateOf(false) }
    val presetName = remember { mutableStateOf(prefs.getString("eq_preset", "Par défaut") ?: "Par défaut") }
    val autoHeadroom = remember { mutableStateOf(prefs.getBoolean("eq_auto_headroom", true)) }
    val preampDb = remember { mutableFloatStateOf(prefs.getFloat("eq_preamp_db", effects.getPreampDb())) }

    val bandStates = remember {
        List(bandCount.intValue) { b ->
            mutableFloatStateOf(effects.getEqBandLevel(b)?.toFloat() ?: 0f)
        }
    }

    LaunchedEffect(Unit) {
        effects.setEqualizerEnabled(eqEnabled.value)
        val saved = prefs.getString("eq_band_levels", null)
        if (!saved.isNullOrBlank()) {
            val parts = saved.split(',')
            for (b in 0 until minOf(bandStates.size, parts.size)) {
                val v = parts[b].toFloatOrNull() ?: continue
                val clamped = clampBandLevel(v)
                bandStates[b].floatValue = clamped
                effects.setEqBandLevel(b, clamped.toInt().toShort())
            }
        }

        effects.setPreampDb(preampDb.floatValue)
        if (autoHeadroom.value) {
            applyAutoHeadroom(bandStates, preampDb, effects)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "EQ activé")
            Switch(
                checked = eqEnabled.value,
                onCheckedChange = {
                    eqEnabled.value = it
                    effects.setEqualizerEnabled(it)
                    prefs.edit().putBoolean("eq_enabled", it).apply()
                }
            )
        }

        Text(text = "Preset: ${presetName.value}")
        Button(onClick = { presetMenuExpanded.value = true }, enabled = eqEnabled.value) {
            Text(text = "Choisir preset")
        }
        DropdownMenu(expanded = presetMenuExpanded.value, onDismissRequest = { presetMenuExpanded.value = false }) {
            listOf("Par défaut", "Bass boost", "Vocal", "Gaming", "Treble boost").forEach { name ->
                DropdownMenuItem(
                    text = { Text(text = name) },
                    onClick = {
                        presetMenuExpanded.value = false
                        presetName.value = name
                        applyEqPreset(name, bandStates, effects, ::clampBandLevel)
                        prefs.edit().putString("eq_preset", name).apply()
                        saveEqBands(prefs, bandStates)
                        if (autoHeadroom.value) {
                            applyAutoHeadroom(bandStates, preampDb, effects)
                            prefs.edit().putFloat("eq_preamp_db", preampDb.floatValue).apply()
                        }
                    }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Headroom auto")
            Switch(
                checked = autoHeadroom.value,
                onCheckedChange = {
                    autoHeadroom.value = it
                    prefs.edit().putBoolean("eq_auto_headroom", it).apply()
                    if (it) {
                        applyAutoHeadroom(bandStates, preampDb, effects)
                        prefs.edit().putFloat("eq_preamp_db", preampDb.floatValue).apply()
                    }
                }
            )
        }

        Text(text = "Préampli: ${"%.1f".format(preampDb.floatValue)} dB")
        Slider(
            value = preampDb.floatValue,
            onValueChange = {
                preampDb.floatValue = it
                effects.setPreampDb(it)
                prefs.edit().putFloat("eq_preamp_db", it).apply()
            },
            valueRange = -12f..6f,
            enabled = eqEnabled.value && !autoHeadroom.value
        )

        Button(
            onClick = {
                for (b in 0 until bandStates.size) {
                    bandStates[b].floatValue = 0f
                    effects.setEqBandLevel(b, 0)
                }
                presetName.value = "Par défaut"
                prefs.edit().putString("eq_preset", presetName.value).apply()
                saveEqBands(prefs, bandStates)
                if (autoHeadroom.value) {
                    applyAutoHeadroom(bandStates, preampDb, effects)
                    prefs.edit().putFloat("eq_preamp_db", preampDb.floatValue).apply()
                }
            }
        ) {
            Text(text = "Reset EQ")
        }

        for (band in 0 until bandCount.intValue) {
            val centerMilliHz = effects.getEqCenterFreqMilliHz(band)
            val labelHz = if (centerMilliHz != null) centerMilliHz / 1000 else 0

            Text(text = "Band ${band + 1} - ${labelHz} Hz")
            Slider(
                value = bandStates[band].floatValue,
                onValueChange = {
                    val clamped = clampBandLevel(it)
                    bandStates[band].floatValue = clamped
                    effects.setEqBandLevel(band, clamped.toInt().toShort())
                    saveEqBands(prefs, bandStates)
                    if (autoHeadroom.value) {
                        applyAutoHeadroom(bandStates, preampDb, effects)
                        prefs.edit().putFloat("eq_preamp_db", preampDb.floatValue).apply()
                    }
                },
                valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                enabled = eqEnabled.value
            )
        }
    }
}

private fun saveEqBands(prefs: android.content.SharedPreferences, bandStates: List<androidx.compose.runtime.MutableFloatState>) {
    val csv = bandStates.joinToString(",") { it.floatValue.toString() }
    prefs.edit().putString("eq_band_levels", csv).apply()
}

private fun applyAutoHeadroom(
    bandStates: List<androidx.compose.runtime.MutableFloatState>,
    preampDb: androidx.compose.runtime.MutableFloatState,
    effects: GlobalAudioEffectsController
) {
    val maxBoostMb = bandStates.maxOfOrNull { it.floatValue } ?: 0f
    val boostDb = (maxBoostMb / 100f).coerceAtLeast(0f)
    val target = (-boostDb).coerceIn(-12f, 0f)
    preampDb.floatValue = target
    effects.setPreampDb(target)
}

private fun applyEqPreset(
    name: String,
    bandStates: List<androidx.compose.runtime.MutableFloatState>,
    effects: GlobalAudioEffectsController,
    clamp: (Float) -> Float
) {
    val bandsMb: List<Float> = when (name) {
        "Bass boost" -> listOf(600f, 500f, 350f, 150f, 0f, -100f, -150f, -150f, -150f, -150f)
        "Treble boost" -> listOf(-200f, -150f, -100f, 0f, 100f, 250f, 400f, 500f, 600f, 650f)
        "Vocal" -> listOf(-150f, -100f, 0f, 200f, 350f, 350f, 200f, 0f, -100f, -150f)
        "Gaming" -> listOf(200f, 150f, 50f, -50f, -50f, 100f, 250f, 250f, 150f, 100f)
        else -> List(10) { 0f }
    }

    for (b in 0 until bandStates.size) {
        val centerMilliHz = effects.getEqCenterFreqMilliHz(b)
        val hz = ((centerMilliHz ?: 0) / 1000).coerceAtLeast(0)
        val idx = when {
            hz <= 45 -> 0
            hz <= 90 -> 1
            hz <= 180 -> 2
            hz <= 355 -> 3
            hz <= 710 -> 4
            hz <= 1400 -> 5
            hz <= 2800 -> 6
            hz <= 5600 -> 7
            hz <= 11000 -> 8
            else -> 9
        }
        val v = clamp(bandsMb[idx])
        bandStates[b].floatValue = v
        effects.setEqBandLevel(b, v.toInt().toShort())
    }
}

@Composable
private fun LoudnessControls(context: Context, effects: GlobalAudioEffectsController) {
    val prefs = remember { context.getSharedPreferences("global_dsp", Context.MODE_PRIVATE) }
    val gainMbState = remember { mutableIntStateOf(prefs.getInt("loudness_mb", effects.getLoudnessGainMilliBels())) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Loudness: ${gainMbState.intValue} mB")
        Slider(
            value = gainMbState.intValue.toFloat(),
            onValueChange = {
                gainMbState.intValue = it.toInt()
                effects.setLoudnessGainMilliBels(gainMbState.intValue)
                prefs.edit().putInt("loudness_mb", gainMbState.intValue).apply()
            },
            valueRange = 0f..1500f
        )
    }
}

@Composable
private fun VirtualizerControls(context: Context, effects: GlobalAudioEffectsController) {
    val prefs = remember { context.getSharedPreferences("global_dsp", Context.MODE_PRIVATE) }
    val strengthState = remember { mutableFloatStateOf(prefs.getFloat("virtualizer_strength", effects.getVirtualizerStrength().toFloat())) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Virtualizer: ${strengthState.floatValue.toInt()} / 1000")
        Slider(
            value = strengthState.floatValue,
            onValueChange = {
                strengthState.floatValue = it
                effects.setVirtualizerStrength(it.toInt().toShort())
                prefs.edit().putFloat("virtualizer_strength", it).apply()
            },
            valueRange = 0f..1000f
        )
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
