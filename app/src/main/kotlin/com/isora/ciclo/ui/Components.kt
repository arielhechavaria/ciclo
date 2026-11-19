package com.isora.ciclo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** DESIGN.md "card base": canvas surface, hairline border, large radius. */
@Composable
fun CicloCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CicloRadius.lg),
        colors = CardDefaults.cardColors(containerColor = CicloColors.canvas),
        border = BorderStroke(1.dp, CicloColors.hairline),
        content = content,
    )
}

/** DESIGN.md "text input": hairline border, medium radius, brand-green focus ring. */
@Composable
fun CicloTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(CicloRadius.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CicloColors.brandGreen,
            unfocusedBorderColor = CicloColors.hairline,
            errorBorderColor = CicloColors.brandError,
            cursorColor = CicloColors.ink,
        ),
    )
}
