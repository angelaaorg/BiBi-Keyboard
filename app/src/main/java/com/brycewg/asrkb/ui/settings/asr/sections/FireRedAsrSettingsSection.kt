/**
 * FireRedASR V2 设置区块。
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

internal class FireRedAsrSettingsSection : AsrSettingsSection {
    override fun bind(binding: AsrSettingsBinding) {
        bindVariantSelection(binding)
        bindKeepAliveSelection(binding)
        bindThreadsSlider(binding)
        bindSwitches(binding)
        bindModelButtons(binding)

        binding.view<MaterialButton>(R.id.btnFrGuide).setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.openUrlSafely(
                binding.activity.getString(R.string.local_model_guide_config_doc_url)
            )
        }
    }

    override fun onResume(binding: AsrSettingsBinding) {
        updateDownloadUiVisibility(binding)
    }

    private fun bindVariantSelection(binding: AsrSettingsBinding) {
        val tvVariant = binding.view<TextView>(R.id.tvFrModelVariantValue)
        val variantLabels = arrayOf(
            binding.activity.getString(R.string.fr_model_ctc_int8)
        )
        val variantCodes = arrayOf("ctc-int8")

        fun updateVariantSummary() {
            val idx = variantCodes.indexOf(binding.prefs.frModelVariant).coerceAtLeast(0)
            tvVariant.text = variantLabels[idx]
        }

        updateVariantSummary()
        tvVariant.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            val cur = variantCodes.indexOf(binding.prefs.frModelVariant).coerceAtLeast(0)
            binding.showSingleChoiceDialog(
                R.string.label_fr_model_variant,
                variantLabels,
                cur
            ) { which ->
                val code = variantCodes.getOrNull(which) ?: "ctc-int8"
                if (code != binding.prefs.frModelVariant) {
                    binding.viewModel.updateFrModelVariant(code)
                }
                updateVariantSummary()
                updateDownloadUiVisibility(binding)
            }
        }
    }

    private fun bindKeepAliveSelection(binding: AsrSettingsBinding) {
        val tvKeep = binding.view<TextView>(R.id.tvFrKeepAliveValue)
        val values = listOf(0, 5, 15, 30, -1)
        val labels = arrayOf(
            binding.activity.getString(R.string.sv_keep_alive_immediate),
            binding.activity.getString(R.string.sv_keep_alive_5m),
            binding.activity.getString(R.string.sv_keep_alive_15m),
            binding.activity.getString(R.string.sv_keep_alive_30m),
            binding.activity.getString(R.string.sv_keep_alive_always)
        )

        fun updateKeepAliveSummary() {
            val idx = values.indexOf(binding.prefs.frKeepAliveMinutes).let {
                if (it >=
                    0
                ) {
                    it
                } else {
                    values.size - 1
                }
            }
            tvKeep.text = labels[idx]
        }

        updateKeepAliveSummary()
        tvKeep.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            val cur = values.indexOf(binding.prefs.frKeepAliveMinutes).let {
                if (it >=
                    0
                ) {
                    it
                } else {
                    values.size - 1
                }
            }
            binding.showSingleChoiceDialog(R.string.label_fr_keep_alive, labels, cur) { which ->
                val vv = values.getOrNull(which) ?: -1
                if (vv != binding.prefs.frKeepAliveMinutes) {
                    binding.viewModel.updateFrKeepAlive(vv)
                }
                updateKeepAliveSummary()
            }
        }
    }

    private fun bindThreadsSlider(binding: AsrSettingsBinding) {
        binding.view<Slider>(R.id.sliderFrThreads).apply {
            value = binding.prefs.frNumThreads.coerceIn(1, 8).toFloat()
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val v = value.toInt().coerceIn(1, 8)
                    if (v != binding.prefs.frNumThreads) {
                        binding.viewModel.updateFrNumThreads(v)
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
        binding.view<MaterialSwitch>(R.id.switchFrPreload).apply {
            isChecked = binding.prefs.frPreloadEnabled
            installExplainedSwitch(
                context = binding.activity,
                titleRes = R.string.label_fr_preload,
                offDescRes = R.string.feature_fr_preload_off_desc,
                onDescRes = R.string.feature_fr_preload_on_desc,
                preferenceKey = "fr_preload_explained",
                readPref = { binding.prefs.frPreloadEnabled },
                writePref = { v -> binding.viewModel.updateFrPreload(v) },
                hapticFeedback = { binding.hapticTapIfEnabled(it) }
            )
        }

        binding.view<MaterialSwitch>(R.id.switchFrUseItn).apply {
            isChecked = binding.prefs.frUseItn
            installExplainedSwitch(
                context = binding.activity,
                titleRes = R.string.label_fr_use_itn,
                offDescRes = R.string.feature_fr_use_itn_off_desc,
                onDescRes = R.string.feature_fr_use_itn_on_desc,
                preferenceKey = "fr_use_itn_explained",
                readPref = { binding.prefs.frUseItn },
                writePref = { v -> binding.viewModel.updateFrUseItn(v) },
                hapticFeedback = { binding.hapticTapIfEnabled(it) }
            )
        }

        binding.view<MaterialSwitch>(R.id.switchFrPseudoStream).apply {
            isChecked = binding.prefs.frPseudoStreamEnabled
            installExplainedSwitch(
                context = binding.activity,
                titleRes = R.string.label_fr_pseudo_stream,
                offDescRes = R.string.feature_fr_pseudo_stream_off_desc,
                onDescRes = R.string.feature_fr_pseudo_stream_on_desc,
                preferenceKey = "fr_pseudo_stream_explained",
                readPref = { binding.prefs.frPseudoStreamEnabled },
                writePref = { v -> binding.viewModel.updateFrPseudoStream(v) },
                hapticFeedback = { binding.hapticTapIfEnabled(it) }
            )
        }
    }

    private fun bindModelButtons(binding: AsrSettingsBinding) {
        val btnDl = binding.view<MaterialButton>(R.id.btnFrDownloadModel)
        val btnImport = binding.view<MaterialButton>(R.id.btnFrImportModel)
        val btnClear = binding.view<MaterialButton>(R.id.btnFrClearModel)
        val tvStatus = binding.view<TextView>(R.id.tvFrDownloadStatus)

        btnImport.setOnClickListener { v ->
            binding.hapticTapIfEnabled(v)
            binding.fireRedAsrModelPicker.launch("application/zip")
        }

        btnDl.setOnClickListener { v ->
            val variant = "ctc-int8"
            val urlOfficial = "https://github.com/BryceWG/BiBi-Keyboard/releases/download/models/sherpa-onnx-fire-red-asr2-ctc-zh_en-int8-2026-02-25.zip"
            binding.modelDownloadUiController.startDownloadFromSourceDialog(
                downloadButton = v,
                statusTextViews = listOf(tvStatus),
                urlOfficial = urlOfficial,
                variant = variant,
                modelType = "firered_asr",
                startedTextResId = R.string.fr_download_started_in_bg,
                failedTextResId = R.string.fr_download_status_failed,
                logTag = TAG,
                logMessage = "Failed to start FireRedASR model download"
            )
        }

        btnClear.setOnClickListener { v ->
            AlertDialog.Builder(binding.activity)
                .setTitle(R.string.fr_clear_confirm_title)
                .setMessage(R.string.fr_clear_confirm_message)
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    d.dismiss()
                    v.isEnabled = false
                    binding.activity.lifecycleScope.launch {
                        try {
                            val base =
                                binding.activity.getExternalFilesDir(null)
                                    ?: binding.activity.filesDir
                            val variant = binding.prefs.frModelVariant
                            val outDirRoot = File(base, "firered_asr")
                            val outDir = File(outDirRoot, variant)
                            if (outDir.exists()) {
                                withContext(Dispatchers.IO) { outDir.deleteRecursively() }
                            }
                            try {
                                com.brycewg.asrkb.asr.unloadFireRedAsrRecognizer()
                            } catch (t: Throwable) {
                                android.util.Log.e(TAG, "Failed to unload FireRedASR recognizer", t)
                            }
                            tvStatus.text = binding.activity.getString(R.string.fr_clear_done)
                        } catch (t: Throwable) {
                            android.util.Log.e(TAG, "Failed to clear FireRedASR model", t)
                            tvStatus.text = binding.activity.getString(R.string.fr_clear_failed)
                        } finally {
                            v.isEnabled = true
                            updateDownloadUiVisibility(binding)
                        }
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
                .show()
        }
    }

    private fun updateDownloadUiVisibility(binding: AsrSettingsBinding) {
        val ready = binding.viewModel.checkFrModelDownloaded(binding.activity)
        val btn = binding.view<MaterialButton>(R.id.btnFrDownloadModel)
        val btnImport = binding.view<MaterialButton>(R.id.btnFrImportModel)
        val btnClear = binding.view<MaterialButton>(R.id.btnFrClearModel)
        val tv = binding.view<TextView>(R.id.tvFrDownloadStatus)

        btn.visibility = if (ready) android.view.View.GONE else android.view.View.VISIBLE
        btnImport.visibility = if (ready) android.view.View.GONE else android.view.View.VISIBLE
        btnClear.visibility = if (ready) android.view.View.VISIBLE else android.view.View.GONE

        if (ready && tv.text.isNullOrBlank()) {
            tv.text = binding.activity.getString(R.string.fr_download_status_done)
        }
    }

    private companion object {
        private const val TAG = "FireRedAsrSettingsSection"
    }
}
