package com.example.todolist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todolist.data.model.RelatedTaskResponse
import com.example.todolist.data.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String?,
    val priority: String,
    val deadline: String?,
    val isDone: Boolean,
    val createdAt: String?,
    val relatedTasks: List<RelatedTaskResponse>?
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    priority = priority,
    deadline = deadline,
    isDone = isDone,
    createdAt = createdAt,
    relatedTasks = relatedTasks
)

fun TaskEntity.toTask() = Task(
    id = id,
    title = title,
    description = description,
    priority = priority,
    deadline = deadline,
    isDone = isDone,
    createdAt = createdAt,
    relatedTasks = relatedTasks
)
