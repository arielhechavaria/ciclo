package com.isora.ciclo.patients

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "patients", indices = [Index(value = ["clinical_record_number"], unique = true)])
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "clinical_record_number") val clinicalRecordNumber: String,
    val diagnosis: String?,
    val notes: String?,
)

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name COLLATE NOCASE, id")
    fun all(): List<PatientEntity>

    @Query("SELECT * FROM patients WHERE id = :id")
    fun findById(id: Long): PatientEntity?

    @Insert
    fun insert(entity: PatientEntity): Long

    /** Returns how many rows changed: 0 when the id does not exist. */
    @Update
    fun update(entity: PatientEntity): Int
}
