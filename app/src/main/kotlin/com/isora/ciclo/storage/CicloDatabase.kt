package com.isora.ciclo.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.isora.ciclo.patients.PatientDao
import com.isora.ciclo.patients.PatientEntity
import com.isora.ciclo.protocols.ProtocolDao
import com.isora.ciclo.protocols.ProtocolEntity
import com.isora.ciclo.treatments.SessionStateEntity
import com.isora.ciclo.treatments.TreatmentDao
import com.isora.ciclo.treatments.TreatmentEntity

/** Version 2 of the stored format (2 adds the follow-up mark). Any schema change needs a migration. */
@Database(
    entities = [ProtocolEntity::class, PatientEntity::class, TreatmentEntity::class, SessionStateEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class CicloDatabase : RoomDatabase() {
    abstract fun protocolDao(): ProtocolDao
    abstract fun patientDao(): PatientDao
    abstract fun treatmentDao(): TreatmentDao

    companion object {
        /** Adds the nullable follow-up mark; existing rows keep every value and start with no follow-up done. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE treatments ADD COLUMN follow_up_done_epoch_day INTEGER")
            }
        }

        /** Opens the on-disk database encrypted with SQLCipher; [passphrase] comes from the Keystore-wrapped key. */
        fun open(context: Context, passphrase: ByteArray): CicloDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context.applicationContext, CicloDatabase::class.java, "ciclo.db")
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
