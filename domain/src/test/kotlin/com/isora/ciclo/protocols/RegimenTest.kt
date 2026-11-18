package com.isora.ciclo.protocols

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegimenTest {

    @Test
    fun `counts the sessions of a cycle`() {
        assertEquals(2, Regimen(cycleLengthDays = 21, sessionDays = listOf(1, 8)).sessionsPerCycle)
    }

    @Test
    fun `rejects a session day outside the cycle`() {
        assertThrows<IllegalArgumentException> { Regimen(cycleLengthDays = 21, sessionDays = listOf(1, 22)) }
    }

    @Test
    fun `rejects a cycle without sessions`() {
        assertThrows<IllegalArgumentException> { Regimen(cycleLengthDays = 21, sessionDays = emptyList()) }
    }

    @Test
    fun `rejects session days out of order or repeated`() {
        assertThrows<IllegalArgumentException> { Regimen(cycleLengthDays = 21, sessionDays = listOf(8, 1)) }
        assertThrows<IllegalArgumentException> { Regimen(cycleLengthDays = 21, sessionDays = listOf(1, 1)) }
    }

    @Test
    fun `rejects a cycle count below one`() {
        assertThrows<IllegalArgumentException> { CycleCount.Fixed(0) }
        assertThrows<IllegalArgumentException> { CycleCount.UntilProgression(reevaluateEveryCycles = 0) }
    }

    @Test
    fun `rejects a protocol without a name`() {
        assertThrows<IllegalArgumentException> {
            Protocol(" ", Regimen(21, listOf(1)), CycleCount.Fixed(4))
        }
    }
}
