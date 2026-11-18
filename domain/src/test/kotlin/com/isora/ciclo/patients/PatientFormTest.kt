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
}
