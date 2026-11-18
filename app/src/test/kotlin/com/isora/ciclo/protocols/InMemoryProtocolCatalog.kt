package com.isora.ciclo.protocols

class InMemoryProtocolCatalog(seed: List<Protocol> = ProtocolSeed.protocols) : ProtocolCatalog {
    private val items = mutableListOf<CatalogProtocol>()
    private var nextId = 1L

    init {
        seed.forEach { add(it) }
    }

    override fun all(includeArchived: Boolean) = items.filter { includeArchived || !it.archived }

    override fun add(protocol: Protocol): CatalogProtocol {
        if (items.any { it.protocol.name == protocol.name }) throw DuplicateProtocolNameException(protocol.name)
        return CatalogProtocol(ProtocolId(nextId++), protocol, archived = false).also { items += it }
    }

    override fun update(id: ProtocolId, protocol: Protocol) {
        if (items.any { it.id != id && it.protocol.name == protocol.name }) {
            throw DuplicateProtocolNameException(protocol.name)
        }
        replace(id) { it.copy(protocol = protocol) }
    }

    override fun archive(id: ProtocolId) = replace(id) { it.copy(archived = true) }

    override fun unarchive(id: ProtocolId) = replace(id) { it.copy(archived = false) }

    private fun replace(id: ProtocolId, change: (CatalogProtocol) -> CatalogProtocol) {
        val index = items.indexOfFirst { it.id == id }
        items[index] = change(items[index])
    }
}
