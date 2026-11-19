package com.isora.ciclo.treatments

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.isora.ciclo.patients.PatientEntity

/**
 * A treatment keeps its own copy (snapshot) of the protocol structure, so editing the catalog
 * never changes it. There is deliberately no link to the protocols table.
 */
@Entity(
    tableName = "treatments",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("patient_id")],
)
data class TreatmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    @ColumnInfo(name = "protocol_name") val protocolName: String,
    @ColumnInfo(name = "cycle_length_days") val cycleLengthDays: Int,
    @ColumnInfo(name = "session_days") val sessionDays: String,
    @ColumnInfo(name = "cycle_mode") val cycleMode: String,
    @ColumnInfo(name = "cycle_value") val cycleValue: Int,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long,
    @ColumnInfo(name = "finish_epoch_day") val finishEpochDay: Long?,
    @ColumnInfo(name = "finish_reason") val finishReason: String?,
    /** Day the follow-up review was done; null while pending. Added in schema version 2. */
    @ColumnInfo(name = "follow_up_done_epoch_day") val followUpDoneEpochDay: Long? = null,
)

/** State of one session that is not simply planned: given, postponed by [delayDays], or both. */
@Entity(
    tableName = "session_states",
    primaryKeys = ["treatment_id", "cycle", "session"],
    foreignKeys = [
        ForeignKey(
            entity = TreatmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["treatment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionStateEntity(
    @ColumnInfo(name = "treatment_id") val treatmentId: Long,
    val cycle: Int,
    val session: Int,
    val given: Boolean,
    /** Days of delay applied when the session was postponed; null when never postponed. */
    @ColumnInfo(name = "delay_days") val delayDays: Long?,
)

@Dao
abstract class TreatmentDao {
    @Query("SELECT * FROM treatments ORDER BY id")
    abstract fun all(): List<TreatmentEntity>

    @Query("SELECT * FROM treatments WHERE patient_id = :patientId ORDER BY id")
    abstract fun forPatient(patientId: Long): List<TreatmentEntity>

    @Query("SELECT * FROM treatments WHERE id = :id")
    abstract fun findById(id: Long): TreatmentEntity?

    @Query("SELECT * FROM session_states WHERE treatment_id = :treatmentId")
    abstract fun statesOf(treatmentId: Long): List<SessionStateEntity>

    @Query("SELECT * FROM session_states ORDER BY treatment_id, cycle, session")
    abstract fun allStates(): List<SessionStateEntity>

    @Insert
    abstract fun insert(entity: TreatmentEntity): Long

    @Insert
    abstract fun insertAll(entities: List<TreatmentEntity>)

    @Query("DELETE FROM treatments")
    abstract fun deleteAll()

    @Query("DELETE FROM session_states")
    abstract fun deleteAllStates()

    @Insert
    abstract fun insertStates(states: List<SessionStateEntity>)

    /** Returns how many rows changed: 0 when the id does not exist. */
    @Update
    abstract fun update(entity: TreatmentEntity): Int

    @Query("DELETE FROM session_states WHERE treatment_id = :treatmentId")
    abstract fun deleteStates(treatmentId: Long)

    @Transaction
    open fun create(entity: TreatmentEntity, states: List<SessionStateEntity>): Long {
        val id = insert(entity)
        insertStates(states.map { it.copy(treatmentId = id) })
        return id
    }

    @Transaction
    open fun replace(entity: TreatmentEntity, states: List<SessionStateEntity>) {
        // Throwing inside the transaction rolls everything back.
        if (update(entity) == 0) throw NoSuchElementException("Treatment ${entity.id} does not exist")
        deleteStates(entity.id)
        insertStates(states)
    }
}
