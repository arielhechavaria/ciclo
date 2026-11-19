package com.isora.ciclo.patients

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import com.isora.ciclo.treatments.SessionRef
import com.isora.ciclo.treatments.StoredTreatment
import com.isora.ciclo.treatments.Treatment
import com.isora.ciclo.treatments.TreatmentId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PatientListScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 18)
    private val ac = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))
    private val folfox = Protocol("FOLFOX", Regimen(14, listOf(1)), CycleCount.Fixed(1))
    private val past = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))
    private val snapshot = CareSnapshot(
        listOf(
            StoredPatient(PatientId(1), Patient("Ana", "HC-1", identityCard = "85010112345")),
            StoredPatient(PatientId(2), Patient("Diana", "HC-2")),
            StoredPatient(PatientId(3), Patient("Berta", "HC-3")),
        ),
        listOf(
            StoredTreatment(
                TreatmentId(1), PatientId(1), "AC",
                Treatment.assign(ac, LocalDate.of(2026, 11, 3)).postpone(SessionRef(1, 1), LocalDate.of(2026, 11, 5)),
            ),
            StoredTreatment(TreatmentId(2), PatientId(2), "FOLFOX", Treatment.assign(folfox, LocalDate.of(2026, 11, 10))),
            // Ended on 2026-03-30, so the 3-month follow-up was due on 2026-06-30.
            StoredTreatment(TreatmentId(3), PatientId(3), "AC", Treatment.assign(past, LocalDate.of(2026, 1, 5))),
        ),
    )

    private fun show() = rule.setContent {
        MaterialTheme { PatientListScreen(snapshot, today, followUpMonths = 3, onOpen = {}, onAdd = { _, _ -> }) }
    }

    @Test
    fun postponedFilterShowsOnlyPatientsWithAPostponedSession() {
        show()
        rule.onNodeWithText("Pospuestos").performClick()
        rule.onNodeWithText("Ana", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Diana", substring = true).assertDoesNotExist()
    }

    @Test
    fun finishingFilterShowsTreatmentsEndingThisMonth() {
        show()
        rule.onNodeWithText("Terminan este mes").performClick()
        rule.onNodeWithText("Diana", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Ana", substring = true).assertDoesNotExist()
    }

    @Test
    fun protocolFilterGroupsUnderProtocolHeaders() {
        show()
        rule.onNodeWithText("Por protocolo").performClick()
        rule.onNodeWithText("AC").assertIsDisplayed()
        rule.onNodeWithText("FOLFOX").assertIsDisplayed()
    }

    @Test
    fun allFilterKeepsTheOriginalList() {
        show()
        rule.onNodeWithText("Ana", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Diana", substring = true).assertIsDisplayed()
    }

    @Test
    fun followUpFilterListsFinishedTreatmentsWithTheirDueDate() {
        show()
        rule.onNodeWithText("Seguimiento").performClick()
        rule.onNodeWithText("Berta", substring = true).assertIsDisplayed()
        rule.onNodeWithText("revisión el", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Ana", substring = true).assertDoesNotExist()
    }

    @Test
    fun searchFindsAPatientByIdentityCard() {
        show()
        rule.onNodeWithText("Buscar", substring = true).performTextInput("8501011")
        rule.onNodeWithText("Ana", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Diana", substring = true).assertDoesNotExist()
    }
}
