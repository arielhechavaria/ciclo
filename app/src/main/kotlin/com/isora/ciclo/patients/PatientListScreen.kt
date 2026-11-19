package com.isora.ciclo.patients

import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.R
import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.ui.formatLong
import java.time.LocalDate

/** Active patients first, then the history of those whose treatments finished; searchable. */
@Composable
fun PatientListScreen(
    snapshot: CareSnapshot,
    today: LocalDate,
    onOpen: (PatientId) -> Unit,
    onAdd: (Patient, (Boolean) -> Unit) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }

    val matching = snapshot.patients.filter {
        val q = query.trim()
        q.isEmpty() || it.patient.name.contains(q, ignoreCase = true) ||
            it.patient.clinicalRecordNumber.contains(q, ignoreCase = true)
    }.sortedBy { it.patient.name.lowercase() }
    val history = matching.filter { snapshot.isHistory(it.id, today) }
    val active = matching - history.toSet()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = true }) { Text(stringResource(R.string.patients_add)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = CicloSpacing.md)) {
            Text(
                stringResource(R.string.patients_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = CicloSpacing.md),
            )
            CicloTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.patients_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(Modifier.padding(top = CicloSpacing.xs)) {
                if (matching.isEmpty()) item { Text(stringResource(R.string.patients_empty)) }
                items(active, key = { "a${it.id.value}" }) { stored -> PatientRow(stored, snapshot, today, onOpen) }
                if (history.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.patients_history),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = CicloSpacing.md, bottom = CicloSpacing.xxs),
                        )
                    }
                    items(history, key = { "h${it.id.value}" }) { stored -> PatientRow(stored, snapshot, today, onOpen) }
                }
            }
        }
    }

    if (adding) {
        PatientFormDialog(
            initial = null,
            onSave = { patient, done -> onAdd(patient) { saved -> if (saved) adding = false; done(saved) } },
            onCancel = { adding = false },
        )
    }
}

@Composable
private fun PatientRow(stored: StoredPatient, snapshot: CareSnapshot, today: LocalDate, onOpen: (PatientId) -> Unit) {
    val treatments = snapshot.treatmentsOf(stored.id)
    val running = treatments.filter { !it.treatment.isFinishedOn(today) }
    val status = when {
        running.isNotEmpty() -> running.first().protocolName
        treatments.isNotEmpty() ->
            stringResource(R.string.patient_finished_on, treatments.mapNotNull { it.treatment.endDate }.max().formatLong())
        else -> stringResource(R.string.patient_no_treatment)
    }
    Column(Modifier.fillMaxWidth().clickable { onOpen(stored.id) }.padding(vertical = CicloSpacing.xs)) {
        Text(stored.patient.name, style = MaterialTheme.typography.titleMedium)
        Text("${stored.patient.clinicalRecordNumber} · $status", style = MaterialTheme.typography.bodyMedium)
    }
}
