package com.isora.ciclo.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.isora.ciclo.patients.PatientDao
import com.isora.ciclo.patients.PatientEntity
import com.isora.ciclo.protocols.ProtocolDao
import com.isora.ciclo.protocols.ProtocolEntity
import com.isora.ciclo.treatments.SessionStateEntity
import com.isora.ciclo.treatments.TreatmentDao
import com.isora.ciclo.treatments.TreatmentEntity

/** Version 1 of the stored format. Any schema change after real data exists needs a migration. */
@Database(
    entities = [ProtocolEntity::class, PatientEntity::class, TreatmentEntity::class, SessionStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CicloDatabase : RoomDatabase() {
    abstract fun protocolDao(): ProtocolDao
    abstract fun patientDao(): PatientDao
    abstract fun treatmentDao(): TreatmentDao

    companion object {
        /** Opens the on-disk database encrypted with SQLCipher; [passphrase] comes from the Keystore-wrapped key. */
        fun open(context: Context, passphrase: ByteArray): CicloDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context.applicationContext, CicloDatabase::class.java, "ciclo.db")
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .build()
        }
    }
}
