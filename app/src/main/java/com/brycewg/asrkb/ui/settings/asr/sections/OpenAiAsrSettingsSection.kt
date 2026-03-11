/**
 * OpenAI ASR 设置区块，负责多渠道管理与当前渠道参数绑定。
 *
 * 归属模块：ui/settings/asr/sections
 */
package com.brycewg.asrkb.ui.settings.asr.sections

import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.installExplainedSwitch
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsBinding
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.bindString
import com.brycewg.asrkb.ui.settings.asr.setTextIfDifferent
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.UUID

internal class OpenAiAsrSettingsSection : AsrSettingsSection {
    private var isUpdatingUi: Boolean = false

    override fun bind(binding: AsrSettingsBinding) {
        bindProfileControls(binding)

        binding.view<EditText>(R.id.etOpenAiProfileName).bindString { value ->
            if (isUpdatingUi) return@bindString
            binding.prefs.updateActiveOpenAiAsrProvider { it.copy(name = value) }
            refreshProfileUi(binding)
        }
        binding.view<EditText>(R.id.etOpenAiAsrEndpoint).bindString { value ->
            if (isUpdatingUi) return@bindString
            binding.prefs.oaAsrEndpoint = value
        }
        binding.view<EditText>(R.id.etOpenAiApiKey).bindString { value ->
            if (isUpdatingUi) return@bindString
            binding.prefs.oaAsrApiKey = value
        }
        binding.view<EditText>(R.id.etOpenAiModel).bindString { value ->
            if (isUpdatingUi) return@bindString
            binding.prefs.oaAsrModel = value
        }
        binding.view<EditText>(R.id.etOpenAiPrompt).bindString { value ->
            if (isUpdatingUi) return@bindString
            binding.prefs.oaAsrPrompt = value
        }

        binding.view<MaterialSwitch>(R.id.switchOpenAiStreaming).apply {
            isChecked = binding.prefs.oaAsrStreamingEnabled
            installExplainedSwitch(
                context = binding.activity,
                titleRes = R.string.label_openai_streaming,
                offDescRes = R.string.feature_openai_streaming_off_desc,
                onDescRes = R.string.feature_openai_streaming_on_desc,
                preferenceKey = "openai_streaming_explained",
                readPref = { binding.prefs.oaAsrStreamingEnabled },
                writePref = { v -> binding.viewModel.updateOpenAiStreaming(v) },
                hapticFeedback = { binding.hapticTapIfEnabled(it) }
            )
        }

        binding.view<MaterialSwitch>(R.id.switchOpenAiUsePrompt).apply {
            isChecked = binding.prefs.oaAsrUsePrompt
            installExplainedSwitch(
                context = binding.activity,
                titleRes = R.string.label_openai_use_prompt,
                offDescRes = R.string.feature_openai_use_prompt_off_desc,
                onDescRes = R.string.feature_openai_use_prompt_on_desc,
                preferenceKey = "openai_use_prompt_explained",
                readPref = { binding.prefs.oaAsrUsePrompt },
                writePref = { v -> binding.viewModel.updateOpenAiUsePrompt(v) },
                hapticFeedback = { binding.hapticTapIfEnabled(it) }
            )
        }

        bindLanguageSelection(binding)

        binding.view<MaterialButton>(R.id.btnOpenAiGetKey).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.openUrlSafely(
                "https://bibidocs.brycewg.com/getting-started/asr-providers.html#openai-%E5%85%BC%E5%AE%B9%E6%8E%A5%E5%8F%A3"
            )
        }

        refreshProfileUi(binding)
    }

    private fun bindProfileControls(binding: AsrSettingsBinding) {
        binding.view<TextView>(R.id.tvOpenAiProfilesValue).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            showProfileSelectionDialog(binding)
        }

        binding.view<MaterialButton>(R.id.btnOpenAiAddProfile).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            addProfile(binding)
        }

        binding.view<MaterialButton>(R.id.btnOpenAiDeleteProfile).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            deleteActiveProfile(binding)
        }
    }

    private fun showProfileSelectionDialog(binding: AsrSettingsBinding) {
        val profiles = binding.prefs.getOpenAiAsrProviders()
        if (profiles.isEmpty()) return
        val activeId = binding.prefs.activeOpenAiAsrProviderId
        val titles = profiles.map { getProfileDisplayName(binding, it) }.toTypedArray()
        val selectedIndex = profiles.indexOfFirst { it.id == activeId }.let { idx ->
            if (idx >= 0) idx else 0
        }
        binding.showSingleChoiceDialog(
            R.string.label_openai_choose_profile,
            titles,
            selectedIndex
        ) { which ->
            val selected = profiles.getOrNull(which) ?: return@showSingleChoiceDialog
            if (binding.prefs.selectOpenAiAsrProvider(selected.id)) {
                binding.viewModel.refreshOpenAiProfileState()
                refreshProfileUi(binding)
            }
        }
    }

    private fun addProfile(binding: AsrSettingsBinding) {
        val list = binding.prefs.getOpenAiAsrProviders().toMutableList()
        val nextIndex = list.size + 1
        val profile = Prefs.OpenAiAsrProvider(
            id = UUID.randomUUID().toString(),
            name = binding.activity.getString(R.string.openai_profile_default_name, nextIndex),
            endpoint = Prefs.DEFAULT_OA_ASR_ENDPOINT,
            apiKey = "",
            model = Prefs.DEFAULT_OA_ASR_MODEL,
            streamingEnabled = false,
            usePrompt = false,
            prompt = "",
            language = ""
        )
        list.add(profile)
        binding.prefs.setOpenAiAsrProviders(list)
        binding.prefs.selectOpenAiAsrProvider(profile.id)
        binding.viewModel.refreshOpenAiProfileState()
        refreshProfileUi(binding)
        Toast.makeText(binding.activity, R.string.toast_openai_profile_added, Toast.LENGTH_SHORT).show()
    }

    private fun deleteActiveProfile(binding: AsrSettingsBinding) {
        val list = binding.prefs.getOpenAiAsrProviders().toMutableList()
        if (list.size <= 1) {
            Toast.makeText(
                binding.activity,
                R.string.toast_openai_profile_delete_blocked,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val activeId = binding.prefs.activeOpenAiAsrProviderId
        val idx = list.indexOfFirst { it.id == activeId }
        if (idx < 0) return
        list.removeAt(idx)
        binding.prefs.setOpenAiAsrProviders(list)
        val nextActive = list.getOrNull(idx.coerceAtMost(list.lastIndex)) ?: list.firstOrNull()
        if (nextActive != null) {
            binding.prefs.selectOpenAiAsrProvider(nextActive.id)
        }
        binding.viewModel.refreshOpenAiProfileState()
        refreshProfileUi(binding)
        Toast.makeText(binding.activity, R.string.toast_openai_profile_deleted, Toast.LENGTH_SHORT).show()
    }

    private fun refreshProfileUi(binding: AsrSettingsBinding) {
        val provider = binding.prefs.getActiveOpenAiAsrProvider()
        val profiles = binding.prefs.getOpenAiAsrProviders()
        isUpdatingUi = true
        binding.view<TextView>(R.id.tvOpenAiProfilesValue).text = getProfileDisplayName(binding, provider)
        binding.view<EditText>(R.id.etOpenAiProfileName).setTextIfDifferent(provider?.name.orEmpty())
        binding.view<EditText>(R.id.etOpenAiAsrEndpoint).setTextIfDifferent(binding.prefs.oaAsrEndpoint)
        binding.view<EditText>(R.id.etOpenAiApiKey).setTextIfDifferent(binding.prefs.oaAsrApiKey)
        binding.view<EditText>(R.id.etOpenAiModel).setTextIfDifferent(binding.prefs.oaAsrModel)
        binding.view<EditText>(R.id.etOpenAiPrompt).setTextIfDifferent(binding.prefs.oaAsrPrompt)
        binding.view<MaterialSwitch>(R.id.switchOpenAiStreaming).isChecked = binding.prefs.oaAsrStreamingEnabled
        binding.view<MaterialSwitch>(R.id.switchOpenAiUsePrompt).isChecked = binding.prefs.oaAsrUsePrompt
        updateLanguageSummary(binding)
        binding.view<MaterialButton>(R.id.btnOpenAiDeleteProfile).isEnabled = profiles.size > 1
        isUpdatingUi = false
    }

    private fun getProfileDisplayName(
        binding: AsrSettingsBinding,
        provider: Prefs.OpenAiAsrProvider?
    ): String = provider?.name?.takeIf { it.isNotBlank() }
        ?: binding.activity.getString(R.string.untitled_profile)

    private fun bindLanguageSelection(binding: AsrSettingsBinding) {
        val langLabels = listOf(
            binding.activity.getString(R.string.dash_lang_auto),
            binding.activity.getString(R.string.dash_lang_zh),
            binding.activity.getString(R.string.dash_lang_en),
            binding.activity.getString(R.string.dash_lang_ja),
            binding.activity.getString(R.string.dash_lang_de),
            binding.activity.getString(R.string.dash_lang_ko),
            binding.activity.getString(R.string.dash_lang_ru),
            binding.activity.getString(R.string.dash_lang_fr),
            binding.activity.getString(R.string.dash_lang_pt),
            binding.activity.getString(R.string.dash_lang_ar),
            binding.activity.getString(R.string.dash_lang_it),
            binding.activity.getString(R.string.dash_lang_es)
        )
        val langCodes = listOf("", "zh", "en", "ja", "de", "ko", "ru", "fr", "pt", "ar", "it", "es")
        val tvOpenAiLanguage = binding.view<TextView>(R.id.tvOpenAiLanguageValue)

        tvOpenAiLanguage.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            val cur = langCodes.indexOf(binding.prefs.oaAsrLanguage).coerceAtLeast(0)
            binding.showSingleChoiceDialog(
                R.string.label_openai_language,
                langLabels.toTypedArray(),
                cur
            ) { which ->
                val code = langCodes.getOrNull(which) ?: ""
                if (code != binding.prefs.oaAsrLanguage) {
                    binding.prefs.oaAsrLanguage = code
                }
                updateLanguageSummary(binding, langLabels, langCodes)
            }
        }

        updateLanguageSummary(binding, langLabels, langCodes)
    }

    private fun updateLanguageSummary(
        binding: AsrSettingsBinding,
        langLabels: List<String> = listOf(
            binding.activity.getString(R.string.dash_lang_auto),
            binding.activity.getString(R.string.dash_lang_zh),
            binding.activity.getString(R.string.dash_lang_en),
            binding.activity.getString(R.string.dash_lang_ja),
            binding.activity.getString(R.string.dash_lang_de),
            binding.activity.getString(R.string.dash_lang_ko),
            binding.activity.getString(R.string.dash_lang_ru),
            binding.activity.getString(R.string.dash_lang_fr),
            binding.activity.getString(R.string.dash_lang_pt),
            binding.activity.getString(R.string.dash_lang_ar),
            binding.activity.getString(R.string.dash_lang_it),
            binding.activity.getString(R.string.dash_lang_es)
        ),
        langCodes: List<String> = listOf(
            "",
            "zh",
            "en",
            "ja",
            "de",
            "ko",
            "ru",
            "fr",
            "pt",
            "ar",
            "it",
            "es"
        )
    ) {
        val idx = langCodes.indexOf(binding.prefs.oaAsrLanguage).coerceAtLeast(0)
        binding.view<TextView>(R.id.tvOpenAiLanguageValue).text = langLabels[idx]
    }
}
