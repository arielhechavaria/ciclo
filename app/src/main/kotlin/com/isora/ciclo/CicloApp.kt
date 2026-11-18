package com.isora.ciclo

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
import com.isora.ciclo.patients.DuplicateClinicalRecordException
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
    var snapshot by remember { mutableStateOf(CareSnapshot(emptyList(), emptyList())) }
    var protocols by remember { mutableStateOf(emptyList<CatalogProtocol>()) }
    var ready by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    suspend fun load() {
        val loaded = withContext(Dispatchers.IO) {
            container.catalog.seedIfEmpty()
            CareSnapshot(container.patients.all(), container.treatments.all()) to container.catalog.all()
        }
        snapshot = loaded.first
        protocols = loaded.second
        ready = true
    }

    /** Runs a repository write off the main thread and reloads; [then] gets false when the write was refused. */
    fun write(then: (Boolean) -> Unit = {}, block: () -> Unit) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    block()
                    true
                } catch (e: DuplicateClinicalRecordException) {
                    false
                }
            }
            load()
            then(ok)
        }
    }

    LaunchedEffect(tab, openPatient) { load() }
    BackHandler(openPatient != null) { openPatient = null }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = {
                            tab = entry
                            openPatient = null
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
                    onBack = { openPatient = null },
                    onEditPatient = { updated: Patient, done: (Boolean) -> Unit ->
                        write(then = done) { container.patients.update(patient.id, updated) }
                    },
                    onAssign = { name: String, treatment: Treatment ->
                        write { container.treatments.assign(patient.id, name, treatment) }
                    },
                    onChange = { stored: StoredTreatment -> write { container.treatments.save(stored) } },
                )
            } else {
                when (tab) {
                    Tab.AGENDA -> AgendaScreen(snapshot, today) { openPatient = it }
                    Tab.PATIENTS -> PatientListScreen(
                        snapshot = snapshot,
                        today = today,
                        onOpen = { openPatient = it },
                        onAdd = { newPatient: Patient, done: (Boolean) -> Unit ->
                            write(then = done) { container.patients.add(newPatient) }
                        },
                    )
                    Tab.PROTOCOLS -> ProtocolCatalogScreen(container.catalog)
                    Tab.SETTINGS -> SettingsScreen(container.settings, pins, onLockNow)
                }
            }
        }
    }
}
