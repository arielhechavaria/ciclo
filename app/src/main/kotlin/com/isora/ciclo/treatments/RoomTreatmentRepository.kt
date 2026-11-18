package com.isora.ciclo.treatments

import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.storage.cycleCountOf
import com.isora.ciclo.storage.mode
import com.isora.ciclo.storage.regimenOf
import com.isora.ciclo.storage.toColumn
import com.isora.ciclo.storage.value
import java.time.LocalDate

class RoomTreatmentRepository(private val dao: TreatmentDao) : TreatmentRepository {

    override fun all(): List<StoredTreatment> = dao.all().map { load(it) }

    override fun forPatient(patientId: PatientId): List<StoredTreatment> =
        dao.forPatient(patientId.value).map { load(it) }

    override fun findById(id: TreatmentId): StoredTreatment? = dao.findById(id.value)?.let { load(it) }

    override fun assign(patientId: PatientId, protocolName: String, treatment: Treatment): StoredTreatment {
        val entity = treatment.toEntity(id = 0, patientId = patientId.value, protocolName = protocolName)
        val id = dao.create(entity, treatment.toStates(treatmentId = 0))
        return StoredTreatment(TreatmentId(id), patientId, protocolName, treatment)
    }

    override fun save(stored: StoredTreatment) {
        val entity = stored.treatment.toEntity(stored.id.value, stored.patientId.value, stored.protocolName)
        dao.replace(entity, stored.treatment.toStates(stored.id.value))
    }

    private fun load(entity: TreatmentEntity): StoredTreatment {
        val states = dao.statesOf(entity.id)
        val treatment = Treatment(
            regimen = regimenOf(entity.cycleLengthDays, entity.sessionDays),
            cycleCount = cycleCountOf(entity.cycleMode, entity.cycleValue),
            startDate = LocalDate.ofEpochDay(entity.startEpochDay),
            postponements = states.filter { it.delayDays != null }
                .associate { SessionRef(it.cycle, it.session) to it.delayDays!! },
            givenSessions = states.filter { it.given }.map { SessionRef(it.cycle, it.session) }.toSet(),
            earlyFinish = entity.finishEpochDay?.let {
                EarlyFinish(LocalDate.ofEpochDay(it), entity.finishReason.orEmpty())
            },
        )
        return StoredTreatment(TreatmentId(entity.id), PatientId(entity.patientId), entity.protocolName, treatment)
    }
}

private fun Treatment.toEntity(id: Long, patientId: Long, protocolName: String) = TreatmentEntity(
    id = id,
    patientId = patientId,
    protocolName = protocolName,
    cycleLengthDays = regimen.cycleLengthDays,
    sessionDays = regimen.sessionDays.toColumn(),
    cycleMode = cycleCount.mode(),
    cycleValue = cycleCount.value(),
    startEpochDay = startDate.toEpochDay(),
    finishEpochDay = earlyFinish?.date?.toEpochDay(),
    finishReason = earlyFinish?.reason,
)

private fun Treatment.toStates(treatmentId: Long): List<SessionStateEntity> =
    (postponements.keys + givenSessions).map { ref ->
        SessionStateEntity(
            treatmentId = treatmentId,
            cycle = ref.cycle,
            session = ref.session,
            given = ref in givenSessions,
            delayDays = postponements[ref],
        )
    }
