package com.isora.ciclo.dailylist

import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.RoomPatientRepository
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import com.isora.ciclo.storage.StorageTestBase
import com.isora.ciclo.treatments.RoomTreatmentRepository
import com.isora.ciclo.treatments.Treatment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyListApplierTest : StorageTestBase() {
    private val patients by lazy { RoomPatientRepository(db.patientDao()) }
    private val treatments by lazy { RoomTreatmentRepository(db.treatmentDao()) }
    private val applier by lazy { DailyListApplier(patients, treatments) }

    private val today = LocalDate.of(2026, 11, 19)
    private val ac = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))
    private val untilProgression = Protocol("Trastuzumab", Regimen(21, listOf(1)), CycleCount.UntilProgression(3))

    private fun givenToday(patient: StoredPatient): Boolean =
        treatments.forPatient(patient.id).single().treatment.sessions()
            .takeWhile { it.date <= today }.first { it.date == today }.given

    @Test
    fun `marks the session planned for today as given`() {
        val ana = patients.add(Patient("Ana", "HC-1"))
        // Started three weeks ago: cycle 2 starts today.
        treatments.assign(ana.id, "AC", Treatment.assign(ac, today.minusDays(21)))

        val refused = applier.apply(listOf(ListAction.UpdateExisting(ana, fillCard = null, markToday = true)), today)

        assertEquals(emptyList<ListAction.UpdateExisting>(), refused)
        assertTrue(givenToday(ana))
    }

    @Test
    fun `leaves today's session alone when not asked`() {
        val ana = patients.add(Patient("Ana", "HC-1"))
        treatments.assign(ana.id, "AC", Treatment.assign(ac, today.minusDays(21)))

        applier.apply(listOf(ListAction.UpdateExisting(ana, fillCard = null, markToday = false)), today)

        assertFalse(givenToday(ana))
    }

    @Test
    fun `works with an until-progression treatment`() {
        val ana = patients.add(Patient("Ana", "HC-1"))
        treatments.assign(ana.id, "Trastuzumab", Treatment.assign(untilProgression, today.minusDays(42)))

        applier.apply(listOf(ListAction.UpdateExisting(ana, fillCard = null, markToday = true)), today)

        assertTrue(givenToday(ana))
    }

    @Test
    fun `fills a missing identity card and keeps every other field`() {
        val maria = patients.add(Patient("María", "HC-2", diagnosis = "Colon", notes = "nota"))

        applier.apply(listOf(ListAction.UpdateExisting(maria, fillCard = "90020254321", markToday = true)), today)

        assertEquals(Patient("María", "HC-2", "Colon", "nota", "90020254321"), patients.findById(maria.id)?.patient)
    }

    @Test
    fun `a card held by another patient is reported and not saved`() {
        patients.add(Patient("Ana", "HC-1", identityCard = "90020254321"))
        val maria = patients.add(Patient("María", "HC-2"))
        val action = ListAction.UpdateExisting(maria, fillCard = "90020254321", markToday = true)

        assertEquals(listOf(action), applier.apply(listOf(action), today))
        assertNull(patients.findById(maria.id)?.patient?.identityCard)
    }
}
