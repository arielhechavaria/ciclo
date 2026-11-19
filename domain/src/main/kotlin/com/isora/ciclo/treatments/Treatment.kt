package com.isora.ciclo.treatments

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Identifies a session by its cycle number and its position inside the cycle, both 1-based. */
data class SessionRef(val cycle: Int, val session: Int)

/**
 * A session placed on the calendar. [postponedFrom] holds the date it had before being postponed;
 * [isLast] marks the last session of a treatment with a fixed number of cycles.
 */
data class ScheduledSession(
    val ref: SessionRef,
    val date: LocalDate,
    val postponedFrom: LocalDate?,
    val given: Boolean,
    val isLast: Boolean,
) {
    val isPostponed: Boolean get() = postponedFrom != null
}

/** A cycle block on the calendar, from its first day to the day the next cycle would start. */
data class ScheduledCycle(
    val number: Int,
    val start: LocalDate,
    val endExclusive: LocalDate,
    val sessions: List<ScheduledSession>,
)

data class EarlyFinish(val date: LocalDate, val reason: String)

/**
 * A patient's treatment. It keeps its own copy of the protocol's [regimen], so later edits to the
 * catalog never change it. Every change returns a new instance.
 *
 * Postponing a session moves it to the new date and shifts every later session, and the end date,
 * by the same number of days. A postponement is stored as a delay in days, so postponing an earlier
 * session later on still moves every later session. A treatment ends the day after its last cycle
 * ends, not at its last session.
 */
class Treatment(
    val regimen: Regimen,
    val cycleCount: CycleCount,
    val startDate: LocalDate,
    /** Delay in days of each postponed session, measured from the date it had when it was postponed. */
    val postponements: Map<SessionRef, Long> = emptyMap(),
    val givenSessions: Set<SessionRef> = emptySet(),
    val earlyFinish: EarlyFinish? = null,
    /** The day the post-treatment follow-up review was done; null while pending. */
    val followUpDoneOn: LocalDate? = null,
) {
    val totalCycles: Int? get() = (cycleCount as? CycleCount.Fixed)?.cycles

    /** The day the patient leaves the active views; null while an until-progression treatment goes on. */
    val endDate: LocalDate?
        get() {
            val natural = totalCycles?.let { generateCycles().last().endExclusive }
            val early = earlyFinish?.date
            return listOfNotNull(natural, early).minOrNull()
        }

    /** Cycles in order. Infinite for an until-progression treatment that has not been finished early. */
    fun cycles(): Sequence<ScheduledCycle> {
        val finish = earlyFinish ?: return generateCycles()
        return generateCycles()
            .takeWhile { it.start < finish.date }
            .map { cycle -> cycle.copy(sessions = cycle.sessions.filter { it.date < finish.date }) }
    }

    fun sessions(): Sequence<ScheduledSession> = cycles().flatMap { it.sessions }

    fun postpone(ref: SessionRef, newDate: LocalDate): Treatment {
        require(earlyFinish == null) { "The treatment is finished" }
        val session = findSession(ref)
        require(!session.given) { "A session already given cannot be postponed" }
        val planned = session.postponedFrom ?: session.date
        require(newDate.isAfter(planned)) { "The new date must be later than the planned date $planned" }
        return copy(postponements = postponements + (ref to ChronoUnit.DAYS.between(planned, newDate)))
    }

    fun markGiven(ref: SessionRef): Treatment {
        findSession(ref)
        return copy(givenSessions = givenSessions + ref)
    }

    fun finishEarly(date: LocalDate, reason: String): Treatment {
        require(reason.isNotBlank()) { "Finishing early needs a reason" }
        require(!date.isBefore(startDate)) { "The finish date cannot be before the start date" }
        return copy(earlyFinish = EarlyFinish(date, reason.trim()))
    }

    /** When the follow-up review is due: [months] after the end date; null while the treatment has no end. */
    fun followUpDate(months: Int): LocalDate? = endDate?.plusMonths(months.toLong())

    fun markFollowUpDone(date: LocalDate): Treatment = copy(followUpDoneOn = date)

    fun isFinishedOn(date: LocalDate): Boolean = endDate?.let { !date.isBefore(it) } ?: false

    /** Start dates of the cycles that follow every N completed cycles of an until-progression treatment. */
    fun reevaluationDates(): Sequence<LocalDate> {
        val every = (cycleCount as? CycleCount.UntilProgression)?.reevaluateEveryCycles ?: return emptySequence()
        return cycles().filter { it.number > 1 && (it.number - 1) % every == 0 }.map { it.start }
    }

    private fun findSession(ref: SessionRef): ScheduledSession {
        val cycleExists = ref.cycle >= 1 && (totalCycles == null || ref.cycle <= totalCycles!!)
        require(cycleExists && ref.session in 1..regimen.sessionsPerCycle) { "Session $ref does not exist" }
        return generateCycles().first { it.number == ref.cycle }.sessions[ref.session - 1]
    }

    /** Walks the calendar applying postponements in order; each one shifts everything after it. */
    private fun generateCycles(): Sequence<ScheduledCycle> = sequence {
        val total = totalCycles
        var shiftDays = 0L
        var number = 1
        while (total == null || number <= total) {
            val baseStart = startDate.plusDays((number - 1L) * regimen.cycleLengthDays)
            val sessions = regimen.sessionDays.mapIndexed { index, day ->
                val ref = SessionRef(number, index + 1)
                val expected = baseStart.plusDays(day - 1L + shiftDays)
                val delay = postponements[ref]
                if (delay != null) shiftDays += delay
                ScheduledSession(
                    ref = ref,
                    date = if (delay != null) expected.plusDays(delay) else expected,
                    postponedFrom = if (delay != null) expected else null,
                    given = ref in givenSessions,
                    isLast = number == total && index == regimen.sessionDays.lastIndex,
                )
            }
            val start = sessions.first().date.minusDays(regimen.sessionDays.first() - 1L)
            val endExclusive = baseStart.plusDays(regimen.cycleLengthDays + shiftDays)
            yield(ScheduledCycle(number, start, endExclusive, sessions))
            number++
        }
    }

    private fun copy(
        postponements: Map<SessionRef, Long> = this.postponements,
        givenSessions: Set<SessionRef> = this.givenSessions,
        earlyFinish: EarlyFinish? = this.earlyFinish,
        followUpDoneOn: LocalDate? = this.followUpDoneOn,
    ) = Treatment(regimen, cycleCount, startDate, postponements, givenSessions, earlyFinish, followUpDoneOn)

    companion object {
        fun assign(
            protocol: Protocol,
            startDate: LocalDate,
            cycleCount: CycleCount = protocol.defaultCycles,
        ): Treatment = Treatment(protocol.regimen, cycleCount, startDate)
    }
}
