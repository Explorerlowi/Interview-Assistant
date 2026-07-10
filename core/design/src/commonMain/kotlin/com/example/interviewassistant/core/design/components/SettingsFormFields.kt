package com.example.interviewassistant.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.theme.AppDesign

/**
 * Plain settings text field with an external label and a clear border.
 *
 * @param value Current field value.
 * @param onValueChange Invoked when the user edits the value.
 * @param label Visible field label rendered above the input.
 * @param modifier Layout modifier for the whole field.
 * @param singleLine Whether the input stays on one line.
 * @param minHeight Minimum height of the input area; useful for multiline prompts.
 */
@Composable
fun SettingsFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp = 46.dp,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) AppDesign.colors.brand else AppDesign.colors.border
    val shape = RoundedCornerShape(AppDesign.radius.sm)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.xs),
    ) {
        Text(
            text = label,
            style = AppDesign.typography.caption,
            color = AppDesign.colors.textSecondary,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .heightIn(min = minHeight)
                .clip(shape)
                .background(AppDesign.colors.surface)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = AppDesign.spacing.md, vertical = AppDesign.spacing.sm)
                .onFocusChanged { focused = it.isFocused },
            singleLine = singleLine,
            textStyle = AppDesign.typography.body.copy(color = AppDesign.colors.textPrimary),
            cursorBrush = SolidColor(AppDesign.colors.brand),
            decorationBox = { innerTextField ->
                Box(contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                    innerTextField()
                }
            },
        )
    }
}

/**
 * Secret settings field that shows the stored value and supports show/hide via an eye toggle.
 *
 * @param value Current secret value.
 * @param onValueChange Invoked when the user edits the value.
 * @param label Visible field label rendered above the input.
 * @param showSecretLabel Accessibility label for revealing the secret.
 * @param hideSecretLabel Accessibility label for masking the secret.
 * @param modifier Layout modifier for the whole field.
 */
@Composable
fun SettingsSecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showSecretLabel: String,
    hideSecretLabel: String,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val borderColor = if (focused) AppDesign.colors.brand else AppDesign.colors.border
    val shape = RoundedCornerShape(AppDesign.radius.sm)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.xs),
    ) {
        Text(
            text = label,
            style = AppDesign.typography.caption,
            color = AppDesign.colors.textSecondary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDesign.sizes.inputHeight)
                .clip(shape)
                .background(AppDesign.colors.surface)
                .border(1.dp, borderColor, shape)
                .padding(start = AppDesign.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = AppDesign.spacing.sm)
                    .onFocusChanged { focused = it.isFocused },
                singleLine = true,
                textStyle = AppDesign.typography.body.copy(color = AppDesign.colors.textPrimary),
                cursorBrush = SolidColor(AppDesign.colors.brand),
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        innerTextField()
                    }
                },
            )
            IconButton(
                onClick = { visible = !visible },
                modifier = Modifier.size(AppDesign.sizes.iconButtonSize),
            ) {
                Icon(
                    painter = rememberVectorPainter(if (visible) VisibilityOffIcon else VisibilityIcon),
                    contentDescription = if (visible) hideSecretLabel else showSecretLabel,
                    tint = AppDesign.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private val VisibilityIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4.5f)
            curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            reflectiveCurveTo(21.27f, 16.39f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            close()
            moveTo(12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            reflectiveCurveTo(9.24f, 7f, 12f, 7f)
            reflectiveCurveTo(17f, 9.24f, 17f, 12f)
            reflectiveCurveTo(14.76f, 17f, 12f, 17f)
            close()
            moveTo(12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            reflectiveCurveTo(10.34f, 15f, 12f, 15f)
            reflectiveCurveTo(15f, 13.66f, 15f, 12f)
            reflectiveCurveTo(13.66f, 9f, 12f, 9f)
            close()
        }
    }.build()
}

private val VisibilityOffIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 12.65f, 16.87f, 13.26f, 16.64f, 13.83f)
            lineTo(19.56f, 16.75f)
            curveTo(21.07f, 15.49f, 22.26f, 13.86f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            curveTo(10.6f, 4.5f, 9.26f, 4.75f, 8.02f, 5.2f)
            lineTo(10.17f, 7.35f)
            curveTo(10.74f, 7.13f, 11.35f, 7f, 12f, 7f)
            close()
            moveTo(2f, 4.27f)
            lineTo(4.28f, 6.55f)
            lineTo(4.74f, 7.01f)
            curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(13.55f, 19.5f, 15.03f, 19.2f, 16.38f, 18.66f)
            lineTo(16.81f, 19.09f)
            lineTo(19.73f, 22f)
            lineTo(21f, 20.73f)
            lineTo(3.27f, 3f)
            close()
            moveTo(7.53f, 9.8f)
            lineTo(9.08f, 11.35f)
            curveTo(9.03f, 11.56f, 9f, 11.78f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(12.22f, 15f, 12.44f, 14.97f, 12.65f, 14.92f)
            lineTo(14.2f, 16.47f)
            curveTo(13.53f, 16.8f, 12.79f, 17f, 12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            curveTo(7f, 11.21f, 7.2f, 10.47f, 7.53f, 9.8f)
            close()
            moveTo(11.84f, 9.02f)
            lineTo(14.99f, 12.17f)
            lineTo(15.01f, 12.01f)
            curveTo(15.01f, 10.35f, 13.67f, 9.01f, 12.01f, 9.01f)
            close()
        }
    }.build()
}
