package com.isora.ciclo.protocols

enum class ProtocolFormError { NAME_REQUIRED, CYCLE_LENGTH_INVALID, SESSION_DAYS_INVALID, CYCLES_INVALID }

sealed interface ProtocolFormResult {
    data class Valid(val protocol: Protocol) : ProtocolFormResult
    data class Invalid(val errors: Set<ProtocolFormError>) : ProtocolFormResult
}

/**
 * The raw text of the protocol form. [cycles] is the number of cycles, or, when [untilProgression]
 * is set, the interval in cycles at which the treatment is reevaluated.
 */
data class ProtocolForm(
    val name: String = "",
    val cycleLength: String = "",
    val sessionDays: String = "",
    val untilProgression: Boolean = false,
    val cycles: String = "",
) {
    fun validate(): ProtocolFormResult {
        val errors = mutableSetOf<ProtocolFormError>()
        if (name.isBlank()) errors += ProtocolFormError.NAME_REQUIRED

        val length = positiveInt(cycleLength)
        if (length == null) errors += ProtocolFormError.CYCLE_LENGTH_INVALID

        val days = parseDays(sessionDays)
        val regimen = if (length != null && days != null) {
            runCatching { Regimen(length, days) }.getOrNull()
        } else {
            null
        }
        if (regimen == null) errors += ProtocolFormError.SESSION_DAYS_INVALID

        val count = positiveInt(cycles)?.let { if (untilProgression) CycleCount.UntilProgression(it) else CycleCount.Fixed(it) }
        if (count == null) errors += ProtocolFormError.CYCLES_INVALID

        if (errors.isNotEmpty()) return ProtocolFormResult.Invalid(errors)
        return ProtocolFormResult.Valid(Protocol(name.trim(), regimen!!, count!!))
    }

    private fun positiveInt(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it >= 1 }

    private fun parseDays(text: String): List<Int>? {
        val parts = text.split(',', ' ').filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        // "1,,": a trailing or doubled separator is a typo, not a list.
        if (text.contains(Regex(",\\s*,|,\\s*$|^\\s*,"))) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    companion object {
        fun from(protocol: Protocol) = ProtocolForm(
            name = protocol.name,
            cycleLength = protocol.regimen.cycleLengthDays.toString(),
            sessionDays = protocol.regimen.sessionDays.joinToString(", "),
            untilProgression = protocol.defaultCycles is CycleCount.UntilProgression,
            cycles = when (val c = protocol.defaultCycles) {
                is CycleCount.Fixed -> c.cycles.toString()
                is CycleCount.UntilProgression -> c.reevaluateEveryCycles.toString()
            },
        )
    }
}
