package com.example.todolist.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.model.Task
import com.example.todolist.data.repository.SearchHistoryRepository
import com.example.todolist.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SortFilterState(
    val sortOption: SortOption = SortOption.DATE_DESC,
    val onlyToday: Boolean = false,
    val onlyUrgent: Boolean = false
)

enum class SortOption {
    DATE_DESC, DATE_ASC,
    PRIORITY_DESC, PRIORITY_ASC,
    DEADLINE_ASC, DEADLINE_DESC
}

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSearchLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchHistory: List<String> = emptyList(),
    val isSearchFocused: Boolean = false,
    val sortFilterState: SortFilterState = SortFilterState()
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var rawTasks: List<Task> = emptyList()

    init {
        loadTasks()
        observeHistory()
        observeSearchQuery()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            searchHistoryRepository.history.collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(400)
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val query = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            val result = taskRepository.getTasks(query)
            result.fold(
                onSuccess = { tasks ->
                    rawTasks = tasks.filter { !it.isDone }
                    _uiState.update { state ->
                        state.copy(tasks = applySort(rawTasks, state.sortFilterState), isLoading = false)
                    }
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
            val query = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            val result = taskRepository.getTasks(query)
            result.fold(
                onSuccess = { tasks ->
                    rawTasks = tasks.filter { !it.isDone }
                    _uiState.update { state ->
                        state.copy(tasks = applySort(rawTasks, state.sortFilterState), isRefreshing = false)
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isRefreshing = false) }
                }
            )
        }
    }

    fun markTaskDone(taskId: Int) {
        viewModelScope.launch {
            val result = taskRepository.markDone(taskId)
            if (result.isSuccess) {
                rawTasks = rawTasks.filter { it.id != taskId }
                _uiState.update { it.copy(tasks = it.tasks.filter { t -> t.id != taskId }) }
            }
        }
    }

    fun applySortFilter(state: SortFilterState) {
        _uiState.update { it.copy(sortFilterState = state, tasks = applySort(rawTasks, state)) }
    }

    private fun applySort(tasks: List<Task>, state: SortFilterState): List<Task> {
        var result = tasks
        if (state.onlyToday) {
            val today = LocalDate.now().toString()
            result = result.filter { it.deadline?.take(10) == today }
        }
        if (state.onlyUrgent) {
            result = result.filter { it.priority == "HIGH" }
        }
        result = when (state.sortOption) {
            SortOption.DATE_DESC -> result.sortedByDescending { it.id }
            SortOption.DATE_ASC -> result.sortedBy { it.id }
            SortOption.PRIORITY_DESC -> result.sortedBy { priorityOrder(it.priority) }
            SortOption.PRIORITY_ASC -> result.sortedByDescending { priorityOrder(it.priority) }
            SortOption.DEADLINE_ASC -> result.sortedBy { it.deadline ?: "9999-99-99" }
            SortOption.DEADLINE_DESC -> result.sortedByDescending { it.deadline ?: "0000-00-00" }
        }
        return result
    }

    private fun priorityOrder(priority: String) = when (priority) {
        "HIGH" -> 0
        "MEDIUM" -> 1
        else -> 2
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true, error = null) }
            val result = taskRepository.getTasks(query.takeIf { it.isNotBlank() })
            result.fold(
                onSuccess = { tasks ->
                    rawTasks = tasks.filter { !it.isDone }
                    _uiState.update { state ->
                        state.copy(tasks = applySort(rawTasks, state.sortFilterState), isSearchLoading = false)
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isSearchLoading = false) }
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun onSearchFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isSearchFocused = focused) }
    }

    fun submitSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchHistoryRepository.addQuery(query)
        }
        performSearch(query)
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
        searchQueryFlow.value = ""
    }

    fun clearHistory() {
        viewModelScope.launch { searchHistoryRepository.clearHistory() }
    }
}
