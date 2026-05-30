package com.example.todolist.data.model

import com.google.gson.annotations.SerializedName

data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val priority: String,
    val deadline: String?,
    @SerializedName("isDone") val isDone: Boolean,
    val createdAt: String? = null,
    val relatedTasks: List<RelatedTaskResponse>? = null
)

data class RelatedTaskResponse(
    val id: Int,
    val title: String,
    @SerializedName("isDone") val isDone: Boolean,
    val groupColor: String? = null
)

data class TaskRequest(
    val title: String,
    val description: String?,
    val priority: String,
    val deadline: String?,
    val isDone: Boolean
)

data class AuthResponse(val token: String)

data class LoginRequest(val login: String, val password: String)

data class RegisterRequest(val login: String, val password: String)

data class RelationRequest(val relatedTaskId: Int)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

data class TasksData(val tasks: List<Task>, val isFromCache: Boolean = false)
