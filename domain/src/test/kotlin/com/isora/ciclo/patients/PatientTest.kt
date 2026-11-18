package com.isora.ciclo.patients

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PatientTest {

    @Test
    fun `requires a name and a clinical record number`() {
        assertThrows<IllegalArgumentException> { Patient(name = " ", clinicalRecordNumber = "10234") }
        assertThrows<IllegalArgumentException> { Patient(name = "Alberto R.", clinicalRecordNumber = "") }
    }

    @Test
    fun `trims text and treats blank optional fields as absent`() {
        val patient = Patient(
            name = "  Alberto R. ",
            clinicalRecordNumber = " 10234 ",
            diagnosis = "  ",
            notes = " alergia ",
        )
        assertEquals("Alberto R.", patient.name)
        assertEquals("10234", patient.clinicalRecordNumber)
        assertNull(patient.diagnosis)
        assertEquals("alergia", patient.notes)
    }
}
