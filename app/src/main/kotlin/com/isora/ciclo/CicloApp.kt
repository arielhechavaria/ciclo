package com.isora.ciclo

import com.isora.ciclo.ui.CicloColors
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.agenda.AgendaScreen
import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.dailylist.DailyListApplier
import com.isora.ciclo.dailylist.DailyListScreen
import com.isora.ciclo.mock.MockSeeder
import com.isora.ciclo.patients.PatientConflict
import com.isora.ciclo.patients.PatientConflictException
import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.PatientListScreen
import com.isora.ciclo.patients.PatientScreen
import com.isora.ciclo.protocols.CatalogProtocol
import com.isora.ciclo.protocols.ProtocolCatalogScreen
import com.isora.ciclo.security.PinStore
import com.isora.ciclo.settings.SettingsScreen
import com.isora.ciclo.treatments.StoredTreatment
import com.isora.ciclo.treatments.Treatment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private enum class Tab(val label: Int) {
    AGENDA(R.string.tab_agenda),
    PATIENTS(R.string.tab_patients),
    PROTOCOLS(R.string.tab_protocols),
    SETTINGS(R.string.tab_settings),
}

/** The unlocked app: Agenda · Pacientes · Protocolos · Ajustes, and the patient view over any of them. */
@Composable
fun CicloApp(container: AppContainer, pins: PinStore, onLockNow: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.AGENDA) }
    var openPatient by remember { mutableStateOf<PatientId?>(null) }
    var readingList by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf(CareSnapshot(emptyList(), emptyList())) }
    var protocols by remember { mutableStateOf(emptyList<CatalogProtocol>()) }
    var ready by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    suspend fun load() {
        val loaded = withContext(Dispatchers.IO) {
            container.catalog.seedIfEmpty()
            if (BuildConfig.MOCK_DATA && container.patients.all().isEmpty()) MockSeeder.seed(container, today)
            CareSnapshot(container.patients.all(), container.treatments.all()) to container.catalog.all()
        }
        snapshot = loaded.first
        protocols = loaded.second
        ready = true
    }

    /** Runs a repository write off the main thread and reloads; [then] gets the conflict when a patient write was refused. */
    fun write(then: (PatientConflict?) -> Unit = {}, block: () -> Unit) {
        scope.launch {
            val conflict = withContext(Dispatchers.IO) {
                try {
                    block()
                    null
                } catch (e: PatientConflictException) {
                    e.conflict
                }
            }
            load()
            then(conflict)
        }
    }

    LaunchedEffect(tab, openPatient) { load() }
    BackHandler(openPatient != null) { openPatient = null }
    BackHandler(openPatient == null && readingList) { readingList = false }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = CicloColors.canvas) {
                Tab.values().forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = {
                            tab = entry
                            openPatient = null
                            readingList = false
                        },
                        icon = {},
                        label = { Text(stringResource(entry.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (!ready) return@Box
            val patient = openPatient?.let { id -> snapshot.patients.firstOrNull { it.id == id } }
            if (patient != null) {
                PatientScreen(
                    patient = patient,
                    treatments = snapshot.treatmentsOf(patient.id),
                    protocols = protocols,
                    today = today,
                    followUpMonths = container.settings.followUpMonths,
                    onBack = { openPatient = null },
                    onEditPatient = { updated: Patient, done: (PatientConflict?) -> Unit ->
                        write(then = done) { container.patients.update(patient.id, updated) }
                    },
                    onAssign = { name: String, treatment: Treatment ->
                        write { container.treatments.assign(patient.id, name, treatment) }
                    },
                    onChange = { stored: StoredTreatment -> write { container.treatments.save(stored) } },
                )
            } else if (readingList) {
                DailyListScreen(
                    snapshot = snapshot,
                    protocols = protocols,
                    today = today,
                    lockGuard = container.lockGuard,
                    showSample = BuildConfig.MOCK_DATA,
                    onBack = { readingList = false },
                    onUpdateExisting = { actions, done ->
                        scope.launch {
                            val refused = withContext(Dispatchers.IO) {
                                DailyListApplier(container.patients, container.treatments).apply(actions, today)
                            }
                            load()
                            done(refused)
                        }
                    },
                    onAddPatient = { newPatient, done ->
                        scope.launch {
                            val (stored, conflict) = withContext(Dispatchers.IO) {
                                try {
                                    container.patients.add(newPatient) to null
                                } catch (e: PatientConflictException) {
                                    null to e.conflict
                                }
                            }
                            load()
                            done(stored, conflict)
                        }
                    },
                    onAssign = { id, name, treatment -> write { container.treatments.assign(id, name, treatment) } },
                )
            } else {
                when (tab) {
                    Tab.AGENDA -> AgendaScreen(snapshot, today) { openPatient = it }
                    Tab.PATIENTS -> PatientListScreen(
                        snapshot = snapshot,
                        today = today,
                        followUpMonths = container.settings.followUpMonths,
                        onOpen = { openPatient = it },
                        onAdd = { newPatient: Patient, done: (PatientConflict?) -> Unit ->
                            write(then = done) { container.patients.add(newPatient) }
                        },
                        onPhoto = { readingList = true },
                    )
                    Tab.PROTOCOLS -> ProtocolCatalogScreen(container.catalog)
                    Tab.SETTINGS -> SettingsScreen(
                        settings = container.settings,
                        pins = pins,
                        backup = container.backup,
                        lockGuard = container.lockGuard,
                        onRestored = { scope.launch { load() } },
                        onLockNow = onLockNow,
                        onLoadMock = if (BuildConfig.MOCK_DATA) {
                            { write { MockSeeder.seed(container, today) } }
                        } else null,
                    )
                }
            }
        }
    }
}
