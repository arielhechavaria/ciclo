package com.isora.ciclo.patients

import com.isora.ciclo.ui.CicloTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.isora.ciclo.R

/**
 * Add or edit a patient: name or initials, clinical record number, optional diagnosis, notes and identity card.
 * [onSave] reports null when saved, or which field already belongs to another patient.
 * [prefill] starts a new patient with text read elsewhere (the daily list photo).
 */
@Composable
fun PatientFormDialog(
    initial: Patient?,
    onSave: (Patient, (PatientConflict?) -> Unit) -> Unit,
    onCancel: () -> Unit,
    prefill: PatientForm? = null,
) {
    var form by remember { mutableStateOf(initial?.let(PatientForm::from) ?: prefill ?: PatientForm()) }
    var errors by remember { mutableStateOf(emptySet<PatientFormError>()) }
    var conflict by remember { mutableStateOf<PatientConflict?>(null) }

    fun submit() {
        when (val result = form.validate()) {
            is PatientFormResult.Invalid -> errors = result.errors
            is PatientFormResult.Valid -> onSave(result.patient) { refused ->
                if (refused != null) {
                    errors = emptySet()
                    conflict = refused
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (initial == null) R.string.patient_form_new else R.string.patient_form_edit)) },
        text = {
            Column {
                CicloTextField(
                    value = form.name,
                    onValueChange = { form = form.copy(name = it) },
                    label = { Text(stringResource(R.string.patient_name)) },
                    isError = PatientFormError.NAME_REQUIRED in errors,
                    supportingText = if (PatientFormError.NAME_REQUIRED in errors) {
                        { Text(stringResource(R.string.protocol_error_name)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CicloTextField(
                    value = form.clinicalRecordNumber,
                    onValueChange = { form = form.copy(clinicalRecordNumber = it); conflict = null },
                    label = { Text(stringResource(R.string.patient_record)) },
                    isError = PatientFormError.RECORD_REQUIRED in errors || conflict == PatientConflict.RECORD,
                    supportingText = when {
                        PatientFormError.RECORD_REQUIRED in errors -> {
                            { Text(stringResource(R.string.patient_error_record)) }
                        }
                        conflict == PatientConflict.RECORD -> {
                            { Text(stringResource(R.string.patient_error_duplicate)) }
                        }
                        else -> null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CicloTextField(
                    value = form.identityCard,
                    onValueChange = { form = form.copy(identityCard = it); conflict = null },
                    label = { Text(stringResource(R.string.patient_identity_card)) },
                    isError = PatientFormError.IDENTITY_CARD_INVALID in errors || conflict == PatientConflict.IDENTITY_CARD,
                    supportingText = when {
                        PatientFormError.IDENTITY_CARD_INVALID in errors -> {
                            { Text(stringResource(R.string.patient_error_identity_card)) }
                        }
                        conflict == PatientConflict.IDENTITY_CARD -> {
                            { Text(stringResource(R.string.patient_error_identity_card_duplicate)) }
                        }
                        else -> null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CicloTextField(
                    value = form.diagnosis,
                    onValueChange = { form = form.copy(diagnosis = it) },
                    label = { Text(stringResource(R.string.patient_diagnosis)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CicloTextField(
                    value = form.notes,
                    onValueChange = { form = form.copy(notes = it) },
                    label = { Text(stringResource(R.string.patient_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(stringResource(R.string.protocol_form_save)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.protocol_form_cancel)) } },
    )
}
