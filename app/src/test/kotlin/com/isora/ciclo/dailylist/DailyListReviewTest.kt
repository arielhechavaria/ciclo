package com.isora.ciclo.dailylist

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import com.isora.ciclo.treatments.StoredTreatment
import com.isora.ciclo.treatments.Treatment
import com.isora.ciclo.treatments.TreatmentId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

// A tall screen so the lazy list composes every row.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h2000dp")
class DailyListReviewTest {
    @get:Rule
    val rule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 19)
    private val ac = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))
    private val ana = StoredPatient(PatientId(1), Patient("Ana Pérez", "HC-1", identityCard = "85010112345"))
    private val maria = StoredPatient(PatientId(2), Patient("María Gómez", "HC-2"))
    // Ana's first cycle was given three weeks ago; the second starts today.
    private val anaTreatment = Treatment.assign(ac, today.minusDays(21)).let { it.markGiven(it.sessions().first().ref) }
    private val snapshot = CareSnapshot(listOf(ana, maria), listOf(StoredTreatment(TreatmentId(1), ana.id, "AC", anaTreatment)))

    private val byCard = ReviewRow(ListEntry("Ana Pérez", "85010112345", "AC"), ana, MatchKind.CARD, "AC")
    private val byName = ReviewRow(ListEntry("Maria Gomez", "90020254321", null), maria, MatchKind.NAME, null)
    private val newOne = ReviewRow(ListEntry("Rosa Díaz", "66050544332", "XELOX"), null, MatchKind.NEW, null)

    private var confirmed: List<ListAction>? = null

    private fun show() = rule.setContent {
        MaterialTheme {
            DailyListReview(
                rows = listOf(byCard, byName, newOne),
                unread = listOf("Consulta de oncología"),
                snapshot = snapshot,
                today = today,
                onConfirm = { confirmed = it },
                onCancel = {},
            )
        }
    }

    @Test
    fun showsWhatWasReadAndHowEachRowMatches() {
        show()
        rule.onNodeWithText("Ya registrada · AC · Ciclo 2 de 4, sesión 1 de 1").assertIsDisplayed()
        rule.onNodeWithText("¿Es María Gómez (HC-2)?").assertIsDisplayed()
        rule.onNodeWithText("Nueva · completar datos").assertIsDisplayed()
        rule.onNodeWithText("«XELOX» no está en el catálogo", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Consulta de oncología").assertIsDisplayed()
    }

    @Test
    fun confirmingWithTheDefaultsMarksTodayAndRegistersTheRest() {
        show()
        rule.onNodeWithText("Confirmar").performClick()
        assertEquals(
            listOf(
                ListAction.UpdateExisting(ana, fillCard = null, markToday = true),
                ListAction.RegisterNew(byName.entry, null),
                ListAction.RegisterNew(newOne.entry, null),
            ),
            confirmed,
        )
    }

    @Test
    fun theUserCanConfirmAProbableMatchUntickTodayAndLeaveARowOut() {
        show()
        rule.onNodeWithText("Sesión de hoy dada").performClick()
        rule.onNodeWithText("Es la misma persona").performClick()
        // The third "Incluir" belongs to the new patient; the list keeps rows in order.
        rule.onAllNodesWithText("Incluir")[2].performClick()
        rule.onNodeWithText("Confirmar").performClick()
        assertEquals(
            listOf(
                ListAction.UpdateExisting(ana, fillCard = null, markToday = false),
                ListAction.UpdateExisting(maria, fillCard = "90020254321", markToday = true),
            ),
            confirmed,
        )
    }
}
