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
import com.isora.ciclo.R

/**
 * Add or edit a patient: name or initials, clinical record number, optional diagnosis and notes.
 * [onSave] returns false when the clinical record number belongs to another patient.
 */
@Composable
fun PatientFormDialog(initial: Patient?, onSave: (Patient, (Boolean) -> Unit) -> Unit, onCancel: () -> Unit) {
    var form by remember { mutableStateOf(initial?.let(PatientForm::from) ?: PatientForm()) }
    var errors by remember { mutableStateOf(emptySet<PatientFormError>()) }
    var duplicate by remember { mutableStateOf(false) }

    fun submit() {
        when (val result = form.validate()) {
            is PatientFormResult.Invalid -> errors = result.errors
            is PatientFormResult.Valid -> onSave(result.patient) { saved ->
                if (!saved) {
                    errors = emptySet()
                    duplicate = true
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
                    onValueChange = { form = form.copy(clinicalRecordNumber = it); duplicate = false },
                    label = { Text(stringResource(R.string.patient_record)) },
                    isError = PatientFormError.RECORD_REQUIRED in errors || duplicate,
                    supportingText = when {
                        PatientFormError.RECORD_REQUIRED in errors -> {
                            { Text(stringResource(R.string.patient_error_record)) }
                        }
                        duplicate -> {
                            { Text(stringResource(R.string.patient_error_duplicate)) }
                        }
                        else -> null
                    },
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
