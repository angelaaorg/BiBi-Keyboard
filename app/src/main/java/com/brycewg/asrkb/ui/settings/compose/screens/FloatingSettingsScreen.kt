/**
 * Compose 悬浮球设置页。
 *
 * 归属模块：ui/settings/compose/screens
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.screens

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.floating.FloatingServiceManager
import com.brycewg.asrkb.ui.settings.compose.components.SettingsFeatureExplainerDialog
import com.brycewg.asrkb.ui.settings.compose.components.SettingsFeatureExplainerDialogState
import com.brycewg.asrkb.ui.settings.compose.components.SettingsLazyColumn
import com.brycewg.asrkb.ui.settings.compose.components.SettingsMessageDialog
import com.brycewg.asrkb.ui.settings.compose.components.SettingsMessageDialogState
import com.brycewg.asrkb.ui.settings.compose.components.settingsFeatureExplainerDialogState
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsActionController
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import kotlinx.coroutines.delay

private const val FLOATING_TAG = "FloatingSettingsScreen"

@Composable
fun FloatingSettingsScreen(
    uiMode: BibiUiMode,
    onBack: () -> Unit,
    actions: SettingsActionController
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember(context) { Prefs(context) }
    val serviceManager = remember(context) { FloatingServiceManager(context) }
    var uiState by remember(context) { mutableStateOf(FloatingSettingsUiState.fromPrefs(prefs)) }
    var compatPackages by remember(context) { mutableStateOf(prefs.floatingWriteCompatPackages) }
    var pastePackages by remember(context) { mutableStateOf(prefs.floatingWritePastePackages) }
    var pendingAsrEnable by remember { mutableStateOf(false) }
    var pendingAsrPermission by remember { mutableStateOf<FloatingPermissionRequest?>(null) }
    var featureExplainerDialog by remember { mutableStateOf<SettingsFeatureExplainerDialogState?>(null) }
    var messageDialog by remember { mutableStateOf<SettingsMessageDialogState?>(null) }
    val latestCompatPackages by rememberUpdatedState(compatPackages)
    val latestPastePackages by rememberUpdatedState(pastePackages)

    LaunchedEffect(compatPackages) {
        delay(300)
        prefs.floatingWriteCompatPackages = compatPackages
    }

    LaunchedEffect(pastePackages) {
        delay(300)
        prefs.floatingWritePastePackages = pastePackages
    }

    fun refreshState() {
        uiState = FloatingSettingsUiState.fromPrefs(prefs)
    }

    fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e(FLOATING_TAG, "Failed to request overlay permission", e)
        }
    }

    fun requestAccessibilityPermission() {
        try {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Throwable) {
            Log.e(FLOATING_TAG, "Failed to request accessibility permission", e)
        }
    }

    fun showFloatingMessage(messageRes: Int) {
        messageDialog = SettingsMessageDialogState(
            title = context.getString(R.string.title_floating_settings),
            message = context.getString(messageRes),
            confirmText = context.getString(android.R.string.ok)
        )
    }

    fun showOverlayPermissionMessage() {
        showFloatingMessage(R.string.toast_need_overlay_perm)
    }

    fun showAccessibilityPermissionMessage() {
        showFloatingMessage(R.string.toast_need_accessibility_perm)
    }

    fun setAsrEnabled(enabled: Boolean): Boolean {
        if (enabled) {
            if (!Settings.canDrawOverlays(context)) {
                pendingAsrEnable = true
                pendingAsrPermission = FloatingPermissionRequest.Overlay
                showOverlayPermissionMessage()
                requestOverlayPermission()
                return false
            }
            if (!isAccessibilityServiceEnabled(context)) {
                pendingAsrEnable = true
                pendingAsrPermission = FloatingPermissionRequest.Accessibility
                showAccessibilityPermissionMessage()
                requestAccessibilityPermission()
                return false
            }
        }

        pendingAsrEnable = false
        pendingAsrPermission = null
        prefs.floatingAsrEnabled = enabled
        if (enabled) {
            serviceManager.showAsrService()
        } else {
            serviceManager.hideAsrService()
        }
        refreshState()
        return true
    }

    fun syncAsrToggleAfterPermissions() {
        if (pendingAsrEnable) {
            val hasOverlay = Settings.canDrawOverlays(context)
            val hasAccessibility = isAccessibilityServiceEnabled(context)
            if (hasOverlay && hasAccessibility) {
                setAsrEnabled(true)
                return
            }
            if (
                hasOverlay &&
                !hasAccessibility &&
                pendingAsrPermission == FloatingPermissionRequest.Overlay
            ) {
                pendingAsrPermission = FloatingPermissionRequest.Accessibility
                showAccessibilityPermissionMessage()
                requestAccessibilityPermission()
            }
            return
        }

        if (prefs.floatingAsrEnabled) {
            val hasOverlay = Settings.canDrawOverlays(context)
            val hasAccessibility = isAccessibilityServiceEnabled(context)
            if (!hasOverlay || !hasAccessibility) {
                prefs.floatingAsrEnabled = false
                serviceManager.hideAsrService()
            }
        }
        refreshState()
    }

    DisposableEffect(lifecycleOwner) {
        fun flushPackageEdits() {
            prefs.floatingWriteCompatPackages = latestCompatPackages
            prefs.floatingWritePastePackages = latestPastePackages
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> syncAsrToggleAfterPermissions()
                Lifecycle.Event.ON_PAUSE -> flushPackageEdits()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            flushPackageEdits()
        }
    }

    fun applyExplainedSwitch(
        current: Boolean,
        target: Boolean,
        titleRes: Int,
        offDescRes: Int,
        onDescRes: Int,
        preferenceKey: String,
        onConfirm: (Boolean) -> Unit
    ) {
        featureExplainerDialog = settingsFeatureExplainerDialogState(
            context = context,
            titleRes = titleRes,
            offDescRes = offDescRes,
            onDescRes = onDescRes,
            currentState = current,
            preferenceKey = preferenceKey,
            onConfirm = {
                onConfirm(target)
                refreshState()
            }
        )
    }

    FloatingScaffold(uiMode = uiMode, onBack = onBack) { innerPadding, scrollModifier ->
        SettingsFeatureExplainerDialog(
            state = featureExplainerDialog,
            uiMode = uiMode,
            onDismiss = { featureExplainerDialog = null }
        )
        SettingsMessageDialog(
            state = messageDialog,
            uiMode = uiMode,
            onDismiss = { messageDialog = null }
        )
        SettingsLazyColumn(
            uiMode = uiMode,
            modifier = Modifier.fillMaxSize(),
            miuixScrollModifier = scrollModifier,
            contentPadding = SettingsLayoutMetrics.pageContentPadding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.SectionSpacing)
        ) {
            item("preview") {
                FloatingPreviewCard(
                    uiMode = uiMode,
                    enabled = uiState.asrEnabled,
                    alphaPercent = uiState.alphaPercent,
                    sizeDp = uiState.sizeDp
                )
            }

            item("basic") {
                FloatingSection(uiMode = uiMode, titleRes = R.string.section_floating_basic) {
                    val basicItemCount = if (uiState.asrEnabled) 5 else 1
                    FloatingExplainedSwitch(
                        id = "floating_asr",
                        titleRes = R.string.label_floating_asr,
                        checked = uiState.asrEnabled,
                        onToggle = { target ->
                            actions.hapticTap()
                            applyExplainedSwitch(
                                current = uiState.asrEnabled,
                                target = target,
                                titleRes = R.string.label_floating_asr,
                                offDescRes = R.string.feature_floating_asr_off_desc,
                                onDescRes = R.string.feature_floating_asr_on_desc,
                                preferenceKey = "floating_asr_explained"
                            ) { setAsrEnabled(it) }
                        },
                        index = 0,
                        count = basicItemCount
                    )
                    if (uiState.asrEnabled) {
                        FloatingExplainedSwitch(
                            id = "floating_only_when_ime_visible",
                            titleRes = R.string.label_floating_only_when_ime_visible,
                            checked = uiState.onlyWhenImeVisible,
                            onToggle = { target ->
                                actions.hapticTap()
                                applyExplainedSwitch(
                                    current = uiState.onlyWhenImeVisible,
                                    target = target,
                                    titleRes = R.string.label_floating_only_when_ime_visible,
                                    offDescRes = R.string.feature_floating_only_when_ime_visible_off_desc,
                                    onDescRes = R.string.feature_floating_only_when_ime_visible_on_desc,
                                    preferenceKey = "floating_only_when_ime_visible_explained"
                                ) { enabled ->
                                    if (enabled && !isAccessibilityServiceEnabled(context)) {
                                        showAccessibilityPermissionMessage()
                                        requestAccessibilityPermission()
                                    } else {
                                        prefs.floatingSwitcherOnlyWhenImeVisible = enabled
                                        serviceManager.refreshAsrService(prefs.floatingAsrEnabled)
                                    }
                                }
                            },
                            index = 1,
                            count = basicItemCount
                        )
                        FloatingExplainedSwitch(
                            id = "floating_direct_drag",
                            titleRes = R.string.label_floating_direct_drag,
                            checked = uiState.directDragEnabled,
                            onToggle = { target ->
                                actions.hapticTap()
                                applyExplainedSwitch(
                                    current = uiState.directDragEnabled,
                                    target = target,
                                    titleRes = R.string.label_floating_direct_drag,
                                    offDescRes = R.string.feature_floating_direct_drag_off_desc,
                                    onDescRes = R.string.feature_floating_direct_drag_on_desc,
                                    preferenceKey = "floating_direct_drag_explained"
                                ) { prefs.floatingBallDirectDragEnabled = it }
                            },
                            index = 2,
                            count = basicItemCount
                        )
                        FloatingSliderPreference(
                            titleRes = R.string.label_floating_alpha,
                            valueLabel = "${uiState.alphaPercent.toInt()}%",
                            value = uiState.alphaPercent,
                            valueRange = 30f..100f,
                            step = 5,
                            uiMode = uiMode,
                            index = 3,
                            count = basicItemCount,
                            onValueChange = { value ->
                                uiState = uiState.copy(alphaPercent = value.roundFloatingToStep(5))
                            },
                            onValueChangeFinished = {
                                actions.hapticTap()
                                prefs.floatingSwitcherAlpha = (uiState.alphaPercent / 100f).coerceIn(0.2f, 1.0f)
                                serviceManager.refreshAsrService(prefs.floatingAsrEnabled)
                                refreshState()
                            }
                        )
                        FloatingSliderPreference(
                            titleRes = R.string.label_floating_size,
                            valueLabel = "${uiState.sizeDp} dp",
                            value = uiState.sizeDp.toFloat(),
                            valueRange = 28f..96f,
                            step = 4,
                            uiMode = uiMode,
                            index = 4,
                            count = basicItemCount,
                            onValueChange = { value ->
                                uiState = uiState.copy(sizeDp = value.roundFloatingToStep(4).toInt().coerceIn(28, 96))
                            },
                            onValueChangeFinished = {
                                actions.hapticTap()
                                prefs.floatingBallSizeDp = uiState.sizeDp
                                if (prefs.floatingAsrEnabled) {
                                    serviceManager.showAsrService()
                                }
                                refreshState()
                            }
                        )
                        FloatingResetButton(
                            uiMode = uiMode,
                            onClick = {
                                actions.hapticTap()
                                val messageRes = if (resetFloatingPosition(context, prefs, serviceManager)) {
                                    R.string.toast_floating_position_reset
                                } else {
                                    R.string.toast_debug_failed
                                }
                                showFloatingMessage(messageRes)
                            }
                        )
                    }
                }
            }

            item("compat") {
                FloatingSection(uiMode = uiMode, titleRes = R.string.section_floating_compat) {
                    FloatingExplainedSwitch(
                        id = "floating_write_compat",
                        titleRes = R.string.label_floating_write_compat,
                        checked = uiState.writeCompatEnabled,
                        onToggle = { target ->
                            actions.hapticTap()
                            applyExplainedSwitch(
                                current = uiState.writeCompatEnabled,
                                target = target,
                                titleRes = R.string.label_floating_write_compat,
                                offDescRes = R.string.feature_floating_write_compat_off_desc,
                                onDescRes = R.string.feature_floating_write_compat_on_desc,
                                preferenceKey = "floating_write_compat_explained"
                        ) { prefs.floatingWriteTextCompatEnabled = it }
                        },
                        index = 0,
                        count = 2
                    )
                    FloatingPackagesField(
                        value = compatPackages,
                        onValueChange = {
                            compatPackages = it
                        },
                        label = stringResource(R.string.label_floating_write_compat_pkgs),
                        helper = stringResource(R.string.hint_floating_write_compat_pkgs),
                        uiMode = uiMode,
                        index = 1,
                        count = 2
                    )
                    FloatingSubsectionGap()
                    FloatingExplainedSwitch(
                        id = "floating_write_paste",
                        titleRes = R.string.label_floating_write_paste,
                        checked = uiState.writePasteEnabled,
                        onToggle = { target ->
                            actions.hapticTap()
                            applyExplainedSwitch(
                                current = uiState.writePasteEnabled,
                                target = target,
                                titleRes = R.string.label_floating_write_paste,
                                offDescRes = R.string.feature_floating_write_paste_off_desc,
                                onDescRes = R.string.feature_floating_write_paste_on_desc,
                                preferenceKey = "floating_write_paste_explained"
                        ) { prefs.floatingWriteTextPasteEnabled = it }
                        },
                        index = 0,
                        count = 2
                    )
                    FloatingPackagesField(
                        value = pastePackages,
                        onValueChange = {
                            pastePackages = it
                        },
                        label = stringResource(R.string.label_floating_write_paste_pkgs),
                        helper = stringResource(R.string.hint_floating_write_paste_pkgs),
                        uiMode = uiMode,
                        index = 1,
                        count = 2
                    )
                }
            }
        }
    }
}
