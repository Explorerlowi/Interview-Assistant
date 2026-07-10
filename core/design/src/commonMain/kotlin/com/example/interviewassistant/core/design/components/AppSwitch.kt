package com.example.interviewassistant.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.interviewassistant.core.design.theme.AppDesign

/**
 * Labeled switch styled with design-system colors for clear on/off contrast.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked when the user toggles the switch.
 * @param label Text shown to the left of the switch.
 * @param modifier Layout modifier for the whole row.
 * @param enabled Whether the control accepts interaction.
 */
@Composable
fun AppSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppDesign.typography.body,
            color = if (enabled) AppDesign.colors.textPrimary else AppDesign.colors.textTertiary,
        )
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/**
 * Material switch with brand-colored checked state and a higher-contrast unchecked track.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked when the user toggles the switch.
 * @param modifier Layout modifier for the switch.
 * @param enabled Whether the control accepts interaction.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppDesign.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onBrand,
            checkedTrackColor = colors.brand,
            checkedBorderColor = colors.brand,
            uncheckedThumbColor = colors.surface,
            uncheckedTrackColor = colors.border,
            uncheckedBorderColor = colors.textTertiary,
            disabledCheckedThumbColor = colors.onBrand.copy(alpha = 0.7f),
            disabledCheckedTrackColor = colors.brand.copy(alpha = 0.4f),
            disabledCheckedBorderColor = colors.brand.copy(alpha = 0.4f),
            disabledUncheckedThumbColor = colors.surface.copy(alpha = 0.8f),
            disabledUncheckedTrackColor = colors.divider,
            disabledUncheckedBorderColor = colors.border,
        ),
    )
}
