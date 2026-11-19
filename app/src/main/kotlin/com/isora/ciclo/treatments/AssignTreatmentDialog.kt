package com.isora.ciclo.treatments

import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.R
import com.isora.ciclo.protocols.CatalogProtocol
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.ui.DatePickerModal
import com.isora.ciclo.ui.formatLong

/** Choose a protocol from the catalog, set the number of cycles and the start date. */
@Composable
fun AssignTreatmentDialog(
    protocols: List<CatalogProtocol>,
    onAssign: (protocolName: String, Treatment) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember { mutableStateOf(protocols.firstOrNull()) }
    var form by remember { mutableStateOf(selected?.let { AssignForm(it.protocol) }) }
    var errors by remember { mutableStateOf(emptySet<AssignFormError>()) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.assign_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (protocols.isEmpty()) Text(stringResource(R.string.protocols_empty))
                protocols.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = entry == selected, role = Role.RadioButton) {
                                selected = entry
                                form = AssignForm(entry.protocol, startDate = form?.startDate)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = entry == selected, onClick = null)
                        Text(entry.protocol.name, Modifier.padding(start = CicloSpacing.xs, top = CicloSpacing.xs, bottom = CicloSpacing.xs))
                    }
                }
                form?.let { current ->
                    val untilProgression = current.protocol.defaultCycles is CycleCount.UntilProgression
                    CicloTextField(
                        value = current.cycles,
                        onValueChange = { form = current.copy(cycles = it) },
                        label = {
                            Text(stringResource(if (untilProgression) R.string.protocol_form_reevaluate else R.string.protocol_form_cycles))
                        },
                        isError = AssignFormError.CYCLES_INVALID in errors,
                        supportingText = if (AssignFormError.CYCLES_INVALID in errors) {
                            { Text(stringResource(R.string.protocol_error_cycles)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
                    )
                    OutlinedButton(onClick = { picking = true }, Modifier.fillMaxWidth().padding(top = CicloSpacing.xs)) {
                        Text(current.startDate?.formatLong() ?: stringResource(R.string.assign_start_date))
                    }
                    if (AssignFormError.START_DATE_REQUIRED in errors) {
                        Text(stringResource(R.string.assign_error_start), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val current = form ?: return@TextButton
                    when (val result = current.validate()) {
                        is AssignFormResult.Invalid -> errors = result.errors
                        is AssignFormResult.Valid -> onAssign(current.protocol.name, result.treatment)
                    }
                },
                enabled = form != null,
            ) { Text(stringResource(R.string.assign_confirm)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.protocol_form_cancel)) } },
    )

    if (picking) {
        DatePickerModal(
            initial = form?.startDate,
            onPicked = { date ->
                form = form?.copy(startDate = date)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}
