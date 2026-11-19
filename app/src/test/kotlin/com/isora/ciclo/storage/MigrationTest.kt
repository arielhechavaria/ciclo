package com.isora.ciclo.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Builds a database exactly as version 1 created it (from the committed schema), opens it with the
 * current code and checks that the 1 to 2 migration keeps every row. Room validates the migrated
 * tables against the version 2 entities when it opens the file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun createVersion1(file: File) {
        // Unit tests run with the module directory as working directory.
        val schema = JSONObject(File("schemas/com.isora.ciclo.storage.CicloDatabase/1.json").readText()).getJSONObject("database")
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
        db.version = 1
        db.close()
    }

    @Test
    fun migratingFrom1To2KeepsEveryRowAndLeavesTheFollowUpEmpty() {
        val file = context.getDatabasePath("migration-test.db").also { it.parentFile?.mkdirs(); it.delete() }
        createVersion1(file)

        val db = Room.databaseBuilder(context, CicloDatabase::class.java, "migration-test.db")
            .addMigrations(CicloDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
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
}
