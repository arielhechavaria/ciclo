package com.isora.ciclo.protocols

/**
 * Treatment structure of a protocol: how long a cycle lasts and on which days of the cycle
 * a session is given (1-based, D1 = first day of the cycle). Never stores doses.
 */
data class Regimen(val cycleLengthDays: Int, val sessionDays: List<Int>) {
    init {
        require(cycleLengthDays >= 1) { "A cycle lasts at least one day" }
        require(sessionDays.isNotEmpty()) { "A cycle needs at least one session" }
        require(sessionDays.zipWithNext().all { (a, b) -> a < b }) { "Session days must be ascending and unique" }
        require(sessionDays.all { it in 1..cycleLengthDays }) { "Session days must fall inside the cycle" }
    }

    val sessionsPerCycle: Int get() = sessionDays.size
}

/** How many cycles a treatment has: a fixed number, or until progression with periodic reevaluation. */
sealed interface CycleCount {
    data class Fixed(val cycles: Int) : CycleCount {
        init {
            require(cycles >= 1) { "A treatment has at least one cycle" }
        }
    }

    data class UntilProgression(val reevaluateEveryCycles: Int) : CycleCount {
        init {
            require(reevaluateEveryCycles >= 1) { "Reevaluation happens every one or more cycles" }
        }
    }
}

data class Protocol(val name: String, val regimen: Regimen, val defaultCycles: CycleCount) {
    init {
        require(name.isNotBlank()) { "A protocol needs a name" }
    }
}
