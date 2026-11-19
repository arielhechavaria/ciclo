package com.isora.ciclo.patients

enum class PatientFormError { NAME_REQUIRED, RECORD_REQUIRED, IDENTITY_CARD_INVALID }

sealed interface PatientFormResult {
    data class Valid(val patient: Patient) : PatientFormResult
    data class Invalid(val errors: Set<PatientFormError>) : PatientFormResult
}

/** The raw text of the patient form. Diagnosis, notes and identity card are optional. */
data class PatientForm(
    val name: String = "",
    val clinicalRecordNumber: String = "",
    val diagnosis: String = "",
    val notes: String = "",
    val identityCard: String = "",
) {
    fun validate(): PatientFormResult {
        val errors = mutableSetOf<PatientFormError>()
        if (name.isBlank()) errors += PatientFormError.NAME_REQUIRED
        if (clinicalRecordNumber.isBlank()) errors += PatientFormError.RECORD_REQUIRED
        if (identityCard.isNotBlank() && !isIdentityCard(identityCard)) errors += PatientFormError.IDENTITY_CARD_INVALID
        if (errors.isNotEmpty()) return PatientFormResult.Invalid(errors)
        return PatientFormResult.Valid(Patient(name, clinicalRecordNumber, diagnosis, notes, identityCard))
    }

    companion object {
        fun from(patient: Patient) = PatientForm(
            name = patient.name,
            clinicalRecordNumber = patient.clinicalRecordNumber,
            diagnosis = patient.diagnosis.orEmpty(),
            notes = patient.notes.orEmpty(),
            identityCard = patient.identityCard.orEmpty(),
        )
    }
}
