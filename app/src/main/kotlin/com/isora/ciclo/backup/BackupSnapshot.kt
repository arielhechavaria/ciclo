package com.isora.ciclo.backup

import com.isora.ciclo.patients.PatientEntity
import com.isora.ciclo.protocols.ProtocolEntity
import com.isora.ciclo.storage.cycleCountOf
import com.isora.ciclo.storage.regimenOf
import com.isora.ciclo.treatments.SessionStateEntity
import com.isora.ciclo.treatments.TreatmentEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.DateTimeException
import java.time.LocalDate

/** Every stored row, with its ids, so a restore reproduces the database exactly. */
data class BackupSnapshot(
    val protocols: List<ProtocolEntity>,
    val patients: List<PatientEntity>,
    val treatments: List<TreatmentEntity>,
    val sessionStates: List<SessionStateEntity>,
) {
    /** @throws BackupFormatException when rows point to missing rows or hold values the app cannot read. */
    fun validate() {
        try {
            val patientIds = patients.map { it.id }.toSet()
            val treatmentIds = treatments.map { it.id }.toSet()
            require(patientIds.size == patients.size && treatmentIds.size == treatments.size) { "Repeated ids" }
            require(protocols.map { it.id }.toSet().size == protocols.size) { "Repeated ids" }
            require(treatments.all { it.patientId in patientIds }) { "A treatment has no patient" }
            require(sessionStates.all { it.treatmentId in treatmentIds }) { "A session has no treatment" }
            treatments.forEach {
                LocalDate.ofEpochDay(it.startEpochDay)
                it.finishEpochDay?.let(LocalDate::ofEpochDay)
                it.followUpDoneEpochDay?.let(LocalDate::ofEpochDay)
            }
            protocols.forEach { regimenOf(it.cycleLengthDays, it.sessionDays); cycleCountOf(it.cycleMode, it.cycleValue) }
            treatments.forEach { regimenOf(it.cycleLengthDays, it.sessionDays); cycleCountOf(it.cycleMode, it.cycleValue) }
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("The backup content is inconsistent")
        } catch (e: IllegalStateException) {
            throw BackupFormatException("The backup content is inconsistent")
        } catch (e: DateTimeException) {
            throw BackupFormatException("The backup content is inconsistent")
        } catch (e: NumberFormatException) {
            throw BackupFormatException("The backup content is inconsistent")
        }
    }
}

/** JSON form of a [BackupSnapshot]. Field names are part of the backup format; keep old ones readable. */
object BackupJson {
    const val FORMAT = 1

    fun encode(snapshot: BackupSnapshot, createdOn: LocalDate): String = JSONObject()
        .put("format", FORMAT)
        .put("createdOn", createdOn.toString())
        .put("protocols", JSONArray(snapshot.protocols.map {
            JSONObject().put("id", it.id).put("name", it.name).put("cycleLengthDays", it.cycleLengthDays)
                .put("sessionDays", it.sessionDays).put("cycleMode", it.cycleMode).put("cycleValue", it.cycleValue)
                .put("archived", it.archived)
        }))
        .put("patients", JSONArray(snapshot.patients.map {
            JSONObject().put("id", it.id).put("name", it.name).put("clinicalRecordNumber", it.clinicalRecordNumber)
                .put("diagnosis", it.diagnosis ?: JSONObject.NULL).put("notes", it.notes ?: JSONObject.NULL)
        }))
        .put("treatments", JSONArray(snapshot.treatments.map {
            JSONObject().put("id", it.id).put("patientId", it.patientId).put("protocolName", it.protocolName)
                .put("cycleLengthDays", it.cycleLengthDays).put("sessionDays", it.sessionDays)
                .put("cycleMode", it.cycleMode).put("cycleValue", it.cycleValue)
                .put("startEpochDay", it.startEpochDay)
                .put("finishEpochDay", it.finishEpochDay ?: JSONObject.NULL)
                .put("finishReason", it.finishReason ?: JSONObject.NULL)
                .put("followUpDoneEpochDay", it.followUpDoneEpochDay ?: JSONObject.NULL)
        }))
        .put("sessionStates", JSONArray(snapshot.sessionStates.map {
            JSONObject().put("treatmentId", it.treatmentId).put("cycle", it.cycle).put("session", it.session)
                .put("given", it.given).put("delayDays", it.delayDays ?: JSONObject.NULL)
        }))
        .toString()

    /** @throws BackupFormatException when the text is not a valid, readable backup of a supported version. */
    fun decode(text: String): BackupSnapshot {
        try {
            val root = JSONObject(text)
            val format = root.getInt("format")
            if (format != FORMAT) throw BackupFormatException("The backup was made by a newer version of the app")
            val snapshot = BackupSnapshot(
                protocols = root.getJSONArray("protocols").objects().map {
                    ProtocolEntity(
                        id = it.getLong("id"), name = it.getString("name"),
                        cycleLengthDays = it.getInt("cycleLengthDays"), sessionDays = it.getString("sessionDays"),
                        cycleMode = it.getString("cycleMode"), cycleValue = it.getInt("cycleValue"),
                        archived = it.getBoolean("archived"),
                    )
                },
                patients = root.getJSONArray("patients").objects().map {
                    PatientEntity(
                        id = it.getLong("id"), name = it.getString("name"),
                        clinicalRecordNumber = it.getString("clinicalRecordNumber"),
                        diagnosis = it.text("diagnosis"), notes = it.text("notes"),
                    )
                },
                treatments = root.getJSONArray("treatments").objects().map {
                    TreatmentEntity(
                        id = it.getLong("id"), patientId = it.getLong("patientId"),
                        protocolName = it.getString("protocolName"),
                        cycleLengthDays = it.getInt("cycleLengthDays"), sessionDays = it.getString("sessionDays"),
                        cycleMode = it.getString("cycleMode"), cycleValue = it.getInt("cycleValue"),
                        startEpochDay = it.getLong("startEpochDay"),
                        finishEpochDay = if (it.isNull("finishEpochDay")) null else it.getLong("finishEpochDay"),
                        finishReason = it.text("finishReason"),
                        // Absent in backups made before follow-up existed.
                        followUpDoneEpochDay = if (it.isNull("followUpDoneEpochDay")) null else it.getLong("followUpDoneEpochDay"),
                    )
                },
                sessionStates = root.getJSONArray("sessionStates").objects().map {
                    SessionStateEntity(
                        treatmentId = it.getLong("treatmentId"), cycle = it.getInt("cycle"),
                        session = it.getInt("session"), given = it.getBoolean("given"),
                        delayDays = if (it.isNull("delayDays")) null else it.getLong("delayDays"),
                    )
                },
            )
            snapshot.validate()
            return snapshot
        } catch (e: JSONException) {
            throw BackupFormatException("The backup content is not readable")
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.text(key: String): String? = if (isNull(key)) null else getString(key)
}
