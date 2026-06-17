/**
 * 输入法桥接广播协议常量。
 *
 * 归属模块：imebridge
 */
package com.brycewg.asrkb.imebridge

internal object ImeBridgeContract {
    const val PROTOCOL_VERSION: Int = 1

    const val ACTION_QUERY_STATUS: String = "com.brycewg.asrkb.imebridge.action.QUERY_STATUS"
    const val ACTION_INSERT_TEXT: String = "com.brycewg.asrkb.imebridge.action.INSERT_TEXT"
    const val ACTION_IME_WINDOW_VISIBILITY_CHANGED: String =
        "com.brycewg.asrkb.imebridge.action.IME_WINDOW_VISIBILITY_CHANGED"

    const val EXTRA_PROTOCOL_VERSION: String = "protocol_version"
    const val EXTRA_REQUEST_ID: String = "request_id"
    const val EXTRA_TEXT: String = "text"
    const val EXTRA_CURSOR_POSITION: String = "cursor_position"
    const val EXTRA_TARGET_PACKAGE: String = "target_package"
    const val EXTRA_HAS_INPUT_CONNECTION: String = "has_input_connection"
    const val EXTRA_IS_SENSITIVE_FIELD: String = "is_sensitive_field"
    const val EXTRA_IME_WINDOW_VISIBLE: String = "ime_window_visible"
    const val EXTRA_MESSAGE: String = "message"

    const val RESULT_OK: Int = 1
    const val RESULT_NO_RECEIVER: Int = 0
    const val RESULT_PROTOCOL_MISMATCH: Int = -2
    const val RESULT_NO_ACTIVE_IME: Int = -3
    const val RESULT_NO_INPUT_CONNECTION: Int = -4
    const val RESULT_SENSITIVE_FIELD: Int = -5
    const val RESULT_COMMIT_FAILED: Int = -6
    const val RESULT_BAD_REQUEST: Int = -7
    const val RESULT_NO_CURRENT_IME: Int = -100
    const val RESULT_TIMEOUT: Int = -101
    const val RESULT_SEND_FAILED: Int = -102
}
