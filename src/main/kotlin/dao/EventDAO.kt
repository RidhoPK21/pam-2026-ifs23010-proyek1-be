package org.delcom.dao

import org.delcom.tables.EventTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import java.util.UUID

class EventDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, EventDAO>(EventTable)

    var userId by EventTable.userId
    var title by EventTable.title
    var description by EventTable.description
    var cover by EventTable.cover

    // Properti Baru
    var status by EventTable.status
    var tanggalPelaksanaan by EventTable.tanggalPelaksanaan
    var tempatPelaksanaan by EventTable.tempatPelaksanaan
    var estimasiBiaya by EventTable.estimasiBiaya
    var divisi by EventTable.divisi

    var createdAt by EventTable.createdAt
    var updatedAt by EventTable.updatedAt
}