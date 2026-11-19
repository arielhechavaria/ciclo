package com.isora.ciclo.patients

import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.R
import com.isora.ciclo.agenda.AgendaEntry
import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.ui.formatLong
import java.time.LocalDate

enum class PatientFilter(val label: Int) {
    ALL(R.string.patients_filter_all),
    BY_PROTOCOL(R.string.patients_filter_protocol),
    POSTPONED(R.string.patients_filter_postponed),
    FINISHING(R.string.patients_filter_finishing),
    FOLLOW_UP(R.string.patients_filter_follow_up),
}

/** Active patients first, then the history of those whose treatments finished; searchable. */
@Composable
fun PatientListScreen(
    snapshot: CareSnapshot,
    today: LocalDate,
    followUpMonths: Int,
    onOpen: (PatientId) -> Unit,
    onAdd: (Patient, (PatientConflict?) -> Unit) -> Unit,
    onPhoto: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(PatientFilter.ALL) }

    val matching = snapshot.patients.filter {
        val q = query.trim()
        q.isEmpty() || it.patient.name.contains(q, ignoreCase = true) ||
            it.patient.clinicalRecordNumber.contains(q, ignoreCase = true) ||
            it.patient.identityCard?.contains(identityCardKey(q)) == true
    }.sortedBy { it.patient.name.lowercase() }
    val history = matching.filter { snapshot.isHistory(it.id, today) }
    val active = matching - history.toSet()

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(CicloSpacing.xs)) {
                onPhoto?.let { open ->
                    ExtendedFloatingActionButton(onClick = open) { Text(stringResource(R.string.list_photo_open)) }
                }
                ExtendedFloatingActionButton(onClick = { adding = true }) { Text(stringResource(R.string.patients_add)) }
            }
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
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(vertical = CicloSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(CicloSpacing.xs),
            ) {
                PatientFilter.values().forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(stringResource(option.label)) },
                    )
                }
            }
            if (filter != PatientFilter.ALL) {
                FilteredList(filter, snapshot, today, followUpMonths, matching.toSet(), onOpen)
                return@Column
            }
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
            onSave = { patient, done -> onAdd(patient) { conflict -> if (conflict == null) adding = false; done(conflict) } },
            onCancel = { adding = false },
        )
    }
}

@Composable
private fun FilteredList(
    filter: PatientFilter,
    snapshot: CareSnapshot,
    today: LocalDate,
    followUpMonths: Int,
    allowed: Set<StoredPatient>,
    onOpen: (PatientId) -> Unit,
) {
    fun List<AgendaEntry>.visible() = filter { it.patient in allowed }
    if (filter == PatientFilter.FOLLOW_UP) {
        FollowUpList(snapshot, today, followUpMonths, allowed, onOpen)
        return
    }
    val groups: Map<String?, List<AgendaEntry>> = when (filter) {
        PatientFilter.BY_PROTOCOL -> snapshot.activeByProtocol(today).mapValues { it.value.visible() }.filterValues { it.isNotEmpty() }
        PatientFilter.POSTPONED -> snapshot.postponed(today).visible().let { if (it.isEmpty()) emptyMap() else mapOf(null to it) }
        else -> snapshot.finishingThisMonth(today).visible().let { if (it.isEmpty()) emptyMap() else mapOf(null to it) }
    }
    LazyColumn {
        if (groups.isEmpty()) item { Text(stringResource(R.string.patients_filter_empty)) }
        groups.forEach { (title, entries) ->
            if (title != null) {
                item(key = "t$title") {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = CicloSpacing.md, bottom = CicloSpacing.xxs),
                    )
                }
            }
            items(entries, key = { "${filter.name}${title}${it.treatment.id.value}" }) { entry ->
                PatientRow(entry.patient, snapshot, today, onOpen)
            }
        }
    }
}

@Composable
private fun FollowUpList(
    snapshot: CareSnapshot,
    today: LocalDate,
    followUpMonths: Int,
    allowed: Set<StoredPatient>,
    onOpen: (PatientId) -> Unit,
) {
    val due = snapshot.followUps(today, followUpMonths).filter { it.entry.patient in allowed }
    LazyColumn {
        if (due.isEmpty()) item { Text(stringResource(R.string.patients_filter_empty)) }
        items(due, key = { "f${it.entry.treatment.id.value}" }) { followUp ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(followUp.entry.patient.id) }.padding(vertical = CicloSpacing.xs)) {
                Text(followUp.entry.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${followUp.entry.patient.patient.clinicalRecordNumber} · " +
                        stringResource(R.string.patients_follow_up_due, followUp.dueOn.formatLong()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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
