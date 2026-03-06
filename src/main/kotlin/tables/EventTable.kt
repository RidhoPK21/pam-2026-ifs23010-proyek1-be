package org.delcom.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object EventTable : UUIDTable("events") {
    val userId = uuid("user_id")
    val title = varchar("title", 100)
    val description = text("description")
    val cover = text("cover").nullable()

    // Properti Baru (Pengganti isDone & urgency)
    val status = varchar("status", 50).default("belum terlaksana")
    val tanggalPelaksanaan = varchar("tanggal_pelaksanaan", 100)
    val tempatPelaksanaan = varchar("tempat_pelaksanaan", 255)
    val estimasiBiaya = varchar("estimasi_biaya", 100)
    val divisi = varchar("divisi", 100)

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}