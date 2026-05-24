package com.example.todolist.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.model.Task
import com.example.todolist.data.model.TaskRequest
import com.example.todolist.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val title: String = "",
    val description: String = "",
    val priority: String = "MEDIUM",
    val deadline: String = "",
    val isDone: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isLoadingTask: Boolean = false
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private var editingTaskId: Int? = null

    fun loadTask(taskId: Int) {
        if (editingTaskId == taskId) return
        editingTaskId = taskId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTask = true) }
            val result = taskRepository.getTasks()
            result.fold(
                onSuccess = { tasks ->
                    val task = tasks.find { it.id == taskId }
                    if (task != null) {
                        _uiState.update {
                            it.copy(
                                title = task.title,
                                description = task.description ?: "",
                                priority = task.priority,
                                deadline = task.deadline ?: "",
                                isDone = task.isDone,
                                isLoadingTask = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoadingTask = false, error = "Задача не найдена") }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoadingTask = false, error = e.message) }
                }
            )
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onPriorityChanged(value: String) = _uiState.update { it.copy(priority = value) }
    fun onDeadlineChanged(value: String) = _uiState.update { it.copy(deadline = value) }
    fun onIsDoneChanged(value: Boolean) = _uiState.update { it.copy(isDone = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Введите название задачи") }
            return
        }
        val request = TaskRequest(
            title = state.title.trim(),
            description = state.description.trim().takeIf { it.isNotBlank() },
            priority = state.priority,
            deadline = state.deadline.trim().takeIf { it.isNotBlank() },
            isDone = state.isDone
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (editingTaskId != null) {
                taskRepository.updateTask(editingTaskId!!, request)
            } else {
                taskRepository.createTask(request)
            }
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaved = true, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }
}
