package com.example.interviewassistant.core.design.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Adds a clickable modifier without ripple indication.
 *
 * Use for selectable chips/tags where selection is shown by color/border changes.
 *
 * @param enabled Whether clicks are accepted.
 * @param onClick Action invoked when the element is clicked.
 */
@Composable
fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    return clickable(
        enabled = enabled,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick,
    )
}
