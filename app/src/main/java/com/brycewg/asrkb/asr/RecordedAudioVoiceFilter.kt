/**
 * 非流式识别前的 VAD 空音频检测与静音片段过滤。
 *
 * 归属模块：asr
 */
package com.brycewg.asrkb.asr

import android.content.Context
import android.util.Log
import com.brycewg.asrkb.BuildConfig
import com.brycewg.asrkb.store.Prefs
import java.io.ByteArrayOutputStream

/**
 * 对一段 PCM 音频做识别前处理。
 *
 * 输入必须是 16-bit little-endian mono PCM，采样率与 [sampleRate] 一致。
 */
object RecordedAudioVoiceFilter {
    private const val TAG = "RecordedAudioVoiceFilter"
    private const val BYTES_PER_SAMPLE = 2
    private const val PRE_ROLL_MS = 350
    private const val POST_ROLL_MS = 500
    private const val MIN_SILENT_RUN_TO_TRIM_MS = 800
    private const val NEAR_SILENCE_MAX_ABS_THRESHOLD = 128
    private const val NEAR_SILENCE_RMS_THRESHOLD = 48
    private const val VAD_DOMINANT_MAX_ABS_THRESHOLD = 1_200
    private const val VAD_DOMINANT_RMS_THRESHOLD = 320
    private const val FILTER_VAD_SENSITIVITY = 1

    private data class ChunkMark(
        val offset: Int,
        val length: Int,
        val durationMs: Int,
        val isSpeech: Boolean,
        val maxAbs: Int,
        val sumSquares: Double,
        val sampleCount: Int
    )

    private data class FilterOutput(
        val pcm: ByteArray,
        val candidateRunCount: Int,
        val trimmedRunCount: Int,
        val trimmedMs: Int
    )

    data class Result(
        val pcm: ByteArray,
        val hasSpeech: Boolean,
        val droppedAsEmptyAudio: Boolean,
        val originalDurationMs: Long,
        val outputDurationMs: Long
    )

    data class SilenceAnalysis(
        val silentPercent: Int
    )

    fun analyzeSilence(
        context: Context,
        prefs: Prefs,
        pcm: ByteArray,
        sampleRate: Int,
        chunkMillis: Int
    ): SilenceAnalysis? {
        if (pcm.isEmpty() || sampleRate <= 0) return null

        val detector = try {
            VadDetector(
                context = context,
                sampleRate = sampleRate,
                windowMs = prefs.autoStopSilenceWindowMs,
                sensitivityLevel = FILTER_VAD_SENSITIVITY
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to create VAD analyzer", t)
            return null
        }
        if (!detector.isAvailable()) {
            detector.release()
            Log.w(TAG, "VAD analyzer is unavailable")
            return null
        }

        return try {
            val marks = buildChunkMarks(
                detector = detector,
                pcm = pcm,
                sampleRate = sampleRate,
                chunkMillis = chunkMillis.coerceAtLeast(20)
            )
            if (marks.isEmpty()) return null

            val contentDurationMs = marks
                .filter { it.shouldKeepAsContent() }
                .sumOf { it.durationMs }
                .toLong()
            val totalDurationMs = marks.sumOf { it.durationMs }.toLong().coerceAtLeast(1L)
            val nonContentDurationMs = (totalDurationMs - contentDurationMs).coerceAtLeast(0L)
            SilenceAnalysis(
                silentPercent = (nonContentDurationMs * 100L / totalDurationMs).toInt().coerceIn(0, 100)
            )
        } catch (t: Throwable) {
            Log.w(TAG, "VAD silence analysis failed", t)
            null
        } finally {
            try {
                detector.release()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to release VAD analyzer", t)
            }
        }
    }

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
                sensitivityLevel = FILTER_VAD_SENSITIVITY
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
        val marks = buildChunkMarks(detector, pcm, sampleRate, chunkMillis)

        val hasContent = marks.any { it.shouldKeepAsContent() }
        val shouldDrop = cancelEmpty && shouldDropAsEmptyAudio(marks)
        val filterOutput = if (filterSilent && hasContent) {
            filterLongNonContentRuns(pcm, marks)
        } else {
            null
        }
        val out = if (filterOutput != null) {
            filterOutput.pcm.takeIf { it.isNotEmpty() } ?: pcm
        } else {
            pcm
        }
        val outputDurationMs = durationMs(out.size, sampleRate)
        logDebugSummary(
            cancelEmpty = cancelEmpty,
            filterSilent = filterSilent,
            sampleRate = sampleRate,
            chunkMillis = chunkMillis,
            marks = marks,
            hasContent = hasContent,
            droppedAsEmptyAudio = shouldDrop,
            originalDurationMs = originalDurationMs,
            outputDurationMs = outputDurationMs,
            filterOutput = filterOutput
        )
        return Result(
            pcm = out,
            hasSpeech = hasContent,
            droppedAsEmptyAudio = shouldDrop,
            originalDurationMs = originalDurationMs,
            outputDurationMs = outputDurationMs
        )
    }

    private fun buildChunkMarks(
        detector: VadDetector,
        pcm: ByteArray,
        sampleRate: Int,
        chunkMillis: Int
    ): List<ChunkMark> {
        val chunkBytes = (((sampleRate * chunkMillis) / 1000) * BYTES_PER_SAMPLE)
            .coerceAtLeast(BYTES_PER_SAMPLE)
        val marks = ArrayList<ChunkMark>()
        var offset = 0
        while (offset < pcm.size) {
            val len = minOf(chunkBytes, pcm.size - offset)
            val chunk = pcm.copyOfRange(offset, offset + len)
            val frameMs = durationMs(len, sampleRate).toInt().coerceAtLeast(1)
            val isSpeech = detector.analyzeFrame(chunk, chunk.size).isSpeech
            val energy = measureEnergy(chunk, chunk.size)
            marks.add(
                ChunkMark(
                    offset = offset,
                    length = len,
                    durationMs = frameMs,
                    isSpeech = isSpeech,
                    maxAbs = energy.maxAbs,
                    sumSquares = energy.sumSquares,
                    sampleCount = energy.sampleCount
                )
            )

            offset += len
        }
        return marks
    }

    private data class Energy(val maxAbs: Int, val sumSquares: Double, val sampleCount: Int)

    private fun measureEnergy(pcm: ByteArray, len: Int): Energy {
        var maxAbs = 0
        var sumSquares = 0.0
        var samples = 0
        var i = 0
        while (i + 1 < len) {
            val lo = pcm[i].toInt() and 0xFF
            val hi = pcm[i + 1].toInt()
            val sample = (hi shl 8) or lo
            val abs = kotlin.math.abs(sample)
            if (abs > maxAbs) maxAbs = abs
            sumSquares += sample.toDouble() * sample.toDouble()
            samples++
            i += BYTES_PER_SAMPLE
        }
        return Energy(maxAbs = maxAbs, sumSquares = sumSquares, sampleCount = samples)
    }

    private fun shouldDropAsEmptyAudio(marks: List<ChunkMark>): Boolean = marks.isNotEmpty() && marks.none { it.shouldKeepAsContent() }

    private fun filterLongNonContentRuns(pcm: ByteArray, marks: List<ChunkMark>): FilterOutput {
        val keep = BooleanArray(marks.size)
        marks.forEachIndexed { index, mark ->
            if (mark.shouldKeepAsContent()) keep[index] = true
        }

        var index = 0
        var hasContentBefore = false
        var candidateRunCount = 0
        var trimmedRunCount = 0
        var trimmedMs = 0
        while (index < marks.size) {
            if (keep[index]) {
                hasContentBefore = true
                index++
                continue
            }

            val runStart = index
            var runDurationMs = 0
            while (index < marks.size && !keep[index]) {
                runDurationMs += marks[index].durationMs
                index++
            }
            val runEndExclusive = index
            val hasContentAfter = runEndExclusive < marks.size
            candidateRunCount++

            if (runDurationMs <= MIN_SILENT_RUN_TO_TRIM_MS) {
                for (i in runStart until runEndExclusive) keep[i] = true
                continue
            }

            var elapsedFromStartMs = 0
            for (i in runStart until runEndExclusive) {
                val mark = marks[i]
                val remainingToEndMs = runDurationMs - elapsedFromStartMs
                keep[i] = (hasContentBefore && elapsedFromStartMs < POST_ROLL_MS) ||
                    (hasContentAfter && remainingToEndMs <= PRE_ROLL_MS)
                elapsedFromStartMs += mark.durationMs
            }
            val keptRunMs = (runStart until runEndExclusive)
                .filter { keep[it] }
                .sumOf { marks[it].durationMs }
            val trimmedRunMs = runDurationMs - keptRunMs
            if (trimmedRunMs > 0) {
                trimmedRunCount++
                trimmedMs += trimmedRunMs
            }
        }

        val out = ByteArrayOutputStream(pcm.size)
        marks.forEachIndexed { i, mark ->
            if (keep[i]) out.write(pcm, mark.offset, mark.length)
        }
        return FilterOutput(
            pcm = out.toByteArray(),
            candidateRunCount = candidateRunCount,
            trimmedRunCount = trimmedRunCount,
            trimmedMs = trimmedMs
        )
    }

    private fun logDebugSummary(
        cancelEmpty: Boolean,
        filterSilent: Boolean,
        sampleRate: Int,
        chunkMillis: Int,
        marks: List<ChunkMark>,
        hasContent: Boolean,
        droppedAsEmptyAudio: Boolean,
        originalDurationMs: Long,
        outputDurationMs: Long,
        filterOutput: FilterOutput?
    ) {
        if (!BuildConfig.DEBUG) return

        var totalSquares = 0.0
        var totalSamples = 0
        var maxAbs = 0
        var maxRms = 0.0
        var vadSpeechCount = 0
        var vadDominantCount = 0
        var vadDominantSpeechCount = 0
        var nearSilenceCount = 0
        var contentCount = 0
        var lowVolumeSoundCount = 0
        marks.forEach { mark ->
            totalSquares += mark.sumSquares
            totalSamples += mark.sampleCount
            if (mark.maxAbs > maxAbs) maxAbs = mark.maxAbs
            val rms = mark.rms()
            if (rms > maxRms) maxRms = rms
            val isVadDominant = mark.isVadDominantVolume()
            val isNearSilence = mark.isNearSilence()
            val keepAsContent = mark.shouldKeepAsContent()
            if (mark.isSpeech) vadSpeechCount++
            if (isVadDominant) vadDominantCount++
            if (isVadDominant && mark.isSpeech) vadDominantSpeechCount++
            if (isNearSilence) nearSilenceCount++
            if (keepAsContent) contentCount++
            if (!isVadDominant && !isNearSilence) lowVolumeSoundCount++
        }
        val avgRms = if (totalSamples > 0) {
            kotlin.math.sqrt(totalSquares / totalSamples)
        } else {
            0.0
        }

        Log.d(
            TAG,
            "voice_filter_summary " +
                "cancelEmpty=$cancelEmpty filterSilent=$filterSilent " +
                "sr=$sampleRate chunkMs=$chunkMillis chunks=${marks.size} " +
                "durationMs=$originalDurationMs->$outputDurationMs " +
                "hasContent=$hasContent dropped=$droppedAsEmptyAudio " +
                "vadSpeech=$vadSpeechCount content=$contentCount " +
                "nearSilence=$nearSilenceCount lowVolumeSound=$lowVolumeSoundCount " +
                "vadDominant=$vadDominantCount vadDominantSpeech=$vadDominantSpeechCount " +
                "maxAbs=$maxAbs avgRms=${avgRms.toInt()} maxRms=${maxRms.toInt()} " +
                "trimCandidateRuns=${filterOutput?.candidateRunCount ?: 0} " +
                "trimmedRuns=${filterOutput?.trimmedRunCount ?: 0} " +
                "trimmedMs=${filterOutput?.trimmedMs ?: 0} " +
                "thresholds={nearMaxAbs=$NEAR_SILENCE_MAX_ABS_THRESHOLD," +
                "nearRms=$NEAR_SILENCE_RMS_THRESHOLD," +
                "vadMaxAbs=$VAD_DOMINANT_MAX_ABS_THRESHOLD," +
                "vadRms=$VAD_DOMINANT_RMS_THRESHOLD," +
                "minRunMs=$MIN_SILENT_RUN_TO_TRIM_MS," +
                "preMs=$PRE_ROLL_MS,postMs=$POST_ROLL_MS," +
                "vadSensitivity=$FILTER_VAD_SENSITIVITY}"
        )
    }

    private fun ChunkMark.isNearSilence(): Boolean {
        if (sampleCount <= 0) return false
        val rms = rms()
        return maxAbs <= NEAR_SILENCE_MAX_ABS_THRESHOLD &&
            rms <= NEAR_SILENCE_RMS_THRESHOLD
    }

    private fun ChunkMark.shouldKeepAsContent(): Boolean {
        if (sampleCount <= 0) return false
        return if (isVadDominantVolume()) {
            isSpeech
        } else {
            !isNearSilence()
        }
    }

    private fun ChunkMark.isVadDominantVolume(): Boolean {
        if (sampleCount <= 0) return false
        val rms = rms()
        return maxAbs >= VAD_DOMINANT_MAX_ABS_THRESHOLD ||
            rms >= VAD_DOMINANT_RMS_THRESHOLD
    }

    private fun ChunkMark.rms(): Double = kotlin.math.sqrt(sumSquares / sampleCount)

    private fun durationMs(byteCount: Int, sampleRate: Int): Long {
        if (sampleRate <= 0) return 0L
        return (byteCount / BYTES_PER_SAMPLE) * 1_000L / sampleRate
    }
}
