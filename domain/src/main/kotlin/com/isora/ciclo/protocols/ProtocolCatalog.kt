package com.isora.ciclo.protocols

@JvmInline
value class ProtocolId(val value: Long)

data class CatalogProtocol(val id: ProtocolId, val protocol: Protocol, val archived: Boolean)

class DuplicateProtocolNameException(val name: String) : RuntimeException("A protocol named \"$name\" already exists")

/** Port for the editable protocol catalog. Archived protocols stay stored but leave the active list. */
interface ProtocolCatalog {
    fun all(includeArchived: Boolean = false): List<CatalogProtocol>
    fun add(protocol: Protocol): CatalogProtocol

    /** Replaces the protocol; treatments already assigned keep their own snapshot. */
    fun update(id: ProtocolId, protocol: Protocol)
    fun archive(id: ProtocolId)
    fun unarchive(id: ProtocolId)
}
