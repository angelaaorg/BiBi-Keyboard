/**
 * ASR 设置页 StepAudio section：API Key、模型、语言与 ITN 配置。
 *
 * 归属模块：ui/settings/asr/sections
 */
package com.brycewg.asrkb.ui.settings.asr.sections

import android.widget.EditText
import android.widget.TextView
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsBinding
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.bindString
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

internal class StepAudioSettingsSection : AsrSettingsSection {
    override fun bind(binding: AsrSettingsBinding) {
        binding.view<EditText>(R.id.etStepAudioApiKey).apply {
            setText(binding.prefs.stepAudioApiKey)
            bindString { binding.prefs.stepAudioApiKey = it }
        }

        bindModel(binding)

        binding.view<MaterialSwitch>(R.id.switchStepAudioUseItn).apply {
            isChecked = binding.prefs.stepAudioUseItn
            setOnCheckedChangeListener { _, isChecked ->
                binding.prefs.stepAudioUseItn = isChecked
                binding.hapticTapIfEnabled(this)
            }
        }

        bindLanguage(binding)

        binding.view<MaterialButton>(R.id.btnStepAudioGetKey).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.openUrlSafely("https://platform.stepfun.com")
        }
    }

    private fun bindModel(binding: AsrSettingsBinding) {
        val models = Prefs.STEPAUDIO_ASR_MODELS
        val tv = binding.view<TextView>(R.id.tvStepAudioModelValue)
        val initialModel = binding.prefs.stepAudioModel
            .takeIf { it in models }
            ?: Prefs.DEFAULT_STEPAUDIO_ASR_MODEL
        if (initialModel != binding.prefs.stepAudioModel) {
            binding.prefs.stepAudioModel = initialModel
        }

        fun updateSummary() {
            tv.text = binding.prefs.stepAudioModel.ifBlank { Prefs.DEFAULT_STEPAUDIO_ASR_MODEL }
        }

        updateSummary()
        tv.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.showSingleChoiceDialog(
                titleResId = R.string.label_stepaudio_model,
                items = models.toTypedArray(),
                currentIndex = models.indexOf(binding.prefs.stepAudioModel).coerceAtLeast(0)
            ) { which ->
                binding.prefs.stepAudioModel = models.getOrElse(which) {
                    Prefs.DEFAULT_STEPAUDIO_ASR_MODEL
                }
                updateSummary()
            }
        }
    }

    private fun bindLanguage(binding: AsrSettingsBinding) {
        val values = listOf("zh", "en", "")
        val labels = listOf(
            binding.activity.getString(R.string.stepaudio_lang_zh),
            binding.activity.getString(R.string.stepaudio_lang_en),
            binding.activity.getString(R.string.stepaudio_lang_auto)
        )
        val tv = binding.view<TextView>(R.id.tvStepAudioLanguageValue)

        fun currentIndex(): Int {
            val current = binding.prefs.stepAudioLanguage.trim()
            return values.indexOf(current).takeIf { it >= 0 } ?: 0
        }

        fun updateSummary() {
            tv.text = labels.getOrElse(currentIndex()) { labels.first() }
        }

        updateSummary()
        tv.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.showSingleChoiceDialog(
                titleResId = R.string.label_stepaudio_language,
                items = labels.toTypedArray(),
                currentIndex = currentIndex()
            ) { which ->
                binding.prefs.stepAudioLanguage = values.getOrElse(which) { "zh" }
                updateSummary()
            }
        }
    }
}
