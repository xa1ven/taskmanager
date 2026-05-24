package com.taskmanager.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.taskmanager.models.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.Date

const val JWT_CLAIM_USER_ID = "userId"
const val JWT_CLAIM_LOGIN = "login"
const val JWT_AUTH = "jwt-auth"

fun Application.configureSecurity() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: error("JWT_SECRET environment variable is not set")
    val algorithm = Algorithm.HMAC256(jwtSecret)

    install(Authentication) {
        jwt(JWT_AUTH) {
            verifier(
                JWT.require(algorithm).build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim(JWT_CLAIM_USER_ID).asInt()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен или отсутствует"))
            }
        }
    }
}

fun generateToken(userId: Int, login: String): String {
    val jwtSecret = System.getenv("JWT_SECRET") ?: error("JWT_SECRET is not set")
    val algorithm = Algorithm.HMAC256(jwtSecret)
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

    return JWT.create()
        .withClaim(JWT_CLAIM_USER_ID, userId)
        .withClaim(JWT_CLAIM_LOGIN, login)
        .withExpiresAt(Date(System.currentTimeMillis() + thirtyDaysMs))
        .sign(algorithm)
}

fun ApplicationCall.getUserId(): Int {
    return principal<JWTPrincipal>()!!
        .payload
        .getClaim(JWT_CLAIM_USER_ID)
        .asInt()
}
