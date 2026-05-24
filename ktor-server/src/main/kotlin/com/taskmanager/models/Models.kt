package com.taskmanager.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val login = varchar("login", 50)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Tasks : Table("tasks") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val priority = varchar("priority", 10)
    val deadline = datetime("deadline").nullable()
    val isDone = bool("is_done")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object TaskRelations : Table("task_relations") {
    val taskId = integer("task_id").references(Tasks.id)
    val relatedTaskId = integer("related_task_id").references(Tasks.id)
    override val primaryKey = PrimaryKey(taskId, relatedTaskId)
}

@Serializable
data class RegisterRequest(val login: String, val password: String)

@Serializable
data class LoginRequest(val login: String, val password: String)

@Serializable
data class AuthResponse(val token: String)

@Serializable
data class TaskRequest(
    val title: String,
    val description: String? = null,
    val priority: String = "MEDIUM",
    val deadline: String? = null,
    val isDone: Boolean = false
)

@Serializable
data class TaskResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val priority: String,
    val deadline: String?,
    val isDone: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val relatedTasks: List<RelatedTaskResponse> = emptyList()
)

@Serializable
data class RelatedTaskResponse(
    val id: Int,
    val title: String,
    val isDone: Boolean
)

@Serializable
data class RelationRequest(val relatedTaskId: Int)

@Serializable
data class ErrorResponse(val error: String)
