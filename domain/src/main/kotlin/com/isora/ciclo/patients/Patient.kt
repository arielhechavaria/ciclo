package com.isora.ciclo.patients

@JvmInline
value class PatientId(val value: Long)

/**
 * Minimum data about a patient. Name or initials, clinical record number, optional diagnosis and notes,
 * and an optional identity card number (11 digits, spaces ignored) used to recognise the patient on a photo.
 */
class Patient(
    name: String,
    clinicalRecordNumber: String,
    diagnosis: String? = null,
    notes: String? = null,
    identityCard: String? = null,
) {
    val name: String = name.trim()
    val clinicalRecordNumber: String = clinicalRecordNumber.trim()
    val diagnosis: String? = diagnosis?.trim()?.takeIf { it.isNotEmpty() }
    val notes: String? = notes?.trim()?.takeIf { it.isNotEmpty() }
    val identityCard: String? = identityCard?.let(::identityCardKey)?.takeIf { it.isNotEmpty() }

    init {
        require(this.name.isNotEmpty()) { "A patient needs a name or initials" }
        require(this.clinicalRecordNumber.isNotEmpty()) { "A patient needs a clinical record number" }
        require(this.identityCard == null || isIdentityCard(this.identityCard)) { "An identity card has 11 digits" }
    }

    override fun equals(other: Any?) = other is Patient &&
        name == other.name && clinicalRecordNumber == other.clinicalRecordNumber &&
        diagnosis == other.diagnosis && notes == other.notes && identityCard == other.identityCard

    override fun hashCode() = listOf(name, clinicalRecordNumber, diagnosis, notes, identityCard).hashCode()

    override fun toString() = "Patient(name=$name, clinicalRecordNumber=$clinicalRecordNumber)"
}

data class StoredPatient(val id: PatientId, val patient: Patient)

/** Two record numbers are the same when they differ only in case or whitespace: "HC1001" = "hc 1001", but "HC-1001" is different. */
fun recordKey(clinicalRecordNumber: String): String = clinicalRecordNumber.filterNot(Char::isWhitespace).lowercase()

/** An identity card without its spaces: "850101 12345" = "85010112345". */
fun identityCardKey(identityCard: String): String = identityCard.filterNot(Char::isWhitespace)

/** True for a Cuban identity card number: exactly 11 digits once spaces are removed. */
fun isIdentityCard(text: String): Boolean = identityCardKey(text).let { it.length == 11 && it.all { c -> c in '0'..'9' } }

/** Which unique field of a patient another patient already holds. */
enum class PatientConflict { RECORD, IDENTITY_CARD }

/** A patient could not be saved because another patient holds the same [conflict] field. */
sealed class PatientConflictException(val conflict: PatientConflict, message: String) : RuntimeException(message)

class DuplicateClinicalRecordException(val clinicalRecordNumber: String) : PatientConflictException(
    PatientConflict.RECORD, "Clinical record number $clinicalRecordNumber already belongs to another patient",
)

class DuplicateIdentityCardException(val identityCard: String) : PatientConflictException(
    PatientConflict.IDENTITY_CARD, "This identity card already belongs to another patient",
)

interface PatientRepository {
    fun all(): List<StoredPatient>
    fun findById(id: PatientId): StoredPatient?

    /** @throws PatientConflictException when the clinical record number or the identity card is taken. */
    fun add(patient: Patient): StoredPatient

    /** @throws PatientConflictException when the number or the card belongs to a different patient. */
    fun update(id: PatientId, patient: Patient)
}
