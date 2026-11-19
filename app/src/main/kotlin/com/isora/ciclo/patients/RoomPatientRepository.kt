package com.isora.ciclo.patients

import android.database.sqlite.SQLiteConstraintException

class RoomPatientRepository(private val dao: PatientDao) : PatientRepository {

    override fun all(): List<StoredPatient> = dao.all().map { it.toStored() }

    override fun findById(id: PatientId): StoredPatient? = dao.findById(id.value)?.toStored()

    /** The unique index only catches exact matches, so case and spacing are checked here. */
    private fun requireFreeRecord(patient: Patient, ownId: Long?) {
        val key = recordKey(patient.clinicalRecordNumber)
        if (dao.all().any { it.id != ownId && recordKey(it.clinicalRecordNumber) == key }) {
            throw DuplicateClinicalRecordException(patient.clinicalRecordNumber)
        }
    }

    override fun add(patient: Patient): StoredPatient {
        requireFreeRecord(patient, ownId = null)
        val id = try {
            dao.insert(patient.toEntity(0))
        } catch (e: SQLiteConstraintException) {
            throw DuplicateClinicalRecordException(patient.clinicalRecordNumber)
        }
        return StoredPatient(PatientId(id), patient)
    }

    override fun update(id: PatientId, patient: Patient) {
        requireFreeRecord(patient, ownId = id.value)
        val changed = try {
            dao.update(patient.toEntity(id.value))
        } catch (e: SQLiteConstraintException) {
            throw DuplicateClinicalRecordException(patient.clinicalRecordNumber)
        }
        if (changed == 0) throw NoSuchElementException("Patient ${id.value} does not exist")
    }
}

private fun Patient.toEntity(id: Long) = PatientEntity(id, name, clinicalRecordNumber, diagnosis, notes)

private fun PatientEntity.toStored() =
    StoredPatient(PatientId(id), Patient(name, clinicalRecordNumber, diagnosis, notes))
