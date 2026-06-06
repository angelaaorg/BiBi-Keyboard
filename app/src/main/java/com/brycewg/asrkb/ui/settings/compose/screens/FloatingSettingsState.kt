/**
 * Compose 悬浮球设置页状态与系统能力 helper。
 *
 * 归属模块：ui/settings/compose/screens
 */
package com.brycewg.asrkb.ui.settings.compose.screens

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.AsrAccessibilityService
import com.brycewg.asrkb.ui.floating.FloatingServiceManager

private const val FLOATING_SETTINGS_STATE_TAG = "FloatingSettingsState"

internal data class FloatingSettingsUiState(
    val asrEnabled: Boolean,
    val onlyWhenImeVisible: Boolean,
    val directDragEnabled: Boolean,
    val alphaPercent: Float,
    val sizeDp: Int,
    val volumeKeyRecordingEnabled: Boolean,
    val volumeKeyRecordingMode: String,
    val volumeKeyStatusToastEnabled: Boolean,
    val volumeKeyStopOnImeHidden: Boolean,
    val writeCompatEnabled: Boolean,
    val writePasteEnabled: Boolean
) {
    companion object {
        val placeholder: FloatingSettingsUiState = FloatingSettingsUiState(
            asrEnabled = false,
            onlyWhenImeVisible = false,
            directDragEnabled = false,
            alphaPercent = 100f,
            sizeDp = 56,
            volumeKeyRecordingEnabled = false,
            volumeKeyRecordingMode = Prefs.VOLUME_KEY_MODE_UP_TOGGLE,
            volumeKeyStatusToastEnabled = false,
            volumeKeyStopOnImeHidden = true,
            writeCompatEnabled = false,
            writePasteEnabled = false
        )

        fun fromPrefs(prefs: Prefs): FloatingSettingsUiState = FloatingSettingsUiState(
            asrEnabled = prefs.floatingAsrEnabled,
            onlyWhenImeVisible = prefs.floatingSwitcherOnlyWhenImeVisible,
            directDragEnabled = prefs.floatingBallDirectDragEnabled,
            alphaPercent = (prefs.floatingSwitcherAlpha * 100f).coerceIn(30f, 100f),
            sizeDp = prefs.floatingBallSizeDp,
            volumeKeyRecordingEnabled = prefs.volumeKeyRecordingEnabled,
            volumeKeyRecordingMode = prefs.volumeKeyRecordingMode,
            volumeKeyStatusToastEnabled = prefs.volumeKeyStatusToastEnabled,
            volumeKeyStopOnImeHidden = prefs.volumeKeyStopOnImeHidden,
            writeCompatEnabled = prefs.floatingWriteTextCompatEnabled,
            writePasteEnabled = prefs.floatingWriteTextPasteEnabled
        )
    }
}

internal data class FloatingSettingsPrefsSnapshot(
    val uiState: FloatingSettingsUiState,
    val compatPackages: String,
    val pastePackages: String
) {
    companion object {
        fun fromPrefs(prefs: Prefs): FloatingSettingsPrefsSnapshot = FloatingSettingsPrefsSnapshot(
            uiState = FloatingSettingsUiState.fromPrefs(prefs),
            compatPackages = prefs.floatingWriteCompatPackages,
            pastePackages = prefs.floatingWritePastePackages
        )
    }
}

internal enum class FloatingPermissionRequest {
    Overlay,
    Accessibility
}

internal fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val component = ComponentName(context, AsrAccessibilityService::class.java)
    val expectedComponentNames = setOf(
        component.flattenToString(),
        component.flattenToShortString()
    )
    val enabledServicesSetting = try {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
    } catch (e: Throwable) {
        Log.e(FLOATING_SETTINGS_STATE_TAG, "Failed to check accessibility service", e)
        return false
    }
    return enabledServicesSetting
        ?.split(':')
        ?.any { it in expectedComponentNames } == true
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
