package com.isora.ciclo.storage

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Regimen

/*
 * Stored format of the value types shared by protocols and treatments.
 * Changing any of these strings or codes is a stored-format change and needs a Room migration.
 */

internal fun CycleCount.mode(): String = when (this) {
    is CycleCount.Fixed -> "FIXED"
    is CycleCount.UntilProgression -> "UNTIL_PROGRESSION"
}

internal fun CycleCount.value(): Int = when (this) {
    is CycleCount.Fixed -> cycles
    is CycleCount.UntilProgression -> reevaluateEveryCycles
}

internal fun cycleCountOf(mode: String, value: Int): CycleCount = when (mode) {
    "FIXED" -> CycleCount.Fixed(value)
    "UNTIL_PROGRESSION" -> CycleCount.UntilProgression(value)
    else -> error("Unknown cycle count mode \"$mode\"")
}

internal fun List<Int>.toColumn(): String = joinToString(",")

internal fun regimenOf(cycleLengthDays: Int, sessionDays: String): Regimen =
    Regimen(cycleLengthDays, sessionDays.split(",").map { it.toInt() })
