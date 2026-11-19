package com.isora.ciclo.patients

import android.database.sqlite.SQLiteConstraintException

class RoomPatientRepository(private val dao: PatientDao) : PatientRepository {

    override fun all(): List<StoredPatient> = dao.all().map { it.toStored() }

    override fun findById(id: PatientId): StoredPatient? = dao.findById(id.value)?.toStored()

    /**
     * The unique index on record numbers only catches exact matches, so case and spacing are checked here.
     * Identity cards are stored normalised, so their unique index is exact; checking here gives the right error.
     */
    private fun requireFree(patient: Patient, ownId: Long?) {
        val others = dao.all().filter { it.id != ownId }
        val key = recordKey(patient.clinicalRecordNumber)
        if (others.any { recordKey(it.clinicalRecordNumber) == key }) {
            throw DuplicateClinicalRecordException(patient.clinicalRecordNumber)
        }
        val card = patient.identityCard
        if (card != null && others.any { it.identityCard == card }) throw DuplicateIdentityCardException(card)
    }

    /** A constraint the checks above missed (a concurrent write): report the field that collides now. */
    private fun conflictOf(patient: Patient, ownId: Long?): PatientConflictException = try {
        requireFree(patient, ownId)
        DuplicateClinicalRecordException(patient.clinicalRecordNumber)
    } catch (e: PatientConflictException) {
        e
    }

    override fun add(patient: Patient): StoredPatient {
        requireFree(patient, ownId = null)
        val id = try {
            dao.insert(patient.toEntity(0))
        } catch (e: SQLiteConstraintException) {
            throw conflictOf(patient, ownId = null)
        }
        return StoredPatient(PatientId(id), patient)
    }

    override fun update(id: PatientId, patient: Patient) {
        requireFree(patient, ownId = id.value)
        val changed = try {
            dao.update(patient.toEntity(id.value))
        } catch (e: SQLiteConstraintException) {
            throw conflictOf(patient, ownId = id.value)
        }
        if (changed == 0) throw NoSuchElementException("Patient ${id.value} does not exist")
    }
}

private fun Patient.toEntity(id: Long) = PatientEntity(id, name, clinicalRecordNumber, diagnosis, notes, identityCard)

private fun PatientEntity.toStored() =
    StoredPatient(PatientId(id), Patient(name, clinicalRecordNumber, diagnosis, notes, identityCard))
