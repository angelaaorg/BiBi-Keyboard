package com.brycewg.asrkb.asr

import android.content.Context
import android.util.Log
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

/**
 * 使用 OpenAI /v1/audio/transcriptions 的非流式 ASR 引擎。
 * 支持自定义 endpoint、API Key 与模型名。
 */
class OpenAiFileAsrEngine(
    context: Context,
    scope: CoroutineScope,
    prefs: Prefs,
    listener: StreamingAsrEngine.Listener,
    onRequestDuration: ((Long) -> Unit)? = null,
    httpClient: OkHttpClient? = null
) : BaseFileAsrEngine(context, scope, prefs, listener, onRequestDuration),
    PcmBatchRecognizer {

    companion object {
        private const val TAG = "OpenAiFileAsrEngine"
    }

    // OpenAI Whisper：未明确限制，本地限制为 20 分钟
    override val maxRecordDurationMillis: Int = 20 * 60 * 1000

    private val http: OkHttpClient = httpClient ?: OkHttpClient.Builder()
        .addInterceptor(ApiLogInterceptor())
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    override val uploadAudioEncodingSpec: UploadAudioEncodingSpec =
        UploadAudioEncodingSpec.M4A_AAC_LC

    override suspend fun recognize(pcm: ByteArray) {
        val audio = if (prefs.uploadAudioCompressionEnabled) {
            encodePcmForUpload(context, pcm, sampleRate, uploadAudioEncodingSpec)
        } else {
            pcmToWavUploadAudio(pcm)
        }
        recognizeEncoded(audio)
    }

    override suspend fun recognizeEncoded(audio: UploadAudioData) {
        var tmp: File? = null
        try {
            val created = File.createTempFile(
                "asr_oa_",
                ".${audio.container.extension}",
                context.cacheDir
            )
            tmp = created
            FileOutputStream(created).use { it.write(audio.bytes) }

            val apiKey = prefs.oaAsrApiKey
            val endpoint = prefs.oaAsrEndpoint.ifBlank { Prefs.DEFAULT_OA_ASR_ENDPOINT }
            val model = prefs.oaAsrModel.ifBlank { Prefs.DEFAULT_OA_ASR_MODEL }

            val usePrompt = prefs.oaAsrUsePrompt
            val basePrompt = prefs.oaAsrPrompt.trim()
            val prompt = basePrompt
            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart(
                    "file",
                    audio.fileName,
                    created.asRequestBody(audio.mimeType.toMediaType())
                )
                .addFormDataPart("response_format", "json")
            if (usePrompt && prompt.isNotEmpty()) {
                multipartBuilder.addFormDataPart("prompt", prompt)
            }
            val lang = prefs.oaAsrLanguage.trim()
            if (lang.isNotEmpty()) {
                multipartBuilder.addFormDataPart("language", lang)
            }
            val multipart = multipartBuilder.build()

            val reqBuilder = Request.Builder()
                .url(endpoint)
                .tag(
                    ApiLogMeta::class.java,
                    ApiLogRecorder.meta(
                        category = "ASR",
                        vendor = "openai",
                        model = model,
                        requestStructure = "multipart fields=model, file, response_format, prompt?, language?"
                    )
                )
                .post(multipart)
            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            val request = reqBuilder.build()

            val t0 = System.nanoTime()
            val resp = http.newCall(request).execute()
            resp.use { r ->
                val bodyStr = r.body?.string().orEmpty()
                if (!r.isSuccessful) {
                    val extra = extractErrorHint(bodyStr)
                    val detail = formatHttpDetail(r.message, extra)
                    listener.onError(
                        context.getString(R.string.error_request_failed_http, r.code, detail)
                    )
                    return
                }
                val text = parseTextFromResponse(bodyStr)
                if (text.isNotBlank()) {
                    val dt = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
                    try {
                        onRequestDuration?.invoke(dt)
                    } catch (_: Throwable) {}
                    listener.onFinal(text)
                } else {
                    listener.onError(context.getString(R.string.error_asr_empty_result))
                }
            }
        } catch (t: Throwable) {
            listener.onError(
                context.getString(R.string.error_recognize_failed_with_reason, t.message ?: "")
            )
        } finally {
            try {
                val file = tmp
                if (file != null && file.exists() && !file.delete()) {
                    file.deleteOnExit()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to delete OpenAI temp upload audio", t)
            }
        }
    }

    override suspend fun recognizeFromPcm(pcm: ByteArray) {
        recognize(pcm)
    }

    /**
     * 从响应体中提取错误提示信息
     */
    private fun extractErrorHint(body: String): String {
        if (body.isBlank()) return ""
        return try {
            val obj = JSONObject(body)
            when {
                obj.has(
                    "error"
                ) -> obj.optJSONObject("error")?.optString("message")?.trim().orEmpty()
                    .ifBlank { obj.optString("message").trim() }
                obj.has("message") -> obj.optString("message").trim()
                else -> body.take(200).trim()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to parse error hint from response", t)
            body.take(200).trim()
        }
    }

    /**
     * 从响应体中解析转写文本
     */
    private fun parseTextFromResponse(body: String): String {
        if (body.isBlank()) return ""
        return try {
            val obj = JSONObject(body)
            obj.optString("text", "").trim()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to parse text from response", t)
            ""
        }
    }
}
