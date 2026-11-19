package com.isora.ciclo.protocols

import android.database.sqlite.SQLiteConstraintException
import com.isora.ciclo.storage.cycleCountOf
import com.isora.ciclo.storage.mode
import com.isora.ciclo.storage.regimenOf
import com.isora.ciclo.storage.toColumn
import com.isora.ciclo.storage.value

class RoomProtocolCatalog(private val dao: ProtocolDao) : ProtocolCatalog {

    /** Loads the standard protocols when the catalog has never been filled. */
    fun seedIfEmpty() {
        if (dao.count() == 0) dao.insertAll(ProtocolSeed.protocols.map { it.toEntity(id = 0, archived = false) })
    }

    override fun all(includeArchived: Boolean): List<CatalogProtocol> =
        (if (includeArchived) dao.all() else dao.active()).map { it.toCatalogProtocol() }

    override fun add(protocol: Protocol): CatalogProtocol {
        val id = try {
            dao.insert(protocol.toEntity(id = 0, archived = false))
        } catch (e: SQLiteConstraintException) {
            throw DuplicateProtocolNameException(protocol.name)
        }
        return CatalogProtocol(ProtocolId(id), protocol, archived = false)
    }

    override fun update(id: ProtocolId, protocol: Protocol) {
        val current = dao.all().firstOrNull { it.id == id.value }
            ?: throw NoSuchElementException("Protocol ${id.value} does not exist")
        try {
            dao.update(protocol.toEntity(id = id.value, archived = current.archived))
        } catch (e: SQLiteConstraintException) {
            throw DuplicateProtocolNameException(protocol.name)
        }
    }

    override fun archive(id: ProtocolId) = dao.setArchived(id.value, true)

    override fun unarchive(id: ProtocolId) = dao.setArchived(id.value, false)
}

private fun Protocol.toEntity(id: Long, archived: Boolean) = ProtocolEntity(
    id = id,
    name = name,
    cycleLengthDays = regimen.cycleLengthDays,
    sessionDays = regimen.sessionDays.toColumn(),
    cycleMode = defaultCycles.mode(),
    cycleValue = defaultCycles.value(),
    archived = archived,
)

private fun ProtocolEntity.toCatalogProtocol() = CatalogProtocol(
    id = ProtocolId(id),
    protocol = Protocol(name, regimenOf(cycleLengthDays, sessionDays), cycleCountOf(cycleMode, cycleValue)),
    archived = archived,
)
