package com.isora.ciclo.treatments

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import java.time.LocalDate

/** Identifies a session by its cycle number and its position inside the cycle, both 1-based. */
data class SessionRef(val cycle: Int, val session: Int)

data class ScheduledSession(
    val ref: SessionRef,
    val date: LocalDate,
    val postponedFrom: LocalDate?,
    val given: Boolean,
    val isLast: Boolean,
) {
    val isPostponed: Boolean get() = postponedFrom != null
}

data class ScheduledCycle(
    val number: Int,
    val start: LocalDate,
    val endExclusive: LocalDate,
    val sessions: List<ScheduledSession>,
)

data class EarlyFinish(val date: LocalDate, val reason: String)

class Treatment(
    val regimen: Regimen,
    val cycleCount: CycleCount,
    val startDate: LocalDate,
    val postponements: Map<SessionRef, LocalDate> = emptyMap(),
    val givenSessions: Set<SessionRef> = emptySet(),
    val earlyFinish: EarlyFinish? = null,
) {
    val totalCycles: Int? get() = TODO()

    val endDate: LocalDate? get() = TODO()

    fun cycles(): Sequence<ScheduledCycle> = TODO()

    fun sessions(): Sequence<ScheduledSession> = TODO()

    fun postpone(ref: SessionRef, newDate: LocalDate): Treatment = TODO()

    fun markGiven(ref: SessionRef): Treatment = TODO()

    fun finishEarly(date: LocalDate, reason: String): Treatment = TODO()

    fun isFinishedOn(date: LocalDate): Boolean = TODO()

    fun reevaluationDates(): Sequence<LocalDate> = TODO()

    companion object {
        fun assign(
            protocol: Protocol,
            startDate: LocalDate,
            cycleCount: CycleCount = protocol.defaultCycles,
        ): Treatment = TODO()
    }
}
