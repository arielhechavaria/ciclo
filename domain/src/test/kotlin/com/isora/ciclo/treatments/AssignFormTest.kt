package com.isora.ciclo.treatments

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AssignFormTest {
    private val folfox = Protocol("FOLFOX", Regimen(14, listOf(1)), CycleCount.Fixed(12))

    @Test
    fun cycleCountDefaultsFromTheProtocolAndCanBeOverridden() {
        assertEquals("12", AssignForm(folfox).cycles)
        val form = AssignForm(folfox, cycles = "6", startDate = LocalDate.of(2026, 11, 3))
        val treatment = (form.validate() as AssignFormResult.Valid).treatment
        assertEquals(6, treatment.totalCycles)
    }

    @Test
    fun needsAStartDateAndAPositiveNumberOfCycles() {
        val result = AssignForm(folfox, cycles = "0").validate() as AssignFormResult.Invalid
        assertEquals(setOf(AssignFormError.CYCLES_INVALID, AssignFormError.START_DATE_REQUIRED), result.errors)
    }

    @Test
    fun untilProgressionKeepsTheNumberAsReevaluationInterval() {
        val protocol = Protocol("Trastuzumab", Regimen(21, listOf(1)), CycleCount.UntilProgression(4))
        val treatment = (AssignForm(protocol, "3", LocalDate.of(2026, 11, 3)).validate() as AssignFormResult.Valid).treatment
        assertEquals(CycleCount.UntilProgression(3), treatment.cycleCount)
    }
}
