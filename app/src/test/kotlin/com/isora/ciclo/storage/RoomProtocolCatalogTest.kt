package com.isora.ciclo.storage

import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.ProtocolId
import com.isora.ciclo.protocols.ProtocolSeed
import com.isora.ciclo.protocols.Regimen
import com.isora.ciclo.protocols.RoomProtocolCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomProtocolCatalogTest : StorageTestBase() {
    private val catalog by lazy { RoomProtocolCatalog(db.protocolDao()) }

    @Test
    fun seedLoadsTheEightProtocolsOnce() {
        catalog.seedIfEmpty()
        catalog.seedIfEmpty()
        assertEquals(ProtocolSeed.protocols, catalog.all().map { it.protocol })
    }

    @Test
    fun seedDoesNotTouchACatalogThatAlreadyHasProtocols() {
        catalog.add(Protocol("Mio", Regimen(28, listOf(1)), CycleCount.Fixed(3)))
        catalog.seedIfEmpty()
        assertEquals(listOf("Mio"), catalog.all().map { it.protocol.name })
    }

    @Test
    fun addedProtocolRoundTripsEveryField() {
        val untilProgression = Protocol("Trastuzumab", Regimen(21, listOf(1)), CycleCount.UntilProgression(4))
        val gemCis = Protocol("Gem", Regimen(21, listOf(1, 8)), CycleCount.Fixed(4))
        val a = catalog.add(untilProgression)
        val b = catalog.add(gemCis)
        assertEquals(listOf(untilProgression, gemCis), catalog.all().map { it.protocol })
        assertEquals(listOf(a.id, b.id), catalog.all().map { it.id })
    }

    @Test
    fun updatingAMissingProtocolFailsWithAClearError() {
        val error = assertThrows(NoSuchElementException::class.java) {
            catalog.update(ProtocolId(999), Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4)))
        }
        assertEquals("Protocol 999 does not exist", error.message)
    }

    @Test
    fun updateChangesTheProtocol() {
        val saved = catalog.add(Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4)))
        catalog.update(saved.id, Protocol("AC", Regimen(14, listOf(1)), CycleCount.Fixed(6)))
        assertEquals(Regimen(14, listOf(1)), catalog.all().single().protocol.regimen)
        assertEquals(CycleCount.Fixed(6), catalog.all().single().protocol.defaultCycles)
    }

    @Test
    fun archivedProtocolsLeaveTheActiveListButCanBeListedAndRestored() {
        val saved = catalog.add(Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4)))
        catalog.archive(saved.id)
        assertTrue(catalog.all().isEmpty())
        assertTrue(catalog.all(includeArchived = true).single().archived)
        catalog.unarchive(saved.id)
        assertFalse(catalog.all().single().archived)
    }

    @Test
    fun rejectsADuplicateProtocolName() {
        catalog.add(Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4)))
        val failure = runCatching { catalog.add(Protocol("AC", Regimen(14, listOf(1)), CycleCount.Fixed(2))) }
        assertTrue(failure.isFailure)
    }
}
