/**
 * Compose 悬浮球设置页状态与系统能力 helper。
 *
 * 归属模块：ui/settings/compose/screens
 */
package com.brycewg.asrkb.ui.settings.compose.screens

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.floating.FloatingServiceManager

private const val FLOATING_SETTINGS_STATE_TAG = "FloatingSettingsState"

internal data class FloatingSettingsUiState(
    val asrEnabled: Boolean,
    val onlyWhenImeVisible: Boolean,
    val directDragEnabled: Boolean,
    val alphaPercent: Float,
    val sizeDp: Int,
    val writeCompatEnabled: Boolean,
    val writePasteEnabled: Boolean
) {
    companion object {
        fun fromPrefs(prefs: Prefs): FloatingSettingsUiState = FloatingSettingsUiState(
            asrEnabled = prefs.floatingAsrEnabled,
            onlyWhenImeVisible = prefs.floatingSwitcherOnlyWhenImeVisible,
            directDragEnabled = prefs.floatingBallDirectDragEnabled,
            alphaPercent = (prefs.floatingSwitcherAlpha * 100f).coerceIn(30f, 100f),
            sizeDp = prefs.floatingBallSizeDp,
            writeCompatEnabled = prefs.floatingWriteTextCompatEnabled,
            writePasteEnabled = prefs.floatingWriteTextPasteEnabled
        )
    }
}

internal enum class FloatingPermissionRequest {
    Overlay,
    Accessibility
}

internal fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = "${context.packageName}/com.brycewg.asrkb.ui.AsrAccessibilityService"
    val enabledServicesSetting = try {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
    } catch (e: Throwable) {
        Log.e(FLOATING_SETTINGS_STATE_TAG, "Failed to check accessibility service", e)
        return false
    }
    return enabledServicesSetting?.contains(expectedComponentName) == true
}

internal fun resetFloatingPosition(
    context: Context,
    prefs: Prefs,
    serviceManager: FloatingServiceManager
): Boolean {
    var success = true
    try {
        prefs.floatingBallPosX = -1
        prefs.floatingBallPosY = -1
        prefs.floatingBallDockSide = 0
        prefs.floatingBallDockFraction = -1f
        prefs.floatingBallDockHidden = false
    } catch (e: Throwable) {
        Log.e(FLOATING_SETTINGS_STATE_TAG, "Failed to reset floating position in prefs", e)
        success = false
    }
    try {
        serviceManager.resetAsrBallPosition()
    } catch (e: Throwable) {
        Log.e(FLOATING_SETTINGS_STATE_TAG, "Failed to dispatch reset to service", e)
        success = false
    }
    return success
}
