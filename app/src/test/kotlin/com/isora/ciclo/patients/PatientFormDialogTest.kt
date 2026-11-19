package com.isora.ciclo.patients

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PatientFormDialogTest {
    @get:Rule
    val rule = createComposeRule()

    private var saved: Patient? = null

    private fun show(initial: Patient? = null, conflict: PatientConflict? = null) = rule.setContent {
        MaterialTheme {
            PatientFormDialog(
                initial = initial,
                onSave = { patient, done -> saved = patient; done(conflict) },
                onCancel = {},
            )
        }
    }

    private fun fill(card: String) {
        rule.onNodeWithText("Nombre", substring = true).performTextInput("Ana")
        rule.onNodeWithText("historia clínica", substring = true).performTextInput("HC-1")
        rule.onNodeWithText("Carnet de identidad", substring = true).performTextInput(card)
        rule.onNodeWithText("Guardar").performClick()
    }

    @Test
    fun savesTheIdentityCard() {
        show()
        fill("850101 12345")
        assertEquals("85010112345", saved?.identityCard)
    }

    @Test
    fun aMalformedIdentityCardIsNotSaved() {
        show()
        fill("1234")
        assertNull(saved)
        rule.onNodeWithText("El carnet tiene 11 dígitos").assertIsDisplayed()
    }

    @Test
    fun anIdentityCardOfAnotherPatientIsReported() {
        show(conflict = PatientConflict.IDENTITY_CARD)
        fill("85010112345")
        rule.onNodeWithText("Ese carnet ya pertenece a otro paciente").assertIsDisplayed()
    }

    @Test
    fun aRecordNumberOfAnotherPatientIsStillReported() {
        show(conflict = PatientConflict.RECORD)
        fill("")
        rule.onNodeWithText("Ese nº de historia ya pertenece a otro paciente").assertIsDisplayed()
    }
}
