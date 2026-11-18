package com.isora.ciclo.treatments

import com.isora.ciclo.patients.PatientId

@JvmInline
value class TreatmentId(val value: Long)

/** A treatment owned by a patient. [protocolName] is a label copied at assignment, not a live link. */
data class StoredTreatment(
    val id: TreatmentId,
    val patientId: PatientId,
    val protocolName: String,
    val treatment: Treatment,
)

interface TreatmentRepository {
    fun all(): List<StoredTreatment>
    fun forPatient(patientId: PatientId): List<StoredTreatment>
    fun findById(id: TreatmentId): StoredTreatment?
    fun assign(patientId: PatientId, protocolName: String, treatment: Treatment): StoredTreatment

    /** Persists the current state of the treatment: postponements, given sessions and early finish. */
    fun save(stored: StoredTreatment)
}
