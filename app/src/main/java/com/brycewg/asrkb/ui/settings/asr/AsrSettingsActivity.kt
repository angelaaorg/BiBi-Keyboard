/**
 * 语音识别设置页面入口。
 *
 * 归属模块：ui/settings/asr
 */
package com.brycewg.asrkb.ui.settings.asr

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.BaseActivity
import com.brycewg.asrkb.ui.WindowInsetsHelper
import com.brycewg.asrkb.ui.settings.asr.sections.AsrSettingsToolbarSection
import com.brycewg.asrkb.ui.settings.asr.sections.AsrSettingsUiRendererSection
import com.brycewg.asrkb.ui.settings.asr.sections.AsrSilenceDetectionSection
import com.brycewg.asrkb.ui.settings.asr.sections.AsrVendorSelectionSection
import com.brycewg.asrkb.ui.settings.asr.sections.BackupAsrSection
import com.brycewg.asrkb.ui.settings.asr.sections.DashScopeAsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.ElevenLabsSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.FireRedAsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.FunAsrNanoSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.GeminiAsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.OpenAiAsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.ParaformerSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.ParakeetSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.PunctuationModelSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.Qwen3AsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.SenseVoiceSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.SiliconFlowSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.SonioxAsrSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.StepAudioSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.VolcengineSettingsSection
import com.brycewg.asrkb.ui.settings.asr.sections.ZhipuAsrSettingsSection
import com.brycewg.asrkb.ui.settings.search.SettingsSearchNavigator
import kotlinx.coroutines.launch

class AsrSettingsActivity : BaseActivity() {

    private lateinit var viewModel: AsrSettingsViewModel
    private lateinit var prefs: Prefs
    private lateinit var binding: AsrSettingsBinding
    private lateinit var sections: List<AsrSettingsSection>

    private val modelImportUiController by lazy(LazyThreadSafetyMode.NONE) {
        ModelImportUiController(this)
    }
    private val modelDownloadUiController by lazy(LazyThreadSafetyMode.NONE) {
        ModelDownloadUiController(this)
    }

    private val senseVoiceModelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleSenseVoiceModelImport(it) }
        }
    private val funAsrNanoModelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleFunAsrNanoModelImport(it) }
        }
    private val qwen3AsrModelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleQwen3AsrModelImport(it) }
        }
    private val parakeetModelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleParakeetModelImport(it) }
        }
    private val fireRedAsrModelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleFireRedAsrModelImport(it) }
        }
    private val paraformerModelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleParaformerModelImport(it) }
        }
    private val punctuationModelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handlePunctuationModelImport(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asr_settings)

        val rootView = findViewById<android.view.View>(android.R.id.content)
        WindowInsetsHelper.applySystemBarsInsets(rootView)

        prefs = Prefs(this)
        viewModel = ViewModelProvider(this)[AsrSettingsViewModel::class.java]
        viewModel.initialize(this)
        consumeSearchForcedVendorIfNeeded()

        binding = AsrSettingsBinding(
            activity = this,
            rootView = rootView,
            prefs = prefs,
            viewModel = viewModel,
            modelImportUiController = modelImportUiController,
            modelDownloadUiController = modelDownloadUiController,
            senseVoiceModelPicker = senseVoiceModelPicker,
            funAsrNanoModelPicker = funAsrNanoModelPicker,
            qwen3AsrModelPicker = qwen3AsrModelPicker,
            parakeetModelPicker = parakeetModelPicker,
            fireRedAsrModelPicker = fireRedAsrModelPicker,
            paraformerModelPicker = paraformerModelPicker,
            punctuationModelPicker = punctuationModelPicker
        )

        sections = listOf(
            AsrSettingsToolbarSection(),
            AsrVendorSelectionSection(),
            AsrSilenceDetectionSection(),
            VolcengineSettingsSection(),
            SiliconFlowSettingsSection(),
            ElevenLabsSettingsSection(),
            OpenAiAsrSettingsSection(),
            DashScopeAsrSettingsSection(),
            GeminiAsrSettingsSection(),
            SonioxAsrSettingsSection(),
            StepAudioSettingsSection(),
            ZhipuAsrSettingsSection(),
            SenseVoiceSettingsSection(),
            FunAsrNanoSettingsSection(),
            Qwen3AsrSettingsSection(),
            ParakeetSettingsSection(),
            FireRedAsrSettingsSection(),
            ParaformerSettingsSection(),
            PunctuationModelSettingsSection(),
            BackupAsrSection(),
            AsrSettingsUiRendererSection()
        )

        sections.forEach { it.bind(binding) }
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        sections.forEach { it.onResume(binding) }
    }

    override fun onPostResume() {
        super.onPostResume()
        SettingsSearchNavigator.applyScrollAndHighlightIfNeeded(this)
    }

    override fun onPause() {
        super.onPause()
        sections.forEach { it.onPause(binding) }
    }

    private fun handleSenseVoiceModelImport(uri: Uri) {
        val tv = findViewById<TextView>(R.id.tvSvDownloadStatus)
        modelImportUiController.startZipImport(
            uri = uri,
            statusTextViews = listOf(tv),
            startedTextResId = R.string.sv_import_started_in_bg,
            failedTextTemplateResId = R.string.sv_import_failed,
            variant = prefs.svModelVariant,
            logTag = TAG,
            logMessage = "Failed to start model import"
        )
    }

    private fun handleFireRedAsrModelImport(uri: Uri) {
        val tv = findViewById<TextView>(R.id.tvFrDownloadStatus)
        modelImportUiController.startZipImport(
            uri = uri,
            statusTextViews = listOf(tv),
            startedTextResId = R.string.fr_import_started_in_bg,
            failedTextTemplateResId = R.string.fr_import_failed,
            variant = prefs.frModelVariant,
            modelType = "firered_asr",
            logTag = TAG,
            logMessage = "Failed to start FireRedASR model import"
        )
    }

    private fun handleParaformerModelImport(uri: Uri) {
        val tv = findViewById<TextView>(R.id.tvPfDownloadStatus)
        modelImportUiController.startZipImport(
            uri = uri,
            statusTextViews = listOf(tv),
            startedTextResId = R.string.pf_import_started_in_bg,
            failedTextTemplateResId = R.string.pf_import_failed,
            variant = prefs.pfModelVariant,
            logTag = TAG,
            logMessage = "Failed to start paraformer model import"
        )
    }

    private fun handleFunAsrNanoModelImport(uri: Uri) {
        val tv = findViewById<TextView>(R.id.tvFnDownloadStatus)
        modelImportUiController.startZipImport(
            uri = uri,
            statusTextViews = listOf(tv),
            startedTextResId = R.string.fn_import_started_in_bg,
            failedTextTemplateResId = R.string.fn_import_failed,
            variant = prefs.fnModelVariant,
            modelType = "funasr_nano",
            logTag = TAG,
            logMessage = "Failed to start FunASR Nano model import"
        )
    }

    private fun handleQwen3AsrModelImport(uri: Uri) {
        val tv = findViewById<TextView>(R.id.tvQwDownloadStatus)
        modelImportUiController.startZipImport(
            uri = uri,
            statusTextViews = listOf(tv),
            startedTextResId = R.string.qw_import_started_in_bg,
            failedTextTemplateResId = R.string.qw_import_failed,
            variant = prefs.qwModelVariant,
            modelType = "qwen3_asr",
            logTag = TAG,
            logMessage = "Failed to start Qwen3-ASR model import"
        )
    }

    private fun handleParakeetModelImport(uri: Uri) {
        val tv = findViewById<TextView>(R.id.tvPkDownloadStatus)
        modelImportUiController.startZipImport(
            uri = uri,
            statusTextViews = listOf(tv),
            startedTextResId = R.string.pk_import_started_in_bg,
            failedTextTemplateResId = R.string.pk_import_failed,
            variant = prefs.pkModelVariant,
            modelType = "parakeet",
            logTag = TAG,
            logMessage = "Failed to start Parakeet model import"
        )
    }

    private fun handlePunctuationModelImport(uri: Uri) {
        val statusTextViews = listOf(
            findViewById<TextView?>(R.id.tvFrPunctStatus),
            findViewById<TextView?>(R.id.tvPfPunctStatus)
        ).filterNotNull()
        modelImportUiController.startZipImport(
            uri = uri,
            statusTextViews = statusTextViews,
            startedTextResId = R.string.punct_import_started_in_bg,
            failedTextTemplateResId = R.string.punct_import_failed,
            variant = "ct-zh-en-int8",
            modelType = "punctuation",
            logTag = TAG,
            logMessage = "Failed to start punctuation model import"
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    sections.forEach { it.render(binding, state) }
                }
            }
        }
    }

    private fun consumeSearchForcedVendorIfNeeded() {
        val id = intent?.getStringExtra(
            SettingsSearchNavigator.EXTRA_FORCE_ASR_VENDOR_ID
        )?.trim().orEmpty()
        if (id.isBlank()) return
        val vendor = com.brycewg.asrkb.asr.AsrVendor.fromId(id)
        val oldVendor = prefs.asrVendor
        if (vendor != oldVendor) {
            viewModel.updateVendor(vendor)
            Toast.makeText(
                this,
                getString(R.string.toast_search_changed_asr_vendor, getAsrVendorLabel(vendor)),
                Toast.LENGTH_SHORT
            ).show()
        }
        intent?.removeExtra(SettingsSearchNavigator.EXTRA_FORCE_ASR_VENDOR_ID)
    }

    private fun getAsrVendorLabel(vendor: com.brycewg.asrkb.asr.AsrVendor): String {
        val resId = when (vendor) {
            com.brycewg.asrkb.asr.AsrVendor.Volc -> R.string.vendor_volc
            com.brycewg.asrkb.asr.AsrVendor.SiliconFlow -> R.string.vendor_sf
            com.brycewg.asrkb.asr.AsrVendor.ElevenLabs -> R.string.vendor_eleven
            com.brycewg.asrkb.asr.AsrVendor.OpenAI -> R.string.vendor_openai
            com.brycewg.asrkb.asr.AsrVendor.DashScope -> R.string.vendor_dashscope
            com.brycewg.asrkb.asr.AsrVendor.Gemini -> R.string.vendor_gemini
            com.brycewg.asrkb.asr.AsrVendor.Soniox -> R.string.vendor_soniox
            com.brycewg.asrkb.asr.AsrVendor.StepAudio -> R.string.vendor_stepaudio
            com.brycewg.asrkb.asr.AsrVendor.Zhipu -> R.string.vendor_zhipu
            com.brycewg.asrkb.asr.AsrVendor.SenseVoice -> R.string.vendor_sensevoice
            com.brycewg.asrkb.asr.AsrVendor.FunAsrNano -> R.string.vendor_funasr_nano
            com.brycewg.asrkb.asr.AsrVendor.Qwen3Asr -> R.string.vendor_qwen3_asr
            com.brycewg.asrkb.asr.AsrVendor.Parakeet -> R.string.vendor_parakeet
            com.brycewg.asrkb.asr.AsrVendor.FireRedAsr -> R.string.vendor_firered_asr
            com.brycewg.asrkb.asr.AsrVendor.Paraformer -> R.string.vendor_paraformer
        }
        return getString(resId)
    }

    private companion object {
        private const val TAG = "AsrSettingsActivity"
    }
}
