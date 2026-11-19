package com.isora.ciclo.dailylist

import com.isora.ciclo.patients.Patient
import com.isora.ciclo.patients.PatientConflictException
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.patients.PatientRepository
import com.isora.ciclo.patients.StoredPatient
import com.isora.ciclo.patients.identityCardKey
import com.isora.ciclo.treatments.TreatmentRepository
import java.text.Normalizer
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/** One line of text found by OCR on the photo, with its box in image pixels. */
data class TextLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val height: Int get() = bottom - top
}

/** One patient row of the daily list: name and surnames, identity card and treatment, as read. */
data class ListEntry(val name: String, val identityCard: String?, val treatment: String?)

/** What was read from the photo: patient rows, and the rows that could not be read as a patient. */
data class DailyListReading(val entries: List<ListEntry>, val unread: List<String>)

/**
 * Reads a daily patient list. Rows are rebuilt from line positions, because OCR returns a table column by
 * column. Each row is expected as "name and surnames · identity card · treatment"; rows with no 11-digit
 * card or no name before it (titles, headers, unreadable rows) are returned as unread.
 */
object DailyListReader {
    // 11 digits, optionally separated by single spaces, not touching other digits.
    private val card = Regex("(?<![0-9])[0-9](?: ?[0-9]){10}(?![0-9])")
    private val numbering = Regex("^\\s*\\d{1,3}\\s*[.)\\-]\\s*")
    private val edges = Regex("^[\\s|,;:\\-–]+|[\\s|,;:\\-–]+$")
    private val spaces = Regex("\\s+")

    /** Lines that overlap vertically by at least half the smaller height form one row, read left to right. */
    fun rows(lines: List<TextLine>): List<String> {
        val rows = mutableListOf<MutableList<TextLine>>()
        lines.filter { it.text.isNotBlank() }.sortedBy { it.top + it.bottom }.forEach { line ->
            val row = rows.lastOrNull()
            if (row != null && sameRow(row.first(), line)) row += line else rows += mutableListOf(line)
        }
        return rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    }

    private fun sameRow(a: TextLine, b: TextLine): Boolean {
        val overlap = min(a.bottom, b.bottom) - max(a.top, b.top)
        return overlap * 2 >= min(a.height, b.height)
    }

    fun read(lines: List<TextLine>): DailyListReading {
        val entries = mutableListOf<ListEntry>()
        val unread = mutableListOf<String>()
        rows(lines).forEach { row ->
            val entry = parseRow(row)
            if (entry != null) entries += entry else unread += row
        }
        return DailyListReading(entries, unread)
    }

    private fun parseRow(row: String): ListEntry? {
        val found = card.find(row) ?: return null
        val name = clean(row.substring(0, found.range.first).replaceFirst(numbering, ""))
        if (name.isEmpty()) return null
        val treatment = clean(row.substring(found.range.last + 1)).takeIf { it.isNotEmpty() }
        return ListEntry(name, identityCardKey(found.value), treatment)
    }

    private fun clean(text: String): String = text.replace(edges, "").replace(spaces, " ")
}

/** How a list row relates to the stored patients. */
enum class MatchKind {
    /** Same identity card: the same patient. */
    CARD,

    /** No card match, but the same name: probably the same patient; the user confirms. */
    NAME,

    /** Not registered yet. */
    NEW,
}

/** A list row ready for review: the patient it matches (none when new) and the catalog protocol of its treatment. */
data class ReviewRow(val entry: ListEntry, val patient: StoredPatient?, val kind: MatchKind, val protocolName: String?)

object DailyListMatcher {
    fun match(entries: List<ListEntry>, patients: List<StoredPatient>, protocolNames: List<String>): List<ReviewRow> =
        entries.map { entry ->
            val protocol = entry.treatment?.let { matchProtocol(it, protocolNames) }
            val byCard = entry.identityCard?.let { card -> patients.firstOrNull { it.patient.identityCard == card } }
            if (byCard != null) return@map ReviewRow(entry, byCard, MatchKind.CARD, protocol)
            // A patient whose card on file differs from the row's is a different person with the same name.
            val byName = patients.filter {
                sameName(it.patient.name, entry.name) && (it.patient.identityCard == null || entry.identityCard == null)
            }.singleOrNull()
            if (byName != null) ReviewRow(entry, byName, MatchKind.NAME, protocol)
            else ReviewRow(entry, null, MatchKind.NEW, protocol)
        }
}

/**
 * The catalog protocol named in [treatment], ignoring case, accents and spacing. With extra words around it
 * ("AC ciclo 2"), the longest protocol name found as whole words wins; null when none is found.
 */
fun matchProtocol(treatment: String, protocolNames: List<String>): String? {
    val text = normalized(treatment)
    if (text.isEmpty()) return null
    protocolNames.firstOrNull { normalized(it) == text }?.let { return it }
    return protocolNames
        .filter { normalized(it).isNotEmpty() && " $text ".contains(" ${normalized(it)} ") }
        .maxByOrNull { normalized(it).length }
}

/** Two names are the same when they differ only in case, accents, punctuation or spacing. */
fun sameName(a: String, b: String): Boolean = normalized(a).let { it.isNotEmpty() && it == normalized(b) }

private val marks = Regex("\\p{Mn}+")
private val separators = Regex("[^a-z0-9]+")

private fun normalized(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(marks, "").lowercase().replace(separators, " ").trim()

/** What confirming a reviewed row does. */
sealed interface ListAction {
    /** Fill the identity card the patient lacked ([fillCard], null to keep it) and maybe mark today's session given. */
    data class UpdateExisting(val patient: StoredPatient, val fillCard: String?, val markToday: Boolean) : ListAction

    /** Open the patient form prefilled with [entry], then assign [protocolName] when it is known. */
    data class RegisterNew(val entry: ListEntry, val protocolName: String?) : ListAction
}

/**
 * The user's choices for one row. A probable name match counts as the same patient only when the user says so
 * ([samePerson]); otherwise the row registers a new patient. [markToday] applies to existing patients only.
 */
data class ReviewDecision(
    val row: ReviewRow,
    val include: Boolean = true,
    val samePerson: Boolean = false,
    val markToday: Boolean = true,
) {
    fun action(): ListAction? {
        if (!include) return null
        val patient = row.patient
        val existing = patient != null && (row.kind == MatchKind.CARD || (row.kind == MatchKind.NAME && samePerson))
        if (!existing || patient == null) return ListAction.RegisterNew(row.entry, row.protocolName)
        val fillCard = row.entry.identityCard.takeIf { patient.patient.identityCard == null }
        return ListAction.UpdateExisting(patient, fillCard, markToday)
    }
}

/** Applies the confirmed changes to patients already registered. */
class DailyListApplier(private val patients: PatientRepository, private val treatments: TreatmentRepository) {

    /**
     * Fills missing identity cards and marks today's planned session given where asked.
     * Returns the actions whose card could not be saved because another patient already holds it;
     * their session is still marked.
     */
    fun apply(actions: List<ListAction.UpdateExisting>, today: LocalDate): List<ListAction.UpdateExisting> {
        val refused = mutableListOf<ListAction.UpdateExisting>()
        actions.forEach { action ->
            action.fillCard?.let { card ->
                val current = patients.findById(action.patient.id)?.patient ?: action.patient.patient
                try {
                    patients.update(
                        action.patient.id,
                        Patient(current.name, current.clinicalRecordNumber, current.diagnosis, current.notes, card),
                    )
                } catch (e: PatientConflictException) {
                    refused += action
                }
            }
            if (action.markToday) markToday(action.patient.id, today)
        }
        return refused
    }

    private fun markToday(patientId: PatientId, today: LocalDate) {
        treatments.forPatient(patientId).forEach { stored ->
            // Until-progression treatments have endless sessions: stop at today.
            val session = stored.treatment.sessions().takeWhile { it.date <= today }
                .firstOrNull { it.date == today && !it.given } ?: return@forEach
            treatments.save(stored.copy(treatment = stored.treatment.markGiven(session.ref)))
        }
    }
}
