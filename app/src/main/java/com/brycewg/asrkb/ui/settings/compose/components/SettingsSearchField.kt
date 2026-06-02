/**
 * Compose 设置系搜索输入框，统一 Material 与 Miuix 的搜索框样式。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clearLabel = stringResource(R.string.cd_settings_search_clear)
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    val keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
    val fieldModifier = modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(120)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    when (uiMode) {
        BibiUiMode.Material -> OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = clearSearchAction(value, onValueChange, clearLabel, uiMode),
            singleLine = true,
            shape = RoundedCornerShape(SettingsLayoutMetrics.TextFieldCorner),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )

        BibiUiMode.Miuix -> MiuixTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            label = label,
            singleLine = true,
            cornerRadius = SettingsLayoutMetrics.TextFieldCorner,
            trailingIcon = clearSearchAction(value, onValueChange, clearLabel, uiMode),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
    }
}

@Composable
private fun clearSearchAction(
    value: String,
    onValueChange: (String) -> Unit,
    clearLabel: String,
    uiMode: BibiUiMode
): (@Composable () -> Unit)? = if (value.isNotEmpty()) {
    {
        when (uiMode) {
            BibiUiMode.Material -> IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = clearLabel)
            }

            BibiUiMode.Miuix -> MiuixIconButton(onClick = { onValueChange("") }) {
                MiuixIcon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = clearLabel,
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }
        }
    }
} else {
    null
}
