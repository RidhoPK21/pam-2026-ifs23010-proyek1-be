package org.delcom.repositories

import org.delcom.entities.Event

interface IEventRepository {
    // Parameter isComplete dan urgency diganti menjadi status dan divisi
    suspend fun getAll(userId: String, search: String, page: Int, perPage: Int, status: String?, divisi: String?): List<Event>

    // Fungsi baru untuk statistik Home
    suspend fun getHomeStats(userId: String): Map<String, Long>

    // Parameter todoId diubah menjadi eventId
    suspend fun getById(eventId: String): Event?

    // Parameter todo diubah menjadi event
    suspend fun create(event: Event): String

    // Parameter newTodo diubah menjadi newEvent
    suspend fun update(userId: String, eventId: String, newEvent: Event): Boolean

    suspend fun delete(userId: String, eventId: String) : Boolean
}