package com.isora.ciclo.patients

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PatientFormTest {
    @Test
    fun requiresNameAndClinicalRecordNumber() {
        val result = PatientForm().validate() as PatientFormResult.Invalid
        assertEquals(setOf(PatientFormError.NAME_REQUIRED, PatientFormError.RECORD_REQUIRED), result.errors)
    }

    @Test
    fun diagnosisAndNotesAreOptional() {
        val result = PatientForm(name = " A.B. ", clinicalRecordNumber = "HC-1").validate() as PatientFormResult.Valid
        assertEquals(Patient("A.B.", "HC-1"), result.patient)
    }

    @Test
    fun anIdentityCardThatIsNot11DigitsIsAnError() {
        val result = PatientForm(name = "A.B.", clinicalRecordNumber = "HC-1", identityCard = "1234").validate()
        assertEquals(PatientFormResult.Invalid(setOf(PatientFormError.IDENTITY_CARD_INVALID)), result)
    }

    @Test
    fun theIdentityCardIsSavedWithoutSpaces() {
        val result = PatientForm(name = "A.B.", clinicalRecordNumber = "HC-1", identityCard = "850101 12345")
            .validate() as PatientFormResult.Valid
        assertEquals("85010112345", result.patient.identityCard)
    }

    @Test
    fun editingAPatientStartsWithItsIdentityCard() {
        assertEquals("85010112345", PatientForm.from(Patient("A.B.", "HC-1", identityCard = "85010112345")).identityCard)
    }
}
