/**
 * Compose 设置页滑块组件，统一 Material 与 Miuix 的控制项布局。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsSliderPreference(
    uiMode: BibiUiMode,
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    showKeyPoints: Boolean = steps in 1..10,
    highlightId: String? = null,
    index: Int = 0,
    count: Int = 1,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val content: @Composable () -> Unit = {
        when (uiMode) {
            BibiUiMode.Material -> SettingsMaterialItemSurface(index = index, count = count) {
                SettingsControlLabel(
                    uiMode = uiMode,
                    title = title,
                    value = valueLabel
                )
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SettingsLayoutMetrics.SliderHorizontalPadding)
                        .padding(bottom = SettingsLayoutMetrics.SliderBottomPadding)
                )
            }

            BibiUiMode.Miuix -> {
                SettingsControlLabel(
                    uiMode = uiMode,
                    title = title,
                    value = valueLabel
                )
                MiuixSlider(
                    value = value,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    steps = if (showKeyPoints) steps else 0,
                    showKeyPoints = showKeyPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SettingsLayoutMetrics.SliderHorizontalPadding)
                        .padding(bottom = SettingsLayoutMetrics.SliderBottomPadding)
                )
            }
        }
    }
    if (highlightId == null) {
        content()
    } else {
        SettingsHighlightContainer(entryId = highlightId, uiMode = uiMode, content = content)
    }
}

@Composable
internal fun SettingsControlLabel(
    uiMode: BibiUiMode,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsLayoutMetrics.ControlLabelHorizontalPadding,
                vertical = SettingsLayoutMetrics.ControlLabelVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (uiMode) {
            BibiUiMode.Material -> {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.size(SettingsLayoutMetrics.ControlLabelSpacing))
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            BibiUiMode.Miuix -> {
                MiuixText(
                    text = title,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    style = MiuixTheme.textStyles.headline1
                )
                Spacer(Modifier.size(SettingsLayoutMetrics.ControlLabelSpacing))
                MiuixText(
                    text = value,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2
                )
            }
        }
    }
}
