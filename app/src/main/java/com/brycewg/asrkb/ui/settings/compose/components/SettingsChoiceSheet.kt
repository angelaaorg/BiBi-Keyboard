/**
 * Compose 设置单选底部弹层。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:OptIn(ExperimentalLayoutApi::class)
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.annotation.ColorRes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.core.settingsSegmentedItemShape
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text as MiuixText

internal data class SettingsChoiceTag(
    val label: String,
    @param:ColorRes val bgColorResId: Int,
    @param:ColorRes val textColorResId: Int
)

internal data class SettingsChoiceItem(
    val title: String,
    val originalIndex: Int,
    val tags: List<SettingsChoiceTag> = emptyList()
)

internal data class SettingsChoiceGroup(
    val label: String,
    val items: List<SettingsChoiceItem>
)

internal data class SettingsChoiceSheetState(
    val title: String,
    val groups: List<SettingsChoiceGroup>,
    val selectedIndex: Int,
    val onChoiceClick: (() -> Unit)? = null,
    val onSelected: (Int) -> Unit
)

internal fun settingsChoiceSheetState(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onChoiceClick: (() -> Unit)? = null,
    onSelected: (Int) -> Unit
): SettingsChoiceSheetState? {
    if (items.isEmpty()) return null
    return SettingsChoiceSheetState(
        title = title,
        groups = listOf(
            SettingsChoiceGroup(
                label = "",
                items = items.mapIndexed { index, label ->
                    SettingsChoiceItem(
                        title = label,
                        originalIndex = index
                    )
                }
            )
        ),
        selectedIndex = selectedIndex.takeIf { it in items.indices } ?: -1,
        onChoiceClick = onChoiceClick,
        onSelected = onSelected
    )
}

@Composable
internal fun SettingsChoiceSheet(
    state: SettingsChoiceSheetState?,
    uiMode: BibiUiMode,
    onDismiss: () -> Unit
) {
    val visibleState = state?.takeIf { sheetState ->
        sheetState.groups.any { it.items.isNotEmpty() }
    } ?: return
    when (uiMode) {
        BibiUiMode.Material -> MaterialChoiceSheet(
            state = visibleState,
            onDismiss = onDismiss
        )

        BibiUiMode.Miuix -> MiuixChoiceSheet(
            state = visibleState,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun MaterialChoiceSheet(
    state: SettingsChoiceSheetState?,
    onDismiss: () -> Unit
) {
    if (state == null) return
    MaterialSettingsSheetScaffold(
        title = state.title,
        onDismiss = onDismiss,
        bottomPadding = 0.dp
    ) { dismissSheet ->
        ChoiceSheetList(
            state = state,
            uiMode = BibiUiMode.Material,
            onDismiss = dismissSheet
        )
    }
}

@Composable
private fun MiuixChoiceSheet(
    state: SettingsChoiceSheetState?,
    onDismiss: () -> Unit
) {
    if (state == null) return
    var show by remember(state) { mutableStateOf(true) }
    var afterDismiss by remember(state) { mutableStateOf<(() -> Unit)?>(null) }
    OverlayDialog(
        show = show,
        title = state.title,
        onDismissRequest = { show = false },
        onDismissFinished = {
            afterDismiss?.invoke()
            onDismiss()
        },
        insideMargin = DpSize(0.dp, 24.dp)
    ) {
        ChoiceSheetList(
            state = state,
            uiMode = BibiUiMode.Miuix,
            onDismiss = { action ->
                afterDismiss = action
                show = false
            }
        )
    }
}

@Composable
private fun ChoiceSheetList(
    state: SettingsChoiceSheetState,
    uiMode: BibiUiMode,
    onDismiss: (afterDismiss: () -> Unit) -> Unit
) {
    var selectionHandled by remember(state) { mutableStateOf(false) }
    val visibleGroups = state.groups.filter { it.items.isNotEmpty() }
    if (visibleGroups.isEmpty()) return
    SettingsSheetLazyColumn(
        uiMode = uiMode,
        contentPadding = PaddingValues(bottom = SettingsLayoutMetrics.SheetBottomPadding)
    ) {
        visibleGroups.forEach { group ->
            if (group.label.isNotBlank()) {
                item("header-${group.label}") {
                    ChoiceGroupHeader(label = group.label, uiMode = uiMode)
                }
            }
            itemsIndexed(
                items = group.items,
                key = { _, item -> item.originalIndex }
            ) { index, item ->
                ChoiceItemRow(
                    item = item,
                    selected = item.originalIndex == state.selectedIndex,
                    uiMode = uiMode,
                    index = index,
                    count = group.items.size,
                    onClick = {
                        if (selectionHandled) return@ChoiceItemRow
                        selectionHandled = true
                        state.onChoiceClick?.invoke()
                        onDismiss { state.onSelected(item.originalIndex) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChoiceGroupHeader(label: String, uiMode: BibiUiMode) {
    when (uiMode) {
        BibiUiMode.Material -> MaterialText(
            text = label,
            modifier = Modifier.padding(
                horizontal = SettingsLayoutMetrics.SheetGroupHeaderHorizontalPadding,
                vertical = SettingsLayoutMetrics.SheetGroupHeaderVerticalPadding
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )

        BibiUiMode.Miuix -> MiuixText(
            text = label,
            modifier = Modifier.padding(
                horizontal = SettingsLayoutMetrics.SheetGroupHeaderHorizontalPadding,
                vertical = SettingsLayoutMetrics.SheetGroupHeaderVerticalPadding
            ),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1
        )
    }
}

@Composable
private fun ChoiceItemRow(
    item: SettingsChoiceItem,
    selected: Boolean,
    uiMode: BibiUiMode,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    when (uiMode) {
        BibiUiMode.Material -> Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SettingsLayoutMetrics.MaterialSectionItemSpacing)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick
                ),
            shape = settingsSegmentedItemShape(index, count),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(SettingsLayoutMetrics.MaterialSectionElevation),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            ListItem(
                modifier = Modifier.heightIn(min = SettingsLayoutMetrics.SettingsPreferenceMinHeight),
                headlineContent = { MaterialText(item.title) },
                supportingContent = item.tags.takeIf { it.isNotEmpty() }?.let { tags ->
                    { ChoiceTags(tags = tags) }
                },
                trailingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                },
                overlineContent = null,
                leadingContent = null,
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    supportingColor = MaterialTheme.colorScheme.outline
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            )
        }

        BibiUiMode.Miuix -> RadioButtonPreference(
            title = item.title,
            selected = selected,
            onClick = onClick,
            radioButtonLocation = RadioButtonLocation.End,
            bottomAction = item.tags.takeIf { it.isNotEmpty() }?.let { tags ->
                { ChoiceTags(tags = tags) }
            }
        )
    }
}

@Composable
private fun ChoiceTags(
    tags: List<SettingsChoiceTag>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(top = SettingsLayoutMetrics.ChoiceTagTopPadding),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(SettingsLayoutMetrics.ChoiceTagSpacing),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(SettingsLayoutMetrics.ChoiceTagSpacing)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = colorResource(tag.bgColorResId),
                contentColor = colorResource(tag.textColorResId)
            ) {
                MaterialText(
                    text = tag.label,
                    modifier = Modifier.padding(
                        horizontal = SettingsLayoutMetrics.ChoiceTagHorizontalPadding,
                        vertical = SettingsLayoutMetrics.ChoiceTagVerticalPadding
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
