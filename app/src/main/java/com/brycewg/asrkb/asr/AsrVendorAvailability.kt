/**
 * ASR 供应商可用性判断与分组工具：
 * - 在线供应商：按 API/鉴权配置是否完整判断
 * - 本地供应商：按模型文件是否已安装判断
 */
package com.brycewg.asrkb.asr

import android.content.Context
import android.util.Log
import com.brycewg.asrkb.store.Prefs
import java.io.File

internal data class AsrVendorPartition(
    val configured: List<AsrVendor>,
    val unconfigured: List<AsrVendor>
)

internal fun partitionAsrVendorsByConfigured(
    context: Context,
    prefs: Prefs,
    vendors: List<AsrVendor>
): AsrVendorPartition {
    val configured = mutableListOf<AsrVendor>()
    val unconfigured = mutableListOf<AsrVendor>()
    vendors.forEach { vendor ->
        if (isAsrVendorConfigured(context, prefs, vendor)) {
            configured.add(vendor)
        } else {
            unconfigured.add(vendor)
        }
    }
    return AsrVendorPartition(
        configured = configured,
        unconfigured = unconfigured
    )
}

internal fun isAsrVendorConfigured(context: Context, prefs: Prefs, vendor: AsrVendor): Boolean = try {
    when (vendor) {
        AsrVendor.SenseVoice -> hasSenseVoiceModelInstalled(context, prefs)
        AsrVendor.FunAsrNano -> hasFunAsrNanoModelInstalled(context, prefs)
        AsrVendor.Qwen3Asr -> hasQwen3AsrModelInstalled(context, prefs)
        AsrVendor.Parakeet -> hasParakeetModelInstalled(context, prefs)
        AsrVendor.FireRedAsr -> hasFireRedAsrModelInstalled(context, prefs)
        AsrVendor.XAsr -> hasXAsrModelInstalled(context)
        AsrVendor.SiliconFlow -> prefs.hasSfKeys()
        else -> prefs.hasVendorKeys(vendor)
    }
} catch (t: Throwable) {
    Log.w(TAG, "Failed to check vendor availability: $vendor", t)
    false
}

private fun hasSenseVoiceModelInstalled(context: Context, prefs: Prefs): Boolean {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    val root = File(base, "sensevoice")
    val variantDir = if (prefs.svModelVariant == "small-full") {
        File(root, "small-full")
    } else {
        File(root, "small-int8")
    }
    val modelDir = findSvModelDir(variantDir) ?: findSvModelDir(root)
    return modelDir != null &&
        File(modelDir, "tokens.txt").exists() &&
        (
            File(modelDir, "model.int8.onnx").exists() ||
                File(modelDir, "model.onnx").exists()
            )
}

private fun hasFunAsrNanoModelInstalled(context: Context, prefs: Prefs): Boolean {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    val root = File(base, "funasr_nano")
    val variantDir = File(root, normalizeFunAsrNanoVariant(prefs.fnModelVariant))
    val modelDir = findFnModelDir(variantDir) ?: findDirectFnModelDir(root)
    val tokenizerDir = modelDir?.let { findFnTokenizerDir(it) }
    return modelDir != null &&
        File(modelDir, "encoder_adaptor.int8.onnx").exists() &&
        File(modelDir, "llm.int8.onnx").exists() &&
        File(modelDir, "embedding.int8.onnx").exists() &&
        tokenizerDir != null &&
        File(tokenizerDir, "tokenizer.json").exists()
}

private fun hasQwen3AsrModelInstalled(context: Context, prefs: Prefs): Boolean {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    val root = File(base, "qwen3_asr")
    val variantDir = File(root, normalizeQwen3AsrVariant(prefs.qwModelVariant))
    val modelDir = findQwen3AsrModelDir(variantDir) ?: findQwen3AsrModelDir(root)
    val tokenizerDir = modelDir?.let { findQwen3AsrTokenizerDir(it) }
    return modelDir != null &&
        File(modelDir, "conv_frontend.onnx").exists() &&
        File(modelDir, "encoder.int8.onnx").exists() &&
        File(modelDir, "decoder.int8.onnx").exists() &&
        tokenizerDir != null &&
        isQwen3AsrTokenizerDir(tokenizerDir)
}

private fun hasFireRedAsrModelInstalled(context: Context, prefs: Prefs): Boolean {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    val root = File(base, "firered_asr")
    val variantDir = File(root, prefs.frModelVariant)
    return findFireRedAsrModelDir(variantDir) != null || findFireRedAsrModelDir(root) != null
}

private fun hasParakeetModelInstalled(context: Context, prefs: Prefs): Boolean {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    val root = File(base, "parakeet")
    val variantDir = File(root, normalizeParakeetVariant(prefs.pkModelVariant))
    val modelDir = findParakeetModelDir(variantDir) ?: findParakeetModelDir(root)
    return modelDir != null &&
        File(modelDir, "tokens.txt").exists() &&
        File(modelDir, "encoder.int8.onnx").exists() &&
        File(modelDir, "decoder.int8.onnx").exists() &&
        File(modelDir, "joiner.int8.onnx").exists()
}

private fun hasXAsrModelInstalled(context: Context): Boolean {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    return findXAsrModelFiles(File(base, "x_asr")) != null
}

private const val TAG = "AsrVendorAvailability"
