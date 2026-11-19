package com.isora.ciclo.storage

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CicloDatabase::class.java)

    @Test
    fun migratingFrom1To2KeepsEveryRowAndLeavesTheFollowUpEmpty() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("INSERT INTO patients (id, name, clinical_record_number) VALUES (1, 'Ana', 'HC-1')")
            execSQL(
                "INSERT INTO treatments (id, patient_id, protocol_name, cycle_length_days, session_days, cycle_mode, " +
                    "cycle_value, start_epoch_day, finish_epoch_day, finish_reason) " +
                    "VALUES (7, 1, 'AC', 21, '1', 'FIXED', 4, 20000, NULL, NULL)",
            )
            execSQL("INSERT INTO session_states (treatment_id, cycle, session, given, delay_days) VALUES (7, 1, 1, 1, 3)")
            close()
        }

        val db = helper.runMigrationsAndValidate("migration-test", 2, true, CicloDatabase.MIGRATION_1_2)

        db.query("SELECT id, protocol_name, start_epoch_day, follow_up_done_epoch_day FROM treatments").use {
            assertTrue(it.moveToFirst())
            assertEquals(7L, it.getLong(0))
            assertEquals("AC", it.getString(1))
            assertEquals(20000L, it.getLong(2))
            assertTrue(it.isNull(3))
        }
        db.query("SELECT delay_days FROM session_states WHERE treatment_id = 7").use {
            assertTrue(it.moveToFirst())
            assertEquals(3L, it.getLong(0))
        }
        db.query("SELECT name FROM patients").use {
            assertTrue(it.moveToFirst())
            assertEquals("Ana", it.getString(0))
        }
    }
}
