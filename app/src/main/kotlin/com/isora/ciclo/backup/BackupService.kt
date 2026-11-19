package com.isora.ciclo.backup

import android.database.sqlite.SQLiteException
import com.isora.ciclo.storage.CicloDatabase
import java.time.LocalDate
import java.util.concurrent.Callable

/** What a restore put back. */
data class RestoreSummary(val patients: Int, val treatments: Int, val protocols: Int)

/** Exports the whole database to a password-protected file and restores it. Blocking: call off the main thread. */
class BackupService(private val db: CicloDatabase, private val today: () -> LocalDate = { LocalDate.now() }) {

    fun export(password: String): ByteArray {
        val snapshot = db.runInTransaction(Callable {
            BackupSnapshot(
                protocols = db.protocolDao().all(),
                patients = db.patientDao().all(),
                treatments = db.treatmentDao().all(),
                sessionStates = db.treatmentDao().allStates(),
            )
        })
        return BackupCrypto.seal(BackupJson.encode(snapshot, today()).toByteArray(Charsets.UTF_8), password)
    }

    /**
     * Replaces every current row by the ones in [file]. The file is fully decrypted and validated before
     * anything is touched, and the replacement is one transaction: any failure leaves the data as it was.
     *
     * @throws BackupFormatException @throws BackupPasswordException
     */
    fun restore(file: ByteArray, password: String): RestoreSummary {
        val snapshot = BackupJson.decode(String(BackupCrypto.open(file, password), Charsets.UTF_8))
        try {
            db.runInTransaction(Runnable {
                // Children first: treatments restrict deleting their patient.
                db.treatmentDao().deleteAllStates()
                db.treatmentDao().deleteAll()
                db.patientDao().deleteAll()
                db.protocolDao().deleteAll()
                db.protocolDao().insertAll(snapshot.protocols)
                db.patientDao().insertAll(snapshot.patients)
                db.treatmentDao().insertAll(snapshot.treatments)
                db.treatmentDao().insertStates(snapshot.sessionStates)
            })
        } catch (e: SQLiteException) {
            throw BackupFormatException("The backup content is inconsistent")
        }
        return RestoreSummary(snapshot.patients.size, snapshot.treatments.size, snapshot.protocols.size)
    }
}
