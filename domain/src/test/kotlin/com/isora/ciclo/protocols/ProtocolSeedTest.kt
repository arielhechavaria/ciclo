package com.isora.ciclo.protocols

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProtocolSeedTest {

    private fun seed(name: String) = ProtocolSeed.protocols.first { it.name == name }

    @Test
    fun `ships the eight standard protocols with unique names`() {
        assertEquals(
            listOf(
                "AC", "AC dosis densa", "Paclitaxel semanal", "TC",
                "FOLFOX", "CAPOX", "Carboplatino + paclitaxel", "Gemcitabina + cisplatino",
            ),
            ProtocolSeed.protocols.map { it.name },
        )
    }

    @Test
    fun `AC is four cycles of 21 days with a session on day 1`() {
        val ac = seed("AC")
        assertEquals(Regimen(21, listOf(1)), ac.regimen)
        assertEquals(CycleCount.Fixed(4), ac.defaultCycles)
    }

    @Test
    fun `gemcitabine plus cisplatin has sessions on day 1 and day 8`() {
        val protocol = seed("Gemcitabina + cisplatino")
        assertEquals(Regimen(21, listOf(1, 8)), protocol.regimen)
        assertEquals(CycleCount.Fixed(4), protocol.defaultCycles)
    }

    @Test
    fun `cycle lengths and default cycles match the proposal table`() {
        val table = mapOf(
            "AC dosis densa" to (14 to 4),
            "Paclitaxel semanal" to (7 to 12),
            "TC" to (21 to 4),
            "FOLFOX" to (14 to 12),
            "CAPOX" to (21 to 8),
            "Carboplatino + paclitaxel" to (21 to 6),
        )
        table.forEach { (name, expected) ->
            val protocol = seed(name)
            assertEquals(expected.first, protocol.regimen.cycleLengthDays, name)
            assertEquals(CycleCount.Fixed(expected.second), protocol.defaultCycles, name)
            assertEquals(listOf(1), protocol.regimen.sessionDays, name)
        }
    }
}
