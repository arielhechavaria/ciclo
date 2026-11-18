package com.isora.ciclo.protocols

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProtocolFormTest {

    private fun form(
        name: String = "AC",
        cycleLength: String = "21",
        sessionDays: String = "1",
        untilProgression: Boolean = false,
        cycles: String = "4",
    ) = ProtocolForm(name, cycleLength, sessionDays, untilProgression, cycles)

    private fun errors(form: ProtocolForm) = (form.validate() as ProtocolFormResult.Invalid).errors

    @Test
    fun `a complete form becomes a protocol with a fixed number of cycles`() {
        assertEquals(
            ProtocolFormResult.Valid(Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))),
            form().validate(),
        )
    }

    @Test
    fun `session days accept commas and spaces`() {
        val expected = ProtocolFormResult.Valid(
            Protocol("Gem", Regimen(21, listOf(1, 8)), CycleCount.Fixed(4)),
        )
        assertEquals(expected, form(name = "Gem", sessionDays = "1, 8").validate())
        assertEquals(expected, form(name = "Gem", sessionDays = "1 8").validate())
        assertEquals(expected, form(name = "Gem", sessionDays = " 1,8 ").validate())
    }

    @Test
    fun `until progression reads the number as the reevaluation interval`() {
        assertEquals(
            ProtocolFormResult.Valid(Protocol("T", Regimen(21, listOf(1)), CycleCount.UntilProgression(4))),
            form(name = "T", untilProgression = true, cycles = "4").validate(),
        )
    }

    @Test
    fun `a blank name is rejected`() {
        assertEquals(setOf(ProtocolFormError.NAME_REQUIRED), errors(form(name = "  ")))
    }

    @Test
    fun `the cycle length must be a positive whole number`() {
        listOf("", "abc", "0", "-3", "2.5").forEach {
            assertEquals(setOf(ProtocolFormError.CYCLE_LENGTH_INVALID), errors(form(cycleLength = it)), it)
        }
    }

    @Test
    fun `session days must be numbers, ascending, unique and inside the cycle`() {
        listOf("", "x", "1,,", "8,1", "1,1", "1,22", "0").forEach {
            assertEquals(setOf(ProtocolFormError.SESSION_DAYS_INVALID), errors(form(sessionDays = it)), it)
        }
    }

    @Test
    fun `the number of cycles must be a positive whole number`() {
        listOf("", "abc", "0").forEach {
            assertEquals(setOf(ProtocolFormError.CYCLES_INVALID), errors(form(cycles = it)), it)
        }
        assertEquals(
            setOf(ProtocolFormError.CYCLES_INVALID),
            errors(form(untilProgression = true, cycles = "0")),
        )
    }

    @Test
    fun `every problem is reported at once`() {
        assertEquals(
            setOf(
                ProtocolFormError.NAME_REQUIRED,
                ProtocolFormError.CYCLE_LENGTH_INVALID,
                ProtocolFormError.CYCLES_INVALID,
            ),
            errors(form(name = "", cycleLength = "x", cycles = "")),
        )
    }

    @Test
    fun `an invalid cycle length does not hide a wrong session day`() {
        assertEquals(
            setOf(ProtocolFormError.CYCLE_LENGTH_INVALID, ProtocolFormError.SESSION_DAYS_INVALID),
            errors(form(cycleLength = "x", sessionDays = "")),
        )
    }

    @Test
    fun `editing a protocol round trips through the form`() {
        listOf(
            Protocol("Gem", Regimen(21, listOf(1, 8)), CycleCount.Fixed(4)),
            Protocol("T", Regimen(21, listOf(1)), CycleCount.UntilProgression(4)),
        ).forEach {
            assertEquals(ProtocolFormResult.Valid(it), ProtocolForm.from(it).validate())
        }
    }
}
