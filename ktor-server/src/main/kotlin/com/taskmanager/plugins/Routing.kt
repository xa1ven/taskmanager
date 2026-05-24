package com.taskmanager.plugins

import com.taskmanager.routes.authRoutes
import com.taskmanager.routes.taskRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        authRoutes()
        authenticate(JWT_AUTH) {
            taskRoutes()
        }
    }
}
