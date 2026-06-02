/**
 * Compose Pro 宣传弹窗内容。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics

internal data class ProPromoDialogActions(
    val onPlayStore: () -> Unit,
    val onTelegram: () -> Unit,
    val onPaymentQr: () -> Unit,
    val onClose: () -> Unit
)

@Composable
internal fun ProPromoDialogContent(
    uiMode: BibiUiMode,
    actions: ProPromoDialogActions,
    modifier: Modifier = Modifier
) {
    ProDialogSurface(uiMode = uiMode, modifier = modifier) {
        Column(
            modifier = Modifier
                .proDialogScrollableContent()
                .padding(SettingsLayoutMetrics.ProDialogContentPadding)
        ) {
            DialogTitle(
                text = stringResource(R.string.pro_promo_title),
                uiMode = uiMode
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSmallSpacing))
            DialogBody(
                text = stringResource(R.string.pro_promo_price),
                uiMode = uiMode,
                primary = true
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogMediumSpacing))
            DialogSectionLabel(
                text = stringResource(R.string.pro_promo_features_title),
                uiMode = uiMode
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSmallSpacing))
            DialogBody(
                text = stringResource(R.string.pro_promo_features),
                uiMode = uiMode
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSmallSpacing))
            DialogCaption(
                text = stringResource(R.string.pro_promo_future),
                uiMode = uiMode
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSmallSpacing))
            DialogCaption(
                text = stringResource(R.string.pro_promo_once_hint),
                uiMode = uiMode
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSectionSpacing))
            ProPromoActions(uiMode = uiMode, actions = actions)
        }
    }
}

@Composable
private fun ProPromoActions(
    uiMode: BibiUiMode,
    actions: ProPromoDialogActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.ProDialogActionSpacing)) {
        DialogPrimaryAction(
            text = stringResource(R.string.pro_promo_btn_play_store),
            uiMode = uiMode,
            onClick = actions.onPlayStore
        )
        DialogTonalAction(
            text = stringResource(R.string.pro_promo_btn_telegram),
            uiMode = uiMode,
            onClick = actions.onTelegram
        )
        DialogTonalAction(
            text = stringResource(R.string.pro_promo_btn_payment_qr),
            uiMode = uiMode,
            onClick = actions.onPaymentQr
        )
        DialogTextAction(
            text = stringResource(R.string.pro_promo_btn_close),
            uiMode = uiMode,
            onClick = actions.onClose
        )
    }
}
