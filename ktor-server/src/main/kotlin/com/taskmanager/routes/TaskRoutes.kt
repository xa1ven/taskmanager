package com.taskmanager.routes

import com.taskmanager.models.*
import com.taskmanager.plugins.getUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private class ILikeOp(expr1: Expression<*>, expr2: Expression<*>) :
    ComparisonOp(expr1, expr2, "ILIKE")

private infix fun <T : String?> ExpressionWithColumnType<T>.ilike(pattern: String): Op<Boolean> =
    ILikeOp(this, stringParam(pattern))

private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun Route.taskRoutes() {
    route("/tasks") {

        get {
            val userId = call.getUserId()
            val query = call.request.queryParameters["query"]?.takeIf { it.isNotBlank() }

            val tasks = transaction {
                val baseQuery = Tasks.select { Tasks.userId eq userId }

                val filtered = if (query != null) {
                    baseQuery.andWhere {
                        (Tasks.title ilike "%$query%") or (Tasks.description ilike "%$query%")
                    }
                } else {
                    baseQuery
                }

                filtered.orderBy(Tasks.createdAt, SortOrder.DESC).map { row ->
                    val taskId = row[Tasks.id]
                    val relatedTasks = loadRelatedTasks(taskId)
                    rowToTaskResponse(row, relatedTasks)
                }
            }

            call.respond(HttpStatusCode.OK, tasks)
        }

        post {
            val userId = call.getUserId()
            val request = call.receive<TaskRequest>()

            if (request.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Название задачи не может быть пустым"))
                return@post
            }

            val task = transaction {
                val now = LocalDateTime.now()
                val id = Tasks.insert {
                    it[Tasks.userId] = userId
                    it[title] = request.title.trim()
                    it[description] = request.description?.trim()
                    it[priority] = request.priority
                    it[deadline] = request.deadline?.let { d -> parseDeadline(d) }
                    it[isDone] = request.isDone
                    it[createdAt] = now
                    it[updatedAt] = now
                }[Tasks.id]

                val row = Tasks.select { Tasks.id eq id }.single()
                rowToTaskResponse(row, emptyList())
            }

            call.respond(HttpStatusCode.Created, task)
        }

        put("/{id}") {
            val userId = call.getUserId()
            val taskId = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный ID задачи"))

            val request = call.receive<TaskRequest>()

            if (request.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Название задачи не может быть пустым"))
                return@put
            }

            val task = transaction {
                val existing = Tasks.select { Tasks.id eq taskId }.singleOrNull()
                    ?: return@transaction null to "not_found"

                if (existing[Tasks.userId] != userId) return@transaction null to "forbidden"

                val now = LocalDateTime.now()
                Tasks.update({ Tasks.id eq taskId }) {
                    it[title] = request.title.trim()
                    it[description] = request.description?.trim()
                    it[priority] = request.priority
                    it[deadline] = request.deadline?.let { d -> parseDeadline(d) }
                    it[isDone] = request.isDone
                    it[updatedAt] = now
                }

                val row = Tasks.select { Tasks.id eq taskId }.single()
                val relatedTasks = loadRelatedTasks(taskId)
                rowToTaskResponse(row, relatedTasks) to "ok"
            }

            when (task.second) {
                "not_found" -> call.respond(HttpStatusCode.NotFound, ErrorResponse("Задача не найдена"))
                "forbidden" -> call.respond(HttpStatusCode.Forbidden, ErrorResponse("Нет доступа к этой задаче"))
                else -> call.respond(HttpStatusCode.OK, task.first!!)
            }
        }

        delete("/{id}") {
            val userId = call.getUserId()
            val taskId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный ID задачи"))

            val result = transaction {
                val existing = Tasks.select { Tasks.id eq taskId }.singleOrNull()
                    ?: return@transaction "not_found"

                if (existing[Tasks.userId] != userId) return@transaction "forbidden"

                Tasks.deleteWhere { Tasks.id eq taskId }
                "ok"
            }

            when (result) {
                "not_found" -> call.respond(HttpStatusCode.NotFound, ErrorResponse("Задача не найдена"))
                "forbidden" -> call.respond(HttpStatusCode.Forbidden, ErrorResponse("Нет доступа к этой задаче"))
                else -> call.respond(HttpStatusCode.NoContent)
            }
        }

        patch("/{id}/done") {
            val userId = call.getUserId()
            val taskId = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный ID задачи"))

            val task = transaction {
                val existing = Tasks.select { Tasks.id eq taskId }.singleOrNull()
                    ?: return@transaction null to "not_found"

                if (existing[Tasks.userId] != userId) return@transaction null to "forbidden"

                Tasks.update({ Tasks.id eq taskId }) {
                    it[isDone] = true
                    it[updatedAt] = LocalDateTime.now()
                }

                val row = Tasks.select { Tasks.id eq taskId }.single()
                val relatedTasks = loadRelatedTasks(taskId)
                rowToTaskResponse(row, relatedTasks) to "ok"
            }

            when (task.second) {
                "not_found" -> call.respond(HttpStatusCode.NotFound, ErrorResponse("Задача не найдена"))
                "forbidden" -> call.respond(HttpStatusCode.Forbidden, ErrorResponse("Нет доступа к этой задаче"))
                else -> call.respond(HttpStatusCode.OK, task.first!!)
            }
        }

        post("/{id}/relations") {
            val userId = call.getUserId()
            val taskId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный ID задачи"))

            val request = call.receive<RelationRequest>()
            val relatedId = request.relatedTaskId

            if (taskId == relatedId) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Нельзя связать задачу саму с собой"))
                return@post
            }

            val result = transaction {
                val task = Tasks.select { Tasks.id eq taskId }.singleOrNull()
                    ?: return@transaction "task_not_found"

                if (task[Tasks.userId] != userId) return@transaction "forbidden"

                val relatedTask = Tasks.select { Tasks.id eq relatedId }.singleOrNull()
                    ?: return@transaction "related_not_found"

                if (relatedTask[Tasks.userId] != userId) return@transaction "related_forbidden"

                val exists = TaskRelations.select {
                    (TaskRelations.taskId eq taskId) and (TaskRelations.relatedTaskId eq relatedId)
                }.count() > 0

                if (exists) return@transaction "conflict"

                TaskRelations.insert {
                    it[TaskRelations.taskId] = taskId
                    it[TaskRelations.relatedTaskId] = relatedId
                }
                TaskRelations.insert {
                    it[TaskRelations.taskId] = relatedId
                    it[TaskRelations.relatedTaskId] = taskId
                }

                "ok"
            }

            when (result) {
                "task_not_found" -> call.respond(HttpStatusCode.NotFound, ErrorResponse("Задача не найдена"))
                "forbidden" -> call.respond(HttpStatusCode.Forbidden, ErrorResponse("Нет доступа к этой задаче"))
                "related_not_found" -> call.respond(HttpStatusCode.NotFound, ErrorResponse("Связываемая задача не найдена"))
                "related_forbidden" -> call.respond(HttpStatusCode.Forbidden, ErrorResponse("Нет доступа к связываемой задаче"))
                "conflict" -> call.respond(HttpStatusCode.Conflict, ErrorResponse("Связь уже существует"))
                else -> call.respond(HttpStatusCode.Created)
            }
        }

        delete("/{id}/relations/{relatedId}") {
            val userId = call.getUserId()
            val taskId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный ID задачи"))
            val relatedId = call.parameters["relatedId"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный ID связанной задачи"))

            val result = transaction {
                val task = Tasks.select { Tasks.id eq taskId }.singleOrNull()
                    ?: return@transaction "not_found"

                if (task[Tasks.userId] != userId) return@transaction "forbidden"

                TaskRelations.deleteWhere {
                    (TaskRelations.taskId eq taskId) and (TaskRelations.relatedTaskId eq relatedId)
                }
                TaskRelations.deleteWhere {
                    (TaskRelations.taskId eq relatedId) and (TaskRelations.relatedTaskId eq taskId)
                }

                "ok"
            }

            when (result) {
                "not_found" -> call.respond(HttpStatusCode.NotFound, ErrorResponse("Задача не найдена"))
                "forbidden" -> call.respond(HttpStatusCode.Forbidden, ErrorResponse("Нет доступа к этой задаче"))
                else -> call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun loadRelatedTasks(taskId: Int): List<RelatedTaskResponse> {
    val relatedIds = TaskRelations
        .select { TaskRelations.taskId eq taskId }
        .map { it[TaskRelations.relatedTaskId] }

    if (relatedIds.isEmpty()) return emptyList()

    return Tasks
        .select { Tasks.id inList relatedIds }
        .map { row ->
            RelatedTaskResponse(
                id = row[Tasks.id],
                title = row[Tasks.title],
                isDone = row[Tasks.isDone]
            )
        }
}

private fun rowToTaskResponse(row: ResultRow, relatedTasks: List<RelatedTaskResponse>): TaskResponse {
    return TaskResponse(
        id = row[Tasks.id],
        title = row[Tasks.title],
        description = row[Tasks.description],
        priority = row[Tasks.priority],
        deadline = row[Tasks.deadline]?.format(ISO_FORMATTER),
        isDone = row[Tasks.isDone],
        createdAt = row[Tasks.createdAt].format(ISO_FORMATTER),
        updatedAt = row[Tasks.updatedAt].format(ISO_FORMATTER),
        relatedTasks = relatedTasks
    )
}

private fun parseDeadline(value: String): LocalDateTime {
    return try {
        LocalDateTime.parse(value, ISO_FORMATTER)
    } catch (e: Exception) {
        LocalDateTime.parse("${value}T00:00:00", ISO_FORMATTER)
    }
}
