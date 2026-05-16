/**
 * 非流式识别前的 VAD 空音频检测与静音片段过滤。
 *
 * 归属模块：asr
 */
package com.brycewg.asrkb.asr

import android.content.Context
import android.util.Log
import com.brycewg.asrkb.store.Prefs
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque

/**
 * 对一段 PCM 音频做识别前处理。
 *
 * 输入必须是 16-bit little-endian mono PCM，采样率与 [sampleRate] 一致。
 */
object RecordedAudioVoiceFilter {
    private const val TAG = "RecordedAudioVoiceFilter"
    private const val BYTES_PER_SAMPLE = 2
    private const val PRE_ROLL_MS = 300
    private const val POST_ROLL_MS = 450

    data class Result(
        val pcm: ByteArray,
        val hasSpeech: Boolean,
        val droppedAsEmptyAudio: Boolean,
        val originalDurationMs: Long,
        val outputDurationMs: Long
    )

    fun processIfEnabled(
        context: Context,
        prefs: Prefs,
        pcm: ByteArray,
        sampleRate: Int,
        chunkMillis: Int
    ): Result {
        val cancelEmpty = prefs.autoCancelEmptyAudioInputEnabled
        val filterSilent = prefs.autoFilterSilentAudioSegmentsEnabled
        val originalDurationMs = durationMs(pcm.size, sampleRate)
        if (pcm.isEmpty() || sampleRate <= 0 || (!cancelEmpty && !filterSilent)) {
            return Result(
                pcm = pcm,
                hasSpeech = pcm.isNotEmpty(),
                droppedAsEmptyAudio = false,
                originalDurationMs = originalDurationMs,
                outputDurationMs = originalDurationMs
            )
        }

        val detector = try {
            VadDetector(
                context = context,
                sampleRate = sampleRate,
                windowMs = prefs.autoStopSilenceWindowMs,
                sensitivityLevel = prefs.autoStopSilenceSensitivity
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to create VAD filter, keeping original audio", t)
            return Result(
                pcm = pcm,
                hasSpeech = true,
                droppedAsEmptyAudio = false,
                originalDurationMs = originalDurationMs,
                outputDurationMs = originalDurationMs
            )
        }
        if (!detector.isAvailable()) {
            detector.release()
            Log.w(TAG, "VAD filter is unavailable, keeping original audio")
            return Result(
                pcm = pcm,
                hasSpeech = true,
                droppedAsEmptyAudio = false,
                originalDurationMs = originalDurationMs,
                outputDurationMs = originalDurationMs
            )
        }

        return try {
            processWithDetector(
                detector = detector,
                pcm = pcm,
                sampleRate = sampleRate,
                chunkMillis = chunkMillis.coerceAtLeast(20),
                cancelEmpty = cancelEmpty,
                filterSilent = filterSilent,
                originalDurationMs = originalDurationMs
            )
        } catch (t: Throwable) {
            Log.w(TAG, "VAD filter failed, keeping original audio", t)
            Result(
                pcm = pcm,
                hasSpeech = true,
                droppedAsEmptyAudio = false,
                originalDurationMs = originalDurationMs,
                outputDurationMs = originalDurationMs
            )
        } finally {
            try {
                detector.release()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to release VAD filter", t)
            }
        }
    }

    private fun processWithDetector(
        detector: VadDetector,
        pcm: ByteArray,
        sampleRate: Int,
        chunkMillis: Int,
        cancelEmpty: Boolean,
        filterSilent: Boolean,
        originalDurationMs: Long
    ): Result {
        val chunkBytes = (((sampleRate * chunkMillis) / 1000) * BYTES_PER_SAMPLE)
            .coerceAtLeast(BYTES_PER_SAMPLE)
        val preRollBytes = ((sampleRate * PRE_ROLL_MS / 1000) * BYTES_PER_SAMPLE)
            .coerceAtLeast(BYTES_PER_SAMPLE)
        var postRollRemainingMs = 0
        var hasSpeech = false
        val filtered = if (filterSilent) ByteArrayOutputStream(pcm.size) else null
        val preRoll = ArrayDeque<ByteArray>()
        var preRollSize = 0

        var offset = 0
        while (offset < pcm.size) {
            val len = minOf(chunkBytes, pcm.size - offset)
            val chunk = pcm.copyOfRange(offset, offset + len)
            val frameMs = durationMs(len, sampleRate).toInt().coerceAtLeast(1)
            val isSpeech = detector.analyzeFrame(chunk, chunk.size).isSpeech

            if (isSpeech) {
                hasSpeech = true
                if (filterSilent) {
                    while (!preRoll.isEmpty()) {
                        filtered?.write(preRoll.removeFirst())
                    }
                    preRollSize = 0
                    filtered?.write(chunk)
                    postRollRemainingMs = POST_ROLL_MS
                }
            } else if (filterSilent) {
                if (!hasSpeech || postRollRemainingMs <= 0) {
                    preRoll.addLast(chunk)
                    preRollSize += chunk.size
                    while (preRollSize > preRollBytes && !preRoll.isEmpty()) {
                        preRollSize -= preRoll.removeFirst().size
                    }
                } else if (postRollRemainingMs > 0) {
                    filtered?.write(chunk)
                    postRollRemainingMs -= frameMs
                }
            }

            offset += len
        }

        val shouldDrop = cancelEmpty && !hasSpeech
        val out = if (filterSilent && hasSpeech) {
            filtered?.toByteArray()?.takeIf { it.isNotEmpty() } ?: pcm
        } else {
            pcm
        }
        return Result(
            pcm = out,
            hasSpeech = hasSpeech,
            droppedAsEmptyAudio = shouldDrop,
            originalDurationMs = originalDurationMs,
            outputDurationMs = durationMs(out.size, sampleRate)
        )
    }

    private fun durationMs(byteCount: Int, sampleRate: Int): Long {
        if (sampleRate <= 0) return 0L
        return (byteCount / BYTES_PER_SAMPLE) * 1_000L / sampleRate
    }
}
