package com.taskmanager

import com.taskmanager.models.ErrorResponse
import com.taskmanager.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.callloging.*
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8000, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
    }
    configureDatabase()
    configureSerialization()
    configureSecurity()
    configureCors()
    configureStatusPages()
    configureRouting()
}

fun Application.configureCors() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный формат запроса: ${cause.message}"))
        }
        exception<Throwable> { call, cause ->
            println("Unhandled error: ${cause.message}")
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Внутренняя ошибка сервера"))
        }
    }
}