package com.subzero.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.subzero.ui.theme.*

@Composable
fun M3TactileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Switch(
        checked = checked,
        onCheckedChange = { newState ->
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onCheckedChange(newState)
        },
        thumbContent = {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Active",
                    tint = M3PinePrimary,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Inactive",
                    tint = M3SurfaceWhite,
                    modifier = Modifier.size(10.dp)
                )
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = M3SwitchThumbActive,
            checkedTrackColor = M3SwitchTrackActive,
            checkedBorderColor = M3SwitchTrackActive,
            uncheckedThumbColor = M3SwitchThumbInactive,
            uncheckedTrackColor = M3SwitchTrackInactive,
            uncheckedBorderColor = M3SwitchTrackInactive
        ),
        modifier = modifier
    )
}
