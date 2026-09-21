package com.subzero.guardian.ui.screens

import androidx.compose.runtime.Composable
import com.subzero.data.MandateEntity

@Composable
fun CancelGuideSheet(
    mandate: MandateEntity,
    onDismiss: () -> Unit,
    onMarkAsRevoked: (String) -> Unit = {}
) {
    com.subzero.ui.screens.CancelGuideSheet(
        mandate = mandate,
        onDismiss = onDismiss,
        onMarkAsRevoked = onMarkAsRevoked
    )
}
