package com.isora.ciclo.mock

import com.isora.ciclo.AppContainer
import com.isora.ciclo.patients.PatientConflictException
import java.time.LocalDate

/** Writes the fictional [MockData] into the database. Blocking: call it off the main thread. */
object MockSeeder {

    /** Adds every mock patient whose record number and identity card are free; returns how many were added. */
    fun seed(container: AppContainer, today: LocalDate): Int {
        var added = 0
        MockData.build(today).forEach { case ->
            try {
                val stored = container.patients.add(case.patient)
                val treatment = case.treatment
                val protocolName = case.protocolName
                if (treatment != null && protocolName != null) {
                    container.treatments.assign(stored.id, protocolName, treatment)
                }
                added++
            } catch (e: PatientConflictException) {
                // Already loaded: leave it as it is.
            }
        }
        return added
    }
}
