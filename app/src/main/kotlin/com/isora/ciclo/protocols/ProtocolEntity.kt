package com.isora.ciclo.protocols

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "protocols", indices = [Index(value = ["name"], unique = true)])
data class ProtocolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "cycle_length_days") val cycleLengthDays: Int,
    /** Comma separated, ascending, 1-based session days of the cycle. */
    @ColumnInfo(name = "session_days") val sessionDays: String,
    @ColumnInfo(name = "cycle_mode") val cycleMode: String,
    @ColumnInfo(name = "cycle_value") val cycleValue: Int,
    val archived: Boolean = false,
)

@Dao
interface ProtocolDao {
    @Query("SELECT * FROM protocols ORDER BY id")
    fun all(): List<ProtocolEntity>

    @Query("SELECT * FROM protocols WHERE archived = 0 ORDER BY id")
    fun active(): List<ProtocolEntity>

    @Query("SELECT COUNT(*) FROM protocols")
    fun count(): Int

    @Insert
    fun insert(entity: ProtocolEntity): Long

    @Insert
    fun insertAll(entities: List<ProtocolEntity>)

    @Query("DELETE FROM protocols")
    fun deleteAll()

    @Update
    fun update(entity: ProtocolEntity)

    @Query("UPDATE protocols SET archived = :archived WHERE id = :id")
    fun setArchived(id: Long, archived: Boolean)
}
