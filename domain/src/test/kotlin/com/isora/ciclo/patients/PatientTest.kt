package com.isora.ciclo.patients

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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

    @Test
    fun `the identity card is optional and ignores spaces`() {
        assertNull(Patient("Ana", "HC-1").identityCard)
        assertNull(Patient("Ana", "HC-1", identityCard = "  ").identityCard)
        assertEquals("85010112345", Patient("Ana", "HC-1", identityCard = " 850101 12345 ").identityCard)
    }

    @Test
    fun `an identity card must have 11 digits`() {
        assertThrows<IllegalArgumentException> { Patient("Ana", "HC-1", identityCard = "8501011234") }
        assertThrows<IllegalArgumentException> { Patient("Ana", "HC-1", identityCard = "850101123456") }
        assertThrows<IllegalArgumentException> { Patient("Ana", "HC-1", identityCard = "85010112A45") }
    }

    @Test
    fun `patients with different identity cards are different`() {
        assertNotEquals(Patient("Ana", "HC-1", identityCard = "85010112345"), Patient("Ana", "HC-1"))
    }
}
