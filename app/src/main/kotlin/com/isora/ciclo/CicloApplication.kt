package com.isora.ciclo

import android.app.Application
import com.isora.ciclo.patients.RoomPatientRepository
import com.isora.ciclo.protocols.RoomProtocolCatalog
import com.isora.ciclo.security.DatabaseKey
import com.isora.ciclo.settings.AppSettings
import com.isora.ciclo.storage.CicloDatabase
import com.isora.ciclo.treatments.RoomTreatmentRepository

/** Wires the encrypted database and the repositories; shared by the activity and the alarm receivers. */
class AppContainer(application: Application) {
    val database: CicloDatabase by lazy { CicloDatabase.open(application, DatabaseKey.passphrase(application)) }
    val catalog by lazy { RoomProtocolCatalog(database.protocolDao()) }
    val patients by lazy { RoomPatientRepository(database.patientDao()) }
    val treatments by lazy { RoomTreatmentRepository(database.treatmentDao()) }
    val settings = AppSettings(application)
}

class CicloApplication : Application() {
    val container by lazy { AppContainer(this) }
}
