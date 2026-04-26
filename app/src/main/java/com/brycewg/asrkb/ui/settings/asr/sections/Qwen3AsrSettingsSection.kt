/**
 * Qwen3-ASR 设置区块。
 *
 * 归属模块：ui/settings/asr/sections
 */
package com.brycewg.asrkb.ui.settings.asr.sections

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.installExplainedSwitch
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsBinding
import com.brycewg.asrkb.ui.settings.asr.AsrSettingsSection
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class Qwen3AsrSettingsSection : AsrSettingsSection {
    private var downloadUiRequestSeq: Long = 0L

    override fun bind(binding: AsrSettingsBinding) {
        bindVariantSelection(binding)
        bindThreadsSlider(binding)
        bindSwitches(binding)
        bindKeepAliveSelection(binding)
        bindModelButtons(binding)

        binding.view<MaterialButton>(R.id.btnQwGuide).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.openUrlSafely(
                binding.activity.getString(R.string.local_model_guide_config_doc_url)
            )
        }
    }

    override fun onResume(binding: AsrSettingsBinding) {
        refreshDownloadUiVisibility(binding)
    }

    private fun bindVariantSelection(binding: AsrSettingsBinding) {
        val tvVariant = binding.view<TextView>(R.id.tvQwModelVariantValue)
        val variantLabels = arrayOf(
            binding.activity.getString(R.string.qw_model_qwen3_06b_int8)
        )
        val variantCodes = arrayOf("qwen3-0.6b-int8")

        fun updateVariantSummary() {
            val normalized = com.brycewg.asrkb.asr.normalizeQwen3AsrVariant(binding.prefs.qwModelVariant)
            val idx = variantCodes.indexOf(normalized).coerceAtLeast(0)
            tvVariant.text = variantLabels[idx]
        }

        updateVariantSummary()
        tvVariant.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            val cur = variantCodes.indexOf(
                com.brycewg.asrkb.asr.normalizeQwen3AsrVariant(binding.prefs.qwModelVariant)
            ).coerceAtLeast(0)
            binding.showSingleChoiceDialog(
                R.string.label_qw_model_variant,
                variantLabels,
                cur
            ) { which ->
                val code = variantCodes.getOrNull(which) ?: "qwen3-0.6b-int8"
                if (code != binding.prefs.qwModelVariant) {
                    binding.viewModel.updateQwModelVariant(code)
                }
                updateVariantSummary()
                refreshDownloadUiVisibility(binding)
            }
        }
    }

    private fun bindThreadsSlider(binding: AsrSettingsBinding) {
        binding.view<Slider>(R.id.sliderQwThreads).apply {
            value = binding.prefs.qwNumThreads.coerceIn(1, 8).toFloat()
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val v = value.toInt().coerceIn(1, 8)
                    if (v != binding.prefs.qwNumThreads) {
                        binding.viewModel.updateQwNumThreads(v)
                    }
                }
            }
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) = binding.hapticTapIfEnabled(slider)
                override fun onStopTrackingTouch(slider: Slider) = binding.hapticTapIfEnabled(slider)
            })
        }
    }

    private fun bindSwitches(binding: AsrSettingsBinding) {
        binding.view<MaterialSwitch>(R.id.switchQwPreload).apply {
            isChecked = binding.prefs.qwPreloadEnabled
            installExplainedSwitch(
                context = binding.activity,
                titleRes = R.string.label_qw_preload,
                offDescRes = R.string.feature_sv_preload_off_desc,
                onDescRes = R.string.feature_sv_preload_on_desc,
                preferenceKey = "qw_preload_explained",
                readPref = { binding.prefs.qwPreloadEnabled },
                writePref = { v -> binding.viewModel.updateQwPreload(v) },
                hapticFeedback = { binding.hapticTapIfEnabled(it) }
            )
        }
    }

    private fun bindKeepAliveSelection(binding: AsrSettingsBinding) {
        val labels = listOf(
            binding.activity.getString(R.string.sv_keep_alive_immediate),
            binding.activity.getString(R.string.sv_keep_alive_5m),
            binding.activity.getString(R.string.sv_keep_alive_15m),
            binding.activity.getString(R.string.sv_keep_alive_30m),
            binding.activity.getString(R.string.sv_keep_alive_always)
        )
        val values = listOf(0, 5, 15, 30, -1)
        val tvQwKeepAlive = binding.view<TextView>(R.id.tvQwKeepAliveValue)

        fun updateKeepAliveSummary() {
            val idx = values.indexOf(binding.prefs.qwKeepAliveMinutes).let {
                if (it >= 0) it else values.size - 1
            }
            tvQwKeepAlive.text = labels[idx]
        }

        updateKeepAliveSummary()
        tvQwKeepAlive.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            val cur = values.indexOf(binding.prefs.qwKeepAliveMinutes).let {
                if (it >= 0) it else values.size - 1
            }
            binding.showSingleChoiceDialog(
                R.string.label_qw_keep_alive,
                labels.toTypedArray(),
                cur
            ) { which ->
                val vv = values.getOrNull(which) ?: -1
                if (vv != binding.prefs.qwKeepAliveMinutes) {
                    binding.viewModel.updateQwKeepAlive(vv)
                }
                updateKeepAliveSummary()
            }
        }
    }

    private fun bindModelButtons(binding: AsrSettingsBinding) {
        val btnQwDownload = binding.view<MaterialButton>(R.id.btnQwDownloadModel)
        val btnQwImport = binding.view<MaterialButton>(R.id.btnQwImportModel)
        val btnQwClear = binding.view<MaterialButton>(R.id.btnQwClearModel)
        val tvQwDownloadStatus = binding.view<TextView>(R.id.tvQwDownloadStatus)

        btnQwDownload.setOnClickListener { v ->
            val variant = com.brycewg.asrkb.asr.normalizeQwen3AsrVariant(binding.prefs.qwModelVariant)
            binding.modelDownloadUiController.startDownloadFromSourceDialog(
                downloadButton = v,
                statusTextViews = listOf(tvQwDownloadStatus),
                urlOfficial = qwen3ModelUrl(variant),
                variant = variant,
                modelType = "qwen3_asr",
                startedTextResId = R.string.qw_download_started_in_bg,
                failedTextResId = R.string.qw_download_status_failed,
                logTag = TAG,
                logMessage = "Failed to start Qwen3-ASR model download"
            )
        }

        btnQwClear.setOnClickListener { v ->
            AlertDialog.Builder(binding.activity)
                .setTitle(R.string.qw_clear_confirm_title)
                .setMessage(R.string.qw_clear_confirm_message)
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    d.dismiss()
                    v.isEnabled = false
                    binding.activity.lifecycleScope.launch {
                        try {
                            val base =
                                binding.activity.getExternalFilesDir(null)
                                    ?: binding.activity.filesDir
                            val target = File(
                                File(base, "qwen3_asr"),
                                com.brycewg.asrkb.asr.normalizeQwen3AsrVariant(binding.prefs.qwModelVariant)
                            )
                            if (target.exists()) {
                                withContext(Dispatchers.IO) { target.deleteRecursively() }
                            }
                            try {
                                com.brycewg.asrkb.asr.unloadQwen3AsrRecognizer()
                            } catch (t: Throwable) {
                                android.util.Log.e(TAG, "Failed to unload Qwen3-ASR recognizer", t)
                            }
                            tvQwDownloadStatus.text =
                                binding.activity.getString(R.string.qw_clear_done)
                        } catch (t: Throwable) {
                            android.util.Log.e(TAG, "Failed to clear Qwen3-ASR model", t)
                            tvQwDownloadStatus.text =
                                binding.activity.getString(R.string.qw_clear_failed)
                        } finally {
                            v.isEnabled = true
                            refreshDownloadUiVisibility(binding)
                        }
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
                .show()
        }

        btnQwImport.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.qwen3AsrModelPicker.launch("application/zip")
        }
    }

    private fun refreshDownloadUiVisibility(binding: AsrSettingsBinding) {
        val requestSeq = ++downloadUiRequestSeq
        binding.activity.lifecycleScope.launch {
            val ready = withContext(Dispatchers.IO) {
                binding.viewModel.checkQwModelDownloaded(binding.activity)
            }
            if (requestSeq != downloadUiRequestSeq || binding.activity.isDestroyed) return@launch
            renderDownloadUiVisibility(binding, ready)
        }
    }

    private fun renderDownloadUiVisibility(binding: AsrSettingsBinding, ready: Boolean) {
        val btn = binding.view<MaterialButton>(R.id.btnQwDownloadModel)
        val btnImport = binding.view<MaterialButton>(R.id.btnQwImportModel)
        val btnClear = binding.view<MaterialButton>(R.id.btnQwClearModel)
        val tv = binding.view<TextView>(R.id.tvQwDownloadStatus)

        btn.visibility = if (ready) android.view.View.GONE else android.view.View.VISIBLE
        btnImport.visibility = if (ready) android.view.View.GONE else android.view.View.VISIBLE
        btnClear.visibility = if (ready) android.view.View.VISIBLE else android.view.View.GONE

        if (ready && tv.text.isNullOrBlank()) {
            tv.text = binding.activity.getString(R.string.qw_download_status_done)
        }
    }

    private companion object {
        private const val TAG = "Qwen3AsrSettingsSection"
        private const val QWEN3_ASR_MODEL_URL =
            "https://github.com/BryceWG/BiBi-Keyboard/releases/download/models/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25.zip"

        private fun qwen3ModelUrl(variant: String): String = when (
            com.brycewg.asrkb.asr.normalizeQwen3AsrVariant(variant)
        ) {
            else -> QWEN3_ASR_MODEL_URL
        }
    }
}
