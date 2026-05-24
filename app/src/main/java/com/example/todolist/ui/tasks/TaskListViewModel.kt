package com.example.todolist.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.model.Task
import com.example.todolist.data.repository.SearchHistoryRepository
import com.example.todolist.data.repository.TaskRepository
import com.example.todolist.data.repository.ThemeRepository
import com.example.todolist.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isSearchLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchHistory: List<String> = emptyList(),
    val isSearchFocused: Boolean = false
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val tokenRepository: TokenRepository,
    private val themeRepository: ThemeRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    val isDarkTheme: StateFlow<Boolean> = themeRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val searchQueryFlow = MutableStateFlow("")

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
                onSuccess = { tasks -> _uiState.update { it.copy(tasks = tasks, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true, error = null) }
            val result = taskRepository.getTasks(query.takeIf { it.isNotBlank() })
            result.fold(
                onSuccess = { tasks -> _uiState.update { it.copy(tasks = tasks, isSearchLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isSearchLoading = false) } }
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

    fun toggleTheme() {
        viewModelScope.launch {
            themeRepository.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun logout() {
        tokenRepository.clearToken()
    }
}
