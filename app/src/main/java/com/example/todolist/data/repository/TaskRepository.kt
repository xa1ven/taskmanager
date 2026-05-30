package com.example.todolist.data.repository

import com.example.todolist.data.api.ApiService
import com.example.todolist.data.local.TaskDao
import com.example.todolist.data.local.toEntity
import com.example.todolist.data.local.toTask
import com.example.todolist.data.model.RelationRequest
import com.example.todolist.data.model.Task
import com.example.todolist.data.model.TaskRequest
import com.example.todolist.data.model.TasksData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val api: ApiService,
    private val taskDao: TaskDao
) {
    suspend fun getTasks(query: String? = null): Result<TasksData> {
        val q = if (query.isNullOrBlank()) null else query
        return try {
            val response = api.getTasks(q)
            if (response.isSuccessful) {
                val tasks = response.body() ?: emptyList()
                if (q == null) {
                    taskDao.deleteAll()
                    taskDao.insertAll(tasks.map { it.toEntity() })
                }
                Result.success(TasksData(tasks, isFromCache = false))
            } else {
                Result.failure(Exception("Ошибка загрузки задач: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (q == null) {
                val cached = taskDao.getAll().map { it.toTask() }
                if (cached.isNotEmpty()) {
                    Result.success(TasksData(cached, isFromCache = true))
                } else {
                    Result.failure(Exception("Нет соединения с сервером"))
                }
            } else {
                Result.failure(Exception("Нет соединения с сервером"))
            }
        }
    }

    suspend fun createTask(request: TaskRequest): Result<Task> {
        return try {
            val response = api.createTask(request)
            if (response.isSuccessful) {
                val task = response.body()!!
                taskDao.insert(task.toEntity())
                Result.success(task)
            } else {
                Result.failure(Exception("Ошибка создания задачи"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    suspend fun updateTask(id: Int, request: TaskRequest): Result<Task> {
        return try {
            val response = api.updateTask(id, request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Ошибка обновления задачи"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    suspend fun deleteTask(id: Int): Result<Unit> {
        return try {
            val response = api.deleteTask(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка удаления задачи"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    suspend fun markDone(id: Int): Result<Task> {
        return try {
            val response = api.markDone(id)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Ошибка обновления статуса"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    suspend fun addRelation(taskId: Int, relatedId: Int): Result<Unit> {
        return try {
            val response = api.addRelation(taskId, RelationRequest(relatedId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка добавления связи"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }

    suspend fun removeRelation(taskId: Int, relatedId: Int): Result<Unit> {
        return try {
            val response = api.removeRelation(taskId, relatedId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка удаления связи"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Нет соединения с сервером"))
        }
    }
}
