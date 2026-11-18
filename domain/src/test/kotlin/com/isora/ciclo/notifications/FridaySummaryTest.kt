package com.isora.ciclo.notifications

import com.isora.ciclo.agenda.CareSnapshot
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class FridaySummaryTest {
    private val ac = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))

    private fun snapshot(vararg treatments: Pair<String, Treatment>): CareSnapshot {
        val patients = treatments.mapIndexed { i, (name, _) ->
            StoredPatient(PatientId(i + 1L), Patient(name, "HC-$i"))
        }
        val stored = treatments.mapIndexed { i, (_, t) ->
            StoredTreatment(TreatmentId(i + 1L), PatientId(i + 1L), "AC", t)
        }
        return CareSnapshot(patients, stored)
    }

    @Test
    fun listsNextWeekSessionsByDayStartingTheMondayAfterFriday() {
        // C1 starts Tue 2026-11-03, C2 on Tue 2026-11-24.
        val snap = snapshot("Ana" to Treatment.assign(ac, LocalDate.of(2026, 11, 3)))
        val summary = buildFridaySummary(snap, LocalDate.of(2026, 11, 20))
        assertEquals(LocalDate.of(2026, 11, 23), summary.weekStart)
        assertEquals(listOf(LocalDate.of(2026, 11, 24)), summary.sessionsByDay.keys.toList())
    }

    @Test
    fun flagsPostponedSessionsAndPatientsFinishingNextWeek() {
        val postponed = Treatment.assign(ac, LocalDate.of(2026, 11, 3))
            .postpone(SessionRef(2, 1), LocalDate.of(2026, 11, 26))
        val finishing = Treatment.assign(ac, LocalDate.of(2026, 9, 1)) // ends 2026-11-24
        val summary = buildFridaySummary(snapshot("Ana" to postponed, "Berta" to finishing), LocalDate.of(2026, 11, 20))
        assertEquals(listOf("Ana"), summary.postponed.map { it.entry.name })
        assertEquals(listOf("Berta"), summary.finishing.map { it.name })
    }

    @Test
    fun emptyWeekIsEmpty() {
        val summary = buildFridaySummary(snapshot(), LocalDate.of(2026, 11, 20))
        assertTrue(summary.isEmpty)
    }

    @Test
    fun reportsUntilProgressionPatientsDueForReevaluation() {
        val protocol = Protocol("Trastuzumab", Regimen(21, listOf(1)), CycleCount.UntilProgression(2))
        // Cycle 3 starts 2026-12-15 (start + 42 days).
        val treatment = Treatment.assign(protocol, LocalDate.of(2026, 11, 3))
        val summary = buildFridaySummary(snapshot("Carla" to treatment), LocalDate.of(2026, 12, 11))
        assertEquals(listOf("Carla"), summary.reevaluate.map { it.name })
    }

    @Test
    fun nextFridayIsStrictlyInTheFuture() {
        val time = LocalTime.of(8, 0)
        assertEquals(LocalDateTime.of(2026, 11, 20, 8, 0), nextFriday(LocalDateTime.of(2026, 11, 18, 12, 0), time))
        assertEquals(LocalDateTime.of(2026, 11, 20, 8, 0), nextFriday(LocalDateTime.of(2026, 11, 20, 7, 59), time))
        assertEquals(LocalDateTime.of(2026, 11, 27, 8, 0), nextFriday(LocalDateTime.of(2026, 11, 20, 8, 0), time))
    }
}
