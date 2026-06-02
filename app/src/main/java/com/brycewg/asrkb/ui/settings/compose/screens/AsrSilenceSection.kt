/**
 * Compose ASR 设置页的静音自动停止区块。
 *
 * 归属模块：ui/settings/compose/screens
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.screens

import androidx.compose.runtime.Composable
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import kotlin.math.roundToInt

@Composable
internal fun AsrSilenceSection(
    uiMode: BibiUiMode,
    enabled: Boolean,
    silenceWindowMs: Int,
    silenceSensitivity: Int,
    onEnabledChange: (Boolean) -> Unit,
    onWindowChange: (Int) -> Unit,
    onWindowFinished: () -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onSensitivityFinished: () -> Unit
) {
    AsrSection(uiMode = uiMode, titleRes = R.string.section_silence_autostop) {
        val itemCount = if (enabled) 3 else 1
        AsrSwitchPreference(
            id = "auto_stop_silence",
            titleRes = R.string.label_auto_stop_silence,
            checked = enabled,
            index = 0,
            count = itemCount,
            onCheckedChange = onEnabledChange
        )
        if (enabled) {
            AsrSliderPreference(
                titleRes = R.string.label_silence_window_ms,
                valueLabel = silenceWindowMs.toString(),
                value = silenceWindowMs.toFloat(),
                valueRange = 300f..5000f,
                steps = 46,
                uiMode = uiMode,
                highlightId = "silence_window_ms",
                index = 1,
                count = itemCount,
                onValueChange = { value -> onWindowChange(value.roundToNearestHundred()) },
                onValueChangeFinished = onWindowFinished
            )
            AsrSliderPreference(
                titleRes = R.string.label_silence_sensitivity,
                valueLabel = silenceSensitivity.toString(),
                value = silenceSensitivity.toFloat(),
                valueRange = 1f..10f,
                steps = 8,
                uiMode = uiMode,
                index = 2,
                count = itemCount,
                onValueChange = { value -> onSensitivityChange(value.toInt()) },
                onValueChangeFinished = onSensitivityFinished
            )
        }
    }
}

private fun Float.roundToNearestHundred(): Int = ((this / 100f).roundToInt() * 100).coerceIn(300, 5000)
