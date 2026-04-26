/**
 * ASR 设置页通用渲染区块。
 *
 * 归属模块：ui/settings/asr/sections
 */
package com.brycewg.asrkb.ui.settings.asr.sections

import android.view.View
import android.widget.TextView
import com.brycewg.asrkb.R
import com.brycewg.asrkb.asr.AsrVendor
import com.brycewg.asrkb.ui.AsrVendorUi
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsBinding
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsUiState
import com.google.android.material.materialswitch.MaterialSwitch

internal class AsrSettingsUiRendererSection : AsrSettingsSection {
    private data class ViewCache(
        val vendorSummary: TextView,
        val backupVendorSummary: TextView,
        val groupVolc: View,
        val groupSf: View,
        val groupEleven: View,
        val groupOpenAi: View,
        val groupDashScope: View,
        val groupGemini: View,
        val groupSoniox: View,
        val groupStepAudio: View,
        val groupZhipu: View,
        val groupSenseVoice: View,
        val groupFunAsrNano: View,
        val groupQwen3Asr: View,
        val groupParakeet: View,
        val groupFireRedAsr: View,
        val groupParaformer: View,
        val silenceWindowLabel: View,
        val silenceWindowSlider: View,
        val silenceSensitivityLabel: View,
        val silenceSensitivitySlider: View,
        val sfOmniPrompt: View,
        val openAiPrompt: View,
        val switchOpenAiStreaming: MaterialSwitch,
        val switchVolcFileStandard: MaterialSwitch,
        val switchVolcModelV2: MaterialSwitch,
        val switchVolcVad: View,
        val tvVolcLanguageValue: View,
        val tvVolcLanguageLabel: View,
        val switchVolcNonstream: View,
        val vendorGroups: List<Pair<AsrVendor, View>>
    )

    private lateinit var cache: ViewCache
    private var vendorOrder: List<AsrVendor> = emptyList()
    private var vendorItems: List<String> = emptyList()
    private var lastState: AsrSettingsUiState? = null
    private var lastBackupVendor: AsrVendor? = null

    override fun bind(binding: AsrSettingsBinding) {
        vendorOrder = AsrVendorUi.ordered()
        vendorItems = AsrVendorUi.names(binding.activity)
        cache = ViewCache(
            vendorSummary = binding.view(R.id.tvAsrVendorValue),
            backupVendorSummary = binding.view(R.id.tvBackupAsrVendorValue),
            groupVolc = binding.view(R.id.groupVolc),
            groupSf = binding.view(R.id.groupSf),
            groupEleven = binding.view(R.id.groupEleven),
            groupOpenAi = binding.view(R.id.groupOpenAI),
            groupDashScope = binding.view(R.id.groupDashScope),
            groupGemini = binding.view(R.id.groupGemini),
            groupSoniox = binding.view(R.id.groupSoniox),
            groupStepAudio = binding.view(R.id.groupStepAudio),
            groupZhipu = binding.view(R.id.groupZhipu),
            groupSenseVoice = binding.view(R.id.groupSenseVoice),
            groupFunAsrNano = binding.view(R.id.groupFunAsrNano),
            groupQwen3Asr = binding.view(R.id.groupQwen3Asr),
            groupParakeet = binding.view(R.id.groupParakeet),
            groupFireRedAsr = binding.view(R.id.groupFireRedAsr),
            groupParaformer = binding.view(R.id.groupParaformer),
            silenceWindowLabel = binding.view(R.id.tvSilenceWindowLabel),
            silenceWindowSlider = binding.view(R.id.sliderSilenceWindow),
            silenceSensitivityLabel = binding.view(R.id.tvSilenceSensitivityLabel),
            silenceSensitivitySlider = binding.view(R.id.sliderSilenceSensitivity),
            sfOmniPrompt = binding.view(R.id.tilSfOmniPrompt),
            openAiPrompt = binding.view(R.id.tilOpenAiPrompt),
            switchOpenAiStreaming = binding.view(R.id.switchOpenAiStreaming),
            switchVolcFileStandard = binding.view(R.id.switchVolcFileStandard),
            switchVolcModelV2 = binding.view(R.id.switchVolcModelV2),
            switchVolcVad = binding.view(R.id.switchVolcVad),
            tvVolcLanguageValue = binding.view(R.id.tvVolcLanguageValue),
            tvVolcLanguageLabel = binding.view(R.id.tvVolcLanguageLabel),
            switchVolcNonstream = binding.view(R.id.switchVolcNonstream),
            vendorGroups = listOf(
                AsrVendor.Volc to binding.view(R.id.groupVolc),
                AsrVendor.SiliconFlow to binding.view(R.id.groupSf),
                AsrVendor.ElevenLabs to binding.view(R.id.groupEleven),
                AsrVendor.OpenAI to binding.view(R.id.groupOpenAI),
                AsrVendor.DashScope to binding.view(R.id.groupDashScope),
                AsrVendor.Gemini to binding.view(R.id.groupGemini),
                AsrVendor.Soniox to binding.view(R.id.groupSoniox),
                AsrVendor.StepAudio to binding.view(R.id.groupStepAudio),
                AsrVendor.Zhipu to binding.view(R.id.groupZhipu),
                AsrVendor.SenseVoice to binding.view(R.id.groupSenseVoice),
                AsrVendor.FunAsrNano to binding.view(R.id.groupFunAsrNano),
                AsrVendor.Qwen3Asr to binding.view(R.id.groupQwen3Asr),
                AsrVendor.Parakeet to binding.view(R.id.groupParakeet),
                AsrVendor.FireRedAsr to binding.view(R.id.groupFireRedAsr),
                AsrVendor.Paraformer to binding.view(R.id.groupParaformer)
            )
        )
    }

    override fun render(binding: AsrSettingsBinding, state: AsrSettingsUiState) {
        val previous = lastState
        if (previous == null || previous.selectedVendor != state.selectedVendor) {
            updateVendorSummary(state.selectedVendor)
            updateVendorVisibility(state.selectedVendor)
        }

        val backupVendor = binding.prefs.backupAsrVendor
        if (lastBackupVendor != backupVendor) {
            updateBackupVendorSummary(backupVendor)
            lastBackupVendor = backupVendor
        }

        if (previous == null || previous.autoStopSilenceEnabled != state.autoStopSilenceEnabled) {
            updateSilenceOptionsVisibility(state.autoStopSilenceEnabled)
        }
        if (previous == null || previous.sfUseOmni != state.sfUseOmni) {
            updateVisibility(cache.sfOmniPrompt, state.sfUseOmni)
        }
        if (previous == null || previous.oaAsrUsePrompt != state.oaAsrUsePrompt) {
            updateVisibility(cache.openAiPrompt, state.oaAsrUsePrompt)
        }
        if (previous == null || previous.oaAsrStreamingEnabled != state.oaAsrStreamingEnabled) {
            setCheckedIfChanged(cache.switchOpenAiStreaming, state.oaAsrStreamingEnabled)
        }
        if (previous == null || previous.volcStreamingEnabled != state.volcStreamingEnabled) {
            updateVolcFileModeVisibility(state.volcStreamingEnabled)
            updateVolcStreamOptionsVisibility(state.volcStreamingEnabled)
            updateVolcTwoPassVisibility(state.volcStreamingEnabled)
        }
        if (previous == null || previous.volcFileStandardEnabled != state.volcFileStandardEnabled) {
            setCheckedIfChanged(cache.switchVolcFileStandard, state.volcFileStandardEnabled)
        }
        if (
            previous == null ||
            previous.volcModelV2Enabled != state.volcModelV2Enabled ||
            previous.volcStreamingEnabled != state.volcStreamingEnabled ||
            previous.volcFileStandardEnabled != state.volcFileStandardEnabled
        ) {
            setCheckedIfChanged(cache.switchVolcModelV2, state.volcModelV2Enabled)
            updateVolcModelV2Visibility(state.volcStreamingEnabled, state.volcFileStandardEnabled)
        }
        lastState = state
    }

    private fun updateVendorSummary(vendor: AsrVendor) {
        val idx = vendorOrder.indexOf(vendor).coerceAtLeast(0)
        val label = vendorItems.getOrElse(idx) { vendorItems.firstOrNull().orEmpty() }
        if (cache.vendorSummary.text?.contentEquals(label) != true) {
            cache.vendorSummary.text = label
        }
    }

    private fun updateBackupVendorSummary(vendor: AsrVendor) {
        val idx = vendorOrder.indexOf(vendor).coerceAtLeast(0)
        val label = vendorItems.getOrElse(idx) { vendorItems.firstOrNull().orEmpty() }
        if (cache.backupVendorSummary.text?.contentEquals(label) != true) {
            cache.backupVendorSummary.text = label
        }
    }

    private fun updateVendorVisibility(selectedVendor: AsrVendor) {
        cache.vendorGroups.forEach { (vendor, view) ->
            val targetVisibility = if (vendor == selectedVendor) View.VISIBLE else View.GONE
            if (view.visibility != targetVisibility) {
                view.visibility = targetVisibility
            }
        }
    }

    private fun updateSilenceOptionsVisibility(enabled: Boolean) {
        updateVisibility(cache.silenceWindowLabel, enabled)
        updateVisibility(cache.silenceWindowSlider, enabled)
        updateVisibility(cache.silenceSensitivityLabel, enabled)
        updateVisibility(cache.silenceSensitivitySlider, enabled)
    }

    private fun updateVolcFileModeVisibility(streamingEnabled: Boolean) {
        val visibility = if (streamingEnabled) View.GONE else View.VISIBLE
        if (cache.switchVolcFileStandard.visibility != visibility) {
            cache.switchVolcFileStandard.visibility = visibility
        }
    }

    private fun updateVolcModelV2Visibility(
        streamingEnabled: Boolean,
        fileStandardEnabled: Boolean
    ) {
        val visibility = if (streamingEnabled || fileStandardEnabled) View.VISIBLE else View.GONE
        if (cache.switchVolcModelV2.visibility != visibility) {
            cache.switchVolcModelV2.visibility = visibility
        }
    }

    private fun updateVolcStreamOptionsVisibility(enabled: Boolean) {
        updateVisibility(cache.switchVolcVad, enabled)
        updateVisibility(cache.tvVolcLanguageValue, enabled)
        updateVisibility(cache.tvVolcLanguageLabel, enabled)
    }

    private fun updateVolcTwoPassVisibility(streamingEnabled: Boolean) {
        updateVisibility(cache.switchVolcNonstream, streamingEnabled)
    }

    private fun updateVisibility(view: View, visible: Boolean) {
        val targetVisibility = if (visible) View.VISIBLE else View.GONE
        if (view.visibility != targetVisibility) {
            view.visibility = targetVisibility
        }
    }

    private fun setCheckedIfChanged(view: MaterialSwitch, checked: Boolean) {
        if (view.isChecked != checked) {
            view.isChecked = checked
        }
    }
}
