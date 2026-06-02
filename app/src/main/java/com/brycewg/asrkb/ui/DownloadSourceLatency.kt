/**
 * 下载源可达性测速工具。
 *
 * 归属模块：ui
 */
package com.brycewg.asrkb.ui

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DownloadSourceLatency"
private const val LATENCY_TIMEOUT_MS = 3000

enum class DownloadSourceLatencyStatus { Pending, Ok, Timeout, Error }

data class DownloadSourceLatencyResult(
    val status: DownloadSourceLatencyStatus,
    val latencyMs: Long = 0L
)

suspend fun measureDownloadSourceLatency(url: String): DownloadSourceLatencyResult = withContext(Dispatchers.IO) {
    val hostPort = parseDownloadSourceHostPort(url)
    if (hostPort == null) {
        Log.w(TAG, "Missing host for latency check: $url")
        return@withContext DownloadSourceLatencyResult(DownloadSourceLatencyStatus.Error)
    }
    val (host, port) = hostPort
    val firstAttempt = connectDownloadSourceOnce(host, port)
    if (firstAttempt.status != DownloadSourceLatencyStatus.Timeout) {
        firstAttempt
    } else {
        Log.w(TAG, "Latency check timeout, retrying: $host:$port")
        connectDownloadSourceOnce(host, port)
    }
}

fun buildDownloadSourceAddressDisplay(url: String): String {
    val uri = Uri.parse(url)
    val host = uri.host ?: return url
    val scheme = uri.scheme ?: "https"
    return "$scheme://$host"
}

private fun connectDownloadSourceOnce(host: String, port: Int): DownloadSourceLatencyResult {
    val start = SystemClock.elapsedRealtime()
    return try {
        Socket().use { socket ->
            socket.soTimeout = LATENCY_TIMEOUT_MS
            socket.connect(InetSocketAddress(host, port), LATENCY_TIMEOUT_MS)
        }
        val cost = (SystemClock.elapsedRealtime() - start).coerceAtLeast(1L)
        DownloadSourceLatencyResult(DownloadSourceLatencyStatus.Ok, cost)
    } catch (e: SocketTimeoutException) {
        Log.w(TAG, "Latency check timeout: $host:$port", e)
        DownloadSourceLatencyResult(DownloadSourceLatencyStatus.Timeout)
    } catch (e: Exception) {
        Log.w(TAG, "Latency check failed: $host:$port", e)
        DownloadSourceLatencyResult(DownloadSourceLatencyStatus.Error)
    }
}

private fun parseDownloadSourceHostPort(url: String): Pair<String, Int>? {
    val uri = Uri.parse(url)
    val host = uri.host ?: return null
    val port = when {
        uri.port > 0 -> uri.port
        uri.scheme.equals("http", true) -> 80
        else -> 443
    }
    return host to port
}
