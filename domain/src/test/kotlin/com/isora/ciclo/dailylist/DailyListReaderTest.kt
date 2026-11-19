package com.isora.ciclo.dailylist

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DailyListReaderTest {
    /** A line [height] pixels tall starting at [top], from x = [left]. */
    private fun line(text: String, left: Int, top: Int, height: Int = 20) = TextLine(text, left, top, left + 100, top + height)

    @Test
    fun `rows are rebuilt from columns read one after another`() {
        // OCR returns a table column by column: every name, then every card, then every treatment.
        val lines = listOf(
            line("Ana Pérez", 10, 100), line("Beto Ruiz", 10, 140),
            line("85010112345", 300, 102), line("90020254321", 300, 141),
            line("AC", 600, 99), line("FOLFOX", 600, 139),
        )
        assertEquals(listOf("Ana Pérez 85010112345 AC", "Beto Ruiz 90020254321 FOLFOX"), DailyListReader.rows(lines))
    }

    @Test
    fun `a row reads name, identity card and treatment in that order`() {
        val reading = DailyListReader.read(listOf(line("1. María Pérez Gómez 85010112345 AC dosis densa", 0, 0)))
        assertEquals(listOf(ListEntry("María Pérez Gómez", "85010112345", "AC dosis densa")), reading.entries)
    }

    @Test
    fun `an identity card written with spaces is read without them`() {
        val reading = DailyListReader.read(listOf(line("Ana Pérez 850101 12345 FOLFOX", 0, 0)))
        assertEquals("85010112345", reading.entries.single().identityCard)
    }

    @Test
    fun `a row with no treatment after the card has none`() {
        val reading = DailyListReader.read(listOf(line("Ana Pérez | 85010112345 |", 0, 0)))
        assertEquals(ListEntry("Ana Pérez", "85010112345", null), reading.entries.single())
    }

    @Test
    fun `rows without an identity card or without a name are left unread`() {
        val reading = DailyListReader.read(
            listOf(
                line("Consulta de oncología 19/11/2026", 0, 0),
                line("Nombre y apellidos Carnet Tratamiento", 0, 40),
                line("Ana Pérez 85010112345 AC", 0, 80),
                line("90020254321 FOLFOX", 0, 120),
            ),
        )
        assertEquals(listOf(ListEntry("Ana Pérez", "85010112345", "AC")), reading.entries)
        assertEquals(
            listOf("Consulta de oncología 19/11/2026", "Nombre y apellidos Carnet Tratamiento", "90020254321 FOLFOX"),
            reading.unread,
        )
    }

    @Test
    fun `a card longer than 11 digits is not a card`() {
        val reading = DailyListReader.read(listOf(line("Ana Pérez 850101123456 AC", 0, 0)))
        assertEquals(emptyList<ListEntry>(), reading.entries)
    }
}
