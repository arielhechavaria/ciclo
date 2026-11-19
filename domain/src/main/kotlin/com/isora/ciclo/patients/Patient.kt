package com.isora.ciclo.patients

@JvmInline
value class PatientId(val value: Long)

/** Minimum data about a patient. Name or initials, clinical record number, optional diagnosis and notes. */
class Patient(
    name: String,
    clinicalRecordNumber: String,
    diagnosis: String? = null,
    notes: String? = null,
) {
    val name: String = name.trim()
    val clinicalRecordNumber: String = clinicalRecordNumber.trim()
    val diagnosis: String? = diagnosis?.trim()?.takeIf { it.isNotEmpty() }
    val notes: String? = notes?.trim()?.takeIf { it.isNotEmpty() }

    init {
        require(this.name.isNotEmpty()) { "A patient needs a name or initials" }
        require(this.clinicalRecordNumber.isNotEmpty()) { "A patient needs a clinical record number" }
    }

    override fun equals(other: Any?) = other is Patient &&
        name == other.name && clinicalRecordNumber == other.clinicalRecordNumber &&
        diagnosis == other.diagnosis && notes == other.notes

    override fun hashCode() = listOf(name, clinicalRecordNumber, diagnosis, notes).hashCode()

    override fun toString() = "Patient(name=$name, clinicalRecordNumber=$clinicalRecordNumber)"
}

data class StoredPatient(val id: PatientId, val patient: Patient)

/** Two record numbers are the same when they differ only in case or whitespace: "HC1001" = "hc 1001", but "HC-1001" is different. */
fun recordKey(clinicalRecordNumber: String): String = clinicalRecordNumber.filterNot(Char::isWhitespace).lowercase()

class DuplicateClinicalRecordException(val clinicalRecordNumber: String) :
    RuntimeException("Clinical record number $clinicalRecordNumber already belongs to another patient")

interface PatientRepository {
    fun all(): List<StoredPatient>
    fun findById(id: PatientId): StoredPatient?

    /** @throws DuplicateClinicalRecordException when the clinical record number is taken. */
    fun add(patient: Patient): StoredPatient

    /** @throws DuplicateClinicalRecordException when the number belongs to a different patient. */
    fun update(id: PatientId, patient: Patient)
}
