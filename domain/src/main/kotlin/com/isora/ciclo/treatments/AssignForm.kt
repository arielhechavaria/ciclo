package com.isora.ciclo.treatments

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import java.time.LocalDate

enum class AssignFormError { CYCLES_INVALID, START_DATE_REQUIRED }

sealed interface AssignFormResult {
    data class Valid(val treatment: Treatment) : AssignFormResult
    data class Invalid(val errors: Set<AssignFormError>) : AssignFormResult
}

/**
 * Assigning a protocol to a patient: protocol, number of cycles and start date. The number of cycles
 * starts from the protocol's default and can be overridden; for an until-progression protocol the
 * number is the reevaluation interval.
 */
data class AssignForm(
    val protocol: Protocol,
    val cycles: String = defaultCycles(protocol),
    val startDate: LocalDate? = null,
) {
    fun validate(): AssignFormResult {
        val errors = mutableSetOf<AssignFormError>()
        val number = cycles.trim().toIntOrNull()?.takeIf { it >= 1 }
        if (number == null) errors += AssignFormError.CYCLES_INVALID
        if (startDate == null) errors += AssignFormError.START_DATE_REQUIRED
        if (errors.isNotEmpty()) return AssignFormResult.Invalid(errors)
        val count = when (protocol.defaultCycles) {
            is CycleCount.Fixed -> CycleCount.Fixed(number!!)
            is CycleCount.UntilProgression -> CycleCount.UntilProgression(number!!)
        }
        return AssignFormResult.Valid(Treatment.assign(protocol, startDate!!, count))
    }

    companion object {
        fun defaultCycles(protocol: Protocol): String = when (val c = protocol.defaultCycles) {
            is CycleCount.Fixed -> c.cycles.toString()
            is CycleCount.UntilProgression -> c.reevaluateEveryCycles.toString()
        }
    }
}
