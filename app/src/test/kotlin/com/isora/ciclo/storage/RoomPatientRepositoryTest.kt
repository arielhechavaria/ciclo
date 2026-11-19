package com.isora.ciclo.storage

import com.isora.ciclo.patients.DuplicateClinicalRecordException
import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.RoomPatientRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RoomPatientRepositoryTest : StorageTestBase() {
    private val repository by lazy { RoomPatientRepository(db.patientDao()) }

    @Test
    fun savedPatientRoundTripsEveryField() {
        val saved = repository.add(Patient("Alberto R.", "10234", diagnosis = "Mama", notes = "alergia"))
        assertEquals(Patient("Alberto R.", "10234", "Mama", "alergia"), repository.findById(saved.id)?.patient)
    }

    @Test
    fun optionalFieldsStayAbsent() {
        val saved = repository.add(Patient("Alberto R.", "10234"))
        assertNull(repository.findById(saved.id)?.patient?.diagnosis)
        assertNull(repository.findById(saved.id)?.patient?.notes)
    }

    @Test
    fun clinicalRecordNumberIsUnique() {
        repository.add(Patient("Alberto R.", "10234"))
        val error = assertThrows(DuplicateClinicalRecordException::class.java) {
            repository.add(Patient("Otra", "10234"))
        }
        assertEquals("10234", error.clinicalRecordNumber)
        assertEquals(1, repository.all().size)
    }

    @Test
    fun updatingAMissingPatientFailsInsteadOfDoingNothing() {
        assertThrows(NoSuchElementException::class.java) {
            repository.update(PatientId(999), Patient("Nadie", "00000"))
        }
        assertEquals(0, repository.all().size)
    }

    @Test
    fun updateKeepsTheUniquenessRule() {
        repository.add(Patient("Alberto R.", "10234"))
        val second = repository.add(Patient("Maria L.", "20999"))
        assertThrows(DuplicateClinicalRecordException::class.java) {
            repository.update(second.id, Patient("Maria L.", "10234"))
        }
        repository.update(second.id, Patient("Maria L.", "20999", notes = "nota"))
        assertEquals("nota", repository.findById(second.id)?.patient?.notes)
    }
}
