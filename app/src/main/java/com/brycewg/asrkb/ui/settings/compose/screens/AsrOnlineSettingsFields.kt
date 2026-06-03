/**
 * Compose ASR 设置页本地可编辑字段状态。
 *
 * 归属模块：ui/settings/compose/screens
 */
package com.brycewg.asrkb.ui.settings.compose.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsViewModel
import java.util.UUID

internal class AsrOnlineSettingsFields(
    private val prefs: Prefs
) {
    var volcAppKey by mutableStateOf(prefs.appKey)
    var volcAccessKey by mutableStateOf(prefs.accessKey)
    var dashApiKey by mutableStateOf(prefs.dashApiKey)
    var dashModel by mutableStateOf(normalizeDashModel(prefs.dashAsrModel))
    var dashPrompt by mutableStateOf(prefs.dashPrompt)
    var dashLanguage by mutableStateOf(prefs.dashLanguage)
    var dashRegion by mutableStateOf(normalizeDashRegion(prefs.dashRegion))
    var dashSemanticPunct by mutableStateOf(prefs.dashFunAsrSemanticPunctEnabled)
    var sfFreeAsrEnabled by mutableStateOf(prefs.sfFreeAsrEnabled)
    var sfFreeAsrModel by mutableStateOf(displaySfFreeAsrModel(prefs))
    var sfApiKey by mutableStateOf(prefs.sfApiKey)
    var sfModel by mutableStateOf(displaySfPaidModel(prefs))
    var elevenApiKey by mutableStateOf(prefs.elevenApiKey)
    var elevenStreaming by mutableStateOf(prefs.elevenStreamingEnabled)
    var elevenLanguageCode by mutableStateOf(prefs.elevenLanguageCode)
    var stepAudioApiKey by mutableStateOf(prefs.stepAudioApiKey)
    var stepAudioModel by mutableStateOf(displayStepAudioModel(prefs))
    var stepAudioLanguage by mutableStateOf(prefs.stepAudioLanguage.trim())
    var stepAudioUseItn by mutableStateOf(prefs.stepAudioUseItn)
    var zhipuApiKey by mutableStateOf(prefs.zhipuApiKey)
    var zhipuTemperature by mutableStateOf(prefs.zhipuTemperature.coerceIn(0f, 1f))
    var geminiApiKey by mutableStateOf(prefs.gemApiKey)
    var geminiEndpoint by mutableStateOf(prefs.gemEndpoint.ifBlank { Prefs.DEFAULT_GEM_ENDPOINT })
    var geminiModel by mutableStateOf(prefs.gemModel)
    var geminiPrompt by mutableStateOf(prefs.gemPrompt)
    var geminiDisableThinking by mutableStateOf(prefs.geminiDisableThinking)
    var openRouterEndpoint by mutableStateOf(
        prefs.openRouterAsrEndpoint.ifBlank { Prefs.DEFAULT_OPENROUTER_ASR_ENDPOINT }
    )
    var openRouterApiKey by mutableStateOf(prefs.openRouterAsrApiKey)
    var openRouterModel by mutableStateOf(
        prefs.openRouterAsrModel.ifBlank { Prefs.DEFAULT_OPENROUTER_ASR_MODEL }
    )
    var mimoApiKey by mutableStateOf(prefs.mimoAsrApiKey)
    var mimoEndpoint by mutableStateOf(
        prefs.mimoAsrEndpoint.ifBlank { Prefs.DEFAULT_MIMO_ASR_ENDPOINT }
    )
    var mimoEndpointPreset by mutableStateOf(prefs.mimoAsrEndpointPreset)
    var mimoLanguage by mutableStateOf(prefs.mimoAsrLanguage)
    var mimoPrompt by mutableStateOf(prefs.mimoAsrPrompt)
    var mimoModel by mutableStateOf(prefs.mimoAsrModel)
    var openAiProviders by mutableStateOf(prefs.getOpenAiAsrProviders())
    var openAiActiveProviderId by mutableStateOf(prefs.activeOpenAiAsrProviderId)
    var openAiProfileName by mutableStateOf(prefs.getActiveOpenAiAsrProvider()?.name.orEmpty())
    var openAiEndpoint by mutableStateOf(prefs.oaAsrEndpoint)
    var openAiApiKey by mutableStateOf(prefs.oaAsrApiKey)
    var openAiModel by mutableStateOf(prefs.oaAsrModel)
    var openAiStreaming by mutableStateOf(prefs.oaAsrStreamingEnabled)
    var openAiUsePrompt by mutableStateOf(prefs.oaAsrUsePrompt)
    var openAiPrompt by mutableStateOf(prefs.oaAsrPrompt)
    var openAiLanguage by mutableStateOf(prefs.oaAsrLanguage)
    var sonioxApiKey by mutableStateOf(prefs.sonioxApiKey)
    var sonioxStreaming by mutableStateOf(prefs.sonioxStreamingEnabled)
    var sonioxLanguages by mutableStateOf(prefs.getSonioxLanguages())
    var sonioxLanguageStrict by mutableStateOf(prefs.sonioxLanguageHintsStrict)

    fun refreshFromPrefs() {
        volcAppKey = prefs.appKey
        volcAccessKey = prefs.accessKey
        dashApiKey = prefs.dashApiKey
        dashModel = normalizeDashModel(prefs.dashAsrModel)
        dashPrompt = prefs.dashPrompt
        dashLanguage = prefs.dashLanguage
        dashRegion = normalizeDashRegion(prefs.dashRegion)
        dashSemanticPunct = prefs.dashFunAsrSemanticPunctEnabled
        sfFreeAsrEnabled = prefs.sfFreeAsrEnabled
        sfFreeAsrModel = displaySfFreeAsrModel(prefs)
        sfApiKey = prefs.sfApiKey
        sfModel = displaySfPaidModel(prefs)
        elevenApiKey = prefs.elevenApiKey
        elevenStreaming = prefs.elevenStreamingEnabled
        elevenLanguageCode = prefs.elevenLanguageCode
        stepAudioApiKey = prefs.stepAudioApiKey
        stepAudioModel = displayStepAudioModel(prefs)
        stepAudioLanguage = prefs.stepAudioLanguage.trim()
        stepAudioUseItn = prefs.stepAudioUseItn
        zhipuApiKey = prefs.zhipuApiKey
        zhipuTemperature = prefs.zhipuTemperature.coerceIn(0f, 1f)
        geminiApiKey = prefs.gemApiKey
        geminiEndpoint = prefs.gemEndpoint.ifBlank { Prefs.DEFAULT_GEM_ENDPOINT }
        geminiModel = prefs.gemModel
        geminiPrompt = prefs.gemPrompt
        geminiDisableThinking = prefs.geminiDisableThinking
        openRouterEndpoint = prefs.openRouterAsrEndpoint.ifBlank {
            Prefs.DEFAULT_OPENROUTER_ASR_ENDPOINT
        }
        openRouterApiKey = prefs.openRouterAsrApiKey
        openRouterModel = prefs.openRouterAsrModel.ifBlank {
            Prefs.DEFAULT_OPENROUTER_ASR_MODEL
        }
        mimoApiKey = prefs.mimoAsrApiKey
        mimoEndpoint = prefs.mimoAsrEndpoint.ifBlank { Prefs.DEFAULT_MIMO_ASR_ENDPOINT }
        mimoEndpointPreset = prefs.mimoAsrEndpointPreset
        mimoLanguage = prefs.mimoAsrLanguage
        mimoPrompt = prefs.mimoAsrPrompt
        mimoModel = prefs.mimoAsrModel
        refreshOpenAiFromPrefs()
        sonioxApiKey = prefs.sonioxApiKey
        sonioxStreaming = prefs.sonioxStreamingEnabled
        sonioxLanguages = prefs.getSonioxLanguages()
        sonioxLanguageStrict = prefs.sonioxLanguageHintsStrict
    }

    private fun refreshOpenAiFromPrefs() {
        openAiProviders = prefs.getOpenAiAsrProviders()
        openAiActiveProviderId = prefs.activeOpenAiAsrProviderId
        openAiProfileName = prefs.getActiveOpenAiAsrProvider()?.name.orEmpty()
        openAiEndpoint = prefs.oaAsrEndpoint
        openAiApiKey = prefs.oaAsrApiKey
        openAiModel = prefs.oaAsrModel
        openAiStreaming = prefs.oaAsrStreamingEnabled
        openAiUsePrompt = prefs.oaAsrUsePrompt
        openAiPrompt = prefs.oaAsrPrompt
        openAiLanguage = prefs.oaAsrLanguage
    }

    fun toRouteState(
        viewModel: AsrSettingsViewModel,
        hapticTap: () -> Unit,
        applyDashSemanticPunctSwitch: (Boolean) -> Unit,
        applyElevenStreamingSwitch: (Boolean) -> Unit,
        applyGeminiThinkingSwitch: (Boolean) -> Unit,
        applyOpenAiStreamingSwitch: (Boolean) -> Unit,
        applyOpenAiUsePromptSwitch: (Boolean) -> Unit,
        applySonioxStreamingSwitch: (Boolean) -> Unit,
        applySonioxLanguageStrictSwitch: (Boolean) -> Unit,
        openAiDefaultProfileName: (Int) -> String
    ): AsrOnlineSettingsRouteState = AsrOnlineSettingsRouteState(
        volcAppKey = volcAppKey,
        onVolcAppKeyChange = { value ->
            volcAppKey = value
            prefs.appKey = value
        },
        volcAccessKey = volcAccessKey,
        onVolcAccessKeyChange = { value ->
            volcAccessKey = value
            prefs.accessKey = value
        },
        dashApiKey = dashApiKey,
        onDashApiKeyChange = { value ->
            dashApiKey = value
            prefs.dashApiKey = value
        },
        dashModel = dashModel,
        dashPrompt = dashPrompt,
        onDashPromptChange = { value ->
            dashPrompt = value
            prefs.dashPrompt = value
        },
        dashLanguage = dashLanguage,
        onDashLanguageChange = { value ->
            dashLanguage = value
            prefs.dashLanguage = value
        },
        dashRegion = dashRegion,
        onDashRegionChange = { value ->
            dashRegion = value
            prefs.dashRegion = value
        },
        dashSemanticPunct = dashSemanticPunct,
        onDashSemanticPunctChange = applyDashSemanticPunctSwitch,
        sfFreeAsrEnabled = sfFreeAsrEnabled,
        onSfFreeAsrEnabledChange = { checked ->
            hapticTap()
            sfFreeAsrEnabled = checked
            prefs.sfFreeAsrEnabled = checked
        },
        sfFreeAsrModel = sfFreeAsrModel,
        sfApiKey = sfApiKey,
        onSfApiKeyChange = { value ->
            sfApiKey = value
            prefs.sfApiKey = value
        },
        sfModel = sfModel,
        elevenApiKey = elevenApiKey,
        onElevenApiKeyChange = { value ->
            elevenApiKey = value
            prefs.elevenApiKey = value
        },
        elevenStreaming = elevenStreaming,
        onElevenStreamingChange = { checked ->
            hapticTap()
            applyElevenStreamingSwitch(checked)
        },
        elevenLanguageCode = elevenLanguageCode,
        onElevenLanguageChange = { value ->
            elevenLanguageCode = value
            prefs.elevenLanguageCode = value
        },
        stepAudioApiKey = stepAudioApiKey,
        onStepAudioApiKeyChange = { value ->
            stepAudioApiKey = value
            prefs.stepAudioApiKey = value
        },
        stepAudioModel = stepAudioModel,
        stepAudioLanguage = stepAudioLanguage,
        onStepAudioLanguageChange = { value ->
            stepAudioLanguage = value
            prefs.stepAudioLanguage = value
        },
        stepAudioUseItn = stepAudioUseItn,
        onStepAudioUseItnChange = { checked ->
            hapticTap()
            stepAudioUseItn = checked
            prefs.stepAudioUseItn = checked
        },
        zhipuApiKey = zhipuApiKey,
        onZhipuApiKeyChange = { value ->
            zhipuApiKey = value
            prefs.zhipuApiKey = value
        },
        zhipuTemperature = zhipuTemperature,
        onZhipuTemperatureChange = { value ->
            val next = value.coerceIn(0f, 1f)
            zhipuTemperature = next
            prefs.zhipuTemperature = next
        },
        geminiApiKey = geminiApiKey,
        onGeminiApiKeyChange = { value ->
            geminiApiKey = value
            prefs.gemApiKey = value
        },
        geminiEndpoint = geminiEndpoint,
        onGeminiEndpointChange = { value ->
            geminiEndpoint = value
            prefs.gemEndpoint = value
        },
        geminiModel = geminiModel,
        onGeminiModelChange = { value ->
            geminiModel = value
            prefs.gemModel = value
        },
        geminiPrompt = geminiPrompt,
        onGeminiPromptChange = { value ->
            geminiPrompt = value
            prefs.gemPrompt = value
        },
        geminiDisableThinking = geminiDisableThinking,
        onGeminiDisableThinkingChange = { checked ->
            hapticTap()
            applyGeminiThinkingSwitch(checked)
        },
        openRouterEndpoint = openRouterEndpoint,
        onOpenRouterEndpointChange = { value ->
            openRouterEndpoint = value
            prefs.openRouterAsrEndpoint = value
        },
        openRouterApiKey = openRouterApiKey,
        onOpenRouterApiKeyChange = { value ->
            val key = value.removeBearerPrefix()
            openRouterApiKey = key
            prefs.openRouterAsrApiKey = key
        },
        openRouterModel = openRouterModel,
        onOpenRouterModelChange = { value ->
            openRouterModel = value
            prefs.openRouterAsrModel = value
        },
        mimoApiKey = mimoApiKey,
        onMimoApiKeyChange = { value ->
            mimoApiKey = value
            prefs.mimoAsrApiKey = value
        },
        mimoEndpoint = mimoEndpoint,
        onMimoEndpointChange = { value ->
            mimoEndpoint = value
            prefs.mimoAsrEndpoint = value
        },
        mimoEndpointPreset = mimoEndpointPreset,
        onMimoEndpointPresetChange = { value ->
            mimoEndpointPreset = value
            prefs.mimoAsrEndpointPreset = value
            val presetUrl = Prefs.MIMO_ENDPOINT_PRESETS[value]
            if (presetUrl != null) {
                mimoEndpoint = presetUrl
                prefs.mimoAsrEndpoint = presetUrl
            } else if (value == Prefs.MIMO_ENDPOINT_PRESET_AUTO) {
                // 自动匹配：根据 key 前缀决定 URL，UI 保留空值让引擎运行时解析
                mimoEndpoint = ""
                prefs.mimoAsrEndpoint = ""
            } else {
                mimoEndpoint = ""
                prefs.mimoAsrEndpoint = ""
            }
        },
        mimoLanguage = mimoLanguage,
        onMimoLanguageChange = { value ->
            mimoLanguage = value
            prefs.mimoAsrLanguage = value
        },
        mimoPrompt = mimoPrompt,
        onMimoPromptChange = { value ->
            mimoPrompt = value
            prefs.mimoAsrPrompt = value
        },
        mimoModel = mimoModel,
        onMimoModelChange = { value ->
            mimoModel = value
            prefs.mimoAsrModel = value
        },
        mimoPromptEnabled = !mimoModel.endsWith("-asr") && mimoModel.isNotBlank(),
        openAiProviders = openAiProviders,
        openAiActiveProviderId = openAiActiveProviderId,
        onOpenAiProviderSelected = { providerId ->
            if (prefs.selectOpenAiAsrProvider(providerId)) {
                refreshOpenAiFromPrefs()
                viewModel.refreshOpenAiProfileState()
            }
        },
        onOpenAiProviderAdded = {
            val list = prefs.getOpenAiAsrProviders().toMutableList()
            val nextIndex = list.size + 1
            val profile = Prefs.OpenAiAsrProvider(
                id = UUID.randomUUID().toString(),
                name = openAiDefaultProfileName(nextIndex),
                endpoint = Prefs.DEFAULT_OA_ASR_ENDPOINT,
                apiKey = "",
                model = Prefs.DEFAULT_OA_ASR_MODEL,
                streamingEnabled = true,
                usePrompt = false,
                prompt = "",
                language = ""
            )
            list.add(profile)
            prefs.setOpenAiAsrProviders(list)
            prefs.selectOpenAiAsrProvider(profile.id)
            refreshOpenAiFromPrefs()
            viewModel.refreshOpenAiProfileState()
        },
        onOpenAiProviderDeleted = {
            val list = prefs.getOpenAiAsrProviders().toMutableList()
            if (list.size <= 1) {
                false
            } else {
                val activeId = prefs.activeOpenAiAsrProviderId
                val idx = list.indexOfFirst { it.id == activeId }
                if (idx < 0) {
                    false
                } else {
                    list.removeAt(idx)
                    prefs.setOpenAiAsrProviders(list)
                    val nextActive = list.getOrNull(idx.coerceAtMost(list.lastIndex))
                        ?: list.firstOrNull()
                    if (nextActive != null) prefs.selectOpenAiAsrProvider(nextActive.id)
                    refreshOpenAiFromPrefs()
                    viewModel.refreshOpenAiProfileState()
                    true
                }
            }
        },
        openAiProfileName = openAiProfileName,
        onOpenAiProfileNameChange = { value ->
            openAiProfileName = value
            prefs.updateActiveOpenAiAsrProvider { it.copy(name = value) }
            openAiProviders = prefs.getOpenAiAsrProviders()
        },
        openAiEndpoint = openAiEndpoint,
        onOpenAiEndpointChange = { value ->
            openAiEndpoint = value
            prefs.oaAsrEndpoint = value
        },
        openAiApiKey = openAiApiKey,
        onOpenAiApiKeyChange = { value ->
            openAiApiKey = value
            prefs.oaAsrApiKey = value
        },
        openAiModel = openAiModel,
        onOpenAiModelChange = { value ->
            openAiModel = value
            prefs.oaAsrModel = value
        },
        openAiStreaming = openAiStreaming,
        onOpenAiStreamingChange = { checked ->
            hapticTap()
            applyOpenAiStreamingSwitch(checked)
        },
        openAiUsePrompt = openAiUsePrompt,
        onOpenAiUsePromptChange = { checked ->
            hapticTap()
            applyOpenAiUsePromptSwitch(checked)
        },
        openAiPrompt = openAiPrompt,
        onOpenAiPromptChange = { value ->
            openAiPrompt = value
            prefs.oaAsrPrompt = value
        },
        openAiLanguage = openAiLanguage,
        onOpenAiLanguageChange = { value ->
            openAiLanguage = value
            prefs.oaAsrLanguage = value
        },
        sonioxApiKey = sonioxApiKey,
        onSonioxApiKeyChange = { value ->
            sonioxApiKey = value
            prefs.sonioxApiKey = value
        },
        sonioxStreaming = sonioxStreaming,
        onSonioxStreamingChange = { checked ->
            hapticTap()
            applySonioxStreamingSwitch(checked)
        },
        sonioxLanguages = sonioxLanguages,
        sonioxLanguageStrict = sonioxLanguageStrict,
        onSonioxLanguageStrictChange = { checked ->
            hapticTap()
            applySonioxLanguageStrictSwitch(checked)
        }
    )
}

@Composable
internal fun rememberAsrOnlineSettingsFields(prefs: Prefs): AsrOnlineSettingsFields = remember(prefs) { AsrOnlineSettingsFields(prefs) }

private fun String.removeBearerPrefix(): String = replace(
    Regex("^Bearer\\s+", RegexOption.IGNORE_CASE),
    ""
).trim()
