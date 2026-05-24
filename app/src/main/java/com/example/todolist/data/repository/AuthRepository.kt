package com.example.todolist.data.repository

import com.example.todolist.data.api.ApiService
import com.example.todolist.data.model.ChangePasswordRequest
import com.example.todolist.data.model.LoginRequest
import com.example.todolist.data.model.RegisterRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenRepository: TokenRepository
) {
    suspend fun login(login: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequest(login, password))
            if (response.isSuccessful) {
                val token = response.body()?.token
                    ?: return Result.failure(Exception("Пустой ответ от сервера"))
                tokenRepository.saveToken(token)
                Result.success(token)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Ошибка входа"
                Result.failure(Exception(parseError(errorMsg)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    suspend fun register(login: String, password: String): Result<String> {
        return try {
            val response = api.register(RegisterRequest(login, password))
            if (response.isSuccessful) {
                val token = response.body()?.token
                    ?: return Result.failure(Exception("Пустой ответ от сервера"))
                tokenRepository.saveToken(token)
                Result.success(token)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Ошибка регистрации"
                Result.failure(Exception(parseError(errorMsg)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val response = api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Ошибка смены пароля"
                Result.failure(Exception(parseError(errorMsg)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    private fun parseError(raw: String): String {
        return try {
            val clean = raw.trim().removePrefix("\"").removeSuffix("\"")
            if (clean.isNotBlank()) clean else "Произошла ошибка"
        } catch (e: Exception) {
            "Произошла ошибка"
        }
    }
}
