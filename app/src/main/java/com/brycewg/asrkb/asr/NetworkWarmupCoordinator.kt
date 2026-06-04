/**
 * 录音启动时的网络预热协调器。
 *
 * 归属模块：asr
 */
package com.brycewg.asrkb.asr

import android.os.SystemClock
import android.util.Log
import com.brycewg.asrkb.BuildConfig
import com.brycewg.asrkb.store.Prefs
import java.net.HttpURLConnection
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 在真正开始录音时，异步预热即将访问的云端服务 origin。
 *
 * 设计约束：
 * - 仅做轻量 HEAD/GET 到根路径，不发送真实音频或文本 payload；
 * - 按 origin 做进程内去重与冷却，避免频繁重复预热；
 * - 失败静默，不影响主识别/后处理链路。
 */
internal object NetworkWarmupCoordinator {
    private const val TAG = "NetworkWarmup"
    private const val WARMUP_COOLDOWN_MS = 15 * 60 * 1000L
    private const val CONNECT_TIMEOUT_SECONDS = 5L
    private const val READ_TIMEOUT_SECONDS = 5L
    private const val WRITE_TIMEOUT_SECONDS = 5L
    private const val CALL_TIMEOUT_SECONDS = 5L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastWarmupAtMs = ConcurrentHashMap<String, Long>()
    private val inFlightKeys = ConcurrentHashMap<String, Boolean>()

    private val asrWarmupClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private val llmWarmupClient: OkHttpClient by lazy {
        LlmPostProcessor.defaultSharedHttpClient().newBuilder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    fun warmupForRecordingStart(prefs: Prefs) {
        val targetsByKey = LinkedHashMap<String, WarmupTarget>()
        resolveAsrTargets(prefs).forEach { target ->
            targetsByKey.putIfAbsent(target.cacheKey, target)
        }
        resolveLlmTarget(prefs)?.let { llmTarget ->
            // 同 origin 时优先保留 LLM 预热，尽量让共享连接池受益。
            targetsByKey[llmTarget.cacheKey] = llmTarget
        }
        targetsByKey.values.forEach(::scheduleWarmup)
    }

    private fun scheduleWarmup(target: WarmupTarget) {
        val now = SystemClock.elapsedRealtime()
        val last = lastWarmupAtMs[target.cacheKey]
        if (last != null && (now - last) < WARMUP_COOLDOWN_MS) {
            return
        }
        if (inFlightKeys.putIfAbsent(target.cacheKey, true) != null) {
            return
        }
        lastWarmupAtMs[target.cacheKey] = now
        scope.launch {
            try {
                performWarmup(target)
            } finally {
                inFlightKeys.remove(target.cacheKey)
            }
        }
    }

    private fun performWarmup(target: WarmupTarget) {
        when (target.mode) {
            WarmupMode.DNS_ONLY -> {
                resolveDns(target.host)
                return
            }
            WarmupMode.HTTP -> Unit
        }
        val client = when (target.kind) {
            WarmupKind.LLM -> llmWarmupClient
            WarmupKind.ASR -> asrWarmupClient
        }
        val rootUrl = target.rootUrl ?: return
        val headCode = executeWarmupRequest(client, rootUrl, useHead = true)
        if (headCode == HttpURLConnection.HTTP_BAD_METHOD ||
            headCode == HttpURLConnection.HTTP_NOT_IMPLEMENTED
        ) {
            executeWarmupRequest(client, rootUrl, useHead = false)
        }
    }

    private fun resolveDns(host: String): List<InetAddress>? = try {
        Dns.SYSTEM.lookup(host)
    } catch (t: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Warmup DNS failed: $host ${t.message}")
        }
        null
    }

    private fun executeWarmupRequest(
        client: OkHttpClient,
        url: HttpUrl,
        useHead: Boolean
    ): Int? {
        val requestBuilder = Request.Builder().url(url)
        if (useHead) {
            requestBuilder.head()
        } else {
            requestBuilder.get()
        }
        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                response.code
            }
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG,
                    "Warmup ${if (useHead) "HEAD" else "GET"} failed: ${url.host} ${t.message}"
                )
            }
            null
        }
    }

    private fun resolveAsrTargets(prefs: Prefs): List<WarmupTarget> {
        val vendors = LinkedHashSet<AsrVendor>().apply {
            resolveConfiguredVendor(prefs.asrVendor, prefs)?.let(::add)
            resolveBackupVendor(prefs)?.let(::add)
        }
        return vendors.mapNotNull { vendor -> resolveAsrTargetForVendor(vendor, prefs) }
    }

    private fun resolveConfiguredVendor(vendor: AsrVendor, prefs: Prefs): AsrVendor? = if (hasVendorConfig(vendor, prefs)) vendor else null

    private fun resolveBackupVendor(prefs: Prefs): AsrVendor? {
        val backupEnabled = try {
            prefs.backupAsrEnabled
        } catch (_: Throwable) {
            false
        }
        if (!backupEnabled) return null
        val primaryVendor = prefs.asrVendor
        val backupVendor = prefs.backupAsrVendor
        if (backupVendor == primaryVendor) return null
        return if (hasVendorConfig(backupVendor, prefs)) backupVendor else null
    }

    private fun hasVendorConfig(vendor: AsrVendor, prefs: Prefs): Boolean = when (vendor) {
        AsrVendor.Volc -> prefs.hasVolcKeys()
        AsrVendor.SiliconFlow -> prefs.hasSfKeys()
        AsrVendor.ElevenLabs -> prefs.hasElevenKeys()
        AsrVendor.OpenAI -> prefs.hasOpenAiKeys()
        AsrVendor.OpenRouter -> prefs.hasOpenRouterKeys()
        AsrVendor.DashScope -> prefs.hasDashKeys()
        AsrVendor.Gemini -> prefs.hasGeminiKeys()
        AsrVendor.Soniox -> prefs.hasSonioxKeys()
        AsrVendor.StepAudio -> prefs.hasStepAudioKeys()
        AsrVendor.Zhipu -> prefs.hasZhipuKeys()
        AsrVendor.MiMo -> prefs.hasMiMoKeys()
        AsrVendor.SenseVoice,
        AsrVendor.FunAsrNano,
        AsrVendor.Qwen3Asr,
        AsrVendor.Parakeet,
        AsrVendor.FireRedAsr,
        AsrVendor.Paraformer -> false
    }

    private fun resolveAsrTargetForVendor(vendor: AsrVendor, prefs: Prefs): WarmupTarget? = when (vendor) {
        AsrVendor.Volc -> {
            if (prefs.volcStreamingEnabled) {
                buildDnsTarget("https://openspeech.bytedance.com")
            } else {
                buildTarget(
                    kind = WarmupKind.ASR,
                    url = Prefs.DEFAULT_ENDPOINT
                )
            }
        }

        AsrVendor.SiliconFlow -> {
            val targetUrl = if (usesSiliconFlowChatAsr(prefs)) {
                Prefs.SF_CHAT_COMPLETIONS_ENDPOINT
            } else {
                Prefs.SF_ENDPOINT
            }
            buildTarget(
                kind = WarmupKind.ASR,
                url = targetUrl
            )
        }

        AsrVendor.ElevenLabs -> {
            if (prefs.elevenStreamingEnabled) {
                buildDnsTarget("https://api.elevenlabs.io")
            } else {
                buildTarget(
                    kind = WarmupKind.ASR,
                    url = "https://api.elevenlabs.io/v1/speech-to-text"
                )
            }
        }

        AsrVendor.OpenAI -> {
            if (prefs.oaAsrStreamingEnabled) {
                buildDnsTarget(prefs.oaAsrEndpoint.ifBlank { Prefs.DEFAULT_OA_ASR_ENDPOINT })
            } else {
                buildTarget(
                    kind = WarmupKind.ASR,
                    url = prefs.oaAsrEndpoint.ifBlank { Prefs.DEFAULT_OA_ASR_ENDPOINT }
                )
            }
        }

        AsrVendor.OpenRouter -> buildTarget(
            kind = WarmupKind.ASR,
            url = prefs.openRouterAsrEndpoint.ifBlank { Prefs.DEFAULT_OPENROUTER_ASR_ENDPOINT }
        )

        AsrVendor.DashScope -> {
            if (prefs.isDashStreamingModelSelected()) {
                buildDnsTarget(
                    if (prefs.dashRegion.equals("intl", ignoreCase = true)) {
                        "https://dashscope-intl.aliyuncs.com"
                    } else {
                        "https://dashscope.aliyuncs.com"
                    }
                )
            } else {
                val targetUrl = if (prefs.isDashOmniModelSelected()) {
                    prefs.getDashCompatibleModeChatEndpoint()
                } else {
                    prefs.getDashHttpBaseUrl()
                }
                buildTarget(
                    kind = WarmupKind.ASR,
                    url = targetUrl
                )
            }
        }

        AsrVendor.Gemini -> buildTarget(
            kind = WarmupKind.ASR,
            url = prefs.gemEndpoint.ifBlank { Prefs.DEFAULT_GEM_ENDPOINT }
        )

        AsrVendor.Soniox -> {
            if (prefs.sonioxStreamingEnabled) {
                buildDnsTarget(Prefs.SONIOX_WS_URL)
            } else {
                buildTarget(
                    kind = WarmupKind.ASR,
                    url = Prefs.SONIOX_FILES_ENDPOINT
                )
            }
        }

        AsrVendor.Zhipu -> buildTarget(
            kind = WarmupKind.ASR,
            url = "https://open.bigmodel.cn/api/paas/v4/audio/transcriptions"
        )

        AsrVendor.StepAudio -> buildTarget(
            kind = WarmupKind.ASR,
            url = Prefs.STEPAUDIO_ASR_ENDPOINT
        )

        AsrVendor.MiMo -> buildTarget(
            kind = WarmupKind.ASR,
            url = if (
                prefs.mimoAsrEndpointPreset == Prefs.MIMO_ENDPOINT_PRESET_AUTO ||
                prefs.mimoAsrEndpoint.isBlank()
            ) {
                Prefs.resolveMimoEndpoint(prefs.mimoAsrApiKey.trim())
            } else {
                prefs.mimoAsrEndpoint
            }
        )

        AsrVendor.SenseVoice,
        AsrVendor.FunAsrNano,
        AsrVendor.Qwen3Asr,
        AsrVendor.Parakeet,
        AsrVendor.FireRedAsr,
        AsrVendor.Paraformer -> null
    }

    private fun resolveLlmTarget(prefs: Prefs): WarmupTarget? {
        val postProcessEnabled = try {
            prefs.postProcessEnabled
        } catch (_: Throwable) {
            false
        }
        if (!postProcessEnabled || !prefs.hasLlmKeys()) {
            return null
        }
        val config = prefs.getEffectiveLlmConfig() ?: return null
        return buildTarget(
            kind = WarmupKind.LLM,
            url = config.endpoint.ifBlank { Prefs.DEFAULT_LLM_ENDPOINT }
        )
    }

    private fun usesSiliconFlowChatAsr(prefs: Prefs): Boolean {
        if (prefs.sfFreeAsrEnabled) return false
        val selectedModel = prefs.sfModel.ifBlank { Prefs.DEFAULT_SF_MODEL }
        return selectedModel.startsWith("Qwen/Qwen3-Omni-30B-A3B-")
    }

    private fun buildTarget(kind: WarmupKind, url: String): WarmupTarget? {
        val parsed = url.trim().toHttpUrlOrNull() ?: return null
        val rootUrl = parsed.newBuilder()
            .encodedPath("/")
            .query(null)
            .fragment(null)
            .build()
        val defaultPort = defaultPortForScheme(rootUrl.scheme)
        val portSuffix = if (rootUrl.port == defaultPort || defaultPort < 0) "" else ":${rootUrl.port}"
        val cacheKey = "${rootUrl.scheme}://${rootUrl.host}$portSuffix"
        return WarmupTarget(
            kind = kind,
            mode = WarmupMode.HTTP,
            host = rootUrl.host,
            rootUrl = rootUrl,
            cacheKey = cacheKey
        )
    }

    private fun buildDnsTarget(url: String): WarmupTarget? {
        val normalized = url.trim()
            .replaceFirst("wss://", "https://", ignoreCase = true)
            .replaceFirst("ws://", "http://", ignoreCase = true)
        val parsed = normalized.toHttpUrlOrNull() ?: return null
        return WarmupTarget(
            kind = WarmupKind.ASR,
            mode = WarmupMode.DNS_ONLY,
            host = parsed.host,
            rootUrl = null,
            cacheKey = "dns://${parsed.host}"
        )
    }

    private fun defaultPortForScheme(scheme: String): Int = when (scheme.lowercase()) {
        "http" -> 80
        "https" -> 443
        else -> -1
    }

    private enum class WarmupKind {
        ASR,
        LLM
    }

    private enum class WarmupMode {
        HTTP,
        DNS_ONLY
    }

    private data class WarmupTarget(
        val kind: WarmupKind,
        val mode: WarmupMode,
        val host: String,
        val rootUrl: HttpUrl?,
        val cacheKey: String
    )
}
