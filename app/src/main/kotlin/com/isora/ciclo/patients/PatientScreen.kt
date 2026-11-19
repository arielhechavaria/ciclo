package com.isora.ciclo.patients

import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloTextField
import com.isora.ciclo.ui.CicloCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.R
import com.isora.ciclo.protocols.CatalogProtocol
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.treatments.AssignTreatmentDialog
import com.isora.ciclo.treatments.ScheduledSession
import com.isora.ciclo.treatments.SessionRef
import com.isora.ciclo.treatments.StoredTreatment
import com.isora.ciclo.treatments.Treatment
import com.isora.ciclo.ui.DatePickerModal
import com.isora.ciclo.ui.CicloPalette
import com.isora.ciclo.ui.formatLong
import com.isora.ciclo.ui.formatShort
import java.time.LocalDate

private const val CYCLES_PER_PAGE = 6

/** One patient: data, their treatments with every cycle and session, and the actions on them. */
@Composable
fun PatientScreen(
    patient: StoredPatient,
    treatments: List<StoredTreatment>,
    protocols: List<CatalogProtocol>,
    today: LocalDate,
    followUpMonths: Int,
    onBack: () -> Unit,
    onEditPatient: (Patient, (PatientConflict?) -> Unit) -> Unit,
    onAssign: (protocolName: String, Treatment) -> Unit,
    onChange: (StoredTreatment) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var assigning by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CicloSpacing.md)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(patient.patient.name, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.patient_record_line, patient.patient.clinicalRecordNumber))
        patient.patient.identityCard?.let { Text(stringResource(R.string.patient_identity_card_line, it)) }
        patient.patient.diagnosis?.let { Text(it) }
        patient.patient.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Row(Modifier.padding(vertical = CicloSpacing.xs), horizontalArrangement = Arrangement.spacedBy(CicloSpacing.xs)) {
            OutlinedButton(onClick = { editing = true }) { Text(stringResource(R.string.protocol_edit)) }
            Button(onClick = { assigning = true }) { Text(stringResource(R.string.assign_title)) }
        }
        if (treatments.isEmpty()) Text(stringResource(R.string.patient_no_treatment))
        treatments.forEach { stored -> TreatmentCard(stored, today, followUpMonths, onChange) }
    }

    if (editing) {
        PatientFormDialog(
            initial = patient.patient,
            onSave = { updated, done -> onEditPatient(updated) { conflict -> if (conflict == null) editing = false; done(conflict) } },
            onCancel = { editing = false },
        )
    }
    if (assigning) {
        AssignTreatmentDialog(
            protocols = protocols,
            onAssign = { name, treatment ->
                assigning = false
                onAssign(name, treatment)
            },
            onCancel = { assigning = false },
        )
    }
}

@Composable
private fun TreatmentCard(stored: StoredTreatment, today: LocalDate, followUpMonths: Int, onChange: (StoredTreatment) -> Unit) {
    val treatment = stored.treatment
    var shownCycles by remember { mutableStateOf(CYCLES_PER_PAGE) }
    var postponing by remember { mutableStateOf<ScheduledSession?>(null) }
    var postponeError by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }

    val cycles = treatment.cycles().take(shownCycles + 1).toList()
    val visible = cycles.take(shownCycles)
    val hasMore = cycles.size > shownCycles
    CicloCard(Modifier.fillMaxWidth().padding(vertical = CicloSpacing.xs)) {
        Column(Modifier.padding(CicloSpacing.lg)) {
            Text(stored.protocolName, style = MaterialTheme.typography.titleLarge)
            Text(treatmentProgress(treatment))
            val end = treatment.endDate
            when {
                treatment.earlyFinish != null -> Text(
                    stringResource(R.string.finished_early, treatment.earlyFinish!!.date.formatLong(), treatment.earlyFinish!!.reason),
                )
                end != null -> Text(stringResource(if (treatment.isFinishedOn(today)) R.string.ended_on else R.string.ends_on, end.formatLong()))
                else -> Text(
                    stringResource(
                        R.string.no_end_date,
                        (treatment.cycleCount as CycleCount.UntilProgression).reevaluateEveryCycles,
                    ),
                )
            }
            if (treatment.isFinishedOn(today)) {
                val done = treatment.followUpDoneOn
                val due = treatment.followUpDate(followUpMonths)
                if (done != null) {
                    Text(stringResource(R.string.follow_up_done_on, done.formatLong()))
                } else if (due != null) {
                    Text(stringResource(R.string.follow_up_due, due.formatLong()))
                    OutlinedButton(
                        onClick = { onChange(stored.copy(treatment = treatment.markFollowUpDone(today))) },
                        Modifier.padding(top = CicloSpacing.xs),
                    ) { Text(stringResource(R.string.follow_up_mark_done)) }
                }
            }
            visible.forEach { cycle ->
                Text(
                    stringResource(R.string.cycle_header, cycle.number, cycle.start.formatShort()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = CicloSpacing.sm),
                )
                cycle.sessions.forEach { session ->
                    SessionRow(
                        session = session,
                        finished = treatment.earlyFinish != null,
                        onGiven = { onChange(stored.copy(treatment = treatment.markGiven(session.ref))) },
                        onPostpone = {
                            postponeError = false
                            postponing = session
                        },
                    )
                }
            }
            if (hasMore) {
                TextButton(onClick = { shownCycles += CYCLES_PER_PAGE }) { Text(stringResource(R.string.show_more_cycles)) }
            }
            if (treatment.earlyFinish == null) {
                OutlinedButton(onClick = { finishing = true }, Modifier.padding(top = CicloSpacing.sm)) {
                    Text(stringResource(R.string.finish_early))
                }
            }
        }
    }

    postponing?.let { session ->
        DatePickerModal(
            initial = session.date.plusDays(1),
            onPicked = { date ->
                val updated = runCatching { treatment.postpone(session.ref, date) }.getOrNull()
                if (updated == null) {
                    postponeError = true
                } else {
                    postponing = null
                    onChange(stored.copy(treatment = updated))
                }
            },
            onDismiss = { postponing = null },
        )
        if (postponeError) {
            AlertDialog(
                onDismissRequest = { postponeError = false },
                text = { Text(stringResource(R.string.postpone_error, session.postponedFrom?.formatLong() ?: session.date.formatLong())) },
                confirmButton = { TextButton(onClick = { postponeError = false }) { Text(stringResource(R.string.ok)) } },
            )
        }
    }
    if (finishing) {
        FinishEarlyDialog(
            today = today,
            onConfirm = { reason ->
                finishing = false
                runCatching { treatment.finishEarly(today, reason) }.getOrNull()?.let { onChange(stored.copy(treatment = it)) }
            },
            onCancel = { finishing = false },
        )
    }
}

@Composable
private fun SessionRow(session: ScheduledSession, finished: Boolean, onGiven: () -> Unit, onPostpone: () -> Unit) {
    val statusText = when {
        session.given -> stringResource(R.string.status_given)
        session.isPostponed -> stringResource(R.string.status_postponed, session.postponedFrom!!.formatShort())
        else -> stringResource(R.string.status_planned)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = CicloSpacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.session_line, session.ref.session, session.date.formatShort()),
                Modifier.padding(end = CicloSpacing.xs),
            )
            if (session.isLast) {
                Text(stringResource(R.string.last_session), color = CicloPalette.lastSession, style = MaterialTheme.typography.labelLarge)
            }
        }
        Text(statusText, style = MaterialTheme.typography.bodySmall, color = if (session.isPostponed) CicloPalette.postponed else MaterialTheme.colorScheme.secondary)
        if (!session.given && !finished) {
            Row(horizontalArrangement = Arrangement.spacedBy(CicloSpacing.xs)) {
                OutlinedButton(onClick = onGiven) { Text(stringResource(R.string.mark_given)) }
                OutlinedButton(onClick = onPostpone) { Text(stringResource(R.string.mark_postponed)) }
            }
        }
    }
}

@Composable
private fun FinishEarlyDialog(today: LocalDate, onConfirm: (String) -> Unit, onCancel: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.finish_early)) },
        text = {
            Column {
                Text(stringResource(R.string.finish_early_hint, today.formatLong()))
                CicloTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.finish_early_reason)) },
                    modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = reason.isNotBlank()) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.protocol_form_cancel)) } },
    )
}

/** "Ciclo X de N, sesión Y de M" for the next session not given yet, or that every session was given. */
@Composable
fun treatmentProgress(treatment: Treatment): String {
    val next = treatment.sessions().firstOrNull { !it.given } ?: return stringResource(R.string.progress_done)
    val total = treatment.totalCycles
    return if (total != null) {
        stringResource(R.string.progress_of, next.ref.cycle, total, next.ref.session, treatment.regimen.sessionsPerCycle)
    } else {
        stringResource(R.string.progress_open, next.ref.cycle, next.ref.session, treatment.regimen.sessionsPerCycle)
    }
}
