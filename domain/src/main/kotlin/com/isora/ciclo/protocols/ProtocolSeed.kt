package com.isora.ciclo.protocols

/** The standard protocols preloaded in the catalog. Structure only, never doses. */
object ProtocolSeed {
    val protocols: List<Protocol> = listOf(
        protocol("AC", cycleDays = 21, cycles = 4),
        protocol("AC dosis densa", cycleDays = 14, cycles = 4),
        protocol("Paclitaxel semanal", cycleDays = 7, cycles = 12),
        protocol("TC", cycleDays = 21, cycles = 4),
        protocol("FOLFOX", cycleDays = 14, cycles = 12),
        protocol("CAPOX", cycleDays = 21, cycles = 8),
        protocol("Carboplatino + paclitaxel", cycleDays = 21, cycles = 6),
        protocol("Gemcitabina + cisplatino", cycleDays = 21, cycles = 4, sessionDays = listOf(1, 8)),
    )

    private fun protocol(name: String, cycleDays: Int, cycles: Int, sessionDays: List<Int> = listOf(1)) =
        Protocol(name, Regimen(cycleDays, sessionDays), CycleCount.Fixed(cycles))
}
