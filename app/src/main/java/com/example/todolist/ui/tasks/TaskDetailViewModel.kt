package com.example.todolist.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.model.Task
import com.example.todolist.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val allTasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var currentTaskId: Int = -1

    fun loadTask(taskId: Int) {
        currentTaskId = taskId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchTask()
        }
    }

    private fun refreshTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            fetchTask()
        }
    }

    private suspend fun fetchTask() {
        val result = taskRepository.getTasks()
        result.fold(
            onSuccess = { tasks ->
                val task = tasks.find { it.id == currentTaskId }
                _uiState.update { it.copy(task = task, allTasks = tasks, isLoading = false, isUpdating = false) }
            },
            onFailure = { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false, isUpdating = false) }
            }
        )
    }

    fun deleteTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = taskRepository.deleteTask(currentTaskId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isDeleted = true, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

    fun markDone() {
        viewModelScope.launch {
            val result = taskRepository.markDone(currentTaskId)
            result.fold(
                onSuccess = { task ->
                    _uiState.update { it.copy(task = task, actionMessage = "Статус обновлён") }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun addRelation(relatedId: Int) {
        viewModelScope.launch {
            val result = taskRepository.addRelation(currentTaskId, relatedId)
            result.fold(
                onSuccess = { refreshTask() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun removeRelation(relatedId: Int) {
        viewModelScope.launch {
            val result = taskRepository.removeRelation(currentTaskId, relatedId)
            result.fold(
                onSuccess = { refreshTask() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearActionMessage() = _uiState.update { it.copy(actionMessage = null) }
}
