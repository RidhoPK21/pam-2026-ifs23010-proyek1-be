package org.delcom

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.delcom.data.AppException
import org.delcom.data.ErrorResponse
import org.delcom.helpers.JWTConstants
import org.delcom.helpers.parseMessageToMap
import org.delcom.services.EventService // Import Service baru
import org.delcom.services.AuthService
import org.delcom.services.UserService
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    // Ubah nama variabel dari todoService menjadi eventService
    val eventService: EventService by inject()
    val authService: AuthService by inject()
    val userService: UserService by inject()

    install(StatusPages) {
        // Tangkap AppException
        exception<AppException> { call, cause ->
            val dataMap: Map<String, List<String>> = parseMessageToMap(cause.message)

            call.respond(
                status = HttpStatusCode.fromValue(cause.code),
                message = ErrorResponse(
                    status = "fail",
                    message = if (dataMap.isEmpty()) cause.message else "Data yang dikirimkan tidak valid!",
                    data = if (dataMap.isEmpty()) null else dataMap.toString()
                )
            )
        }

        // Tangkap semua Throwable lainnya
        exception<Throwable> { call, cause ->
            call.respond(
                status = HttpStatusCode.fromValue(500),
                message = ErrorResponse(
                    status = "error",
                    message = cause.message ?: "Unknown error",
                    data = ""
                )
            )
        }
    }

    routing {
        get("/") {
            call.respondText("API telah berjalan. Dibuat oleh Abdullah Ubaid.")
        }

        // Route Auth
        route("/auth") {
            post("/login") {
                authService.postLogin(call)
            }
            post("/register") {
                authService.postRegister(call)
            }
            post("/refresh-token") {
                authService.postRefreshToken(call)
            }

            post("/logout") {
                authService.postLogout(call)
            }
        }

        authenticate(JWTConstants.NAME) {
            // Route User
            route("/users") {
                get("/me") {
                    userService.getMe(call)
                }
                put("/me") {
                    userService.putMe(call)
                }
                put("/me/password") {
                    userService.putMyPassword(call)
                }
                put("/me/photo") {
                    userService.putMyPhoto(call)
                }
            }

            // Route Events (Ubah dari /todos menjadi /events)
            route("/events") {
                get("/stats") {
                    eventService.getStats(call)
                }
                get {
                    eventService.getAll(call)
                }
                post {
                    eventService.post(call)
                }
                get("/{id}") {
                    eventService.getById(call)
                }
                put("/{id}") {
                    eventService.put(call)
                }
                put("/{id}/cover") {
                    eventService.putCover(call)
                }
                delete("/{id}") {
                    eventService.delete(call)
                }
            }
        }

        route("/images") {
            get("users/{id}") {
                userService.getPhoto(call)
            }

            // Ubah dari todos/{id} menjadi events/{id}
            get("events/{id}") {
                eventService.getCover(call)
            }
        }

    }
}