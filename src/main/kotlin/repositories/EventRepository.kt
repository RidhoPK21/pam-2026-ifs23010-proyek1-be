package org.delcom.repositories

import org.delcom.dao.EventDAO
import org.delcom.entities.Event
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.eventDAOToModel // Pastikan import diubah
import org.delcom.tables.EventTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import java.util.*

class EventRepository : IEventRepository {
    override suspend fun getAll(userId: String, search: String, page: Int, perPage: Int, status: String?, divisi: String?): List<Event> = suspendTransaction {
        val query = if (search.isBlank()) {
            EventDAO.find {
                var op: org.jetbrains.exposed.sql.Op<Boolean> = (EventTable.userId eq UUID.fromString(userId))
                if (status != null) op = op and (EventTable.status eq status)
                if (divisi != null) op = op and (EventTable.divisi eq divisi)
                op
            }
        } else {
            val keyword = "%${search.lowercase()}%"
            EventDAO.find {
                var op: org.jetbrains.exposed.sql.Op<Boolean> = (EventTable.userId eq UUID.fromString(userId)) and (EventTable.title.lowerCase() like keyword)
                if (status != null) op = op and (EventTable.status eq status)
                if (divisi != null) op = op and (EventTable.divisi eq divisi)
                op
            }
        }

        // Terapkan Pengurutan berdasarkan Waktu Dibuat (Terbaru)
        query.orderBy(EventTable.createdAt to SortOrder.DESC)
            .limit(perPage).offset(((page - 1) * perPage).toLong()).map(::eventDAOToModel)
    }

    override suspend fun getHomeStats(userId: String): Map<String, Long> = suspendTransaction {
        val total = EventDAO.find { EventTable.userId eq UUID.fromString(userId) }.count()
        val completed = EventDAO.find { (EventTable.userId eq UUID.fromString(userId)) and (EventTable.status eq "sudah terlaksana") }.count()
        val canceled = EventDAO.find { (EventTable.userId eq UUID.fromString(userId)) and (EventTable.status eq "dibatalkan") }.count()
        val active = EventDAO.find { (EventTable.userId eq UUID.fromString(userId)) and (EventTable.status eq "belum terlaksana") }.count()

        mapOf("total" to total, "complete" to completed, "active" to active, "canceled" to canceled)
    }

    override suspend fun getById(eventId: String): Event? = suspendTransaction {
        EventDAO
            .find {
                (EventTable.id eq UUID.fromString(eventId))
            }
            .limit(1)
            .map(::eventDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(event: Event): String = suspendTransaction {
        val eventDAO = EventDAO.new {
            userId = UUID.fromString(event.userId)
            title = event.title
            description = event.description
            cover = event.cover
            status = event.status
            tanggalPelaksanaan = event.tanggalPelaksanaan
            tempatPelaksanaan = event.tempatPelaksanaan
            estimasiBiaya = event.estimasiBiaya
            divisi = event.divisi
            createdAt = event.createdAt
            updatedAt = event.updatedAt
        }

        eventDAO.id.value.toString()
    }

    override suspend fun update(userId: String, eventId: String, newEvent: Event): Boolean = suspendTransaction {
        val eventDAO = EventDAO
            .find {
                (EventTable.id eq UUID.fromString(eventId)) and
                        (EventTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (eventDAO != null) {
            eventDAO.title = newEvent.title
            eventDAO.description = newEvent.description
            eventDAO.cover = newEvent.cover
            eventDAO.status = newEvent.status
            eventDAO.tanggalPelaksanaan = newEvent.tanggalPelaksanaan
            eventDAO.tempatPelaksanaan = newEvent.tempatPelaksanaan
            eventDAO.estimasiBiaya = newEvent.estimasiBiaya
            eventDAO.divisi = newEvent.divisi
            eventDAO.updatedAt = newEvent.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, eventId: String): Boolean = suspendTransaction {
        val rowsDeleted = EventTable.deleteWhere {
            (EventTable.id eq UUID.fromString(eventId)) and
                    (EventTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }
}