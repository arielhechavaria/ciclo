package com.isora.ciclo.agenda

import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.treatments.ScheduledCycle
import com.isora.ciclo.treatments.ScheduledSession
import com.isora.ciclo.treatments.StoredTreatment
import java.time.DayOfWeek
import java.time.LocalDate

/** A treatment together with the patient who owns it. */
data class AgendaEntry(val patient: StoredPatient, val treatment: StoredTreatment) {
    val name: String get() = patient.patient.name
}

data class AgendaSession(val entry: AgendaEntry, val session: ScheduledSession)

/** Everything the views need: all patients and all their treatments. */
class CareSnapshot(val patients: List<StoredPatient>, val treatments: List<StoredTreatment>) {
    val entries: List<AgendaEntry> = treatments.mapNotNull { stored ->
        patients.firstOrNull { it.id == stored.patientId }?.let { AgendaEntry(it, stored) }
    }

    fun treatmentsOf(patientId: PatientId): List<StoredTreatment> = treatments.filter { it.patientId == patientId }

    /** Treatments still running on [today]. */
    fun active(today: LocalDate): List<AgendaEntry> = entries.filter { !it.treatment.treatment.isFinishedOn(today) }

    fun hasActiveTreatment(patientId: PatientId, today: LocalDate): Boolean =
        treatmentsOf(patientId).any { !it.treatment.isFinishedOn(today) }

    /** Patients who had treatments and have none running; they live in the history section. */
    fun isHistory(patientId: PatientId, today: LocalDate): Boolean {
        val own = treatmentsOf(patientId)
        return own.isNotEmpty() && own.all { it.treatment.isFinishedOn(today) }
    }

    /** Sessions in [from, toExclusive), ordered by date and patient name. */
    fun sessionsBetween(from: LocalDate, toExclusive: LocalDate): List<AgendaSession> =
        entries.flatMap { entry ->
            entry.treatment.treatment.sessions()
                .dropWhile { it.date < from }
                .takeWhile { it.date < toExclusive }
                .map { AgendaSession(entry, it) }
                .toList()
        }.sortedWith(compareBy({ it.session.date }, { it.entry.name.lowercase() }))

    fun sessionsOn(date: LocalDate): List<AgendaSession> = sessionsBetween(date, date.plusDays(1))

    /** Entries whose treatment ends in [from, toExclusive). */
    fun finishingBetween(from: LocalDate, toExclusive: LocalDate): List<AgendaEntry> =
        entries.filter { entry -> entry.treatment.treatment.endDate?.let { it >= from && it < toExclusive } == true }
            .sortedBy { it.name.lowercase() }

    /** Entries of until-progression treatments whose reevaluation falls in [from, toExclusive). */
    fun reevaluationBetween(from: LocalDate, toExclusive: LocalDate): List<AgendaEntry> =
        entries.filter { entry ->
            entry.treatment.treatment.reevaluationDates()
                .dropWhile { it < from }
                .firstOrNull()
                ?.let { it < toExclusive } == true
        }.sortedBy { it.name.lowercase() }
}

/** Cycles of a treatment overlapping [from, toExclusive). */
fun cyclesOverlapping(entry: AgendaEntry, from: LocalDate, toExclusive: LocalDate): List<ScheduledCycle> =
    entry.treatment.treatment.cycles()
        .dropWhile { it.endExclusive <= from }
        .takeWhile { it.start < toExclusive }
        .toList()

fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)
