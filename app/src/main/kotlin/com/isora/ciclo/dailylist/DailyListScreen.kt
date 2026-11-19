package com.isora.ciclo.dailylist

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.isora.ciclo.R
import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.mock.MockData
import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientConflict
import com.isora.ciclo.patients.PatientForm
import com.isora.ciclo.patients.PatientFormDialog
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.patients.treatmentProgress
import com.isora.ciclo.protocols.CatalogProtocol
import com.isora.ciclo.security.LockGuard
import com.isora.ciclo.treatments.AssignTreatmentDialog
import com.isora.ciclo.treatments.Treatment
import com.isora.ciclo.ui.CicloCard
import com.isora.ciclo.ui.CicloSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private sealed interface Stage {
    data class Start(val message: Int? = null) : Stage
    object Reading : Stage
    data class Review(val rows: List<ReviewRow>, val unread: List<String>) : Stage

    /**
     * New patients go one by one through the form, then the assign dialog once [added] is saved.
     * [refusedCards] names the registered patients whose card could not be saved, shown at the end.
     */
    data class Registering(
        val queue: List<ListAction.RegisterNew>,
        val total: Int,
        val refusedCards: List<String>,
        val added: StoredPatient? = null,
    ) : Stage
    data class Done(val refusedCards: List<String>) : Stage
}

/**
 * Registers patients from a photo of the day's list: take the photo, read it on the phone, review, confirm.
 * [onUpdateExisting] applies the changes to registered patients and returns the ones whose card was refused.
 * [onAddPatient] saves a new patient and returns it, or the field another patient already holds.
 * [showSample] offers the fictional sample list (mocked data, until the public release).
 */
@Composable
fun DailyListScreen(
    snapshot: CareSnapshot,
    protocols: List<CatalogProtocol>,
    today: LocalDate,
    lockGuard: LockGuard,
    showSample: Boolean,
    onBack: () -> Unit,
    onUpdateExisting: (List<ListAction.UpdateExisting>, (List<ListAction.UpdateExisting>) -> Unit) -> Unit,
    onAddPatient: (Patient, (StoredPatient?, PatientConflict?) -> Unit) -> Unit,
    onAssign: (PatientId, String, Treatment) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photo = remember { ListPhoto(context) }
    var stage by remember { mutableStateOf<Stage>(Stage.Start()) }

    // A photo left behind by a run that was interrupted is never kept.
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { photo.deleteAll() } }

    fun review(lines: List<TextLine>) {
        val reading = DailyListReader.read(lines)
        stage = if (reading.entries.isEmpty()) {
            Stage.Start(R.string.list_photo_empty)
        } else {
            val names = protocols.map { it.protocol.name }
            Stage.Review(DailyListMatcher.match(reading.entries, snapshot.patients, names), reading.unread)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        lockGuard.suspended = false
        if (!taken) {
            photo.deleteAll()
            stage = Stage.Start()
            return@rememberLauncherForActivityResult
        }
        stage = Stage.Reading
        scope.launch {
            val lines = try {
                photo.readAndDelete()
            } catch (e: Exception) {
                null
            }
            when (lines) {
                null -> stage = Stage.Start(R.string.list_photo_failed)
                else -> review(lines)
            }
        }
    }

    fun confirm(actions: List<ListAction>) {
        val existing = actions.filterIsInstance<ListAction.UpdateExisting>()
        val newOnes = actions.filterIsInstance<ListAction.RegisterNew>()
        onUpdateExisting(existing) { refused ->
            val names = refused.map { it.patient.patient.name }
            stage = if (newOnes.isEmpty()) Stage.Done(names) else Stage.Registering(newOnes, newOnes.size, names)
        }
    }

    Column(Modifier.fillMaxWidth().padding(CicloSpacing.md)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(
            stringResource(R.string.list_photo_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = CicloSpacing.xs),
        )
        when (val current = stage) {
            is Stage.Start -> {
                Text(stringResource(R.string.list_photo_intro))
                Text(stringResource(R.string.list_photo_hint), style = MaterialTheme.typography.bodyMedium)
                current.message?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = CicloSpacing.xs))
                }
                Button(
                    onClick = {
                        lockGuard.suspended = true
                        try {
                            camera.launch(photo.target())
                        } catch (e: ActivityNotFoundException) {
                            lockGuard.suspended = false
                            photo.deleteAll()
                            stage = Stage.Start(R.string.list_photo_no_camera)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.md),
                ) { Text(stringResource(R.string.list_photo_take)) }
                if (showSample) {
                    OutlinedButton(
                        onClick = { review(MockData.sampleList()) },
                        modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
                    ) { Text(stringResource(R.string.list_photo_sample)) }
                }
            }
            Stage.Reading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Text(stringResource(R.string.list_photo_reading), Modifier.padding(start = CicloSpacing.md))
            }
            is Stage.Review -> DailyListReview(
                rows = current.rows,
                unread = current.unread,
                snapshot = snapshot,
                today = today,
                onConfirm = ::confirm,
                onCancel = { stage = Stage.Start() },
            )
            is Stage.Registering -> Text(
                stringResource(R.string.list_photo_registering, current.total - current.queue.size + 1, current.total),
            )
            is Stage.Done -> {
                Text(stringResource(R.string.list_photo_done))
                current.refusedCards.forEach {
                    Text(stringResource(R.string.list_photo_card_taken, it), color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onBack, modifier = Modifier.padding(top = CicloSpacing.md)) { Text(stringResource(R.string.back)) }
            }
        }
    }

    val registering = stage as? Stage.Registering ?: return
    val head = registering.queue.first()
    fun next() {
        val rest = registering.queue.drop(1)
        stage = if (rest.isEmpty()) Stage.Done(registering.refusedCards) else registering.copy(queue = rest, added = null)
    }
    val added = registering.added
    // Each new patient starts with its own fresh form and dialog state.
    key(registering.queue.size, added) {
        if (added == null) {
            PatientFormDialog(
                initial = null,
                prefill = PatientForm(name = head.entry.name, identityCard = head.entry.identityCard.orEmpty()),
                onSave = { patient, done ->
                    onAddPatient(patient) { stored, conflict ->
                        done(conflict)
                        when {
                            stored == null -> Unit
                            protocols.isEmpty() -> next()
                            else -> stage = registering.copy(added = stored)
                        }
                    }
                },
                onCancel = ::next,
            )
        } else {
            AssignTreatmentDialog(
                protocols = protocols,
                preselected = head.protocolName,
                startDate = today,
                onAssign = { name, treatment ->
                    onAssign(added.id, name, treatment)
                    next()
                },
                onCancel = ::next,
            )
        }
    }
}

/**
 * The rows read from the photo, each with the user's choices, and the lines that could not be read.
 * [onConfirm] receives what confirming does, row by row (rows left out are skipped).
 */
@Composable
fun DailyListReview(
    rows: List<ReviewRow>,
    unread: List<String>,
    snapshot: CareSnapshot,
    today: LocalDate,
    onConfirm: (List<ListAction>) -> Unit,
    onCancel: () -> Unit,
) {
    var decisions by remember(rows) { mutableStateOf(rows.map { ReviewDecision(it) }) }
    fun update(index: Int, change: (ReviewDecision) -> ReviewDecision) {
        decisions = decisions.toMutableList().also { it[index] = change(it[index]) }
    }
    fun hasSessionToday(patient: StoredPatient) =
        snapshot.sessionsOn(today).any { it.entry.patient.id == patient.id && !it.session.given }

    Column {
        Text(stringResource(R.string.list_photo_review), style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f, fill = false)) {
            itemsIndexed(decisions) { index, decision ->
                val row = decision.row
                val patient = row.patient
                CicloCard(Modifier.fillMaxWidth().padding(vertical = CicloSpacing.xxs)) {
                    Column(Modifier.padding(CicloSpacing.md)) {
                        Text(row.entry.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(
                                row.entry.identityCard?.let { stringResource(R.string.list_photo_card, it) },
                                row.entry.treatment ?: stringResource(R.string.list_photo_no_treatment),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        when {
                            row.kind == MatchKind.CARD && patient != null -> Text(registeredText(patient, snapshot, today))
                            row.kind == MatchKind.NAME && patient != null -> {
                                Text(stringResource(R.string.list_photo_probable, patient.patient.name, patient.patient.clinicalRecordNumber))
                                Check(stringResource(R.string.list_photo_same_person), decision.samePerson) { on ->
                                    update(index) { it.copy(samePerson = on) }
                                }
                                if (decision.samePerson) Text(registeredText(patient, snapshot, today))
                            }
                            else -> Text(stringResource(R.string.list_photo_new))
                        }
                        val existing = decision.action() is ListAction.UpdateExisting
                        if (!existing && row.entry.treatment != null && row.protocolName == null) {
                            Text(
                                stringResource(R.string.list_photo_unknown_treatment, row.entry.treatment.orEmpty()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Check(stringResource(R.string.list_photo_include), decision.include) { on ->
                            update(index) { it.copy(include = on) }
                        }
                        if (existing && patient != null && hasSessionToday(patient)) {
                            Check(stringResource(R.string.list_photo_today), decision.markToday) { on ->
                                update(index) { it.copy(markToday = on) }
                            }
                        }
                    }
                }
            }
            if (unread.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.list_photo_unread),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = CicloSpacing.md),
                    )
                }
                items(unread) { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Row(Modifier.padding(top = CicloSpacing.md), horizontalArrangement = Arrangement.spacedBy(CicloSpacing.xs)) {
            OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.protocol_form_cancel)) }
            Button(onClick = { onConfirm(decisions.mapNotNull { it.action() }) }) { Text(stringResource(R.string.list_photo_confirm)) }
        }
    }
}

@Composable
private fun registeredText(patient: StoredPatient, snapshot: CareSnapshot, today: LocalDate): String {
    val running = snapshot.treatmentsOf(patient.id).firstOrNull { !it.treatment.isFinishedOn(today) }
        ?: return stringResource(R.string.list_photo_registered_idle)
    return stringResource(R.string.list_photo_registered, "${running.protocolName} · ${treatmentProgress(running.treatment)}")
}

@Composable
private fun Check(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, Modifier.padding(start = CicloSpacing.xs))
    }
}
