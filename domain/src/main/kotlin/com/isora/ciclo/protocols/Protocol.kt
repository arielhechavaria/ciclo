package com.isora.ciclo.protocols

/**
 * Treatment structure of a protocol: how long a cycle lasts and on which days of the cycle
 * a session is given (1-based, D1 = first day of the cycle). Never stores doses.
 */
data class Regimen(val cycleLengthDays: Int, val sessionDays: List<Int>) {
    val sessionsPerCycle: Int get() = sessionDays.size
}

/** How many cycles a treatment has: a fixed number, or until progression with periodic reevaluation. */
sealed interface CycleCount {
    data class Fixed(val cycles: Int) : CycleCount

    data class UntilProgression(val reevaluateEveryCycles: Int) : CycleCount
}

data class Protocol(val name: String, val regimen: Regimen, val defaultCycles: CycleCount)
