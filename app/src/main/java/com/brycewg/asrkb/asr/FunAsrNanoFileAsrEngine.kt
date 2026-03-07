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

/**
 * 本地 FunASR Nano（通过 sherpa-onnx）非流式文件识别引擎。
 * - 基于 OfflineRecognizer + OfflineModelConfig.funasrNano 字段（多组件：embedding/encoder_adaptor/llm/tokenizer）。
 * - 目前仅支持 int8 版本。
 */
class FunAsrNanoFileAsrEngine(
    context: Context,
    scope: CoroutineScope,
    prefs: Prefs,
    listener: StreamingAsrEngine.Listener,
    onRequestDuration: ((Long) -> Unit)? = null
) : BaseFileAsrEngine(context, scope, prefs, listener, onRequestDuration),
    PcmBatchRecognizer {

    // FunASR Nano 本地：同 SenseVoice/TeleSpeech，默认限制为 5 分钟以控制内存与处理时长
    override val maxRecordDurationMillis: Int = 5 * 60 * 1000

    private fun showToast(resId: Int) {
        try {
            Handler(Looper.getMainLooper()).post {
                try {
                    Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
                } catch (t: Throwable) {
                    Log.e("FunAsrNanoFileAsrEngine", "Failed to show toast", t)
                }
            }
        } catch (t: Throwable) {
            Log.e("FunAsrNanoFileAsrEngine", "Failed to post toast", t)
        }
    }

    private fun notifyLoadStart() {
        val ui = (listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi)
        if (ui != null) {
            try {
                ui.onLocalModelLoadStart()
            } catch (t: Throwable) {
                Log.e("FunAsrNanoFileAsrEngine", "Failed to notify load start", t)
            }
        } else {
            showToast(R.string.sv_loading_model)
        }
    }

    private fun notifyLoadDone() {
        val ui = (listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi)
        if (ui != null) {
            try {
                ui.onLocalModelLoadDone()
            } catch (t: Throwable) {
                Log.e("FunAsrNanoFileAsrEngine", "Failed to notify load done", t)
            }
        }
    }

    override fun ensureReady(): Boolean {
        if (!super.ensureReady()) return false
        val manager = FunAsrNanoOnnxManager.getInstance()
        if (!manager.isOnnxAvailable()) {
            try {
                listener.onError(context.getString(R.string.error_local_asr_not_ready))
            } catch (t: Throwable) {
                Log.e("FunAsrNanoFileAsrEngine", "Failed to send error callback", t)
            }
            return false
        }
        return true
    }

    override suspend fun recognize(pcm: ByteArray) {
        val t0 = System.currentTimeMillis()
        try {
            val manager = FunAsrNanoOnnxManager.getInstance()
            if (!manager.isOnnxAvailable()) {
                listener.onError(context.getString(R.string.error_local_asr_not_ready))
                return
            }

            val base = try {
                context.getExternalFilesDir(null)
            } catch (t: Throwable) {
                Log.w("FunAsrNanoFileAsrEngine", "Failed to get external files dir", t)
                null
            } ?: context.filesDir

            val probeRoot = File(base, "funasr_nano")
            val variantDir = File(probeRoot, "nano-int8")
            val modelDir = findFnModelDir(variantDir) ?: findFnModelDir(probeRoot)
            if (modelDir == null) {
                listener.onError(context.getString(R.string.error_funasr_model_missing))
                return
            }

            val encoderAdaptor = File(modelDir, "encoder_adaptor.int8.onnx")
            val llm = File(modelDir, "llm.int8.onnx")
            val embedding = File(modelDir, "embedding.int8.onnx")
            val tokenizerDir = findFnTokenizerDir(modelDir)

            val minOnnxBytes = 8L * 1024L * 1024L
            val minLlmBytes = 32L * 1024L * 1024L
            val tokenizerJsonOk = tokenizerDir?.let { File(it, "tokenizer.json").exists() } == true
            if (
                !encoderAdaptor.exists() ||
                !embedding.exists() ||
                !llm.exists() ||
                tokenizerDir == null ||
                !tokenizerJsonOk ||
                encoderAdaptor.length() < minOnnxBytes ||
                embedding.length() < minOnnxBytes ||
                llm.length() < minLlmBytes
            ) {
                listener.onError(context.getString(R.string.error_funasr_model_missing))
                return
            }

            val samples = pcmToFloatArray(pcm)
            if (samples.isEmpty()) {
                listener.onError(context.getString(R.string.error_audio_empty))
                return
            }

            val keepMinutes = try {
                prefs.fnKeepAliveMinutes
            } catch (t: Throwable) {
                Log.w("FunAsrNanoFileAsrEngine", "Failed to get keep alive minutes", t)
                -1
            }
            val keepMs = if (keepMinutes <= 0) 0L else keepMinutes.toLong() * 60_000L
            val alwaysKeep = keepMinutes < 0

            val userPrompt = try {
                prefs.fnUserPrompt.trim().ifBlank { "语音转写：" }
            } catch (t: Throwable) {
                Log.w("FunAsrNanoFileAsrEngine", "Failed to get fnUserPrompt", t)
                "语音转写："
            }

            val text = manager.decodeOffline(
                assetManager = null,
                encoderAdaptor = encoderAdaptor.absolutePath,
                llm = llm.absolutePath,
                embedding = embedding.absolutePath,
                tokenizerDir = tokenizerDir.absolutePath,
                userPrompt = userPrompt,
                provider = "cpu",
                numThreads = try {
                    prefs.fnNumThreads
                } catch (t: Throwable) {
                    Log.w("FunAsrNanoFileAsrEngine", "Failed to get num threads", t)
                    2
                },
                samples = samples,
                sampleRate = sampleRate,
                keepAliveMs = keepMs,
                alwaysKeep = alwaysKeep,
                onLoadStart = { notifyLoadStart() },
                onLoadDone = { notifyLoadDone() }
            )

            if (text.isNullOrBlank()) {
                listener.onError(context.getString(R.string.error_asr_empty_result))
            } else {
                val raw = text.trim()
                val useItn = try {
                    prefs.fnUseItn
                } catch (t: Throwable) {
                    Log.w("FunAsrNanoFileAsrEngine", "Failed to get fnUseItn", t)
                    false
                }
                val finalText = if (useItn) ChineseItn.normalize(raw) else raw
                listener.onFinal(finalText)
            }
        } catch (t: Throwable) {
            Log.e("FunAsrNanoFileAsrEngine", "Recognition failed", t)
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
                Log.e("FunAsrNanoFileAsrEngine", "Failed to invoke duration callback", t)
            }
        }
    }

    override suspend fun recognizeFromPcm(pcm: ByteArray) {
        recognize(pcm)
    }

    private fun pcmToFloatArray(pcm: ByteArray): FloatArray {
        if (pcm.isEmpty()) return FloatArray(0)
        val n = pcm.size / 2
        val out = FloatArray(n)
        val bb = java.nio.ByteBuffer.wrap(pcm).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i < n) {
            val s = bb.short.toInt()
            var f = s / 32768.0f
            if (f > 1f) {
                f = 1f
            } else if (f < -1f) {
                f = -1f
            }
            out[i] = f
            i++
        }
        return out
    }
}

// 公开卸载入口：供设置页在清除模型后释放本地识别器内存
fun unloadFunAsrNanoRecognizer() {
    LocalModelLoadCoordinator.cancel()
    FunAsrNanoOnnxManager.getInstance().unload()
}

// 判断是否已有缓存的本地识别器（已加载或正在加载中）
fun isFunAsrNanoPrepared(): Boolean {
    val manager = FunAsrNanoOnnxManager.getInstance()
    return manager.isPrepared() || manager.isPreparing()
}

// FunASR Nano 模型目录探测：
// - 官方包内不含 tokens.txt；通过 onnx + tokenizer 目录判定（最多一层）
fun findFnModelDir(root: File?): File? {
    if (root == null || !root.exists()) return null
    if (isFnModelDir(root)) return root
    val subs = root.listFiles() ?: return null
    for (f in subs) {
        if (f.isDirectory && isFnModelDir(f)) return f
    }
    return null
}

fun findFnTokenizerDir(modelDir: File): File? {
    val direct = File(modelDir, "tokenizer.json")
    if (direct.exists()) return modelDir
    val qwen = File(modelDir, "Qwen3-0.6B")
    if (File(qwen, "tokenizer.json").exists()) return qwen
    val subs = modelDir.listFiles() ?: return null
    for (f in subs) {
        if (f.isDirectory && File(f, "tokenizer.json").exists()) return f
    }
    return null
}

private fun isFnModelDir(dir: File): Boolean {
    val encoderAdaptor = File(dir, "encoder_adaptor.int8.onnx")
    val llm = File(dir, "llm.int8.onnx")
    val embedding = File(dir, "embedding.int8.onnx")
    if (!encoderAdaptor.exists() || !llm.exists() || !embedding.exists()) return false
    return findFnTokenizerDir(dir) != null
}

/**
 * FunASR Nano ONNX 识别器管理器（基于 OfflineRecognizer）
 */
internal class FunAsrNanoOnnxManager private constructor() : BaseSherpaOfflineRecognizerManager() {

    companion object {
        private const val TAG = "FunAsrNanoOnnxManager"

        @Volatile
        private var instance: FunAsrNanoOnnxManager? = null

        fun getInstance(): FunAsrNanoOnnxManager = instance ?: synchronized(this) {
            instance ?: FunAsrNanoOnnxManager().also { instance = it }
        }
    }

    protected override val tag: String = TAG

    @Volatile
    private var clsOfflineRecognizer: Class<*>? = null

    @Volatile
    private var clsOfflineRecognizerConfig: Class<*>? = null

    @Volatile
    private var clsOfflineModelConfig: Class<*>? = null

    @Volatile
    private var clsFeatureConfig: Class<*>? = null

    @Volatile
    private var clsOfflineFunAsrNanoModelConfig: Class<*>? = null

    fun isOnnxAvailable(): Boolean = sherpaIsClassAvailable(
        TAG,
        "com.k2fsa.sherpa.onnx.OfflineRecognizer",
        "com.k2fsa.sherpa.onnx.OfflineFunAsrNanoModelConfig"
    )

    private data class RecognizerConfig(
        val encoderAdaptor: String,
        val llm: String,
        val embedding: String,
        val tokenizerDir: String,
        val userPrompt: String,
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
            clsOfflineFunAsrNanoModelConfig =
                Class.forName("com.k2fsa.sherpa.onnx.OfflineFunAsrNanoModelConfig")
            Log.d(TAG, "Initialized sherpa-onnx reflection classes for FunASR Nano")
        }
    }

    private fun trySetField(target: Any, name: String, value: Any?): Boolean = sherpaTrySetField(TAG, target, name, value)

    private fun buildFeatureConfig(sampleRate: Int, featureDim: Int): Any {
        val feat = clsFeatureConfig!!.getDeclaredConstructor().newInstance()
        trySetField(feat, "sampleRate", sampleRate)
        trySetField(feat, "featureDim", featureDim)
        return feat
    }

    private fun buildFunAsrNanoModelConfig(
        encoderAdaptor: String,
        llm: String,
        embedding: String,
        tokenizerDir: String,
        userPrompt: String
    ): Any {
        val inst = clsOfflineFunAsrNanoModelConfig!!.getDeclaredConstructor().newInstance()
        trySetField(inst, "encoderAdaptor", encoderAdaptor)
        trySetField(inst, "llm", llm)
        trySetField(inst, "embedding", embedding)
        trySetField(inst, "tokenizer", tokenizerDir)
        trySetField(inst, "userPrompt", userPrompt)
        return inst
    }

    private fun buildModelConfig(
        encoderAdaptor: String,
        llm: String,
        embedding: String,
        tokenizerDir: String,
        userPrompt: String,
        numThreads: Int,
        provider: String
    ): Any {
        val modelConfig = clsOfflineModelConfig!!.getDeclaredConstructor().newInstance()
        trySetField(modelConfig, "tokens", "")
        trySetField(modelConfig, "numThreads", numThreads)
        trySetField(modelConfig, "provider", provider)
        trySetField(modelConfig, "debug", false)

        val funasrNano =
            buildFunAsrNanoModelConfig(encoderAdaptor, llm, embedding, tokenizerDir, userPrompt)
        if (!trySetField(modelConfig, "funasrNano", funasrNano)) {
            trySetField(modelConfig, "funasr_nano", funasrNano)
        }
        return modelConfig
    }

    protected override fun buildRecognizerConfig(config: Any): Any {
        config as RecognizerConfig
        val modelConfig = buildModelConfig(
            encoderAdaptor = config.encoderAdaptor,
            llm = config.llm,
            embedding = config.embedding,
            tokenizerDir = config.tokenizerDir,
            userPrompt = config.userPrompt,
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
        encoderAdaptor: String,
        llm: String,
        embedding: String,
        tokenizerDir: String,
        userPrompt: String,
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
                encoderAdaptor = encoderAdaptor,
                llm = llm,
                embedding = embedding,
                tokenizerDir = tokenizerDir,
                userPrompt = userPrompt,
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
            Log.e(TAG, "Failed to decode offline FunASR Nano: ${t.message}", t)
            return@withLock null
        }
    }

    suspend fun prepare(
        assetManager: android.content.res.AssetManager?,
        encoderAdaptor: String,
        llm: String,
        embedding: String,
        tokenizerDir: String,
        userPrompt: String,
        provider: String,
        numThreads: Int,
        keepAliveMs: Long,
        alwaysKeep: Boolean,
        onLoadStart: (() -> Unit)? = null,
        onLoadDone: (() -> Unit)? = null
    ): Boolean = mutex.withLock {
        try {
            val cfg = RecognizerConfig(
                encoderAdaptor = encoderAdaptor,
                llm = llm,
                embedding = embedding,
                tokenizerDir = tokenizerDir,
                userPrompt = userPrompt,
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
            Log.e(TAG, "Failed to prepare FunASR Nano recognizer: ${t.message}", t)
            false
        }
    }
}

/**
 * FunASR Nano 预加载：根据当前配置尝试构建本地识别器，便于降低首次点击等待
 */
fun preloadFunAsrNanoIfConfigured(
    context: Context,
    prefs: Prefs,
    onLoadStart: (() -> Unit)? = null,
    onLoadDone: (() -> Unit)? = null,
    suppressToastOnStart: Boolean = false,
    forImmediateUse: Boolean = false
) {
    try {
        val manager = FunAsrNanoOnnxManager.getInstance()
        if (!manager.isOnnxAvailable()) return

        val base = context.getExternalFilesDir(null) ?: context.filesDir

        val probeRoot = File(base, "funasr_nano")
        val variantDir = File(probeRoot, "nano-int8")
        val modelDir = findFnModelDir(variantDir) ?: findFnModelDir(probeRoot) ?: return

        val encoderAdaptor = File(modelDir, "encoder_adaptor.int8.onnx")
        val llm = File(modelDir, "llm.int8.onnx")
        val embedding = File(modelDir, "embedding.int8.onnx")
        val tokenizerDir = findFnTokenizerDir(modelDir) ?: return

        val minOnnxBytes = 8L * 1024L * 1024L
        val minLlmBytes = 32L * 1024L * 1024L
        if (
            !encoderAdaptor.exists() ||
            !embedding.exists() ||
            !llm.exists() ||
            encoderAdaptor.length() < minOnnxBytes ||
            embedding.length() < minOnnxBytes ||
            llm.length() < minLlmBytes ||
            !File(tokenizerDir, "tokenizer.json").exists()
        ) {
            return
        }

        val keepMinutes = prefs.fnKeepAliveMinutes
        val keepMs = if (keepMinutes <= 0) 0L else keepMinutes.toLong() * 60_000L
        val alwaysKeep = keepMinutes < 0

        val userPrompt = prefs.fnUserPrompt.trim().ifBlank { "语音转写：" }

        val numThreads = prefs.fnNumThreads
        val key = "funasr_nano|" +
            "encoder=${encoderAdaptor.absolutePath}|" +
            "llm=${llm.absolutePath}|" +
            "embedding=${embedding.absolutePath}|" +
            "tokenizer=${tokenizerDir.absolutePath}|" +
            "prompt=$userPrompt|" +
            "provider=cpu|" +
            "threads=$numThreads"

        val mainHandler = Handler(Looper.getMainLooper())
        LocalModelLoadCoordinator.request(key) {
            val t0 = android.os.SystemClock.uptimeMillis()
            val ok = manager.prepare(
                assetManager = null,
                encoderAdaptor = encoderAdaptor.absolutePath,
                llm = llm.absolutePath,
                embedding = embedding.absolutePath,
                tokenizerDir = tokenizerDir.absolutePath,
                userPrompt = userPrompt,
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
        Log.e("FunAsrNanoFileAsrEngine", "Failed to preload FunASR Nano model", t)
    }
}
