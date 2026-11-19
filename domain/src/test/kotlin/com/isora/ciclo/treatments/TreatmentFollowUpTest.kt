package com.isora.ciclo.treatments

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TreatmentFollowUpTest {
    private val ac = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))
    private val treatment = Treatment.assign(ac, LocalDate.of(2026, 11, 3))

    @Test
    fun followUpIsDueTheGivenNumberOfMonthsAfterTheEndDate() {
        // The treatment ends 2027-01-26.
        assertEquals(LocalDate.of(2027, 4, 26), treatment.followUpDate(3))
        assertEquals(LocalDate.of(2027, 7, 26), treatment.followUpDate(6))
    }

    @Test
    fun followUpFollowsAnEarlyFinish() {
        val early = treatment.finishEarly(LocalDate.of(2026, 12, 1), "toxicidad")
        assertEquals(LocalDate.of(2027, 3, 1), early.followUpDate(3))
    }

    @Test
    fun anUntilProgressionTreatmentHasNoFollowUpWhileItGoesOn() {
        val open = Treatment.assign(ac, LocalDate.of(2026, 11, 3), CycleCount.UntilProgression(3))
        assertNull(open.followUpDate(3))
    }

    @Test
    fun markingTheFollowUpDoneKeepsTheRestOfTheTreatment() {
        val done = treatment.markFollowUpDone(LocalDate.of(2027, 4, 28))
        assertEquals(LocalDate.of(2027, 4, 28), done.followUpDoneOn)
        assertNull(treatment.followUpDoneOn)
        assertEquals(treatment.endDate, done.endDate)
    }

    @Test
    fun otherChangesKeepTheFollowUpMark() {
        val done = treatment.markFollowUpDone(LocalDate.of(2027, 4, 28)).markGiven(SessionRef(1, 1))
        assertEquals(LocalDate.of(2027, 4, 28), done.followUpDoneOn)
    }
}
