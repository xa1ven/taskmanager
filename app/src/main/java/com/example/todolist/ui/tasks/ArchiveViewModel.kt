package com.example.todolist.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.model.Task
import com.example.todolist.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = taskRepository.getTasks()
            result.fold(
                onSuccess = { data ->
                    val tasks = data.tasks
                    _uiState.update { it.copy(tasks = tasks.filter { t -> t.isDone }, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val result = taskRepository.getTasks()
            result.fold(
                onSuccess = { data ->
                    val tasks = data.tasks
                    _uiState.update { it.copy(tasks = tasks.filter { t -> t.isDone }, isRefreshing = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isRefreshing = false) }
                }
            )
        }
    }
}
