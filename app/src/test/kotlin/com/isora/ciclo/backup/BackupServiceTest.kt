package com.isora.ciclo.backup

import com.isora.ciclo.patients.PatientEntity
import com.isora.ciclo.patients.RoomPatientRepository
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.RoomProtocolCatalog
import com.isora.ciclo.storage.StorageTestBase
import com.isora.ciclo.treatments.RoomTreatmentRepository
import com.isora.ciclo.treatments.SessionRef
import com.isora.ciclo.treatments.Treatment
import com.isora.ciclo.patients.Patient
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupServiceTest : StorageTestBase() {
    private val password = "clave-segura-1"
    private val day = LocalDate.of(2026, 11, 19)
    private val service get() = BackupService(db) { day }
    private val patients get() = RoomPatientRepository(db.patientDao())
    private val treatments get() = RoomTreatmentRepository(db.treatmentDao())
    private val catalog get() = RoomProtocolCatalog(db.protocolDao())

    private fun fill() {
        catalog.add(Protocol("FOLFOX", Regimen(14, listOf(1)), CycleCount.Fixed(12)))
        val ana = patients.add(Patient("Ana", "HC1", diagnosis = "Colon", notes = "alérgica"))
        patients.add(Patient("Beto", "HC2"))
        val treatment = Treatment.assign(catalog.all().first().protocol, day)
            .postpone(SessionRef(1, 1), day.plusDays(3)).markGiven(SessionRef(2, 1))
            .finishEarly(day.plusDays(60), "progresión")
        treatments.assign(ana.id, "FOLFOX", treatment)
    }

    @Test
    fun `restoring an export on an empty database brings back every row`() {
        fill()
        val file = service.export(password)
        val before = Triple(patients.all(), treatments.all(), catalog.all(includeArchived = true))

        db.clearAllTables()
        val summary = service.restore(file, password)

        assertEquals(RestoreSummary(patients = 2, treatments = 1, protocols = 1), summary)
        assertEquals(before.first, patients.all())
        assertEquals(before.second.map { it.id to it.treatment.sessions().toList() },
            treatments.all().map { it.id to it.treatment.sessions().toList() })
        assertEquals(before.second.single().treatment.earlyFinish, treatments.all().single().treatment.earlyFinish)
        assertEquals(before.third, catalog.all(includeArchived = true))
    }

    @Test
    fun `restoring replaces what was there before`() {
        fill()
        val file = service.export(password)
        patients.add(Patient("Carla", "HC3"))

        service.restore(file, password)

        assertEquals(listOf("Ana", "Beto"), patients.all().map { it.patient.name })
    }

    @Test
    fun `new rows after a restore get fresh ids`() {
        fill()
        val file = service.export(password)
        service.restore(file, password)

        val added = patients.add(Patient("Carla", "HC3"))

        assertTrue(added.id.value > patients.all().filter { it.patient.name != "Carla" }.maxOf { it.id.value })
    }

    @Test
    fun `a wrong password leaves the data untouched`() {
        fill()
        val file = service.export(password)
        patients.add(Patient("Carla", "HC3"))

        assertThrows(BackupPasswordException::class.java) { service.restore(file, "otra-clave-12") }

        assertEquals(3, patients.all().size)
    }

    @Test
    fun `a file that is not a backup leaves the data untouched`() {
        fill()

        assertThrows(BackupFormatException::class.java) { service.restore("hola".toByteArray(), password) }

        assertEquals(2, patients.all().size)
    }

    @Test
    fun `a backup whose rows break the database rules leaves the data untouched`() {
        fill()
        val duplicated = BackupSnapshot(
            protocols = emptyList(),
            patients = listOf(PatientEntity(1, "X", "HC9", null, null), PatientEntity(2, "Y", "HC9", null, null)),
            treatments = emptyList(),
            sessionStates = emptyList(),
        )
        val file = BackupCrypto.seal(BackupJson.encode(duplicated, day).toByteArray(), password)

        assertThrows(BackupFormatException::class.java) { service.restore(file, password) }

        assertEquals(listOf("Ana", "Beto"), patients.all().map { it.patient.name })
        assertNotNull(treatments.all().singleOrNull())
    }

    @Test
    fun `a backup from a newer format version is refused`() {
        val newer = """{"format":2,"protocols":[],"patients":[],"treatments":[],"sessionStates":[]}"""
        val file = BackupCrypto.seal(newer.toByteArray(), password)

        assertThrows(BackupFormatException::class.java) { service.restore(file, password) }
    }

    @Test
    fun `a backup with a treatment pointing to a missing patient is refused`() {
        val text = BackupJson.encode(BackupSnapshot(emptyList(), emptyList(), emptyList(), emptyList()), day)
            .replace("\"treatments\":[]", """"treatments":[{"id":1,"patientId":99,"protocolName":"X","cycleLengthDays":14,"sessionDays":"1","cycleMode":"FIXED","cycleValue":6,"startEpochDay":1,"finishEpochDay":null,"finishReason":null}]""")
        val file = BackupCrypto.seal(text.toByteArray(), password)

        assertThrows(BackupFormatException::class.java) { service.restore(file, password) }
    }

    @Test
    fun `a backup with a date the app cannot read is refused and leaves the data untouched`() {
        fill()
        val text = BackupJson.encode(
            BackupSnapshot(
                protocols = emptyList(),
                patients = listOf(PatientEntity(1, "X", "HC9", null, null)),
                treatments = emptyList(),
                sessionStates = emptyList(),
            ),
            day,
        ).replace("\"treatments\":[]", """"treatments":[{"id":1,"patientId":1,"protocolName":"X","cycleLengthDays":14,"sessionDays":"1","cycleMode":"FIXED","cycleValue":6,"startEpochDay":9999999999999,"finishEpochDay":null,"finishReason":null}]""")
        val file = BackupCrypto.seal(text.toByteArray(), password)

        assertThrows(BackupFormatException::class.java) { service.restore(file, password) }

        assertEquals(listOf("Ana", "Beto"), patients.all().map { it.patient.name })
    }

    @Test
    fun `an export of an empty database restores to an empty database`() {
        val file = service.export(password)
        patients.add(Patient("Ana", "HC1"))

        service.restore(file, password)

        assertTrue(patients.all().isEmpty())
    }

    @Test
    fun `the follow-up mark survives an export and a restore`() {
        fill()
        val stored = treatments.all().single()
        treatments.save(stored.copy(treatment = stored.treatment.markFollowUpDone(day.plusDays(200))))
        val file = service.export(password)

        db.clearAllTables()
        service.restore(file, password)

        assertEquals(day.plusDays(200), treatments.all().single().treatment.followUpDoneOn)
    }

    @Test
    fun `a backup made before follow-up existed still restores with no follow-up done`() {
        fill()
        val json = org.json.JSONObject(BackupJson.encode(
            BackupSnapshot(
                emptyList(),
                db.patientDao().all(), db.treatmentDao().all(), db.treatmentDao().allStates(),
            ),
            day,
        ))
        json.getJSONArray("treatments").let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).remove("followUpDoneEpochDay")
        }

        val decoded = BackupJson.decode(json.toString())

        assertEquals(null, decoded.treatments.single().followUpDoneEpochDay)
    }

    @Test
    fun `the identity card survives an export and a restore`() {
        patients.add(Patient("Ana", "HC1", identityCard = "85010112345"))
        val file = service.export(password)

        db.clearAllTables()
        service.restore(file, password)

        assertEquals("85010112345", patients.all().single().patient.identityCard)
    }

    @Test
    fun `a backup made before the identity card existed still restores with no card`() {
        fill()
        val json = org.json.JSONObject(BackupJson.encode(
            BackupSnapshot(emptyList(), db.patientDao().all(), emptyList(), emptyList()),
            day,
        ))
        json.getJSONArray("patients").let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).remove("identityCard")
        }

        val decoded = BackupJson.decode(json.toString())

        assertEquals(listOf(null, null), decoded.patients.map { it.identityCard })
    }

    @Test
    fun `a backup with a repeated identity card is refused and leaves the data untouched`() {
        fill()
        val repeated = BackupSnapshot(
            protocols = emptyList(),
            patients = listOf(
                PatientEntity(1, "X", "HC8", null, null, "85010112345"),
                PatientEntity(2, "Y", "HC9", null, null, "85010112345"),
            ),
            treatments = emptyList(),
            sessionStates = emptyList(),
        )
        val file = BackupCrypto.seal(BackupJson.encode(repeated, day).toByteArray(), password)

        assertThrows(BackupFormatException::class.java) { service.restore(file, password) }

        assertEquals(listOf("Ana", "Beto"), patients.all().map { it.patient.name })
    }

    @Test
    fun `a backup with a malformed identity card is refused`() {
        val bad = BackupSnapshot(emptyList(), listOf(PatientEntity(1, "X", "HC8", null, null, "12A")), emptyList(), emptyList())

        assertThrows(BackupFormatException::class.java) { BackupJson.decode(BackupJson.encode(bad, day)) }
    }
}
