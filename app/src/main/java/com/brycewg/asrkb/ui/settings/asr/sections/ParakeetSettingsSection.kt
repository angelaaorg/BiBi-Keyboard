/**
 * Parakeet 设置区块。
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

internal class ParakeetSettingsSection : AsrSettingsSection {
    private var downloadUiRequestSeq: Long = 0L

    override fun bind(binding: AsrSettingsBinding) {
        bindVariantSelection(binding)
        bindThreadsSlider(binding)
        bindSwitches(binding)
        bindKeepAliveSelection(binding)
        bindModelButtons(binding)

        binding.view<MaterialButton>(R.id.btnPkGuide).setOnClickListener { v ->
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
        val tvVariant = binding.view<TextView>(R.id.tvPkModelVariantValue)
        val variantLabels = arrayOf(
            binding.activity.getString(R.string.pk_model_06b_v3_int8),
            binding.activity.getString(R.string.pk_model_06b_v2_int8)
        )
        val variantCodes = arrayOf("0.6b-v3-int8", "0.6b-v2-int8")

        fun updateVariantSummary() {
            val normalized = com.brycewg.asrkb.asr.normalizeParakeetVariant(binding.prefs.pkModelVariant)
            val idx = variantCodes.indexOf(normalized).coerceAtLeast(0)
            tvVariant.text = variantLabels[idx]
        }

        updateVariantSummary()
        tvVariant.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            val cur = variantCodes.indexOf(
                com.brycewg.asrkb.asr.normalizeParakeetVariant(binding.prefs.pkModelVariant)
            ).coerceAtLeast(0)
            binding.showSingleChoiceDialog(
                R.string.label_pk_model_variant,
                variantLabels,
                cur
            ) { which ->
                val code = variantCodes.getOrNull(which) ?: "0.6b-v3-int8"
                if (code != binding.prefs.pkModelVariant) {
                    binding.viewModel.updatePkModelVariant(code)
                }
                updateVariantSummary()
                refreshDownloadUiVisibility(binding)
            }
        }
    }

    private fun bindThreadsSlider(binding: AsrSettingsBinding) {
        binding.view<Slider>(R.id.sliderPkThreads).apply {
            value = binding.prefs.pkNumThreads.coerceIn(1, 8).toFloat()
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val v = value.toInt().coerceIn(1, 8)
                    if (v != binding.prefs.pkNumThreads) {
                        binding.viewModel.updatePkNumThreads(v)
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
        binding.view<MaterialSwitch>(R.id.switchPkPreload).apply {
            isChecked = binding.prefs.pkPreloadEnabled
            installExplainedSwitch(
                context = binding.activity,
                titleRes = R.string.label_pk_preload,
                offDescRes = R.string.feature_sv_preload_off_desc,
                onDescRes = R.string.feature_sv_preload_on_desc,
                preferenceKey = "pk_preload_explained",
                readPref = { binding.prefs.pkPreloadEnabled },
                writePref = { v -> binding.viewModel.updatePkPreload(v) },
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
        val tvKeepAlive = binding.view<TextView>(R.id.tvPkKeepAliveValue)

        fun updateKeepAliveSummary() {
            val idx = values.indexOf(binding.prefs.pkKeepAliveMinutes).let {
                if (it >= 0) it else values.size - 1
            }
            tvKeepAlive.text = labels[idx]
        }

        updateKeepAliveSummary()
        tvKeepAlive.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            val cur = values.indexOf(binding.prefs.pkKeepAliveMinutes).let {
                if (it >= 0) it else values.size - 1
            }
            binding.showSingleChoiceDialog(
                R.string.label_pk_keep_alive,
                labels.toTypedArray(),
                cur
            ) { which ->
                val vv = values.getOrNull(which) ?: -1
                if (vv != binding.prefs.pkKeepAliveMinutes) {
                    binding.viewModel.updatePkKeepAlive(vv)
                }
                updateKeepAliveSummary()
            }
        }
    }

    private fun bindModelButtons(binding: AsrSettingsBinding) {
        val btnDownload = binding.view<MaterialButton>(R.id.btnPkDownloadModel)
        val btnImport = binding.view<MaterialButton>(R.id.btnPkImportModel)
        val btnClear = binding.view<MaterialButton>(R.id.btnPkClearModel)
        val tvStatus = binding.view<TextView>(R.id.tvPkDownloadStatus)

        btnDownload.setOnClickListener { v ->
            val variant = com.brycewg.asrkb.asr.normalizeParakeetVariant(binding.prefs.pkModelVariant)
            val urlOfficial = when (variant) {
                "0.6b-v2-int8" -> PARAKEET_V2_MODEL_URL
                else -> PARAKEET_V3_MODEL_URL
            }
            binding.modelDownloadUiController.startDownloadFromSourceDialog(
                downloadButton = v,
                statusTextViews = listOf(tvStatus),
                urlOfficial = urlOfficial,
                variant = variant,
                modelType = "parakeet",
                startedTextResId = R.string.pk_download_started_in_bg,
                failedTextResId = R.string.pk_download_status_failed,
                logTag = TAG,
                logMessage = "Failed to start Parakeet model download"
            )
        }

        btnClear.setOnClickListener { v ->
            AlertDialog.Builder(binding.activity)
                .setTitle(R.string.pk_clear_confirm_title)
                .setMessage(R.string.pk_clear_confirm_message)
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    d.dismiss()
                    v.isEnabled = false
                    binding.activity.lifecycleScope.launch {
                        try {
                            val base =
                                binding.activity.getExternalFilesDir(null)
                                    ?: binding.activity.filesDir
                            val target = File(
                                File(base, "parakeet"),
                                com.brycewg.asrkb.asr.normalizeParakeetVariant(binding.prefs.pkModelVariant)
                            )
                            if (target.exists()) {
                                withContext(Dispatchers.IO) { target.deleteRecursively() }
                            }
                            try {
                                com.brycewg.asrkb.asr.unloadParakeetRecognizer()
                            } catch (t: Throwable) {
                                android.util.Log.e(TAG, "Failed to unload Parakeet recognizer", t)
                            }
                            tvStatus.text = binding.activity.getString(R.string.pk_clear_done)
                        } catch (t: Throwable) {
                            android.util.Log.e(TAG, "Failed to clear Parakeet model", t)
                            tvStatus.text = binding.activity.getString(R.string.pk_clear_failed)
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

        btnImport.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.parakeetModelPicker.launch("application/zip")
        }
    }

    private fun refreshDownloadUiVisibility(binding: AsrSettingsBinding) {
        val requestSeq = ++downloadUiRequestSeq
        binding.activity.lifecycleScope.launch {
            val ready = withContext(Dispatchers.IO) {
                binding.viewModel.checkPkModelDownloaded(binding.activity)
            }
            if (requestSeq != downloadUiRequestSeq || binding.activity.isDestroyed) return@launch
            renderDownloadUiVisibility(binding, ready)
        }
    }

    private fun renderDownloadUiVisibility(binding: AsrSettingsBinding, ready: Boolean) {
        val btn = binding.view<MaterialButton>(R.id.btnPkDownloadModel)
        val btnImport = binding.view<MaterialButton>(R.id.btnPkImportModel)
        val btnClear = binding.view<MaterialButton>(R.id.btnPkClearModel)
        val tv = binding.view<TextView>(R.id.tvPkDownloadStatus)

        btn.visibility = if (ready) android.view.View.GONE else android.view.View.VISIBLE
        btnImport.visibility = if (ready) android.view.View.GONE else android.view.View.VISIBLE
        btnClear.visibility = if (ready) android.view.View.VISIBLE else android.view.View.GONE

        if (ready && tv.text.isNullOrBlank()) {
            tv.text = binding.activity.getString(R.string.pk_download_status_done)
        }
    }

    private companion object {
        private const val TAG = "ParakeetSettingsSection"
        private const val PARAKEET_V3_MODEL_URL =
            "https://github.com/BryceWG/BiBi-Keyboard/releases/download/models/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8.zip"
        private const val PARAKEET_V2_MODEL_URL =
            "https://github.com/BryceWG/BiBi-Keyboard/releases/download/models/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8.zip"
    }
}
