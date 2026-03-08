/**
 * FireRedASR V2 本地离线识别引擎与预加载入口。
 *
 * 归属模块：asr
 */
package com.brycewg.asrkb.asr

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.withLock

internal class FireRedAsrFileAsrEngine(
    context: Context,
    scope: CoroutineScope,
    prefs: Prefs,
    listener: StreamingAsrEngine.Listener,
    onRequestDuration: ((Long) -> Unit)? = null
) : BaseFileAsrEngine(context, scope, prefs, listener, onRequestDuration),
    PcmBatchRecognizer {

    override val maxRecordDurationMillis: Int = 5 * 60 * 1000

    private fun showToast(resId: Int) {
        try {
            Handler(Looper.getMainLooper()).post {
                try {
                    Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to show toast", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to post toast", t)
        }
    }

    private fun notifyLoadStart() {
        val ui = listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi
        if (ui != null) {
            try {
                ui.onLocalModelLoadStart()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to notify load start", t)
            }
        } else {
            showToast(R.string.sv_loading_model)
        }
    }

    private fun notifyLoadDone() {
        val ui = listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi
        if (ui != null) {
            try {
                ui.onLocalModelLoadDone()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to notify load done", t)
            }
        }
    }

    override fun ensureReady(): Boolean {
        if (!super.ensureReady()) return false
        val manager = FireRedAsrOnnxManager.getInstance()
        if (!manager.isOnnxAvailable()) {
            try {
                listener.onError(context.getString(R.string.error_local_asr_not_ready))
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to send error callback", t)
            }
            return false
        }
        return true
    }

    override suspend fun recognize(pcm: ByteArray) {
        val t0 = System.currentTimeMillis()
        try {
            try {
                SherpaPunctuationManager.maybeWarnModelMissing(context)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to warn punctuation model missing", t)
            }

            val manager = FireRedAsrOnnxManager.getInstance()
            if (!manager.isOnnxAvailable()) {
                listener.onError(context.getString(R.string.error_local_asr_not_ready))
                return
            }

            val modelFiles = resolveFireRedAsrModelFiles(context, prefs)
            if (modelFiles == null) {
                listener.onError(context.getString(R.string.error_firered_asr_model_missing))
                return
            }

            val samples = fireRedAsrPcmToFloatArray(pcm)
            if (samples.isEmpty()) {
                listener.onError(context.getString(R.string.error_audio_empty))
                return
            }

            val keepMinutes = try {
                prefs.frKeepAliveMinutes
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to get keep alive minutes", t)
                -1
            }
            val keepMs = if (keepMinutes <= 0) 0L else keepMinutes.toLong() * 60_000L
            val alwaysKeep = keepMinutes < 0
            val numThreads = try {
                prefs.frNumThreads
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to get num threads", t)
                2
            }

            val text = manager.decodeOffline(
                assetManager = null,
                tokens = modelFiles.tokensPath,
                ctcModel = modelFiles.ctcModelPath,
                encoder = modelFiles.encoderPath,
                decoder = modelFiles.decoderPath,
                provider = "cpu",
                numThreads = numThreads,
                samples = samples,
                sampleRate = sampleRate,
                keepAliveMs = keepMs,
                alwaysKeep = alwaysKeep,
                onLoadStart = { notifyLoadStart() },
                onLoadDone = { notifyLoadDone() }
            )

            val sanitizedText = sanitizeFireRedAsrResult(text)
            if (sanitizedText.isNullOrEmpty()) {
                listener.onError(context.getString(R.string.error_asr_empty_result))
            } else {
                val useItn = try {
                    prefs.frUseItn
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to get frUseItn", t)
                    false
                }
                val normalized = if (useItn) ChineseItn.normalize(sanitizedText) else sanitizedText
                val finalText = try {
                    SherpaPunctuationManager.getInstance().addOfflinePunctuation(
                        context = context,
                        text = normalized,
                        numThreads = numThreads
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to apply offline punctuation", t)
                    normalized
                }
                listener.onFinal(finalText)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Recognition failed", t)
            listener.onError(
                context.getString(
                    R.string.error_recognize_failed_with_reason,
                    t.message ?: ""
                )
            )
        } finally {
            val dt = System.currentTimeMillis() - t0
            try {
                onRequestDuration?.invoke(dt)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to invoke duration callback", t)
            }
        }
    }

    override suspend fun recognizeFromPcm(pcm: ByteArray) {
        recognize(pcm)
    }

    private companion object {
        private const val TAG = "FireRedAsrFileAsrEngine"
    }
}

internal data class FireRedAsrResolvedModel(
    val variant: String,
    val modelDir: File,
    val tokensPath: String,
    val ctcModelPath: String? = null,
    val encoderPath: String? = null,
    val decoderPath: String? = null
)

internal fun unloadFireRedAsrRecognizer() {
    LocalModelLoadCoordinator.cancel()
    FireRedAsrOnnxManager.getInstance().unload()
}

internal fun isFireRedAsrPrepared(): Boolean {
    val manager = FireRedAsrOnnxManager.getInstance()
    return manager.isPrepared() || manager.isPreparing()
}

internal fun preloadFireRedAsrIfConfigured(
    context: Context,
    prefs: Prefs,
    onLoadStart: (() -> Unit)? = null,
    onLoadDone: (() -> Unit)? = null,
    suppressToastOnStart: Boolean = false,
    forImmediateUse: Boolean = false
) {
    try {
        val manager = FireRedAsrOnnxManager.getInstance()
        if (!manager.isOnnxAvailable()) return

        val modelFiles = resolveFireRedAsrModelFiles(context, prefs) ?: return
        val keepMinutes = prefs.frKeepAliveMinutes
        val keepMs = if (keepMinutes <= 0) 0L else keepMinutes.toLong() * 60_000L
        val alwaysKeep = keepMinutes < 0
        val numThreads = prefs.frNumThreads
        val key = buildString {
            append("firered_asr|")
            append("variant=")
            append(modelFiles.variant)
            append("|tokens=")
            append(modelFiles.tokensPath)
            append("|ctc=")
            append(modelFiles.ctcModelPath ?: "")
            append("|encoder=")
            append(modelFiles.encoderPath ?: "")
            append("|decoder=")
            append(modelFiles.decoderPath ?: "")
            append("|provider=cpu|threads=")
            append(numThreads)
        }

        val mainHandler = Handler(Looper.getMainLooper())
        LocalModelLoadCoordinator.request(key) {
            val t0 = android.os.SystemClock.uptimeMillis()
            val ok = manager.prepare(
                assetManager = null,
                tokens = modelFiles.tokensPath,
                ctcModel = modelFiles.ctcModelPath,
                encoder = modelFiles.encoderPath,
                decoder = modelFiles.decoderPath,
                provider = "cpu",
                numThreads = numThreads,
                keepAliveMs = keepMs,
                alwaysKeep = alwaysKeep,
                onLoadStart = {
                    if (!suppressToastOnStart) {
                        mainHandler.post {
                            Toast.makeText(
                                context,
                                context.getString(R.string.sv_loading_model),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    onLoadStart?.invoke()
                },
                onLoadDone = onLoadDone
            )
            if (ok) {
                try {
                    SherpaPunctuationManager.getInstance().ensureOfflineLoaded(
                        context = context,
                        numThreads = numThreads
                    )
                } catch (t: Throwable) {
                    Log.w("FireRedAsrFileAsrEngine", "Failed to preload punctuation model with FireRedASR", t)
                }
            }
            if (ok && !forImmediateUse) {
                val dt = (android.os.SystemClock.uptimeMillis() - t0).coerceAtLeast(0)
                mainHandler.post {
                    Toast.makeText(
                        context,
                        context.getString(R.string.sv_model_ready_with_ms, dt),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    } catch (t: Throwable) {
        Log.e("FireRedAsrFileAsrEngine", "Failed to preload FireRedASR model", t)
    }
}

internal fun findFireRedAsrModelDir(root: File?): File? {
    if (root == null || !root.exists() || !root.isDirectory || isFireRedTempModelDir(root)) return null
    val hasTokens = File(root, "tokens.txt").exists()
    val hasCtcModel = File(root, "model.int8.onnx").exists() || File(root, "model.onnx").exists()
    val hasAedModel =
        (File(root, "encoder.int8.onnx").exists() || File(root, "encoder.onnx").exists()) &&
            (File(root, "decoder.int8.onnx").exists() || File(root, "decoder.onnx").exists())
    if (hasTokens && (hasCtcModel || hasAedModel)) {
        return root
    }
    return root.listFiles()?.firstNotNullOfOrNull { child ->
        if (child.isDirectory && !isFireRedTempModelDir(child)) findFireRedAsrModelDir(child) else null
    }
}

private fun isFireRedTempModelDir(dir: File): Boolean = dir.name.startsWith(".tmp_")

internal fun resolveFireRedAsrModelFiles(context: Context, prefs: Prefs): FireRedAsrResolvedModel? {
    val base = try {
        context.getExternalFilesDir(null)
    } catch (t: Throwable) {
        Log.w("FireRedAsrResolver", "Failed to get external files dir", t)
        null
    } ?: context.filesDir

    val probeRoot = File(base, "firered_asr")
    val preferredVariant = try {
        prefs.frModelVariant
    } catch (t: Throwable) {
        Log.w("FireRedAsrResolver", "Failed to get frModelVariant", t)
        "ctc-int8"
    }
    val cacheKey = probeRoot.absolutePath + "|" + preferredVariant
    val cached = FireRedAsrModelResolverCache.get(cacheKey)
    if (cached != null) return cached

    val variantDir = File(probeRoot, preferredVariant)
    val modelDir = findFireRedAsrModelDir(variantDir) ?: findFireRedAsrModelDir(probeRoot) ?: return null

    val tokens = File(modelDir, "tokens.txt")
    if (!tokens.exists()) return null

    val ctcModel = firstExistingFile(modelDir, "model.int8.onnx", "model.onnx") ?: return null

    return FireRedAsrResolvedModel(
        variant = "ctc-int8",
        modelDir = modelDir,
        tokensPath = tokens.absolutePath,
        ctcModelPath = ctcModel.absolutePath,
        encoderPath = null,
        decoderPath = null
    ).also {
        FireRedAsrModelResolverCache.put(cacheKey, it)
    }
}

private object FireRedAsrModelResolverCache {
    @Volatile private var cachedKey: String? = null

    @Volatile private var cachedValue: FireRedAsrResolvedModel? = null

    fun get(key: String): FireRedAsrResolvedModel? {
        val cached = if (cachedKey == key) cachedValue else null
        return cached?.takeIf(::isUsable)
    }

    fun put(key: String, model: FireRedAsrResolvedModel) {
        cachedKey = key
        cachedValue = model
    }

    private fun isUsable(model: FireRedAsrResolvedModel): Boolean {
        if (!File(model.tokensPath).exists()) return false
        val ctcModelPath = model.ctcModelPath
        if (ctcModelPath != null) {
            return File(ctcModelPath).exists()
        }
        val encoderPath = model.encoderPath ?: return false
        val decoderPath = model.decoderPath ?: return false
        return File(encoderPath).exists() && File(decoderPath).exists()
    }
}

private fun firstExistingFile(dir: File, vararg names: String): File? {
    for (name in names) {
        val file = File(dir, name)
        if (file.exists()) return file
    }
    return null
}

internal fun sanitizeFireRedAsrResult(text: String?): String {
    if (text.isNullOrBlank()) return ""
    return text
        .replace("<sil>", "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

internal fun fireRedAsrPcmToFloatArray(pcm: ByteArray): FloatArray {
    if (pcm.isEmpty()) return FloatArray(0)
    val n = pcm.size / 2
    val out = FloatArray(n)
    var index = 0
    var offset = 0
    while (index < n) {
        val sample = (pcm[offset + 1].toInt() shl 8) or (pcm[offset].toInt() and 0xFF)
        var value = sample / 32768.0f
        if (value > 1f) {
            value = 1f
        } else if (value < -1f) {
            value = -1f
        }
        out[index] = value
        index++
        offset += 2
    }
    return out
}

internal class FireRedAsrOnnxManager private constructor() : BaseSherpaOfflineRecognizerManager() {

    companion object {
        private const val TAG = "FireRedAsrOnnxManager"

        @Volatile
        private var instance: FireRedAsrOnnxManager? = null

        fun getInstance(): FireRedAsrOnnxManager = instance ?: synchronized(this) {
            instance ?: FireRedAsrOnnxManager().also { instance = it }
        }
    }

    protected override val tag: String = TAG

    @Volatile private var clsOfflineRecognizer: Class<*>? = null

    @Volatile private var clsOfflineRecognizerConfig: Class<*>? = null

    @Volatile private var clsOfflineModelConfig: Class<*>? = null

    @Volatile private var clsFeatureConfig: Class<*>? = null

    @Volatile private var clsOfflineFireRedAsrModelConfig: Class<*>? = null

    @Volatile private var clsOfflineFireRedAsrCtcModelConfig: Class<*>? = null

    fun isOnnxAvailable(): Boolean = sherpaIsClassAvailable(
        TAG,
        "com.k2fsa.sherpa.onnx.OfflineRecognizer"
    )

    private data class RecognizerConfig(
        val tokens: String,
        val ctcModel: String?,
        val encoder: String?,
        val decoder: String?,
        val provider: String,
        val numThreads: Int,
        val sampleRate: Int,
        val featureDim: Int
    )

    protected override fun initClasses() {
        if (clsOfflineRecognizer == null) {
            clsOfflineRecognizer = Class.forName("com.k2fsa.sherpa.onnx.OfflineRecognizer")
            clsOfflineRecognizerConfig =
                Class.forName("com.k2fsa.sherpa.onnx.OfflineRecognizerConfig")
            clsOfflineModelConfig = Class.forName("com.k2fsa.sherpa.onnx.OfflineModelConfig")
            clsFeatureConfig = Class.forName("com.k2fsa.sherpa.onnx.FeatureConfig")
            clsOfflineFireRedAsrModelConfig = runCatching {
                Class.forName("com.k2fsa.sherpa.onnx.OfflineFireRedAsrModelConfig")
            }.getOrNull()
            clsOfflineFireRedAsrCtcModelConfig = runCatching {
                Class.forName("com.k2fsa.sherpa.onnx.OfflineFireRedAsrCtcModelConfig")
            }.getOrNull()
            Log.d(TAG, "Initialized sherpa-onnx reflection classes for FireRedASR")
        }
    }

    private fun trySetField(target: Any, name: String, value: Any?): Boolean = sherpaTrySetField(TAG, target, name, value)

    private fun ensureAedClass(): Class<*> {
        val cached = clsOfflineFireRedAsrModelConfig
        if (cached != null) return cached
        return Class.forName("com.k2fsa.sherpa.onnx.OfflineFireRedAsrModelConfig").also {
            clsOfflineFireRedAsrModelConfig = it
        }
    }

    private fun ensureCtcClass(): Class<*> {
        val cached = clsOfflineFireRedAsrCtcModelConfig
        if (cached != null) return cached
        return Class.forName("com.k2fsa.sherpa.onnx.OfflineFireRedAsrCtcModelConfig").also {
            clsOfflineFireRedAsrCtcModelConfig = it
        }
    }

    private fun buildFeatureConfig(sampleRate: Int, featureDim: Int): Any {
        val feat = clsFeatureConfig!!.getDeclaredConstructor().newInstance()
        trySetField(feat, "sampleRate", sampleRate)
        trySetField(feat, "featureDim", featureDim)
        trySetField(feat, "dither", 0.0f)
        return feat
    }

    private fun buildFireRedAsrModelConfig(encoder: String, decoder: String): Any {
        val inst = ensureAedClass().getDeclaredConstructor().newInstance()
        trySetField(inst, "encoder", encoder)
        trySetField(inst, "decoder", decoder)
        return inst
    }

    private fun buildFireRedAsrCtcModelConfig(model: String): Any {
        val inst = ensureCtcClass().getDeclaredConstructor().newInstance()
        trySetField(inst, "model", model)
        return inst
    }

    private fun buildModelConfig(
        tokens: String,
        ctcModel: String?,
        encoder: String?,
        decoder: String?,
        numThreads: Int,
        provider: String
    ): Any {
        val modelConfig = clsOfflineModelConfig!!.getDeclaredConstructor().newInstance()
        trySetField(modelConfig, "tokens", tokens)
        trySetField(modelConfig, "numThreads", numThreads)
        trySetField(modelConfig, "provider", provider)
        trySetField(modelConfig, "debug", false)

        if (!ctcModel.isNullOrBlank()) {
            val fireRedAsrCtc = buildFireRedAsrCtcModelConfig(ctcModel)
            if (!trySetField(modelConfig, "fireRedAsrCtc", fireRedAsrCtc)) {
                trySetField(modelConfig, "fire_red_asr_ctc", fireRedAsrCtc)
            }
        } else {
            require(!encoder.isNullOrBlank() && !decoder.isNullOrBlank()) {
                "FireRedASR AED model files are missing"
            }
            val fireRedAsr = buildFireRedAsrModelConfig(encoder, decoder)
            if (!trySetField(modelConfig, "fireRedAsr", fireRedAsr)) {
                trySetField(modelConfig, "fire_red_asr", fireRedAsr)
            }
        }

        return modelConfig
    }

    protected override fun buildRecognizerConfig(config: Any): Any {
        config as RecognizerConfig
        val modelConfig = buildModelConfig(
            tokens = config.tokens,
            ctcModel = config.ctcModel,
            encoder = config.encoder,
            decoder = config.decoder,
            numThreads = config.numThreads,
            provider = config.provider
        )
        val featConfig = buildFeatureConfig(config.sampleRate, config.featureDim)
        val recConfig = clsOfflineRecognizerConfig!!.getDeclaredConstructor().newInstance()
        if (!trySetField(recConfig, "modelConfig", modelConfig)) {
            trySetField(recConfig, "model_config", modelConfig)
        }
        if (!trySetField(recConfig, "featConfig", featConfig)) {
            trySetField(recConfig, "feat_config", featConfig)
        }
        trySetField(recConfig, "decodingMethod", "greedy_search")
        trySetField(recConfig, "maxActivePaths", 4)
        return recConfig
    }

    protected override fun createRecognizer(
        assetManager: android.content.res.AssetManager?,
        recognizerConfig: Any
    ): Any = sherpaCreateOfflineRecognizer(
        tag = TAG,
        recognizerClass = clsOfflineRecognizer!!,
        recognizerConfigClass = clsOfflineRecognizerConfig!!,
        assetManager = assetManager,
        recognizerConfig = recognizerConfig
    )

    protected override fun offlineRecognizerClass(): Class<*> = clsOfflineRecognizer!!

    suspend fun decodeOffline(
        assetManager: android.content.res.AssetManager?,
        tokens: String,
        ctcModel: String?,
        encoder: String?,
        decoder: String?,
        provider: String,
        numThreads: Int,
        samples: FloatArray,
        sampleRate: Int,
        keepAliveMs: Long,
        alwaysKeep: Boolean,
        onLoadStart: (() -> Unit)? = null,
        onLoadDone: (() -> Unit)? = null
    ): String? = mutex.withLock {
        try {
            val cfg = RecognizerConfig(
                tokens = tokens,
                ctcModel = ctcModel,
                encoder = encoder,
                decoder = decoder,
                provider = provider,
                numThreads = numThreads,
                sampleRate = sampleRate,
                featureDim = 80
            )
            val recognizer = ensurePreparedLocked(assetManager, cfg, onLoadStart, onLoadDone)
                ?: return@withLock null
            val stream = recognizer.createStream()
            try {
                stream.acceptWaveform(samples, sampleRate)
                val text = recognizer.decode(stream)
                scheduleAutoUnload(keepAliveMs, alwaysKeep)
                return@withLock text
            } finally {
                stream.release()
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to decode offline FireRedASR: ${t.message}", t)
            return@withLock null
        }
    }

    suspend fun prepare(
        assetManager: android.content.res.AssetManager?,
        tokens: String,
        ctcModel: String?,
        encoder: String?,
        decoder: String?,
        provider: String,
        numThreads: Int,
        keepAliveMs: Long,
        alwaysKeep: Boolean,
        onLoadStart: (() -> Unit)? = null,
        onLoadDone: (() -> Unit)? = null
    ): Boolean = mutex.withLock {
        try {
            val cfg = RecognizerConfig(
                tokens = tokens,
                ctcModel = ctcModel,
                encoder = encoder,
                decoder = decoder,
                provider = provider,
                numThreads = numThreads,
                sampleRate = 16000,
                featureDim = 80
            )
            val ok = ensurePreparedLocked(assetManager, cfg, onLoadStart, onLoadDone) != null
            if (!ok) return@withLock false
            true
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to prepare FireRedASR recognizer: ${t.message}", t)
            false
        }
    }
}
