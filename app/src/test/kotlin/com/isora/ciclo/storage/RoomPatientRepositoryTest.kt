package com.isora.ciclo.storage

import com.isora.ciclo.patients.DuplicateClinicalRecordException
import com.isora.ciclo.patients.DuplicateIdentityCardException
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
    fun recordNumbersDifferingOnlyInCaseOrSpacesAreTheSame() {
        repository.add(Patient("Alberto R.", "HC 1001"))
        listOf("hc1001", "Hc  1001", " HC1001 ").forEach { variant ->
            assertThrows(DuplicateClinicalRecordException::class.java) { repository.add(Patient("Otra", variant)) }
        }
        val other = repository.add(Patient("Maria L.", "HC-1001"))
        assertThrows(DuplicateClinicalRecordException::class.java) { repository.update(other.id, Patient("Maria L.", "hc1001")) }
        assertEquals(2, repository.all().size)
    }

    @Test
    fun aPatientCanChangeOnlyTheCaseOfItsOwnRecordNumber() {
        val saved = repository.add(Patient("Alberto R.", "hc1001"))
        repository.update(saved.id, Patient("Alberto R.", "HC1001"))
        assertEquals("HC1001", repository.findById(saved.id)?.patient?.clinicalRecordNumber)
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

    @Test
    fun identityCardRoundTrips() {
        val saved = repository.add(Patient("Alberto R.", "10234", identityCard = "85010112345"))
        assertEquals("85010112345", repository.findById(saved.id)?.patient?.identityCard)
    }

    @Test
    fun identityCardIsUniqueAmongPatients() {
        repository.add(Patient("Alberto R.", "10234", identityCard = "85010112345"))
        val error = assertThrows(DuplicateIdentityCardException::class.java) {
            repository.add(Patient("Otra", "20999", identityCard = "85010112345"))
        }
        assertEquals("85010112345", error.identityCard)
        val other = repository.add(Patient("Maria L.", "30111", identityCard = "90020254321"))
        assertThrows(DuplicateIdentityCardException::class.java) {
            repository.update(other.id, Patient("Maria L.", "30111", identityCard = "85010112345"))
        }
        assertEquals(2, repository.all().size)
    }

    @Test
    fun manyPatientsCanHaveNoIdentityCard() {
        repository.add(Patient("Alberto R.", "10234"))
        repository.add(Patient("Maria L.", "20999"))
        assertEquals(2, repository.all().size)
    }

    @Test
    fun aPatientKeepsItsOwnIdentityCardWhenUpdated() {
        val saved = repository.add(Patient("Alberto R.", "10234", identityCard = "85010112345"))
        repository.update(saved.id, Patient("Alberto Ruiz", "10234", identityCard = "85010112345"))
        assertEquals("Alberto Ruiz", repository.findById(saved.id)?.patient?.name)
    }
}
