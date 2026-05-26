package com.taskmanager.routes

import com.taskmanager.models.*
import com.taskmanager.plugins.generateToken
import com.taskmanager.plugins.getUserId
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
            val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
            val userId = transaction {
                Users.insert {
                    it[login] = request.login
                    it[passwordHash] = hashedPassword
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
            if (!BCrypt.checkpw(request.password, user[Users.passwordHash])) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Неверный логин или пароль"))
                return@post
            }
            val token = generateToken(user[Users.id], user[Users.login])
            call.respond(HttpStatusCode.OK, AuthResponse(token))
        }
    }
}

fun Route.changePasswordRoute() {
    patch("/auth/password") {
        val userId = call.getUserId()
        val request = call.receive<ChangePasswordRequest>()
        if (request.newPassword.length < 6) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Новый пароль должен содержать минимум 6 символов"))
            return@patch
        }
        val user = transaction {
            Users.select { Users.id eq userId }.singleOrNull()
        } ?: run {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Пользователь не найден"))
            return@patch
        }
        if (!BCrypt.checkpw(request.currentPassword, user[Users.passwordHash])) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Неверный текущий пароль"))
            return@patch
        }
        val newHash = BCrypt.hashpw(request.newPassword, BCrypt.gensalt())
        transaction {
            Users.update({ Users.id eq userId }) {
                it[passwordHash] = newHash
            }
        }
        call.respond(HttpStatusCode.OK, mapOf("message" to "Пароль успешно изменён"))
    }
}
