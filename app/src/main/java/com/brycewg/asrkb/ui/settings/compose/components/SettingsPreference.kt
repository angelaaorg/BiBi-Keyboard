/**
 * 设置项渲染入口，根据当前 UI 风格分发。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalBibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHighlightTarget
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.material.MaterialSettingsEntry
import com.brycewg.asrkb.ui.settings.compose.miuix.MiuixSettingsEntry
import com.brycewg.asrkb.ui.settings.compose.model.SettingsEntry
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPreference(
    entry: SettingsEntry,
    index: Int = 0,
    count: Int = 1
) {
    val uiMode = LocalBibiUiMode.current
    SettingsHighlightContainer(
        entryId = entry.id,
        uiMode = uiMode
    ) {
        when (uiMode) {
            BibiUiMode.Material -> MaterialSettingsEntry(entry, index, count)
            BibiUiMode.Miuix -> MiuixSettingsEntry(entry)
        }
    }
}

@Composable
fun SettingsPreferenceGroup(vararg entries: SettingsEntry?) {
    val visibleEntries = entries.filterNotNull()
    val uiMode = LocalBibiUiMode.current
    visibleEntries.forEachIndexed { index, entry ->
        SettingsPreference(
            entry = entry,
            index = index,
            count = visibleEntries.size
        )
        if (uiMode == BibiUiMode.Material && index < visibleEntries.lastIndex) {
            Spacer(Modifier.height(SettingsLayoutMetrics.MaterialSectionItemSpacing))
        }
    }
}

@Composable
internal fun SettingsHighlightContainer(
    entryId: String,
    uiMode: BibiUiMode,
    content: @Composable () -> Unit
) {
    val highlightTargetId = LocalSettingsHighlightTarget.current
    var active by remember(entryId, highlightTargetId) {
        mutableStateOf(highlightTargetId == entryId)
    }
    LaunchedEffect(entryId, highlightTargetId) {
        active = highlightTargetId == entryId
        if (active) {
            delay(HighlightDurationMillis)
            active = false
        }
    }

    val highlightColor = when (uiMode) {
        BibiUiMode.Material -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = HighlightAlpha)
        BibiUiMode.Miuix -> MiuixTheme.colorScheme.primary.copy(alpha = HighlightAlpha)
    }
    val color by animateColorAsState(
        targetValue = if (active) highlightColor else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(durationMillis = HighlightFadeMillis),
        label = "SettingsSearchHighlight"
    )
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .background(
                color = color,
                shape = RoundedCornerShape(SettingsLayoutMetrics.MaterialSectionShape)
            )
    ) {
        content()
    }
}

private const val HighlightDurationMillis = 1800L
private const val HighlightFadeMillis = 260
private const val HighlightAlpha = 0.38f
