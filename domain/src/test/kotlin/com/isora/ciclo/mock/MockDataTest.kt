package com.isora.ciclo.mock

import com.isora.ciclo.agenda.CareSnapshot
import com.isora.ciclo.dailylist.DailyListMatcher
import com.isora.ciclo.dailylist.DailyListReader
import com.isora.ciclo.dailylist.MatchKind
import com.isora.ciclo.protocols.ProtocolSeed
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.treatments.StoredTreatment
import com.isora.ciclo.treatments.TreatmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MockDataTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val cases = MockData.build(today)

    private fun snapshot(): CareSnapshot {
        val patients = cases.mapIndexed { i, c -> StoredPatient(PatientId(i + 1L), c.patient) }
        val treatments = cases.mapIndexedNotNull { i, c ->
            c.treatment?.let { StoredTreatment(TreatmentId(i + 1L), PatientId(i + 1L), c.protocolName!!, it) }
        }
        return CareSnapshot(patients, treatments)
    }

    @Test
    fun `clinical record numbers are unique`() {
        assertEquals(cases.size, cases.map { it.patient.clinicalRecordNumber }.toSet().size)
    }

    @Test
    fun `covers active, history and patients without treatment`() {
        val s = snapshot()
        val ids = s.patients.map { it.id }
        assertTrue(ids.any { s.isHistory(it, today) }, "history")
        assertTrue(ids.any { s.treatmentsOf(it).isEmpty() }, "no treatment")
        assertTrue(s.active(today).size >= 10, "active")
    }

    @Test
    fun `has sessions today, a busy day, postponements, a last session and a reevaluation`() {
        val s = snapshot()
        assertTrue(s.sessionsOn(today).size >= 2, "sessions today")
        val busy = s.sessionsBetween(today, today.plusDays(14)).groupBy { it.session.date }.values.maxOf { it.size }
        assertTrue(busy >= 4, "busiest day has $busy")
        assertTrue(s.sessionsBetween(today.minusDays(60), today.plusDays(60)).any { it.session.isPostponed }, "postponed")
        assertTrue(s.sessionsOn(today).any { it.session.isLast }, "last session today")
        assertTrue(s.reevaluationBetween(today, today.plusDays(1)).isNotEmpty(), "reevaluation today")
    }

    @Test
    fun `covers a treatment finished early and one that is open ended`() {
        val treatments = cases.mapNotNull { it.treatment }
        assertEquals("Toxicidad hematológica", treatments.mapNotNull { it.earlyFinish }.single().reason)
        assertTrue(treatments.any { it.endDate == null })
    }

    @Test
    fun `every session before today is marked given and none from today on`() {
        cases.mapNotNull { it.treatment }.forEach { t ->
            t.sessions().takeWhile { it.date < today.plusDays(30) }.forEach { assertEquals(it.date < today, it.given) }
        }
    }

    @Test
    fun `identity cards are unique`() {
        val cards = cases.mapNotNull { it.patient.identityCard }
        assertEquals(cards.size, cards.toSet().size)
    }

    @Test
    fun `the sample daily list exercises a card match, a name match, a new patient and an unknown treatment`() {
        val reading = DailyListReader.read(MockData.sampleList())
        val rows = DailyListMatcher.match(reading.entries, snapshot().patients, ProtocolSeed.protocols.map { it.name })

        assertEquals(listOf(MatchKind.CARD, MatchKind.NAME, MatchKind.NEW, MatchKind.NEW), rows.map { it.kind })
        assertEquals(listOf("M.R.G.", "S.E.L.", null, null), rows.map { it.patient?.patient?.name })
        assertEquals(listOf("AC", "Gemcitabina + cisplatino", "FOLFOX", null), rows.map { it.protocolName })
        assertEquals(listOf("Consulta de oncología", "Nombre y apellidos Carnet Tratamiento"), reading.unread)
    }
}
