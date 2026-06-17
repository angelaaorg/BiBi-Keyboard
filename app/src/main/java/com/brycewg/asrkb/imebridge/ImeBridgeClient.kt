/**
 * 输入法桥接客户端：主应用通过有序广播请求被 Hook 的输入法提交文本。
 *
 * 归属模块：imebridge
 */
package com.brycewg.asrkb.imebridge

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class ImeBridgeResult(
    val code: Int,
    val message: String,
    val targetPackage: String?,
    val hasInputConnection: Boolean,
    val isSensitiveField: Boolean,
    val isImeWindowVisible: Boolean,
    val supportsComposingPreview: Boolean = false
) {
    val isSuccess: Boolean get() = code == ImeBridgeContract.RESULT_OK
    val isBridgePresent: Boolean
        get() = code != ImeBridgeContract.RESULT_NO_RECEIVER &&
            code != ImeBridgeContract.RESULT_NO_CURRENT_IME &&
            code != ImeBridgeContract.RESULT_TIMEOUT &&
            code != ImeBridgeContract.RESULT_SEND_FAILED
}

class ImeBridgeClient(private val context: Context) {
    fun queryStatus(timeoutMs: Long = DEFAULT_STATUS_TIMEOUT_MS): ImeBridgeResult =
        sendBridgeRequest(ImeBridgeContract.ACTION_QUERY_STATUS, null, timeoutMs)

    fun insertText(
        text: String,
        cursorPosition: Int = 1,
        timeoutMs: Long = DEFAULT_INSERT_TIMEOUT_MS
    ): ImeBridgeResult {
        if (text.isEmpty()) {
            return ImeBridgeResult(
                code = ImeBridgeContract.RESULT_BAD_REQUEST,
                message = "empty text",
                targetPackage = resolveCurrentImePackage(),
                hasInputConnection = false,
                isSensitiveField = false,
                isImeWindowVisible = false
            )
        }
        return sendBridgeRequest(ImeBridgeContract.ACTION_INSERT_TEXT, text, timeoutMs, cursorPosition)
    }

    fun setComposingText(
        text: String,
        cursorPosition: Int = 1,
        timeoutMs: Long = DEFAULT_COMPOSING_TIMEOUT_MS
    ): ImeBridgeResult =
        sendBridgeRequest(
            ImeBridgeContract.ACTION_SET_COMPOSING_TEXT,
            text,
            timeoutMs,
            cursorPosition
        )

    fun finishComposingText(timeoutMs: Long = DEFAULT_COMPOSING_TIMEOUT_MS): ImeBridgeResult =
        sendBridgeRequest(ImeBridgeContract.ACTION_FINISH_COMPOSING_TEXT, null, timeoutMs)

    fun resolveCurrentImePackage(): String? = resolveCurrentImePackage(context)

    private fun sendBridgeRequest(
        action: String,
        text: String?,
        timeoutMs: Long,
        cursorPosition: Int = 1
    ): ImeBridgeResult {
        val targetPackage = resolveCurrentImePackage()
            ?: return ImeBridgeResult(
                code = ImeBridgeContract.RESULT_NO_CURRENT_IME,
                message = "no current ime",
                targetPackage = null,
                hasInputConnection = false,
                isSensitiveField = false,
                isImeWindowVisible = false
            )

        val latch = CountDownLatch(1)
        var result: ImeBridgeResult? = null
        val requestId = UUID.randomUUID().toString()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val extras = getResultExtras(false)
                result = ImeBridgeResult(
                    code = resultCode,
                    message = extras?.getString(ImeBridgeContract.EXTRA_MESSAGE)
                        ?: resultData
                        ?: messageForCode(resultCode),
                    targetPackage = extras?.getString(ImeBridgeContract.EXTRA_TARGET_PACKAGE) ?: targetPackage,
                    hasInputConnection = extras?.getBoolean(
                        ImeBridgeContract.EXTRA_HAS_INPUT_CONNECTION,
                        false
                    ) == true,
                    isSensitiveField = extras?.getBoolean(
                        ImeBridgeContract.EXTRA_IS_SENSITIVE_FIELD,
                        false
                    ) == true,
                    isImeWindowVisible = extras?.getBoolean(
                        ImeBridgeContract.EXTRA_IME_WINDOW_VISIBLE,
                        false
                    ) == true,
                    supportsComposingPreview = extras?.getBoolean(
                        ImeBridgeContract.EXTRA_SUPPORTS_COMPOSING_PREVIEW,
                        false
                    ) == true
                )
                latch.countDown()
            }
        }

        val intent = Intent(action).apply {
            setPackage(targetPackage)
            putExtra(ImeBridgeContract.EXTRA_PROTOCOL_VERSION, ImeBridgeContract.PROTOCOL_VERSION)
            putExtra(ImeBridgeContract.EXTRA_REQUEST_ID, requestId)
            putExtra(ImeBridgeContract.EXTRA_CURSOR_POSITION, cursorPosition)
            if (text != null) putExtra(ImeBridgeContract.EXTRA_TEXT, text)
        }

        try {
            context.applicationContext.sendOrderedBroadcast(
                intent,
                null,
                receiver,
                BridgeResultHandler.handler,
                ImeBridgeContract.RESULT_NO_RECEIVER,
                "no receiver",
                null
            )
        } catch (t: Throwable) {
            Log.w(TAG, "sendBridgeRequest failed: action=$action target=$targetPackage", t)
            return ImeBridgeResult(
                code = ImeBridgeContract.RESULT_SEND_FAILED,
                message = t.message ?: "send failed",
                targetPackage = targetPackage,
                hasInputConnection = false,
                isSensitiveField = false,
                isImeWindowVisible = false
            )
        }

        val received = try {
            latch.await(timeoutMs.coerceAtLeast(1L), TimeUnit.MILLISECONDS)
        } catch (t: Throwable) {
            Log.w(TAG, "waiting bridge result failed", t)
            false
        }

        return if (received) {
            result ?: ImeBridgeResult(
                code = ImeBridgeContract.RESULT_NO_RECEIVER,
                message = "no receiver",
                targetPackage = targetPackage,
                hasInputConnection = false,
                isSensitiveField = false,
                isImeWindowVisible = false
            )
        } else {
            ImeBridgeResult(
                code = ImeBridgeContract.RESULT_TIMEOUT,
                message = "timeout",
                targetPackage = targetPackage,
                hasInputConnection = false,
                isSensitiveField = false,
                isImeWindowVisible = false
            )
        }
    }

    companion object {
        private const val TAG = "ImeBridgeClient"
        private const val DEFAULT_STATUS_TIMEOUT_MS = 350L
        private const val DEFAULT_INSERT_TIMEOUT_MS = 700L
        private const val DEFAULT_COMPOSING_TIMEOUT_MS = 120L

        fun resolveCurrentImePackage(context: Context): String? {
            val raw = try {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD
                )
            } catch (t: Throwable) {
                Log.w(TAG, "resolveCurrentImePackage failed", t)
                null
            } ?: return null

            ComponentName.unflattenFromString(raw)?.packageName?.let { return it }
            return raw.substringBefore('/').takeIf { it.isNotBlank() }
        }

        fun messageForCode(code: Int): String = when (code) {
            ImeBridgeContract.RESULT_OK -> "ok"
            ImeBridgeContract.RESULT_NO_RECEIVER -> "no receiver"
            ImeBridgeContract.RESULT_PROTOCOL_MISMATCH -> "protocol mismatch"
            ImeBridgeContract.RESULT_NO_ACTIVE_IME -> "no active ime"
            ImeBridgeContract.RESULT_NO_INPUT_CONNECTION -> "no input connection"
            ImeBridgeContract.RESULT_SENSITIVE_FIELD -> "sensitive field"
            ImeBridgeContract.RESULT_COMMIT_FAILED -> "commit failed"
            ImeBridgeContract.RESULT_BAD_REQUEST -> "bad request"
            ImeBridgeContract.RESULT_COMPOSING_FAILED -> "composing failed"
            ImeBridgeContract.RESULT_NO_CURRENT_IME -> "no current ime"
            ImeBridgeContract.RESULT_TIMEOUT -> "timeout"
            ImeBridgeContract.RESULT_SEND_FAILED -> "send failed"
            else -> "unknown: $code"
        }
    }
}

private object BridgeResultHandler {
    val handler: Handler by lazy {
        val thread = HandlerThread("ImeBridgeResult")
        thread.start()
        Handler(thread.looper)
    }
}
