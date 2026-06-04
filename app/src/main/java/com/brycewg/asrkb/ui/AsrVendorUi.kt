package com.brycewg.asrkb.ui

import android.content.Context
import com.brycewg.asrkb.R
import com.brycewg.asrkb.asr.AsrVendor

/**
 * 统一提供 ASR 供应商的顺序与显示名，避免各处硬编码 listOf。
 */
object AsrVendorUi {
    /** 固定的供应商顺序（设置页/菜单统一使用） */
    fun ordered(): List<AsrVendor> = listOf(
        AsrVendor.SiliconFlow,
        AsrVendor.Volc,
        AsrVendor.ElevenLabs,
        AsrVendor.OpenAI,
        AsrVendor.OpenRouter,
        AsrVendor.DashScope,
        AsrVendor.Gemini,
        AsrVendor.MiMo,
        AsrVendor.Soniox,
        AsrVendor.StepAudio,
        AsrVendor.Zhipu,
        AsrVendor.SenseVoice,
        AsrVendor.FunAsrNano,
        AsrVendor.Qwen3Asr,
        AsrVendor.Parakeet,
        AsrVendor.FireRedAsr,
        AsrVendor.Paraformer
    )

    /** 指定 vendor 的多语言显示名 */
    fun name(context: Context, v: AsrVendor): String = when (v) {
        AsrVendor.Volc -> context.getString(R.string.vendor_volc)
        AsrVendor.SiliconFlow -> context.getString(R.string.vendor_sf)
        AsrVendor.ElevenLabs -> context.getString(R.string.vendor_eleven)
        AsrVendor.OpenAI -> context.getString(R.string.vendor_openai)
        AsrVendor.OpenRouter -> context.getString(R.string.vendor_openrouter)
        AsrVendor.DashScope -> context.getString(R.string.vendor_dashscope)
        AsrVendor.Gemini -> context.getString(R.string.vendor_gemini)
        AsrVendor.MiMo -> context.getString(R.string.vendor_mimo)
        AsrVendor.Soniox -> context.getString(R.string.vendor_soniox)
        AsrVendor.StepAudio -> context.getString(R.string.vendor_stepaudio)
        AsrVendor.Zhipu -> context.getString(R.string.vendor_zhipu)
        AsrVendor.SenseVoice -> context.getString(R.string.vendor_sensevoice)
        AsrVendor.FunAsrNano -> context.getString(R.string.vendor_funasr_nano)
        AsrVendor.Qwen3Asr -> context.getString(R.string.vendor_qwen3_asr)
        AsrVendor.Parakeet -> context.getString(R.string.vendor_parakeet)
        AsrVendor.FireRedAsr -> context.getString(R.string.vendor_firered_asr)
        AsrVendor.Paraformer -> context.getString(R.string.vendor_paraformer)
    }

    /** 指定 vendor 的标签（用于选择器展示；可后续按需调整） */
    fun tags(v: AsrVendor): List<AsrVendorTag> = when (v) {
        AsrVendor.SiliconFlow -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.Custom
        )
        AsrVendor.Volc -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.Streaming,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.ChineseDialect,
            AsrVendorTag.Accurate
        )
        AsrVendor.ElevenLabs -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.Streaming,
            AsrVendorTag.NonStreaming
        )
        AsrVendor.OpenAI -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.Streaming,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.Custom
        )
        AsrVendor.OpenRouter -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.Custom
        )
        AsrVendor.DashScope -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.Streaming,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.ChineseDialect,
            AsrVendorTag.Accurate
        )
        AsrVendor.Gemini -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.Accurate,
            AsrVendorTag.Custom
        )
        AsrVendor.MiMo -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.Accurate,
            AsrVendorTag.Custom
        )
        AsrVendor.Soniox -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.Streaming,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.Accurate
        )
        AsrVendor.StepAudio -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.Accurate
        )
        AsrVendor.Zhipu -> listOf(
            AsrVendorTag.Online,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.ChineseDialect
        )
        AsrVendor.SenseVoice -> listOf(
            AsrVendorTag.Local,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.PseudoStreaming
        )
        AsrVendor.FunAsrNano -> listOf(
            AsrVendorTag.Local,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.ChineseDialect,
            AsrVendorTag.Accurate
        )
        AsrVendor.Qwen3Asr -> listOf(
            AsrVendorTag.Local,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.ChineseDialect,
            AsrVendorTag.Accurate
        )
        AsrVendor.Parakeet -> listOf(
            AsrVendorTag.Local,
            AsrVendorTag.NonStreaming
        )
        AsrVendor.FireRedAsr -> listOf(
            AsrVendorTag.Local,
            AsrVendorTag.NonStreaming,
            AsrVendorTag.PseudoStreaming
        )
        AsrVendor.Paraformer -> listOf(
            AsrVendorTag.Local,
            AsrVendorTag.Streaming
        )
    }

    /** 顺序化的 (Vendor, 显示名) 列表 */
    fun pairs(context: Context): List<Pair<AsrVendor, String>> = ordered().map {
        it to
            name(context, it)
    }

    /** 顺序化的显示名列表 */
    fun names(context: Context): List<String> = ordered().map { name(context, it) }
}
