/**
 * Compose AI 设置页路由内容编排。
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
import androidx.compose.ui.res.stringResource
import com.brycewg.asrkb.R
import com.brycewg.asrkb.asr.LlmVendor
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.settings.ai.AiPostSettingsViewModel
import com.brycewg.asrkb.ui.settings.compose.components.SettingsLazyColumn
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics

@Composable
internal fun AiSettingsRouteContent(
    uiMode: BibiUiMode,
    innerPadding: PaddingValues,
    scrollModifier: Modifier,
    prefs: Prefs,
    viewModel: AiPostSettingsViewModel,
    routeState: AiSettingsRouteState,
    routeActions: AiSettingsRouteActions
) {
    val untitledProfile = stringResource(R.string.untitled_profile)
    val untitledPreset = stringResource(R.string.untitled_preset)

    with(routeState) {
        with(routeActions) {
            SettingsLazyColumn(
                uiMode = uiMode,
                modifier = Modifier.fillMaxSize(),
                miuixScrollModifier = scrollModifier,
                contentPadding = SettingsLayoutMetrics.pageContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.SectionSpacing)
            ) {
                item("post_process_scope") {
                    AiPostProcessSection(
                        uiMode = uiMode,
                        postProcessEnabled = postProcessEnabled,
                        typewriterEnabled = typewriterEnabled,
                        aiEditPreferLastAsr = aiEditPreferLastAsr,
                        skipUnderChars = skipUnderChars,
                        onPostProcessChange = { checked ->
                            onHapticTap()
                            onShowExplainedSwitch(
                                postProcessEnabled,
                                checked,
                                R.string.label_ai_post_process_enabled,
                                R.string.feature_ai_post_process_off_desc,
                                R.string.feature_ai_post_process_on_desc,
                                "ai_post_process_enabled_explained",
                                { value ->
                                    prefs.postProcessEnabled = value
                                    onPostProcessEnabledChange(value)
                                },
                                { onSendRefreshBroadcast() }
                            )
                        },
                        onTypewriterChange = { checked ->
                            onHapticTap()
                            onShowExplainedSwitch(
                                typewriterEnabled,
                                checked,
                                R.string.label_postproc_typewriter_enabled,
                                R.string.feature_postproc_typewriter_off_desc,
                                R.string.feature_postproc_typewriter_on_desc,
                                "postproc_typewriter_explained",
                                { value ->
                                    prefs.postprocTypewriterEnabled = value
                                    onTypewriterEnabledChange(value)
                                },
                                {}
                            )
                        },
                        onAiEditPreferLastAsrChange = { checked ->
                            onHapticTap()
                            onShowExplainedSwitch(
                                aiEditPreferLastAsr,
                                checked,
                                R.string.label_ai_edit_default_use_last_asr,
                                R.string.feature_ai_edit_default_use_last_asr_off_desc,
                                R.string.feature_ai_edit_default_use_last_asr_on_desc,
                                "ai_edit_default_use_last_asr_explained",
                                { value ->
                                    prefs.aiEditDefaultToLastAsr = value
                                    onAiEditPreferLastAsrChange(value)
                                },
                                {}
                            )
                        },
                        onSkipUnderCharsChange = { next ->
                            onSkipUnderCharsChange(next)
                            prefs.postprocSkipUnderChars = next
                        },
                        onSkipUnderCharsFinished = { onHapticTap() }
                    )
                }

                item("post_process_model") {
                    AiPostProcessModelSection(
                        uiMode = uiMode,
                        selectedVendor = selectedVendor,
                        selectedVendorName = stringResource(selectedVendor.displayNameResId),
                        builtinConfig = builtinConfig,
                        activeProfile = activeProfile,
                        sfUseFreeService = sfUseFreeService,
                        sfApiKey = sfApiKey,
                        sfModel = sfModel,
                        sfReasoningEnabled = sfReasoningEnabled,
                        sfReasoningOnJson = sfReasoningOnJson,
                        sfReasoningOffJson = sfReasoningOffJson,
                        sfTemperature = sfTemperature,
                        sfPresetModels = sfPresetModels,
                        sfStaticModels = sfStaticModels,
                        builtinPresetModels = builtinPresetModels,
                        sfCustomModelInputVisible = sfCustomModelInputVisible,
                        builtinCustomModelInputVisible = builtinCustomModelInputVisible,
                        builtinReasoningOnJson = builtinReasoningOnJson,
                        builtinReasoningOffJson = builtinReasoningOffJson,
                        customModelInputVisible = customModelInputVisible,
                        focusProfileNameAfterAdd = focusProfileNameAfterAdd,
                        llmTestRunning = llmTestRunning,
                        sfActions = AiVendorActions(
                            onToggleSfFree = { checked ->
                                onHapticTap()
                                prefs.sfFreeLlmUsePaidKey = !checked
                                onRefreshSfState()
                            },
                            onSfApiKeyChange = { value ->
                                onSfApiKeyChange(value)
                                prefs.setLlmVendorApiKey(LlmVendor.SF_FREE, value)
                            },
                            onShowModelDialog = {
                                onHapticTap()
                                onShowSfModelDialog()
                            },
                            onCustomModelChange = { value ->
                                onSfCustomModelInputVisibleChange(true)
                                onSfModelChange(value)
                                if (value.isNotBlank()) {
                                    if (prefs.sfFreeLlmUsePaidKey) {
                                        prefs.setLlmVendorModel(LlmVendor.SF_FREE, value)
                                    } else {
                                        prefs.sfFreeLlmModel = value
                                    }
                                }
                            },
                            onFetchModels = {
                                onHapticTap()
                                if (prefs.sfFreeLlmUsePaidKey) {
                                    onFetchModels(
                                        LlmVendor.SF_FREE.endpoint,
                                        prefs.getLlmVendorApiKey(LlmVendor.SF_FREE)
                                    ) { models -> onShowBuiltinModelsPicker(LlmVendor.SF_FREE, models) }
                                }
                            },
                            onReasoningChange = { checked ->
                                onHapticTap()
                                onSfReasoningEnabledChange(checked)
                                prefs.setLlmVendorReasoningEnabled(LlmVendor.SF_FREE, checked)
                            },
                            onReasoningOnJsonChange = { value ->
                                onSfReasoningOnJsonChange(value)
                                prefs.setLlmVendorReasoningParamsOnJson(LlmVendor.SF_FREE, value)
                            },
                            onReasoningOffJsonChange = { value ->
                                onSfReasoningOffJsonChange(value)
                                prefs.setLlmVendorReasoningParamsOffJson(LlmVendor.SF_FREE, value)
                            },
                            onTemperatureChange = { value ->
                                val next = value.coerceIn(0f, 2f)
                                onSfTemperatureChange(next)
                                prefs.setLlmVendorTemperature(LlmVendor.SF_FREE, next)
                            },
                            onOpenRegister = {
                                onHapticTap()
                                onOpenUrl(LlmVendor.SF_FREE.registerUrl)
                            },
                            onTestCall = {
                                onHapticTap()
                                onTestLlmCall()
                            }
                        ),
                        onChooseVendor = {
                            onHapticTap()
                            onShowVendorSelectionDialog()
                        },
                        onFocusedProfileName = { onFocusProfileNameAfterAddChange(false) },
                        onChooseProfile = {
                            onHapticTap()
                            onShowProfileDialog()
                        },
                        onProfileNameChange = { value ->
                            viewModel.updateActiveLlmProvider(prefs) { it.copy(name = value) }
                        },
                        onCustomEndpointChange = { value ->
                            viewModel.updateActiveLlmProvider(prefs) { it.copy(endpoint = value) }
                        },
                        onCustomApiKeyChange = { value ->
                            viewModel.updateActiveLlmProvider(prefs) { it.copy(apiKey = value) }
                        },
                        onChooseCustomModel = {
                            onHapticTap()
                            onShowCustomModelDialog()
                        },
                        onCustomModelChange = { value ->
                            viewModel.updateActiveLlmProvider(prefs) { it.copy(model = value) }
                        },
                        onFetchCustomModels = {
                            onHapticTap()
                            val provider = activeProfile
                            onFetchModels(
                                provider?.endpoint?.ifBlank { prefs.llmEndpoint } ?: prefs.llmEndpoint,
                                provider?.apiKey?.ifBlank { prefs.llmApiKey } ?: prefs.llmApiKey
                            ) { models -> onShowCustomModelsPicker(models) }
                        },
                        onCustomReasoningChange = { checked ->
                            onHapticTap()
                            viewModel.updateActiveLlmProvider(prefs) {
                                it.copy(enableReasoning = checked)
                            }
                        },
                        onCustomReasoningOnJsonChange = { value ->
                            viewModel.updateActiveLlmProvider(prefs) {
                                it.copy(reasoningParamsOnJson = value)
                            }
                        },
                        onCustomReasoningOffJsonChange = { value ->
                            viewModel.updateActiveLlmProvider(prefs) {
                                it.copy(reasoningParamsOffJson = value)
                            }
                        },
                        onCustomTemperatureChange = { value ->
                            val next = value.coerceIn(0f, 2f)
                            viewModel.updateActiveLlmProvider(prefs) {
                                it.copy(temperature = next)
                            }
                        },
                        onAddProfile = {
                            onHapticTap()
                            if (viewModel.addLlmProvider(prefs, untitledProfile)) {
                                onFocusProfileNameAfterAddChange(true)
                                onMessage(R.string.toast_llm_profile_added)
                            }
                        },
                        onDeleteProfile = {
                            onHapticTap()
                            if (viewModel.deleteActiveLlmProvider(prefs)) {
                                onMessage(R.string.toast_llm_profile_deleted)
                            }
                        },
                        onBuiltinApiKeyChange = { value -> viewModel.updateBuiltinApiKey(prefs, value) },
                        onChooseBuiltinModel = {
                            onHapticTap()
                            onShowBuiltinModelDialog()
                        },
                        onBuiltinCustomModelChange = { value ->
                            onBuiltinCustomModelInputVisibleChange(true)
                            viewModel.updateBuiltinModel(prefs, value)
                        },
                        onFetchBuiltinModels = {
                            onHapticTap()
                            onFetchModels(
                                selectedVendor.endpoint,
                                prefs.getLlmVendorApiKey(selectedVendor)
                            ) { models -> onShowBuiltinModelsPicker(selectedVendor, models) }
                        },
                        onBuiltinReasoningChange = { checked ->
                            onHapticTap()
                            viewModel.updateBuiltinReasoningEnabled(prefs, checked)
                        },
                        onBuiltinReasoningOnJsonChange = { value ->
                            onBuiltinReasoningOnJsonChange(value)
                            prefs.setLlmVendorReasoningParamsOnJson(selectedVendor, value)
                        },
                        onBuiltinReasoningOffJsonChange = { value ->
                            onBuiltinReasoningOffJsonChange(value)
                            prefs.setLlmVendorReasoningParamsOffJson(selectedVendor, value)
                        },
                        onBuiltinTemperatureChange = { value ->
                            val next = value.coerceIn(
                                selectedVendor.temperatureMin,
                                selectedVendor.temperatureMax
                            )
                            viewModel.updateBuiltinTemperature(prefs, next)
                        },
                        onOpenBuiltinRegister = {
                            onHapticTap()
                            onOpenUrl(selectedVendor.registerUrl)
                        },
                        onTestCall = {
                            onHapticTap()
                            onTestLlmCall()
                        }
                    )
                }

                item("prompt_presets") {
                    AiPromptPresetRouteSection(
                        uiMode = uiMode,
                        preset = activePromptPreset,
                        focusTitleAfterAdd = focusPromptTitleAfterAdd,
                        onFocusedTitle = { onFocusPromptTitleAfterAddChange(false) },
                        onChoosePreset = {
                            onHapticTap()
                            onShowPromptPresetDialog()
                        },
                        onTitleChange = { value ->
                            viewModel.updateActivePromptPreset(prefs) { it.copy(title = value) }
                        },
                        onContentChange = { value ->
                            viewModel.updateActivePromptPreset(prefs) { it.copy(content = value) }
                        },
                        onAddPreset = {
                            onHapticTap()
                            if (viewModel.addPromptPreset(prefs, untitledPreset, "")) {
                                onFocusPromptTitleAfterAddChange(true)
                                onMessage(R.string.toast_preset_added)
                            }
                        },
                        onDeletePreset = {
                            onHapticTap()
                            if (viewModel.deleteActivePromptPreset(prefs)) {
                                onMessage(R.string.toast_preset_deleted)
                            }
                        }
                    )
                }
            }
        }
    }
}
