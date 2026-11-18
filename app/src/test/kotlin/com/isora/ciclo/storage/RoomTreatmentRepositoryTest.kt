package com.isora.ciclo.storage

import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.RoomPatientRepository
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.Regimen
import com.isora.ciclo.protocols.RoomProtocolCatalog
import com.isora.ciclo.treatments.RoomTreatmentRepository
import com.isora.ciclo.treatments.SessionRef
import com.isora.ciclo.treatments.Treatment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RoomTreatmentRepositoryTest : StorageTestBase() {
    private val patients by lazy { RoomPatientRepository(db.patientDao()) }
    private val catalog by lazy { RoomProtocolCatalog(db.protocolDao()) }
    private val treatments by lazy { RoomTreatmentRepository(db.treatmentDao()) }

    private val start = LocalDate.of(2026, 11, 3)
    private val ac = Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4))

    @Test
    fun assignedTreatmentGeneratesTheSameCalendarAfterReloading() {
        val patient = patients.add(Patient("Alberto R.", "10234"))
        val stored = treatments.assign(patient.id, ac.name, Treatment.assign(ac, start))
        val loaded = treatments.findById(stored.id)!!.treatment
        assertEquals(LocalDate.of(2027, 1, 26), loaded.endDate)
        assertEquals(
            Treatment.assign(ac, start).sessions().map { it.date }.toList(),
            loaded.sessions().map { it.date }.toList(),
        )
    }

    @Test
    fun postponementsGivenSessionsAndEarlyFinishArePersisted() {
        val patient = patients.add(Patient("Alberto R.", "10234"))
        val stored = treatments.assign(patient.id, ac.name, Treatment.assign(ac, start))
        val changed = stored.treatment
            .markGiven(SessionRef(1, 1))
            .postpone(SessionRef(2, 1), LocalDate.of(2026, 12, 1))
            .finishEarly(LocalDate.of(2027, 1, 5), "Toxicidad")
        treatments.save(stored.copy(treatment = changed))

        val loaded = treatments.findById(stored.id)!!.treatment
        assertEquals(mapOf(SessionRef(2, 1) to 7L), loaded.postponements)
        assertEquals(setOf(SessionRef(1, 1)), loaded.givenSessions)
        assertEquals(LocalDate.of(2027, 1, 5), loaded.earlyFinish?.date)
        assertEquals("Toxicidad", loaded.earlyFinish?.reason)
    }

    @Test
    fun savingAgainReplacesPreviousSessionState() {
        val patient = patients.add(Patient("Alberto R.", "10234"))
        val stored = treatments.assign(patient.id, ac.name, Treatment.assign(ac, start))
        val postponed = stored.treatment.postpone(SessionRef(2, 1), LocalDate.of(2026, 12, 1))
        treatments.save(stored.copy(treatment = postponed))
        treatments.save(stored.copy(treatment = Treatment.assign(ac, start)))
        assertEquals(emptyMap<SessionRef, Long>(), treatments.findById(stored.id)!!.treatment.postponements)
    }

    @Test
    fun untilProgressionTreatmentRoundTrips() {
        val patient = patients.add(Patient("Alberto R.", "10234"))
        val protocol = Protocol("Trastuzumab", Regimen(21, listOf(1)), CycleCount.UntilProgression(4))
        val stored = treatments.assign(patient.id, protocol.name, Treatment.assign(protocol, start))
        val loaded = treatments.findById(stored.id)!!.treatment
        assertEquals(CycleCount.UntilProgression(4), loaded.cycleCount)
        assertNull(loaded.endDate)
    }

    @Test
    fun overriddenCycleCountIsKept() {
        val patient = patients.add(Patient("Alberto R.", "10234"))
        val stored = treatments.assign(patient.id, ac.name, Treatment.assign(ac, start, CycleCount.Fixed(6)))
        assertEquals(CycleCount.Fixed(6), treatments.findById(stored.id)!!.treatment.cycleCount)
    }

    @Test
    fun editingTheCatalogProtocolDoesNotChangeAnAssignedTreatment() {
        val patient = patients.add(Patient("Alberto R.", "10234"))
        val entry = catalog.add(ac)
        val stored = treatments.assign(patient.id, ac.name, Treatment.assign(entry.protocol, start))

        catalog.update(entry.id, Protocol("AC", Regimen(14, listOf(1, 5)), CycleCount.Fixed(8)))

        val loaded = treatments.findById(stored.id)!!
        assertEquals(Regimen(21, listOf(1)), loaded.treatment.regimen)
        assertEquals(CycleCount.Fixed(4), loaded.treatment.cycleCount)
        assertEquals("AC", loaded.protocolName)
    }

    @Test
    fun listsTreatmentsOfAPatient() {
        val alberto = patients.add(Patient("Alberto R.", "10234"))
        val maria = patients.add(Patient("Maria L.", "20999"))
        treatments.assign(alberto.id, ac.name, Treatment.assign(ac, start))
        treatments.assign(maria.id, ac.name, Treatment.assign(ac, start.plusDays(7)))
        assertEquals(1, treatments.forPatient(alberto.id).size)
        assertEquals(2, treatments.all().size)
    }
}
