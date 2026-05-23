/**
 * API Log 页面：展示 ASR/LLM 外部请求的脱敏摘要。
 *
 * 归属模块：ui/history
 */
package com.brycewg.asrkb.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.brycewg.asrkb.R
import com.brycewg.asrkb.UiColorTokens
import com.brycewg.asrkb.UiColors
import com.brycewg.asrkb.store.ApiLogStore
import com.brycewg.asrkb.ui.BaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApiLogActivity : BaseActivity() {
    companion object {
        private const val TAG = "ApiLogActivity"
    }

    private enum class CategoryFilter { ALL, ASR, LLM, FAILED }

    private lateinit var adapter: ApiLogAdapter
    private lateinit var rv: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvEmpty: TextView
    private lateinit var chipAll: Chip
    private lateinit var chipAsr: Chip
    private lateinit var chipLlm: Chip
    private lateinit var chipFailed: Chip

    private var allRecords: List<ApiLogStore.ApiLogRecord> = emptyList()
    private var activeFilter: CategoryFilter = CategoryFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_log)
        findViewById<View>(android.R.id.content).let { rootView ->
            com.brycewg.asrkb.ui.WindowInsetsHelper.applySystemBarsInsets(rootView)
        }

        val tb = findViewById<MaterialToolbar>(R.id.toolbar)
        tb.setTitle(R.string.title_api_log)
        tb.setNavigationOnClickListener { finish() }
        try {
            setSupportActionBar(tb)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to setSupportActionBar", e)
            tb.inflateMenu(R.menu.menu_api_log)
            tb.setOnMenuItemClickListener { onOptionsItemSelected(it) }
        }

        rv = findViewById(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ApiLogAdapter()
        rv.adapter = adapter

        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                render()
            }
        })

        chipAll = findViewById(R.id.chipAll)
        chipAsr = findViewById(R.id.chipAsr)
        chipLlm = findViewById(R.id.chipLlm)
        chipFailed = findViewById(R.id.chipFailed)
        listOf(
            chipAll to CategoryFilter.ALL,
            chipAsr to CategoryFilter.ASR,
            chipLlm to CategoryFilter.LLM,
            chipFailed to CategoryFilter.FAILED
        ).forEach { (chip, filter) ->
            chip.setOnClickListener {
                activeFilter = filter
                updateFilterChips()
                render()
            }
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_api_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_clear_api_log -> {
            confirmClear()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun loadData() {
        allRecords = ApiLogStore.listAll()
        render()
    }

    private fun render() {
        val q = etSearch.text?.toString()?.trim().orEmpty()
        val filtered = allRecords.filter { r ->
            val okCategory = when (activeFilter) {
                CategoryFilter.ALL -> true
                CategoryFilter.ASR -> r.category.equals("ASR", ignoreCase = true)
                CategoryFilter.LLM -> r.category.equals("LLM", ignoreCase = true)
                CategoryFilter.FAILED -> !r.success && !r.canceled
            }
            val okQuery = q.isEmpty() || searchableText(r).contains(q, ignoreCase = true)
            okCategory && okQuery
        }
        adapter.submit(filtered)
        tvEmpty.text = if (allRecords.isEmpty()) {
            getString(R.string.empty_api_log)
        } else {
            getString(R.string.empty_api_log_filtered)
        }
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateFilterChips() {
        chipAll.isChecked = activeFilter == CategoryFilter.ALL
        chipAsr.isChecked = activeFilter == CategoryFilter.ASR
        chipLlm.isChecked = activeFilter == CategoryFilter.LLM
        chipFailed.isChecked = activeFilter == CategoryFilter.FAILED
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_clear_api_log_title)
            .setMessage(R.string.dialog_clear_api_log_message)
            .setPositiveButton(R.string.dialog_filter_ok) { _, _ ->
                ApiLogStore.clearAll()
                Toast.makeText(this, R.string.toast_api_log_cleared, Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton(R.string.dialog_filter_cancel, null)
            .show()
    }

    private fun copyToClipboard(text: String) {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("api_log", text))
            Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "copyToClipboard failed", e)
        }
    }

    private fun searchableText(r: ApiLogStore.ApiLogRecord): String = listOf(
        r.category,
        r.vendor,
        r.model,
        r.source,
        r.protocol,
        r.method,
        r.host,
        r.path,
        r.queryKeys.joinToString(","),
        r.requestSummary,
        r.requestStructure,
        r.responseSummary,
        r.errorSummary
    ).joinToString(" ")

    private class ApiLogDiff(
        private val old: List<ApiLogStore.ApiLogRecord>,
        private val new: List<ApiLogStore.ApiLogRecord>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = old[oldItemPosition].id == new[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = old[oldItemPosition] == new[newItemPosition]
    }

    private inner class ApiLogAdapter : RecyclerView.Adapter<ApiLogAdapter.VH>() {
        private val items = mutableListOf<ApiLogStore.ApiLogRecord>()

        fun submit(list: List<ApiLogStore.ApiLogRecord>) {
            val diff = ApiLogDiff(items, list)
            val result = DiffUtil.calculateDiff(diff)
            items.clear()
            items.addAll(list)
            result.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_api_log, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val card = v as MaterialCardView
            private val statusDot = v.findViewById<View>(R.id.statusDot)
            private val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
            private val tvTime = v.findViewById<TextView>(R.id.tvTime)
            private val tvEndpoint = v.findViewById<TextView>(R.id.tvEndpoint)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            fun bind(r: ApiLogStore.ApiLogRecord) {
                val status = when {
                    r.canceled -> itemView.context.getString(R.string.api_log_status_cancelled)
                    r.success -> itemView.context.getString(R.string.api_log_status_success)
                    else -> itemView.context.getString(R.string.api_log_status_failed)
                }
                val vendor = formatVendorName(r.vendor.ifBlank { itemView.context.getString(R.string.api_log_unknown) })
                tvTitle.text = "$status · ${r.category} · $vendor"
                tvTime.text = fmt.format(Date(r.timestamp))
                tvEndpoint.text = formatEndpoint(itemView.context, r)
                val codePart = if (r.httpCode != 0) "code ${r.httpCode}" else r.protocol
                val modelPart = r.model.takeIf { it.isNotBlank() } ?: "-"
                tvMeta.text = itemView.context.getString(
                    R.string.api_log_meta_format,
                    codePart,
                    r.durationMs,
                    modelPart
                )
                card.setCardBackgroundColor(UiColors.get(itemView, UiColorTokens.panelBg))
                statusDot.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(
                        UiColors.get(
                            itemView,
                            when {
                                r.canceled -> UiColorTokens.outline
                                r.success -> UiColorTokens.primary
                                else -> UiColorTokens.error
                            }
                        )
                    )
                }
                card.setOnClickListener {
                    showDetailsDialog(r)
                }
            }
        }
    }

    private fun showDetailsDialog(r: ApiLogStore.ApiLogRecord) {
        val content = layoutInflater.inflate(R.layout.dialog_api_log_details, null)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val tvDialogTitle = content.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDialogEndpoint = content.findViewById<TextView>(R.id.tvDialogEndpoint)
        val tvDialogMeta = content.findViewById<TextView>(R.id.tvDialogMeta)
        val tvDialogRequestValue = content.findViewById<TextView>(R.id.tvDialogRequestValue)
        val tvDialogStructureValue = content.findViewById<TextView>(R.id.tvDialogStructureValue)
        val tvDialogResponseValue = content.findViewById<TextView>(R.id.tvDialogResponseValue)
        val tvDialogErrorValue = content.findViewById<TextView>(R.id.tvDialogErrorValue)

        val status = when {
            r.canceled -> getString(R.string.api_log_status_cancelled)
            r.success -> getString(R.string.api_log_status_success)
            else -> getString(R.string.api_log_status_failed)
        }
        val vendor = formatVendorName(r.vendor.ifBlank { getString(R.string.api_log_unknown) })
        tvDialogTitle.text = "$status · ${r.category} · $vendor"
        tvDialogEndpoint.text = formatEndpoint(this, r)
        val codePart = if (r.httpCode != 0) "code ${r.httpCode}" else r.protocol
        val modelPart = r.model.takeIf { it.isNotBlank() } ?: "-"
        tvDialogMeta.text = getString(
            R.string.api_log_meta_format,
            codePart,
            r.durationMs,
            modelPart
        )
        tvDialogRequestValue.text = r.requestSummary.ifBlank { "-" }
        tvDialogStructureValue.text = r.requestStructure.ifBlank { "-" }
        tvDialogResponseValue.text = r.responseSummary.ifBlank { "-" }
        tvDialogErrorValue.text = r.errorSummary.ifBlank { "-" }

        MaterialAlertDialogBuilder(this)
            .setView(content)
            .setPositiveButton(R.string.btn_copy) { _, _ ->
                copyToClipboard(formatDetail(r, fmt))
            }
            .setNegativeButton(R.string.btn_close, null)
            .show()
    }

    private fun formatDetail(
        r: ApiLogStore.ApiLogRecord,
        fmt: SimpleDateFormat
    ): String = buildString {
        val status = when {
            r.canceled -> "CANCELED"
            r.success -> "SUCCESS"
            else -> "FAILED"
        }
        appendLine("${r.category} $status")
        appendLine("time=${fmt.format(Date(r.timestamp))}")
        appendLine("protocol=${r.protocol}")
        appendLine("method=${r.method}")
        appendLine("target=${formatEndpoint(this@ApiLogActivity, r)}")
        appendLine("queryKeys=${r.queryKeys}")
        appendLine("vendor=${formatVendorName(r.vendor)}")
        appendLine("model=${r.model}")
        appendLine("httpCode=${r.httpCode}")
        appendLine("durationMs=${r.durationMs}")
        appendLine("request=${r.requestSummary}")
        appendLine("structure=${r.requestStructure}")
        appendLine("response=${r.responseSummary}")
        if (r.errorSummary.isNotBlank()) appendLine("error=${r.errorSummary}")
    }

    private fun formatEndpoint(context: Context, r: ApiLogStore.ApiLogRecord): String {
        if (r.protocol.equals("Local", ignoreCase = true)) {
            val action = formatLocalAction(context, r.method)
            val source = formatLocalSource(context, r.source.ifBlank { r.path.ifBlank { r.vendor } })
            return context.getString(R.string.api_log_local_endpoint_format, action, source)
        }
        return "${r.method.ifBlank { r.protocol }} ${r.host}${r.path}"
    }

    private fun formatLocalAction(context: Context, action: String): String = when (action.lowercase(Locale.US)) {
        "infer" -> context.getString(R.string.api_log_local_action_infer)
        "load" -> context.getString(R.string.api_log_local_action_load)
        else -> action.ifBlank { context.getString(R.string.api_log_unknown) }
    }

    private fun formatLocalSource(context: Context, source: String): String = when (source.lowercase(Locale.US)) {
        "preload" -> context.getString(R.string.api_log_local_source_preload)
        "file" -> context.getString(R.string.api_log_local_source_file)
        "inference_load" -> context.getString(R.string.api_log_local_source_inference_load)
        "pseudo_stream_final" -> context.getString(R.string.api_log_local_source_pseudo_stream_final)
        "pseudo_stream_final_load" -> context.getString(R.string.api_log_local_source_pseudo_stream_final_load)
        "pseudo_stream_preview_load" -> context.getString(R.string.api_log_local_source_pseudo_stream_preview_load)
        "streaming_load" -> context.getString(R.string.api_log_local_source_streaming_load)
        "streaming" -> context.getString(R.string.api_log_local_source_streaming)
        "external_pcm_stream" -> context.getString(R.string.api_log_local_source_external_pcm_stream)
        else -> source.ifBlank { context.getString(R.string.api_log_unknown) }
    }

    private fun formatVendorName(vendor: String): String = when (vendor.lowercase(Locale.US)) {
        "sf_free", "siliconflow_free" -> "SiliconFlow Free"
        "siliconflow" -> "SiliconFlow"
        "openai" -> "OpenAI"
        "openrouter" -> "OpenRouter"
        "dashscope" -> "DashScope"
        "gemini" -> "Gemini"
        "soniox" -> "Soniox"
        "stepaudio" -> "StepAudio"
        "zhipu" -> "Zhipu"
        "volcengine" -> "Volcengine"
        "elevenlabs" -> "ElevenLabs"
        "deepseek" -> "DeepSeek"
        "moonshot" -> "Moonshot"
        "groq" -> "Groq"
        "cerebras" -> "Cerebras"
        "ohmygpt" -> "OhMyGPT"
        "fireworks" -> "Fireworks"
        "custom" -> "Custom"
        else -> vendor
    }
}
