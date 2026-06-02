/**
 * Compose 输入设置页路由内容编排。
 *
 * 归属模块：ui/settings/compose/screens
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.brycewg.asrkb.R
import com.brycewg.asrkb.asr.BluetoothRouteManager
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.settings.compose.components.SettingsLazyColumn
import com.brycewg.asrkb.ui.settings.compose.components.SettingsPreference
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsActionController
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.model.DropdownOption
import com.brycewg.asrkb.ui.settings.compose.model.SettingsEntry

internal typealias InputExplainedSwitchHandler = (
    current: Boolean,
    target: Boolean,
    titleRes: Int,
    offDescRes: Int,
    onDescRes: Int,
    preferenceKey: String,
    preCheck: ((Boolean) -> Boolean)?,
    onChanged: ((Boolean) -> Unit)?,
    write: (Boolean) -> Unit
) -> Unit

@Composable
internal fun InputSettingsRouteContent(
    uiMode: BibiUiMode,
    innerPadding: PaddingValues,
    scrollModifier: Modifier,
    prefs: Prefs,
    uiState: InputSettingsUiState,
    lastHapticLevel: Int,
    actions: SettingsActionController,
    onUiStateChange: (InputSettingsUiState) -> Unit,
    onLastHapticLevelChange: (Int) -> Unit,
    onPendingHeadsetPermissionChange: (Boolean) -> Unit,
    onRequestBluetoothConnectPermission: () -> Unit,
    onRefreshState: () -> Unit,
    onShowExternalAidlGuideDialog: () -> Unit,
    onShowExtensionButtonsPicker: () -> Unit,
    onApplyExplainedSwitch: InputExplainedSwitchHandler
) {
    val context = LocalContext.current
    val imeOptions = context.buildImeOptions()
    val languageOptions = context.languageOptions().mapIndexed { index, label ->
        DropdownOption(languageTagForIndex(index), label)
    }

    SettingsLazyColumn(
        uiMode = uiMode,
        modifier = Modifier.fillMaxSize(),
        miuixScrollModifier = scrollModifier,
        contentPadding = SettingsLayoutMetrics.pageContentPadding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.SectionSpacing)
    ) {
        item("behavior") {
            InputSection(uiMode = uiMode, titleRes = R.string.section_input_behavior) {
                InputExplainedSwitch(
                    id = "trim_trailing_punct",
                    titleRes = R.string.label_trim_trailing_punct,
                    checked = uiState.trimTrailingPunct,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.trimTrailingPunct,
                            target,
                            R.string.label_trim_trailing_punct,
                            R.string.feature_trim_trailing_punct_off_desc,
                            R.string.feature_trim_trailing_punct_on_desc,
                            "trim_trailing_punct_explained",
                            null,
                            null
                        ) { prefs.trimFinalTrailingPunct = it }
                    },
                    index = 0,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "mic_tap_toggle",
                    titleRes = R.string.label_mic_tap_toggle,
                    checked = uiState.micTapToggle,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.micTapToggle,
                            target,
                            R.string.label_mic_tap_toggle,
                            R.string.feature_mic_tap_toggle_off_desc,
                            R.string.feature_mic_tap_toggle_on_desc,
                            "mic_tap_toggle_explained",
                            null,
                            null
                        ) { prefs.micTapToggleEnabled = it }
                    },
                    index = 1,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "auto_start_recording_on_show",
                    titleRes = R.string.label_auto_start_recording_on_show,
                    checked = uiState.autoStartRecordingOnShow,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.autoStartRecordingOnShow,
                            target,
                            R.string.label_auto_start_recording_on_show,
                            R.string.feature_auto_start_recording_on_show_off_desc,
                            R.string.feature_auto_start_recording_on_show_on_desc,
                            "auto_start_recording_on_show_explained",
                            null,
                            null
                        ) { prefs.autoStartRecordingOnShow = it }
                    },
                    index = 2,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "hide_recent_task_card",
                    titleRes = R.string.label_hide_recent_task_card,
                    checked = uiState.hideRecentTasks,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.hideRecentTasks,
                            target,
                            R.string.label_hide_recent_task_card,
                            R.string.feature_hide_recent_tasks_off_desc,
                            R.string.feature_hide_recent_tasks_on_desc,
                            "hide_recent_tasks_explained",
                            null,
                            { applyExcludeFromRecents(context, it) }
                        ) { prefs.hideRecentTaskCard = it }
                    },
                    index = 3,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "fcitx5_return_on_switcher",
                    titleRes = R.string.label_fcitx5_return_on_switcher,
                    checked = uiState.fcitx5ReturnOnSwitcher,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.fcitx5ReturnOnSwitcher,
                            target,
                            R.string.label_fcitx5_return_on_switcher,
                            R.string.feature_fcitx5_return_on_switcher_off_desc,
                            R.string.feature_fcitx5_return_on_switcher_on_desc,
                            "fcitx5_return_on_switcher_explained",
                            null,
                            null
                        ) { prefs.fcitx5ReturnOnImeSwitch = it }
                    },
                    index = 4,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "return_prev_ime_on_hide",
                    titleRes = R.string.label_return_prev_ime_on_hide,
                    checked = uiState.returnPrevImeOnHide,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.returnPrevImeOnHide,
                            target,
                            R.string.label_return_prev_ime_on_hide,
                            R.string.feature_return_prev_ime_on_hide_off_desc,
                            R.string.feature_return_prev_ime_on_hide_on_desc,
                            "return_prev_ime_on_hide_explained",
                            null,
                            null
                        ) { prefs.returnPrevImeOnHide = it }
                    },
                    index = 5,
                    count = 7
                )
                SettingsPreference(
                    entry = SettingsEntry.Dropdown(
                        id = "ime_switch_target",
                        titleRes = R.string.label_ime_switch_target,
                        options = imeOptions.map { DropdownOption(it.id, it.label) },
                        selectedOptionId = prefs.imeSwitchTargetId.takeIf { targetId ->
                            imeOptions.any { it.id == targetId }
                        }.orEmpty(),
                        onSelectedOptionChange = { id ->
                            actions.hapticTap()
                            prefs.imeSwitchTargetId = id
                            onRefreshState()
                        }
                    ),
                    index = 6,
                    count = 7
                )
            }
        }

        item("audio") {
            InputSection(uiMode = uiMode, titleRes = R.string.section_audio_and_link) {
                InputExplainedSwitch(
                    id = "duck_media_on_record",
                    titleRes = R.string.label_audio_ducking_on_record,
                    checked = uiState.duckMediaOnRecord,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.duckMediaOnRecord,
                            target,
                            R.string.label_audio_ducking_on_record,
                            R.string.feature_duck_media_on_record_off_desc,
                            R.string.feature_duck_media_on_record_on_desc,
                            "duck_media_on_record_explained",
                            null,
                            null
                        ) { prefs.duckMediaOnRecordEnabled = it }
                    },
                    index = 0,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "offline_denoise",
                    titleRes = R.string.label_offline_denoise,
                    checked = uiState.offlineDenoise,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.offlineDenoise,
                            target,
                            R.string.label_offline_denoise,
                            R.string.feature_offline_denoise_off_desc,
                            R.string.feature_offline_denoise_on_desc,
                            "offline_denoise_explained",
                            null,
                            null
                        ) { prefs.offlineDenoiseEnabled = it }
                    },
                    index = 1,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "auto_cancel_empty_audio_input",
                    titleRes = R.string.label_auto_cancel_empty_audio_input,
                    checked = uiState.autoCancelEmptyAudioInput,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.autoCancelEmptyAudioInput,
                            target,
                            R.string.label_auto_cancel_empty_audio_input,
                            R.string.feature_auto_cancel_empty_audio_input_off_desc,
                            R.string.feature_auto_cancel_empty_audio_input_on_desc,
                            "auto_cancel_empty_audio_input_explained",
                            null,
                            null
                        ) { prefs.autoCancelEmptyAudioInputEnabled = it }
                    },
                    index = 2,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "auto_filter_silent_audio_segments",
                    titleRes = R.string.label_auto_filter_silent_audio_segments,
                    checked = uiState.autoFilterSilentAudioSegments,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.autoFilterSilentAudioSegments,
                            target,
                            R.string.label_auto_filter_silent_audio_segments,
                            R.string.feature_auto_filter_silent_audio_segments_off_desc,
                            R.string.feature_auto_filter_silent_audio_segments_on_desc,
                            "auto_filter_silent_audio_segments_explained",
                            null,
                            null
                        ) { prefs.autoFilterSilentAudioSegmentsEnabled = it }
                    },
                    index = 3,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "upload_audio_compression",
                    titleRes = R.string.label_upload_audio_compression,
                    checked = uiState.uploadAudioCompression,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.uploadAudioCompression,
                            target,
                            R.string.label_upload_audio_compression,
                            R.string.feature_upload_audio_compression_off_desc,
                            R.string.feature_upload_audio_compression_on_desc,
                            "upload_audio_compression_explained",
                            null,
                            null
                        ) { prefs.uploadAudioCompressionEnabled = it }
                    },
                    index = 4,
                    count = 7
                )
                InputExplainedSwitch(
                    id = "headset_mic_priority",
                    titleRes = R.string.label_headset_mic_priority,
                    checked = uiState.headsetMicPriority,
                    onToggle = { target ->
                        actions.hapticTap()
                        onApplyExplainedSwitch(
                            uiState.headsetMicPriority,
                            target,
                            R.string.label_headset_mic_priority,
                            R.string.feature_headset_mic_priority_off_desc,
                            R.string.feature_headset_mic_priority_on_desc,
                            "headset_mic_priority_explained",
                            { enable ->
                                if (enable && needsBluetoothConnectPermission(context)) {
                                    onPendingHeadsetPermissionChange(true)
                                    onRequestBluetoothConnectPermission()
                                    false
                                } else {
                                    true
                                }
                            },
                            { enabled ->
                                if (!enabled) {
                                    BluetoothRouteManager.onRecordingStopped(context)
                                    BluetoothRouteManager.setImeActive(context, false)
                                }
                            }
                        ) { prefs.headsetMicPriorityEnabled = it }
                    },
                    index = 5,
                    count = 7
                )
                SettingsPreference(
                    entry = SettingsEntry.Switch(
                        id = "external_aidl",
                        titleRes = R.string.label_external_ime_link_aidl,
                        checked = uiState.externalAidl,
                        onCheckedChange = { enabled ->
                            actions.hapticTap()
                            prefs.externalAidlEnabled = enabled
                            onUiStateChange(uiState.copy(externalAidl = enabled))
                            if (enabled) onShowExternalAidlGuideDialog()
                        }
                    ),
                    index = 6,
                    count = 7
                )
            }
        }

        item("ui") {
            InputSection(uiMode = uiMode, titleRes = R.string.section_ui_settings) {
                InputKeyboardHeightControl(
                    selectedTier = uiState.keyboardHeightTier,
                    uiMode = uiMode,
                    index = 0,
                    count = 6,
                    onSelected = { tier ->
                        actions.hapticTap()
                        prefs.keyboardHeightTier = tier
                        onUiStateChange(uiState.copy(keyboardHeightTier = prefs.keyboardHeightTier))
                        context.sendImeRefreshBroadcast()
                    }
                )
                InputSliderPreference(
                    titleRes = R.string.label_haptic_feedback_strength,
                    valueLabel = uiState.hapticFeedbackLabel,
                    value = uiState.hapticFeedbackLevel.toFloat(),
                    valueRange = Prefs.HAPTIC_FEEDBACK_LEVEL_OFF.toFloat()..Prefs.HAPTIC_FEEDBACK_LEVEL_HEAVY.toFloat(),
                    steps = 5,
                    uiMode = uiMode,
                    index = 1,
                    count = 6,
                    onValueChange = { value ->
                        val level = value.toInt().coerceIn(
                            Prefs.HAPTIC_FEEDBACK_LEVEL_OFF,
                            Prefs.HAPTIC_FEEDBACK_LEVEL_HEAVY
                        )
                        if (lastHapticLevel != level) {
                            onLastHapticLevelChange(level)
                            onUiStateChange(uiState.withHapticFeedbackLevel(context, level))
                        }
                    },
                    onValueChangeFinished = {
                        prefs.hapticFeedbackLevel = uiState.hapticFeedbackLevel
                        actions.hapticTap()
                        onRefreshState()
                    }
                )
                InputSliderPreference(
                    titleRes = R.string.label_keyboard_bottom_padding,
                    valueLabel = stringResource(
                        R.string.keyboard_bottom_padding_value,
                        uiState.keyboardBottomPaddingDp
                    ),
                    value = uiState.keyboardBottomPaddingDp.toFloat(),
                    valueRange = 0f..100f,
                    steps = 19,
                    uiMode = uiMode,
                    index = 2,
                    count = 6,
                    onValueChange = { value ->
                        val next = value.roundToStep(step = 5).toInt().coerceIn(0, 100)
                        if (next != uiState.keyboardBottomPaddingDp) {
                            onUiStateChange(uiState.copy(keyboardBottomPaddingDp = next))
                        }
                    },
                    onValueChangeFinished = {
                        prefs.keyboardBottomPaddingDp = uiState.keyboardBottomPaddingDp
                        context.sendImeRefreshBroadcast()
                        actions.hapticTap()
                        onRefreshState()
                    }
                )
                InputSliderPreference(
                    titleRes = R.string.label_waveform_sensitivity,
                    valueLabel = uiState.waveformSensitivity.toString(),
                    value = uiState.waveformSensitivity.toFloat(),
                    valueRange = 1f..10f,
                    steps = 8,
                    uiMode = uiMode,
                    index = 3,
                    count = 6,
                    onValueChange = { value ->
                        val next = value.roundToStep(step = 1).toInt().coerceIn(1, 10)
                        if (next != uiState.waveformSensitivity) {
                            onUiStateChange(uiState.copy(waveformSensitivity = next))
                        }
                    },
                    onValueChangeFinished = {
                        prefs.waveformSensitivity = uiState.waveformSensitivity
                        context.sendImeRefreshBroadcast()
                        actions.hapticTap()
                        onRefreshState()
                    }
                )
                SettingsPreference(
                    entry = SettingsEntry.Dropdown(
                        id = "app_language",
                        titleRes = R.string.label_language,
                        options = languageOptions,
                        selectedOptionId = normalizeLanguageTag(prefs.appLanguageTag),
                        onSelectedOptionChange = { tag ->
                            actions.hapticTap()
                            if (tag != prefs.appLanguageTag) {
                                prefs.appLanguageTag = tag
                                val locales = if (tag.isBlank()) {
                                    LocaleListCompat.getEmptyLocaleList()
                                } else {
                                    LocaleListCompat.forLanguageTags(tag)
                                }
                                AppCompatDelegate.setApplicationLocales(locales)
                            }
                            onRefreshState()
                        }
                    ),
                    index = 4,
                    count = 6
                )
                InputValuePreference(
                    titleRes = R.string.label_extension_buttons,
                    value = uiState.extensionButtonsLabel,
                    uiMode = uiMode,
                    index = 5,
                    count = 6,
                    onClick = {
                        actions.hapticTap()
                        onShowExtensionButtonsPicker()
                    }
                )
            }
        }
    }
}
