package com.isora.ciclo.dailylist

import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.StoredPatient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DailyListMatcherTest {
    private val protocols = listOf("AC", "AC dosis densa", "FOLFOX", "Carboplatino + paclitaxel", "Paclitaxel semanal")
    private val ana = StoredPatient(PatientId(1), Patient("Ana Pérez", "HC-1", identityCard = "85010112345"))
    private val maria = StoredPatient(PatientId(2), Patient("María Gómez", "HC-2"))
    private val patients = listOf(ana, maria)

    private fun matchOne(entry: ListEntry, among: List<StoredPatient> = patients) =
        DailyListMatcher.match(listOf(entry), among, protocols).single()

    @Test
    fun `a row matches the patient with the same identity card`() {
        val row = matchOne(ListEntry("A. Perez", "85010112345", "AC"))
        assertEquals(MatchKind.CARD, row.kind)
        assertEquals(ana, row.patient)
    }

    @Test
    fun `without a card match, the same name ignoring case, accents and spaces is a probable match`() {
        val row = matchOne(ListEntry("MARIA  GOMEZ", "90020254321", null))
        assertEquals(MatchKind.NAME, row.kind)
        assertEquals(maria, row.patient)
    }

    @Test
    fun `the same name with a different card on file is a new patient`() {
        val row = matchOne(ListEntry("Ana Perez", "90020254321", null))
        assertEquals(MatchKind.NEW, row.kind)
        assertNull(row.patient)
    }

    @Test
    fun `a name shared by two patients is not guessed`() {
        val twin = StoredPatient(PatientId(3), Patient("Maria Gomez", "HC-3"))
        val row = matchOne(ListEntry("María Gómez", "90020254321", null), patients + twin)
        assertEquals(MatchKind.NEW, row.kind)
    }

    @Test
    fun `an unknown name and card is a new patient`() {
        assertEquals(MatchKind.NEW, matchOne(ListEntry("Luis Fernández", "70010111111", "FOLFOX")).kind)
    }

    @Test
    fun `the treatment text is matched to a catalog protocol ignoring case, accents and spacing`() {
        assertEquals("AC dosis densa", matchProtocol("ac  DOSIS densa", protocols))
        assertEquals("Carboplatino + paclitaxel", matchProtocol("Carboplatino+Paclitaxel", protocols))
        assertEquals("FOLFOX", matchProtocol("folfox", protocols))
    }

    @Test
    fun `extra words around a protocol name still find it, preferring the longest name`() {
        assertEquals("AC", matchProtocol("AC ciclo 2", protocols))
        assertEquals("AC dosis densa", matchProtocol("AC dosis densa C3", protocols))
    }

    @Test
    fun `a treatment that is not in the catalog has no protocol`() {
        assertNull(matchProtocol("XELOX", protocols))
        assertNull(matchProtocol("Paclitaxel", protocols))
        assertEquals(null, matchOne(ListEntry("Luis", "70010111111", "XELOX")).protocolName)
    }

    @Test
    fun `the matched protocol is part of the row`() {
        assertEquals("FOLFOX", matchOne(ListEntry("Luis", "70010111111", "Folfox")).protocolName)
    }
}
