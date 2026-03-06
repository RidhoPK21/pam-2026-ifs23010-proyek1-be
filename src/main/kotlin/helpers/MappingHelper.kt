package org.delcom.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.dao.EventDAO
import org.delcom.dao.RefreshTokenDAO
import org.delcom.dao.UserDAO
import org.delcom.entities.Event
import org.delcom.entities.RefreshToken
import org.delcom.entities.User
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun userDAOToModel(dao: UserDAO) = User(
    dao.id.value.toString(),
    dao.name,
    dao.username,
    dao.password,
    dao.photo,
    dao.about,
    dao.createdAt,
    dao.updatedAt
).apply { about = dao.about }

fun refreshTokenDAOToModel(dao: RefreshTokenDAO) = RefreshToken(
    dao.id.value.toString(),
    dao.userId.toString(),
    dao.refreshToken,
    dao.authToken,
    dao.createdAt,
)

// Mengubah nama fungsi dan mapping data sesuai dengan struktur Event yang baru
fun eventDAOToModel(dao: EventDAO) = Event(
    id = dao.id.value.toString(),
    userId = dao.userId.toString(),
    title = dao.title,
    description = dao.description,
    cover = dao.cover,

    // Mapping field baru
    status = dao.status,
    tanggalPelaksanaan = dao.tanggalPelaksanaan,
    tempatPelaksanaan = dao.tempatPelaksanaan,
    estimasiBiaya = dao.estimasiBiaya,
    divisi = dao.divisi,

    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)