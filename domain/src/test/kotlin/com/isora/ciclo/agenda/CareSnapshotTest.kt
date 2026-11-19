package com.isora.ciclo.agenda

import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import com.isora.ciclo.treatments.SessionRef
import com.isora.ciclo.treatments.StoredTreatment
import com.isora.ciclo.treatments.Treatment
import com.isora.ciclo.treatments.TreatmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CareSnapshotTest {
    private val ac = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))
    private val patients = listOf(
        StoredPatient(PatientId(1), Patient("Ana", "HC-1")),
        StoredPatient(PatientId(2), Patient("Berta", "HC-2")),
        StoredPatient(PatientId(3), Patient("Carla", "HC-3")),
    )
    private val treatments = listOf(
        StoredTreatment(TreatmentId(1), PatientId(1), "AC", Treatment.assign(ac, LocalDate.of(2026, 11, 3))),
        StoredTreatment(TreatmentId(2), PatientId(2), "AC", Treatment.assign(ac, LocalDate.of(2026, 1, 5))),
    )
    private val snapshot = CareSnapshot(patients, treatments)
    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun splitsActiveHistoryAndPatientsWithoutTreatment() {
        assertTrue(snapshot.hasActiveTreatment(PatientId(1), today))
        assertTrue(snapshot.isHistory(PatientId(2), today))
        assertFalse(snapshot.isHistory(PatientId(3), today))
        assertFalse(snapshot.hasActiveTreatment(PatientId(3), today))
        assertEquals(listOf("Ana"), snapshot.active(today).map { it.name })
    }

    @Test
    fun sessionsOnADayListTheSessionsFallingOnIt() {
        assertEquals(listOf("Ana"), snapshot.sessionsOn(LocalDate.of(2026, 11, 24)).map { it.entry.name })
        assertTrue(snapshot.sessionsOn(LocalDate.of(2026, 11, 25)).isEmpty())
    }

    @Test
    fun endDateIsTheDayAfterTheLastCycle() {
        assertEquals(listOf("Ana"), snapshot.finishingBetween(LocalDate.of(2027, 1, 26), LocalDate.of(2027, 1, 27)).map { it.name })
    }

    private val folfox = Protocol("FOLFOX", Regimen(14, listOf(1)), CycleCount.Fixed(1))
    private val listed = CareSnapshot(
        patients + StoredPatient(PatientId(4), Patient("Diana", "HC-4")),
        treatments.map {
            if (it.patientId == PatientId(1)) {
                StoredTreatment(it.id, it.patientId, it.protocolName, it.treatment.postpone(SessionRef(1, 1), LocalDate.of(2026, 11, 5)))
            } else {
                it
            }
        } + StoredTreatment(TreatmentId(3), PatientId(4), "FOLFOX", Treatment.assign(folfox, LocalDate.of(2026, 11, 10))),
    )

    @Test
    fun groupsActiveTreatmentsByProtocolAndLeavesOutFinishedOnes() {
        val grouped = listed.activeByProtocol(today)
        assertEquals(listOf("AC", "FOLFOX"), grouped.keys.toList())
        assertEquals(listOf("Ana"), grouped.getValue("AC").map { it.name })
        assertEquals(listOf("Diana"), grouped.getValue("FOLFOX").map { it.name })
    }

    @Test
    fun postponedListsActiveTreatmentsWithAPostponedSessionNotYetGiven() {
        assertEquals(listOf("Ana"), listed.postponed(today).map { it.name })
        assertTrue(snapshot.postponed(today).isEmpty())
    }

    @Test
    fun finishingThisMonthListsTreatmentsEndingFromTodayToMonthEnd() {
        assertEquals(listOf("Diana"), listed.finishingThisMonth(today).map { it.name })
        assertTrue(listed.finishingThisMonth(LocalDate.of(2026, 11, 25)).isEmpty())
    }
}
