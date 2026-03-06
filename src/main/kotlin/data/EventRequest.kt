package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.Event

@Serializable
data class EventRequest(
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var cover: String? = null,

    // Field Baru (Pengganti isDone & urgency)
    var status: String = "belum terlaksana", // Default value
    var tanggalPelaksanaan: String = "",
    var tempatPelaksanaan: String = "",
    var estimasiBiaya: String = "",
    var divisi: String = ""
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "cover" to cover,
            "status" to status,
            "tanggalPelaksanaan" to tanggalPelaksanaan,
            "tempatPelaksanaan" to tempatPelaksanaan,
            "estimasiBiaya" to estimasiBiaya,
            "divisi" to divisi
        )
    }

    fun toEntity(): Event {
        return Event(
            userId = userId,
            title = title,
            description = description,
            cover = cover,
            status = status,
            tanggalPelaksanaan = tanggalPelaksanaan,
            tempatPelaksanaan = tempatPelaksanaan,
            estimasiBiaya = estimasiBiaya,
            divisi = divisi,
            updatedAt = Clock.System.now()
        )
    }
}