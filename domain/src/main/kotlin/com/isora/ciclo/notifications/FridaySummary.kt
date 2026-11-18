package com.isora.ciclo.notifications

import com.isora.ciclo.agenda.AgendaEntry
import com.isora.ciclo.agenda.AgendaSession
import com.isora.ciclo.agenda.CareSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** What the Friday notification says about the following Monday-to-Sunday week. */
data class FridaySummary(
    val weekStart: LocalDate,
    val sessionsByDay: Map<LocalDate, List<AgendaSession>>,
    val finishing: List<AgendaEntry>,
    val postponed: List<AgendaSession>,
    val reevaluate: List<AgendaEntry>,
) {
    val isEmpty: Boolean
        get() = sessionsByDay.isEmpty() && finishing.isEmpty() && postponed.isEmpty() && reevaluate.isEmpty()
}

/** Builds the summary of the week that starts on the Monday after [friday]. */
fun buildFridaySummary(snapshot: CareSnapshot, friday: LocalDate): FridaySummary {
    val start = friday.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    val end = start.plusDays(7)
    val sessions = snapshot.sessionsBetween(start, end)
    return FridaySummary(
        weekStart = start,
        sessionsByDay = sessions.groupBy { it.session.date },
        finishing = snapshot.finishingBetween(start, end),
        postponed = sessions.filter { it.session.isPostponed },
        reevaluate = snapshot.reevaluationBetween(start, end),
    )
}

/** The next Friday at [time] strictly after [now]. */
fun nextFriday(now: LocalDateTime, time: LocalTime): LocalDateTime {
    val thisFriday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)).atTime(time)
    return if (thisFriday.isAfter(now)) thisFriday else thisFriday.plusWeeks(1)
}
