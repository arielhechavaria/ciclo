package com.isora.ciclo.mock

import com.isora.ciclo.dailylist.TextLine
import com.isora.ciclo.patients.Patient
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.ProtocolSeed
import com.isora.ciclo.protocols.Regimen
import com.isora.ciclo.treatments.SessionRef
import com.isora.ciclo.treatments.Treatment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A fictional patient, with the treatment they were assigned (none when [treatment] is null). */
data class MockCase(val patient: Patient, val protocolName: String?, val treatment: Treatment?)

/**
 * Fictional patients that exercise every feature of the app: active, finishing, postponed,
 * until-progression with reevaluation, finished early, finished, no treatment, and several
 * patients on the same day. Everything is placed relative to [today], so it always looks current.
 * Names are invented initials; none of this is real clinical data. Remove before the public release.
 */
object MockData {

    fun build(today: LocalDate): List<MockCase> {
        val nextTuesday = today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
        return listOf(
            // Session today, second cycle of four. Its identity card appears on the sample daily list.
            case("M.R.G.", "HC-1001", "Ca de mama HER2 negativo", "AC", "Alérgica al látex.", card = SAMPLE_CARD) {
                protocol("AC", today.minusDays(21))
            },
            // Last session of the treatment is today.
            case("L.P.S.", "HC-1002", "Ca de mama triple negativo", "AC dosis densa", card = "90020254321") {
                protocol("AC dosis densa", today.minusDays(42))
            },
            // Weekly protocol, many sessions.
            case("A.C.D.", "HC-1003", "Ca de mama luminal", "Paclitaxel semanal") { protocol("Paclitaxel semanal", today.minusDays(35)) },
            // Second session postponed three days.
            case("Y.F.O.", "HC-1004", "Ca de mama luminal B", "TC") {
                protocol("TC", today.minusDays(10)).let { it.postpone(SessionRef(2, 1), it.dateOf(SessionRef(2, 1)).plusDays(3)) }
            },
            case("R.T.B.", "HC-1005", "Ca de colon estadio III", "FOLFOX") { protocol("FOLFOX", today.minusDays(28)) },
            case("N.V.H.", "HC-1006", "Ca de recto", "CAPOX") { protocol("CAPOX", today.minusDays(63)) },
            // Finishes within four weeks.
            case("D.M.Q.", "HC-1007", "Ca de ovario", "Carboplatino + paclitaxel") { protocol("Carboplatino + paclitaxel", today.minusDays(100)) },
            // Two sessions per cycle, day 1 a week ago and day 8 today.
            case("S.E.L.", "HC-1008", "Ca de pulmón", "Gemcitabina + cisplatino") { protocol("Gemcitabina + cisplatino", today.minusDays(7)) },
            // Until progression: reevaluation starts a cycle today.
            case("O.A.P.", "HC-1009", "Ca de mama HER2 positivo", "Trastuzumab", "Reevaluar con ecocardiograma.") {
                Treatment(Regimen(21, listOf(1)), CycleCount.UntilProgression(3), today.minusDays(63))
            },
            // History: finished early, with its reason.
            case("C.J.N.", "HC-1010", "Ca de mama luminal", "AC") {
                protocol("AC", today.minusDays(50)).finishEarly(today.minusDays(8), "Toxicidad hematológica")
            },
            // History: finished on schedule.
            case("E.B.R.", "HC-1011", "Ca de mama luminal A", "TC") { protocol("TC", today.minusDays(120)) },
            // No treatment yet, with long notes.
            Patient(
                "María Fernanda Pérez Gómez",
                "HC-1012",
                "Adenocarcinoma de colon sigmoide",
                "Pendiente de comité oncológico. Traer resultados de patología y tomografía antes de asignar protocolo. Familiar de contacto: hija.",
            ).let { MockCase(it, null, null) },
            // Four patients start on the same day, to see a busy day in Mes.
            case("P.G.A.", "HC-1013", "Ca de mama", "AC") { protocol("AC", nextTuesday) },
            case("T.H.S.", "HC-1014", "Ca de mama", "AC") { protocol("AC", nextTuesday) },
            case("I.O.V.", "HC-1015", "Ca de colon", "FOLFOX") { protocol("FOLFOX", nextTuesday) },
            case("K.D.R.", "HC-1016", "Ca de ovario", "Carboplatino + paclitaxel") { protocol("Carboplatino + paclitaxel", nextTuesday) },
            // Starts in two weeks; no diagnosis or notes.
            Patient("Z.Q.T.", "HC-1017").let { MockCase(it, "Paclitaxel semanal", protocol("Paclitaxel semanal", today.plusDays(14))) },
        ).map { it.markPastSessionsGiven(today) }
    }

    private fun case(
        name: String,
        record: String,
        diagnosis: String,
        protocolName: String,
        notes: String? = null,
        card: String? = null,
        treatment: () -> Treatment,
    ) = MockCase(Patient(name, record, diagnosis, notes, card), protocolName, treatment())

    private const val SAMPLE_CARD = "75031512345"

    /**
     * A fictional daily list as OCR would return it: a title, a header and four rows, column by column.
     * Rows: M.R.G. by identity card; S.E.L. by name only (no card on file); a new patient on FOLFOX;
     * a new patient on a treatment that is not in the catalog.
     */
    fun sampleList(): List<TextLine> {
        val rows = listOf(
            Triple("M.R.G.", "750315 12345", "AC"),
            Triple("S.E.L.", "68042298765", "Gemcitabina + cisplatino"),
            Triple("Luis Fernández Rey", "70010111111", "FOLFOX"),
            Triple("Rosa Díaz Martín", "66050544332", "XELOX"),
        )
        fun line(text: String, left: Int, top: Int) = TextLine(text, left, top, left + 250, top + 30)
        val top = 200
        return listOf(line("Consulta de oncología", 40, 40), line("Nombre y apellidos", 40, 140), line("Carnet", 400, 141), line("Tratamiento", 700, 139)) +
            rows.mapIndexed { i, r -> line(r.first, 40, top + i * 60) } +
            rows.mapIndexed { i, r -> line(r.second, 400, top + i * 60 + 2) } +
            rows.mapIndexed { i, r -> line(r.third, 700, top + i * 60 - 1) }
    }

    private fun protocol(name: String, start: LocalDate): Treatment =
        Treatment.assign(ProtocolSeed.protocols.first { it.name == name }, start)

    private fun Treatment.dateOf(ref: SessionRef): LocalDate = sessions().first { it.ref == ref }.date

    /** Sessions before today were already given, so the history of each patient looks lived in. */
    private fun MockCase.markPastSessionsGiven(today: LocalDate): MockCase {
        val t = treatment ?: return this
        val past = t.sessions().takeWhile { it.date < today }.toList()
        return copy(treatment = past.fold(t) { acc, s -> acc.markGiven(s.ref) })
    }
}
