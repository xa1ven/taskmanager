package com.taskmanager.routes

import com.taskmanager.models.*
import com.taskmanager.plugins.generateToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

fun Route.authRoutes() {
    route("/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()

            if (request.login.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Логин и пароль не могут быть пустыми"))
                return@post
            }

            val existingUser = transaction {
                Users.select { Users.login eq request.login }.singleOrNull()
            }

            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Пользователь с таким логином уже существует"))
                return@post
            }

            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())

            val userId = transaction {
                Users.insert {
                    it[login] = request.login
                    it[passwordHash] = passwordHash
                    it[createdAt] = LocalDateTime.now()
                }[Users.id]
            }

            val token = generateToken(userId, request.login)
            call.respond(HttpStatusCode.OK, AuthResponse(token))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = transaction {
                Users.select { Users.login eq request.login }.singleOrNull()
            }

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Неверный логин или пароль"))
                return@post
            }

            val passwordValid = BCrypt.checkpw(request.password, user[Users.passwordHash])
            if (!passwordValid) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Неверный логин или пароль"))
                return@post
            }

            val token = generateToken(user[Users.id], user[Users.login])
            call.respond(HttpStatusCode.OK, AuthResponse(token))
        }
    }
}
