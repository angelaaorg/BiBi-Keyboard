package com.brycewg.asrkb.asr

/**
 * 统一的 ASR 超时计算。
 * - 云端/默认：基准 10 秒，每增加 5 秒录音增加 2 秒，上限 40 秒。
 * - 本地模型：按 vendor 放宽最短等待与上限，避免重模型在设备侧推理时过早超时。
 */
object AsrTimeoutCalculator {
    private const val BASE_TIMEOUT_MS = 10000L
    private const val EXTRA_PER_FIVE_SEC_MS = 2000L
    private const val DEFAULT_MIN_TIMEOUT_MS = BASE_TIMEOUT_MS
    private const val DEFAULT_MAX_TIMEOUT_MS = 40000L

    private data class TimeoutProfile(
        val minTimeoutMs: Long,
        val maxTimeoutMs: Long
    )

    fun calculateTimeoutMs(audioMs: Long): Long = calculateTimeoutMs(audioMs, vendor = null)

    fun calculateTimeoutMs(audioMs: Long, vendor: AsrVendor?): Long {
        val profile = profileFor(vendor)
        val extra = (audioMs.coerceAtLeast(0L) / 5000L) * EXTRA_PER_FIVE_SEC_MS
        val rawTimeoutMs = BASE_TIMEOUT_MS + extra
        return rawTimeoutMs.coerceIn(profile.minTimeoutMs, profile.maxTimeoutMs)
    }

    private fun profileFor(vendor: AsrVendor?): TimeoutProfile = when (vendor) {
        // Paraformer 为本地流式，其余三个为设备侧整段推理，分别使用独立超时范围。
        AsrVendor.Paraformer -> TimeoutProfile(minTimeoutMs = 10_000L, maxTimeoutMs = 40_000L)
        AsrVendor.SenseVoice -> TimeoutProfile(minTimeoutMs = 10_000L, maxTimeoutMs = 40_000L)
        AsrVendor.FireRedAsr -> TimeoutProfile(minTimeoutMs = 15_000L, maxTimeoutMs = 70_000L)
        AsrVendor.FunAsrNano -> TimeoutProfile(minTimeoutMs = 15_000L, maxTimeoutMs = 90_000L)
        AsrVendor.Qwen3Asr -> TimeoutProfile(minTimeoutMs = 15_000L, maxTimeoutMs = 90_000L)

        else -> TimeoutProfile(
            minTimeoutMs = DEFAULT_MIN_TIMEOUT_MS,
            maxTimeoutMs = DEFAULT_MAX_TIMEOUT_MS
        )
    }
}
