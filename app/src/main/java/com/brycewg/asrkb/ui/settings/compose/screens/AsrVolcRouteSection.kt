/**
 * ASR 设置页火山引擎配置路由区块。
 *
 * 归属模块：ui/settings/compose/screens
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.screens

import android.content.Context
import androidx.compose.runtime.Composable
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsUiState
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode

internal typealias AsrExplainedSwitchApplier = (
    target: Boolean,
    titleResId: Int,
    offDescResId: Int,
    onDescResId: Int,
    currentState: Boolean,
    preferenceKey: String,
    onConfirm: (Boolean) -> Unit
) -> Unit

@Composable
internal fun AsrVolcRouteSection(
    context: Context,
    uiMode: BibiUiMode,
    uiState: AsrSettingsUiState,
    appKey: String,
    accessKey: String,
    onAppKeyChange: (String) -> Unit,
    onAccessKeyChange: (String) -> Unit,
    onUpdateStreaming: (Boolean) -> Unit,
    onUpdateFileStandard: (Boolean) -> Unit,
    onUpdateModelV2: (Boolean) -> Unit,
    onUpdateNonstream: (Boolean) -> Unit,
    onUpdateDdc: (Boolean) -> Unit,
    onUpdateVad: (Boolean) -> Unit,
    applySwitch: AsrExplainedSwitchApplier,
    hapticTap: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    primaryIndexOffset: Int = 0,
    primaryGroupCount: Int? = null
) {
    VolcengineConfig(
        uiMode = uiMode,
        appKey = appKey,
        onAppKeyChange = onAppKeyChange,
        accessKey = accessKey,
        onAccessKeyChange = onAccessKeyChange,
        streaming = uiState.volcStreamingEnabled,
        onStreamingChange = { checked ->
            hapticTap()
            applySwitch(
                checked,
                R.string.label_volc_streaming,
                R.string.feature_volc_streaming_off_desc,
                R.string.feature_volc_streaming_on_desc,
                uiState.volcStreamingEnabled,
                "volc_streaming_explained",
                onUpdateStreaming
            )
        },
        fileStandard = uiState.volcFileStandardEnabled,
        onFileStandardChange = { checked ->
            hapticTap()
            applySwitch(
                checked,
                R.string.label_volc_file_standard,
                R.string.feature_volc_file_standard_off_desc,
                R.string.feature_volc_file_standard_on_desc,
                uiState.volcFileStandardEnabled,
                "volc_file_standard_explained",
                onUpdateFileStandard
            )
        },
        modelV2 = uiState.volcModelV2Enabled,
        onModelV2Change = { checked ->
            hapticTap()
            applySwitch(
                checked,
                R.string.label_volc_model_v2,
                R.string.feature_volc_model_v2_off_desc,
                R.string.feature_volc_model_v2_on_desc,
                uiState.volcModelV2Enabled,
                "volc_model_v2_explained",
                onUpdateModelV2
            )
        },
        nonstream = uiState.volcNonstreamEnabled,
        onNonstreamChange = { checked ->
            hapticTap()
            applySwitch(
                checked,
                R.string.label_volc_nonstream,
                R.string.feature_volc_nonstream_off_desc,
                R.string.feature_volc_nonstream_on_desc,
                uiState.volcNonstreamEnabled,
                "volc_nonstream_explained",
                onUpdateNonstream
            )
        },
        ddc = uiState.volcDdcEnabled,
        onDdcChange = { checked ->
            hapticTap()
            applySwitch(
                checked,
                R.string.label_volc_ddc,
                R.string.feature_volc_ddc_off_desc,
                R.string.feature_volc_ddc_on_desc,
                uiState.volcDdcEnabled,
                "volc_ddc_explained",
                onUpdateDdc
            )
        },
        vad = uiState.volcVadEnabled,
        onVadChange = { checked ->
            hapticTap()
            applySwitch(
                checked,
                R.string.label_volc_vad,
                R.string.feature_volc_vad_off_desc,
                R.string.feature_volc_vad_on_desc,
                uiState.volcVadEnabled,
                "volc_vad_explained",
                onUpdateVad
            )
        },
        selectedLanguage = uiState.volcLanguage,
        onLanguageSelected = { language ->
            hapticTap()
            onLanguageSelected(language)
        },
        primaryIndexOffset = primaryIndexOffset,
        primaryGroupCount = primaryGroupCount
    )
}
