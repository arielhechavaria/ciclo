package com.isora.ciclo.dailylist

import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.StoredPatient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReviewDecisionTest {
    private val withCard = StoredPatient(PatientId(1), Patient("Ana Pérez", "HC-1", identityCard = "85010112345"))
    private val withoutCard = StoredPatient(PatientId(2), Patient("María Gómez", "HC-2"))

    @Test
    fun `an existing patient by card keeps its card and gets today's session marked`() {
        val row = ReviewRow(ListEntry("Ana Pérez", "85010112345", "AC"), withCard, MatchKind.CARD, "AC")
        assertEquals(ListAction.UpdateExisting(withCard, fillCard = null, markToday = true), ReviewDecision(row).action())
    }

    @Test
    fun `today's session is left alone when the check is off`() {
        val row = ReviewRow(ListEntry("Ana Pérez", "85010112345", "AC"), withCard, MatchKind.CARD, "AC")
        assertEquals(
            ListAction.UpdateExisting(withCard, fillCard = null, markToday = false),
            ReviewDecision(row, markToday = false).action(),
        )
    }

    @Test
    fun `a confirmed probable match gets the card it was missing`() {
        val row = ReviewRow(ListEntry("Maria Gomez", "90020254321", null), withoutCard, MatchKind.NAME, null)
        assertEquals(
            ListAction.UpdateExisting(withoutCard, fillCard = "90020254321", markToday = true),
            ReviewDecision(row, samePerson = true).action(),
        )
    }

    @Test
    fun `a probable match is a new patient until the user confirms it`() {
        val entry = ListEntry("Maria Gomez", "90020254321", "FOLFOX")
        val row = ReviewRow(entry, withoutCard, MatchKind.NAME, "FOLFOX")
        assertEquals(ListAction.RegisterNew(entry, "FOLFOX"), ReviewDecision(row).action())
    }

    @Test
    fun `a new row registers the patient with the matched protocol`() {
        val entry = ListEntry("Luis Fernández", "70010111111", "Folfox")
        assertEquals(ListAction.RegisterNew(entry, "FOLFOX"), ReviewDecision(ReviewRow(entry, null, MatchKind.NEW, "FOLFOX")).action())
    }

    @Test
    fun `a row left out does nothing`() {
        val entry = ListEntry("Luis Fernández", "70010111111", null)
        assertNull(ReviewDecision(ReviewRow(entry, null, MatchKind.NEW, null), include = false).action())
    }
}
