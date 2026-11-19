package com.isora.ciclo.storage

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.isora.ciclo.patients.PatientEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Builds a database exactly as an older version created it (from the committed schema), opens it with
 * the current code and checks that the migrations keep every row. Room validates the migrated tables
 * against the current entities when it opens the file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun createVersion(version: Int, file: File) {
        // Unit tests run with the module directory as working directory.
        val schema = JSONObject(File("schemas/com.isora.ciclo.storage.CicloDatabase/$version.json").readText()).getJSONObject("database")
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices")
            for (j in 0 until (indices?.length() ?: 0)) {
                db.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        db.execSQL("INSERT INTO patients (id, name, clinical_record_number) VALUES (1, 'Ana', 'HC-1')")
        db.execSQL(
            "INSERT INTO treatments (id, patient_id, protocol_name, cycle_length_days, session_days, cycle_mode, " +
                "cycle_value, start_epoch_day, finish_epoch_day, finish_reason) " +
                "VALUES (7, 1, 'AC', 21, '1', 'FIXED', 4, 20000, NULL, NULL)",
        )
        db.execSQL("INSERT INTO session_states (treatment_id, cycle, session, given, delay_days) VALUES (7, 1, 1, 1, 3)")
        if (version >= 2) db.execSQL("UPDATE treatments SET follow_up_done_epoch_day = 20300 WHERE id = 7")
        db.version = version
        db.close()
    }

    private fun open(name: String) = Room.databaseBuilder(context, CicloDatabase::class.java, name)
        .addMigrations(*CicloDatabase.MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    @Test
    fun migratingFrom1To2KeepsEveryRowAndLeavesTheFollowUpEmpty() {
        val file = context.getDatabasePath("migration-test.db").also { it.parentFile?.mkdirs(); it.delete() }
        createVersion(1, file)

        val db = open("migration-test.db")
        try {
            val treatment = db.treatmentDao().all().single()
            assertEquals(7L, treatment.id)
            assertEquals("AC", treatment.protocolName)
            assertEquals(20000L, treatment.startEpochDay)
            assertNull(treatment.followUpDoneEpochDay)
            assertEquals(3L, db.treatmentDao().allStates().single().delayDays)
            assertEquals("Ana", db.patientDao().all().single().name)
        } finally {
            db.close()
        }
    }

    @Test
    fun migratingFrom2To3KeepsEveryRowAndLeavesTheIdentityCardEmpty() {
        val file = context.getDatabasePath("migration-test-2.db").also { it.parentFile?.mkdirs(); it.delete() }
        createVersion(2, file)

        val db = open("migration-test-2.db")
        try {
            val patient = db.patientDao().all().single()
            assertEquals("Ana", patient.name)
            assertEquals("HC-1", patient.clinicalRecordNumber)
            assertNull(patient.identityCard)
            assertEquals(20300L, db.treatmentDao().all().single().followUpDoneEpochDay)
            assertEquals(3L, db.treatmentDao().allStates().single().delayDays)
        } finally {
            db.close()
        }
    }

    @Test
    fun afterMigratingTheIdentityCardIsUnique() {
        val file = context.getDatabasePath("migration-test-3.db").also { it.parentFile?.mkdirs(); it.delete() }
        createVersion(2, file)

        val db = open("migration-test-3.db")
        try {
            db.patientDao().insert(PatientEntity(0, "Beto", "HC-2", null, null, "85010112345"))
            assertThrows(SQLiteConstraintException::class.java) {
                db.patientDao().insert(PatientEntity(0, "Carla", "HC-3", null, null, "85010112345"))
            }
        } finally {
            db.close()
        }
    }
}
