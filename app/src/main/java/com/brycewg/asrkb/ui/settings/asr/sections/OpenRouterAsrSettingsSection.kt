/**
 * OpenRouter ASR 设置区块：Endpoint、API Key 与模型配置。
 *
 * 归属模块：ui/settings/asr/sections
 */
package com.brycewg.asrkb.ui.settings.asr.sections

import android.widget.EditText
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsBinding
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.bindString
import com.google.android.material.button.MaterialButton

internal class OpenRouterAsrSettingsSection : AsrSettingsSection {
    override fun bind(binding: AsrSettingsBinding) {
        binding.view<EditText>(R.id.etOpenRouterAsrEndpoint).apply {
            setText(
                binding.prefs.openRouterAsrEndpoint.ifBlank {
                    Prefs.DEFAULT_OPENROUTER_ASR_ENDPOINT
                }
            )
            bindString { binding.prefs.openRouterAsrEndpoint = it }
        }

        binding.view<EditText>(R.id.etOpenRouterApiKey).apply {
            setText(binding.prefs.openRouterAsrApiKey)
            bindString { binding.prefs.openRouterAsrApiKey = it.removeBearerPrefix() }
        }

        binding.view<EditText>(R.id.etOpenRouterModel).apply {
            setText(
                binding.prefs.openRouterAsrModel.ifBlank {
                    Prefs.DEFAULT_OPENROUTER_ASR_MODEL
                }
            )
            bindString { binding.prefs.openRouterAsrModel = it }
        }

        binding.view<MaterialButton>(R.id.btnOpenRouterGetKey).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.openUrlSafely("https://openrouter.ai/settings/keys")
        }
    }

    private fun String.removeBearerPrefix(): String = replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "").trim()
}
