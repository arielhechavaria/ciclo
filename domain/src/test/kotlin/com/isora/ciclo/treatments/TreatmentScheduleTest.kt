package com.isora.ciclo.treatments

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

private fun date(text: String) = LocalDate.parse(text)

private val AC = Protocol("AC", Regimen(cycleLengthDays = 21, sessionDays = listOf(1)), CycleCount.Fixed(4))
private val GEM_CIS = Protocol("Gemcitabina + cisplatino", Regimen(21, listOf(1, 8)), CycleCount.Fixed(4))
private val FOLFOX = Protocol("FOLFOX", Regimen(14, listOf(1)), CycleCount.Fixed(12))

class TreatmentScheduleTest {

    @Nested
    inner class `S7 reference example from the proposal` {
        private val treatment = Treatment.assign(AC, startDate = date("2026-11-03"))

        @Test
        fun `plans every cycle and the end date`() {
            val starts = treatment.sessions().map { it.date }.toList()

            assertEquals(listOf(date("2026-11-03"), date("2026-11-24"), date("2026-12-15"), date("2027-01-05")), starts)
            assertEquals(date("2027-01-26"), treatment.endDate)
        }

        @Test
        fun `postponing C2 by seven days recalculates everything after it`() {
            val postponed = treatment.postpone(SessionRef(cycle = 2, session = 1), date("2026-12-01"))

            val sessions = postponed.sessions().toList()
            assertEquals(
                listOf(date("2026-11-03"), date("2026-12-01"), date("2026-12-22"), date("2027-01-12")),
                sessions.map { it.date },
            )
            assertEquals(date("2026-11-24"), sessions[1].postponedFrom)
            assertTrue(sessions[1].isPostponed)
            assertFalse(sessions[2].isPostponed)
            assertEquals(date("2027-02-02"), postponed.endDate)
        }

        @Test
        fun `postponing does not change the original treatment`() {
            treatment.postpone(SessionRef(2, 1), date("2026-12-01"))

            assertEquals(date("2027-01-26"), treatment.endDate)
        }

        @Test
        fun `cycle blocks follow the postponed dates`() {
            val cycles = treatment.postpone(SessionRef(2, 1), date("2026-12-01")).cycles().toList()

            assertEquals(date("2026-11-03"), cycles[0].start)
            assertEquals(date("2026-11-24"), cycles[0].endExclusive)
            assertEquals(date("2026-12-01"), cycles[1].start)
            assertEquals(date("2026-12-22"), cycles[1].endExclusive)
        }
    }

    @Nested
    inner class `S3 cycles with several sessions` {
        private val treatment = Treatment.assign(GEM_CIS, startDate = date("2026-11-03"))

        @Test
        fun `generates every session of every cycle`() {
            val sessions = treatment.sessions().toList()

            assertEquals(8, sessions.size)
            assertEquals(
                listOf(
                    date("2026-11-03"), date("2026-11-10"),
                    date("2026-11-24"), date("2026-12-01"),
                    date("2026-12-15"), date("2026-12-22"),
                    date("2027-01-05"), date("2027-01-12"),
                ),
                sessions.map { it.date },
            )
            assertEquals(SessionRef(cycle = 1, session = 2), sessions[1].ref)
            assertEquals(4, treatment.totalCycles)
            assertEquals(2, treatment.regimen.sessionsPerCycle)
        }

        @Test
        fun `flags only the last session of the last cycle`() {
            val last = treatment.sessions().filter { it.isLast }.toList()

            assertEquals(listOf(SessionRef(4, 2)), last.map { it.ref })
        }

        @Test
        fun `postponing a D8 session shifts every later session and the end date`() {
            val postponed = treatment.postpone(SessionRef(1, 2), date("2026-11-17"))

            val dates = postponed.sessions().map { it.date }.toList()
            assertEquals(date("2026-11-17"), dates[1])
            assertEquals(date("2026-12-01"), dates[2])
            assertEquals(date("2027-01-19"), dates[7])
            assertEquals(date("2027-02-02"), postponed.endDate)
        }
    }

    @Nested
    inner class `S6 assigning a treatment` {
        @Test
        fun `uses the protocol default number of cycles`() {
            assertEquals(12, Treatment.assign(FOLFOX, date("2026-11-03")).totalCycles)
        }

        @Test
        fun `accepts a different number of cycles for this patient`() {
            val treatment = Treatment.assign(FOLFOX, date("2026-11-03"), CycleCount.Fixed(6))

            assertEquals(6, treatment.sessions().count())
            assertEquals(date("2027-01-26"), treatment.endDate)
        }

        @Test
        fun `keeps its own copy of the regimen`() {
            val treatment = Treatment.assign(AC, date("2026-11-03"))

            assertEquals(AC.regimen, treatment.regimen)
        }
    }

    @Nested
    inner class `S7 S8 postponing and marking sessions` {
        private val treatment = Treatment.assign(AC, startDate = date("2026-11-03"))

        @Test
        fun `rejects a new date that is not later than the planned one`() {
            assertThrows<IllegalArgumentException> { treatment.postpone(SessionRef(2, 1), date("2026-11-24")) }
            assertThrows<IllegalArgumentException> { treatment.postpone(SessionRef(2, 1), date("2026-11-20")) }
        }

        @Test
        fun `rejects a session that does not exist`() {
            assertThrows<IllegalArgumentException> { treatment.postpone(SessionRef(5, 1), date("2027-03-01")) }
            assertThrows<IllegalArgumentException> { treatment.postpone(SessionRef(1, 2), date("2026-12-01")) }
            assertThrows<IllegalArgumentException> { treatment.markGiven(SessionRef(0, 1)) }
        }

        @Test
        fun `marks a session as given`() {
            val sessions = treatment.markGiven(SessionRef(1, 1)).sessions().toList()

            assertTrue(sessions[0].given)
            assertFalse(sessions[1].given)
        }

        @Test
        fun `rejects postponing a session already given`() {
            val given = treatment.markGiven(SessionRef(2, 1))

            assertThrows<IllegalArgumentException> { given.postpone(SessionRef(2, 1), date("2026-12-01")) }
        }

        @Test
        fun `postponing the same session again replaces the previous new date`() {
            val twice = treatment
                .postpone(SessionRef(2, 1), date("2026-12-01"))
                .postpone(SessionRef(2, 1), date("2026-11-27"))

            val c2 = twice.sessions().toList()[1]
            assertEquals(date("2026-11-27"), c2.date)
            assertEquals(date("2026-11-24"), c2.postponedFrom)
            assertEquals(date("2027-01-29"), twice.endDate)
        }

        @Test
        fun `postponing an earlier session afterwards still moves a later postponed session`() {
            val treatment = treatment
                .postpone(SessionRef(3, 1), date("2026-12-20"))
                .postpone(SessionRef(2, 1), date("2026-12-01"))

            val c3 = treatment.sessions().toList()[2]
            assertEquals(date("2026-12-27"), c3.date)
            assertEquals(date("2026-12-22"), c3.postponedFrom)
            assertEquals(date("2027-02-07"), treatment.endDate)
        }
    }

    @Nested
    inner class `S9 end of treatment` {
        private val treatment = Treatment.assign(AC, startDate = date("2026-11-03"))

        @Test
        fun `the treatment finishes when the last cycle ends, not at the last session`() {
            assertFalse(treatment.isFinishedOn(date("2027-01-05")))
            assertFalse(treatment.isFinishedOn(date("2027-01-25")))
            assertTrue(treatment.isFinishedOn(date("2027-01-26")))
        }

        @Test
        fun `finishing early ends the treatment on that date and drops later sessions`() {
            val finished = treatment.finishEarly(date("2026-12-10"), reason = "Toxicidad")

            assertEquals(date("2026-12-10"), finished.endDate)
            assertEquals(listOf(date("2026-11-03"), date("2026-11-24")), finished.sessions().map { it.date }.toList())
            assertFalse(finished.isFinishedOn(date("2026-12-09")))
            assertTrue(finished.isFinishedOn(date("2026-12-10")))
            assertEquals("Toxicidad", finished.earlyFinish?.reason)
        }

        @Test
        fun `finishing early needs a reason and a date inside the treatment`() {
            assertThrows<IllegalArgumentException> { treatment.finishEarly(date("2026-12-10"), reason = " ") }
            assertThrows<IllegalArgumentException> { treatment.finishEarly(date("2026-11-02"), reason = "Error") }
        }

        @Test
        fun `a finished treatment cannot be postponed`() {
            val finished = treatment.finishEarly(date("2026-12-10"), reason = "Toxicidad")

            assertThrows<IllegalArgumentException> { finished.postpone(SessionRef(2, 1), date("2026-12-01")) }
        }
    }

    @Nested
    inner class `S10 until progression` {
        private val treatment = Treatment.assign(
            AC,
            startDate = date("2026-11-03"),
            cycleCount = CycleCount.UntilProgression(reevaluateEveryCycles = 3),
        )

        @Test
        fun `has no end date and never finishes on its own`() {
            assertNull(treatment.endDate)
            assertNull(treatment.totalCycles)
            assertFalse(treatment.isFinishedOn(date("2030-01-01")))
            assertTrue(treatment.sessions().take(20).none { it.isLast })
        }

        @Test
        fun `keeps generating cycles`() {
            assertEquals(date("2027-01-26"), treatment.sessions().elementAt(4).date)
        }

        @Test
        fun `asks for reevaluation every chosen number of cycles`() {
            assertEquals(listOf(date("2027-01-05"), date("2027-03-09")), treatment.reevaluationDates().take(2).toList())
        }

        @Test
        fun `fixed treatments have no reevaluation dates`() {
            assertTrue(Treatment.assign(AC, date("2026-11-03")).reevaluationDates().none())
        }

        @Test
        fun `finishing early stops the cycles`() {
            val finished = treatment.finishEarly(date("2026-12-10"), reason = "Progresión")

            assertEquals(2, finished.sessions().count())
            assertEquals(date("2026-12-10"), finished.endDate)
            assertTrue(finished.reevaluationDates().none())
        }
    }
}
