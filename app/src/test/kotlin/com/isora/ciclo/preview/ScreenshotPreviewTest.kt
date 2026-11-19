package com.isora.ciclo.preview

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.isora.ciclo.agenda.AgendaScreen
import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.PatientListScreen
import com.isora.ciclo.patients.PatientScreen
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.protocols.InMemoryProtocolCatalog
import com.isora.ciclo.protocols.ProtocolCatalogScreen
import com.isora.ciclo.security.LockScreen
import com.isora.ciclo.security.PinStore
import com.isora.ciclo.settings.AppSettings
import com.isora.ciclo.settings.SettingsScreen
import com.isora.ciclo.treatments.SessionRef
import com.isora.ciclo.treatments.StoredTreatment
import com.isora.ciclo.treatments.Treatment
import com.isora.ciclo.treatments.TreatmentId
import com.isora.ciclo.ui.CicloTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Renders the main screens with fake data and writes PNGs to app/build/screenshots so UI changes
 * can be reviewed from the CI artifact without installing the app. Not a visual regression test:
 * it only asserts that the image is not blank.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotPreviewTest {
    @get:Rule
    val rule = createComposeRule()

    // A Wednesday, so the week and month views have sessions before and after it.
    private val today = LocalDate.of(2026, 11, 18)
    private val catalog = InMemoryProtocolCatalog()
    private fun protocol(name: String) = catalog.all().first { it.protocol.name == name }.protocol

    private val patients = listOf(
        StoredPatient(PatientId(1), Patient("María P.", "HC-1042", "Cáncer de mama HER2-", "Alergia a penicilina")),
        StoredPatient(PatientId(2), Patient("José R.", "HC-2217", "Cáncer de pulmón")),
        StoredPatient(PatientId(3), Patient("Ana L.", "HC-3310", "Cáncer de colon")),
        StoredPatient(PatientId(4), Patient("Luis G.", "HC-0877", "Linfoma")),
    )
    private val treatments = listOf(
        StoredTreatment(
            TreatmentId(1), PatientId(1), "AC",
            Treatment.assign(protocol("AC"), today.minusDays(25))
                .markGiven(SessionRef(1, 1)).postpone(SessionRef(2, 1), today.plusDays(3)),
        ),
        StoredTreatment(
            TreatmentId(2), PatientId(2), "Gemcitabina + cisplatino",
            Treatment.assign(protocol("Gemcitabina + cisplatino"), today.minusDays(6)).markGiven(SessionRef(1, 1)),
        ),
        StoredTreatment(
            TreatmentId(3), PatientId(3), "AC dosis densa",
            Treatment.assign(protocol("AC dosis densa"), today.plusDays(2)),
        ),
        StoredTreatment(
            TreatmentId(4), PatientId(4), "AC",
            Treatment.assign(protocol("AC"), today.minusDays(120)),
        ),
    )
    private val snapshot = CareSnapshot(patients, treatments)

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent { CicloTheme { Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) { content() } } }
        rule.waitForIdle()
        capture(name)
    }

    private fun capture(name: String) {
        val bitmap: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Guard against silently blank captures: more than one distinct colour must be present.
        val seen = HashSet<Int>()
        for (x in 0 until bitmap.width step 7) for (y in 0 until bitmap.height step 7) {
            seen += bitmap.getPixel(x, y)
            if (seen.size > 1) break
        }
        assertTrue("Screenshot $name is blank", seen.size > 1)
    }

    @Test
    fun agendaLine() = shot("agenda-linea") { AgendaScreen(snapshot, today) {} }

    @Test
    fun agendaWeek() {
        rule.setContent { CicloTheme { AgendaScreen(snapshot, today) {} } }
        rule.onNodeWithText("Semana").performClick()
        rule.waitForIdle()
        capture("agenda-semana")
    }

    @Test
    fun agendaMonth() {
        rule.setContent { CicloTheme { AgendaScreen(snapshot, today) {} } }
        rule.onNodeWithText("Mes").performClick()
        rule.waitForIdle()
        capture("agenda-mes")
    }

    @Test
    fun patientList() = shot("pacientes") { PatientListScreen(snapshot, today, {}, { _, _ -> }) }

    @Test
    fun patientWithTreatment() = shot("paciente") {
        PatientScreen(
            patient = patients[0],
            treatments = snapshot.treatmentsOf(PatientId(1)),
            protocols = catalog.all(),
            today = today,
            onBack = {},
            onEditPatient = { _, _ -> },
            onAssign = { _, _ -> },
            onChange = {},
        )
    }

    @Test
    fun protocolCatalog() = shot("protocolos") { ProtocolCatalogScreen(catalog, EmptyCoroutineContext) }

    @Test
    fun settings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        shot("ajustes") { SettingsScreen(AppSettings(context), PinStore(context), {}) }
    }

    @Test
    fun lock() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val activity: Activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        shot("bloqueo") {
            CompositionLocalProvider(LocalContext provides activity) { LockScreen(PinStore(context), {}) }
        }
    }
}
