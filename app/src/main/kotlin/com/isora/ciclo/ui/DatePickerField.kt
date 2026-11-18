package com.isora.ciclo.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A modal calendar; [onPicked] receives the chosen day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(initial: LocalDate?, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.protocol_form_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}
