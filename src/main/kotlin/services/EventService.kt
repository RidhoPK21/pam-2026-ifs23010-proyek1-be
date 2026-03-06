package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.delcom.data.AppException
import org.delcom.data.DataResponse
import org.delcom.data.EventRequest
import org.delcom.helpers.ServiceHelper
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.IEventRepository
import org.delcom.repositories.IUserRepository
import java.io.File
import java.util.*

class EventService(
    private val userRepo: IUserRepository,
    private val eventRepo: IEventRepository
) {
    suspend fun getAll(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val search = call.request.queryParameters["search"] ?: ""

        // Ambil query parameter untuk pagination & filter
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val perPage = call.request.queryParameters["perPage"]?.toIntOrNull() ?: 10

        // Filter baru menggunakan status dan divisi
        val status = call.request.queryParameters["status"]
        val divisi = call.request.queryParameters["divisi"]

        // Panggil fungsi getAll yang baru
        val events = eventRepo.getAll(user.id, search, page, perPage, status, divisi)

        val response = DataResponse(
            "success",
            "Berhasil mengambil daftar kegiatan",
            mapOf(Pair("events", events))
        )
        call.respond(response)
    }

    suspend fun getStats(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val stats = eventRepo.getHomeStats(user.id)

        val response = DataResponse(
            "success",
            "Berhasil mengambil statistik kegiatan",
            mapOf(Pair("stats", stats))
        )
        call.respond(response)
    }

    // Mengambil data kegiatan berdasarkan id
    suspend fun getById(call: ApplicationCall) {
        val eventId = call.parameters["id"]
            ?: throw AppException(400, "Data kegiatan tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val event = eventRepo.getById(eventId)
        if (event == null || event.userId != user.id) {
            throw AppException(404, "Data kegiatan tidak tersedia!")
        }

        val response = DataResponse(
            "success",
            "Berhasil mengambil data kegiatan",
            mapOf(Pair("event", event))
        )
        call.respond(response)
    }

    // Ubah cover kegiatan
    suspend fun putCover(call: ApplicationCall) {
        val eventId = call.parameters["id"]
            ?: throw AppException(400, "Data kegiatan tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        // Ambil data request
        val request = EventRequest()
        request.userId = user.id

        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            when (part) {
                // Upload file
                is PartData.FileItem -> {
                    val ext = part.originalFileName
                        ?.substringAfterLast('.', "")
                        ?.let { if (it.isNotEmpty()) ".$it" else "" }
                        ?: ""

                    val fileName = UUID.randomUUID().toString() + ext
                    val filePath = "uploads/events/$fileName" // Ubah folder tujuan

                    withContext(Dispatchers.IO) {
                        val file = File(filePath)
                        file.parentFile.mkdirs() // pastikan folder ada

                        part.provider().copyAndClose(file.writeChannel())
                        request.cover = filePath
                    }
                }

                else -> {}
            }

            part.dispose()
        }

        if (request.cover == null) {
            throw AppException(404, "Cover kegiatan tidak tersedia!")
        }

        val newFile = File(request.cover!!)
        // Cek apakah gambar berhasil diunggah
        if (!newFile.exists()) {
            throw AppException(404, "Cover kegiatan gagal diunggah!")
        }

        val oldEvent = eventRepo.getById(eventId)
        if (oldEvent == null || oldEvent.userId != user.id) {
            throw AppException(404, "Data kegiatan tidak tersedia!")
        }

        // Pertahankan data lama
        request.title = oldEvent.title
        request.description = oldEvent.description
        request.status = oldEvent.status
        request.tanggalPelaksanaan = oldEvent.tanggalPelaksanaan
        request.tempatPelaksanaan = oldEvent.tempatPelaksanaan
        request.estimasiBiaya = oldEvent.estimasiBiaya
        request.divisi = oldEvent.divisi

        val isUpdated = eventRepo.update(
            user.id,
            eventId,
            request.toEntity()
        )
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui cover kegiatan!")
        }

        // Hapus cover lama
        if (oldEvent.cover != null) {
            val oldFile = File(oldEvent.cover!!)
            if (oldFile.exists()) {
                oldFile.delete()
            }
        }

        val response = DataResponse(
            "success",
            "Berhasil mengubah cover kegiatan",
            null
        )
        call.respond(response)
    }

    // Menambahkan data kegiatan
    suspend fun post(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        // Ambil data request
        val request = call.receive<EventRequest>()
        request.userId = user.id

        // Validasi request
        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul kegiatan tidak boleh kosong")
        validator.required("description", "Deskripsi tidak boleh kosong")
        validator.required("status", "Status tidak boleh kosong")
        validator.required("tanggalPelaksanaan", "Tanggal pelaksanaan tidak boleh kosong")
        validator.required("tempatPelaksanaan", "Tempat pelaksanaan tidak boleh kosong")
        validator.required("estimasiBiaya", "Estimasi biaya tidak boleh kosong")
        validator.required("divisi", "Divisi tidak boleh kosong")
        validator.validate()

        // Tambahkan kegiatan
        val eventId = eventRepo.create(
            request.toEntity()
        )

        val response = DataResponse(
            "success",
            "Berhasil menambahkan data kegiatan",
            mapOf(Pair("eventId", eventId))
        )
        call.respond(response)
    }

    // Mengubah data kegiatan
    suspend fun put(call: ApplicationCall) {
        val eventId = call.parameters["id"]
            ?: throw AppException(400, "Data kegiatan tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        // Ambil data request
        val request = call.receive<EventRequest>()
        request.userId = user.id

        // Validasi request
        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul kegiatan tidak boleh kosong")
        validator.required("description", "Deskripsi tidak boleh kosong")
        validator.required("status", "Status tidak boleh kosong")
        validator.required("tanggalPelaksanaan", "Tanggal pelaksanaan tidak boleh kosong")
        validator.required("tempatPelaksanaan", "Tempat pelaksanaan tidak boleh kosong")
        validator.required("estimasiBiaya", "Estimasi biaya tidak boleh kosong")
        validator.required("divisi", "Divisi tidak boleh kosong")
        validator.validate()

        val oldEvent = eventRepo.getById(eventId)
        if (oldEvent == null || oldEvent.userId != user.id) {
            throw AppException(404, "Data kegiatan tidak tersedia!")
        }
        request.cover = oldEvent.cover // Pertahankan cover lama

        val isUpdated = eventRepo.update(
            user.id,
            eventId,
            request.toEntity()
        )
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui data kegiatan!")
        }

        val response = DataResponse(
            "success",
            "Berhasil mengubah data kegiatan",
            null
        )
        call.respond(response)
    }

    // Menghapus data kegiatan
    suspend fun delete(call: ApplicationCall) {
        val eventId = call.parameters["id"]
            ?: throw AppException(400, "Data kegiatan tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val oldEvent = eventRepo.getById(eventId)
        if (oldEvent == null || oldEvent.userId != user.id) {
            throw AppException(404, "Data kegiatan tidak tersedia!")
        }

        val isDeleted = eventRepo.delete(user.id, eventId)
        if (!isDeleted) {
            throw AppException(400, "Gagal menghapus data kegiatan!")
        }

        if (oldEvent.cover != null) {
            val oldFile = File(oldEvent.cover!!)

            // Hapus data gambar jika data kegiatan sudah dihapus
            if (oldFile.exists()) {
                oldFile.delete()
            }
        }

        val response = DataResponse(
            "success",
            "Berhasil menghapus data kegiatan",
            null
        )
        call.respond(response)
    }

    // Mengambil gambar kegiatan
    suspend fun getCover(call: ApplicationCall) {
        val eventId = call.parameters["id"]
            ?: throw AppException(400, "Data kegiatan tidak valid!")

        val event = eventRepo.getById(eventId)
            ?: return call.respond(HttpStatusCode.NotFound)

        if (event.cover == null) {
            throw AppException(404, "Kegiatan belum memiliki cover")
        }

        val file = File(event.cover!!)
        if (!file.exists()) {
            throw AppException(404, "Cover kegiatan tidak tersedia")
        }

        call.respondFile(file)
    }
}