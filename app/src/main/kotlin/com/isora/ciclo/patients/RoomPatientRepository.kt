package com.isora.ciclo.patients

import android.database.sqlite.SQLiteConstraintException

class RoomPatientRepository(private val dao: PatientDao) : PatientRepository {

    override fun all(): List<StoredPatient> = dao.all().map { it.toStored() }

    override fun findById(id: PatientId): StoredPatient? = dao.findById(id.value)?.toStored()

    override fun add(patient: Patient): StoredPatient {
        val id = try {
            dao.insert(patient.toEntity(0))
        } catch (e: SQLiteConstraintException) {
            throw DuplicateClinicalRecordException(patient.clinicalRecordNumber)
        }
        return StoredPatient(PatientId(id), patient)
    }

    override fun update(id: PatientId, patient: Patient) {
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
