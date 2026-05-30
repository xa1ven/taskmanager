package com.example.todolist.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todolist.data.model.RelatedTaskResponse
import com.example.todolist.data.model.Task
import com.example.todolist.ui.common.EmptyTasksPlaceholder
import com.example.todolist.ui.common.ErrorPlaceholder
import com.example.todolist.ui.common.NoSearchResultsPlaceholder
import com.example.todolist.ui.theme.LocalPriorityColors
import com.example.todolist.ui.theme.RelationColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onOpenDrawer: () -> Unit,
    onAddTask: () -> Unit,
    onTaskClick: (Int) -> Unit,
    shouldRefresh: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showSortSheet by remember { mutableStateOf(false) }
    val isFiltersActive = uiState.sortFilterState != SortFilterState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.loadTasks()
            onRefreshHandled()
        }
    }

    LaunchedEffect(uiState.isOffline) {
        if (uiState.isOffline) {
            snackbarHostState.showSnackbar(
                message = "Сервер недоступен, данные могут быть устаревшими",
                duration = SnackbarDuration.Long
            )
        }
    }

    if (showSortSheet) {
        SortFilterSheet(
            currentState = uiState.sortFilterState,
            onApply = viewModel::applySortFilter,
            onDismiss = { showSortSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои задачи") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Сортировка и фильтры",
                            tint = if (isFiltersActive) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onSearch = { q ->
                    viewModel.submitSearch(q)
                    focusManager.clearFocus()
                },
                onClear = {
                    viewModel.clearSearch()
                    focusManager.clearFocus()
                },
                onFocusChanged = viewModel::onSearchFocusChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isSearchLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.isSearchFocused && uiState.searchQuery.isBlank() && uiState.searchHistory.isNotEmpty()) {
                SearchHistoryList(
                    history = uiState.searchHistory,
                    onItemClick = { query ->
                        viewModel.onSearchQueryChanged(query)
                        viewModel.submitSearch(query)
                        focusManager.clearFocus()
                    },
                    onClearHistory = viewModel::clearHistory
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        uiState.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        uiState.error != null -> {
                            ErrorPlaceholder(onRetry = viewModel::loadTasks)
                        }
                        uiState.tasks.isEmpty() && uiState.searchQuery.isBlank() -> {
                            EmptyTasksPlaceholder()
                        }
                        uiState.tasks.isEmpty() -> {
                            NoSearchResultsPlaceholder(uiState.searchQuery)
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.tasks, key = { it.id }) { task ->
                                    SwipeToMarkDone(
                                        onMarkDone = { viewModel.markTaskDone(task.id) },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(300),
                                            fadeOutSpec = tween(250),
                                            placementSpec = tween(350)
                                        )
                                    ) {
                                        TaskCard(task = task, onClick = {
                                            if (uiState.searchQuery.isNotBlank()) {
                                                viewModel.submitSearch(uiState.searchQuery)
                                            }
                                            onTaskClick(task.id)
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortFilterSheet(
    currentState: SortFilterState,
    onApply: (SortFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var sortOption by remember { mutableStateOf(currentState.sortOption) }
    var onlyToday by remember { mutableStateOf(currentState.onlyToday) }
    var onlyUrgent by remember { mutableStateOf(currentState.onlyUrgent) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Сортировка", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            SortRadioOption("По дате создания (новые сверху)", sortOption == SortOption.DATE_DESC) {
                sortOption = SortOption.DATE_DESC
            }
            SortRadioOption("По дате создания (старые сверху)", sortOption == SortOption.DATE_ASC) {
                sortOption = SortOption.DATE_ASC
            }
            SortRadioOption("По приоритету (высокий сверху)", sortOption == SortOption.PRIORITY_DESC) {
                sortOption = SortOption.PRIORITY_DESC
            }
            SortRadioOption("По приоритету (низкий сверху)", sortOption == SortOption.PRIORITY_ASC) {
                sortOption = SortOption.PRIORITY_ASC
            }
            SortRadioOption("По дедлайну (ближайшие сверху)", sortOption == SortOption.DEADLINE_ASC) {
                sortOption = SortOption.DEADLINE_ASC
            }
            SortRadioOption("По дедлайну (дальние сверху)", sortOption == SortOption.DEADLINE_DESC) {
                sortOption = SortOption.DEADLINE_DESC
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Фильтры", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            SortCheckboxOption("Только задачи на сегодня", onlyToday) { onlyToday = it }
            SortCheckboxOption("Только срочные (приоритет Высокий)", onlyUrgent) { onlyUrgent = it }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    onApply(SortFilterState())
                    onDismiss()
                }) {
                    Text("Сбросить")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onApply(SortFilterState(sortOption, onlyToday, onlyUrgent))
                    onDismiss()
                }) {
                    Text("Применить")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SortRadioOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SortCheckboxOption(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun RelationDots(relatedTasks: List<RelatedTaskResponse>) {
    val colors = relatedTasks
        .mapNotNull { RelationColors.colorMap[it.groupColor] }
        .distinct()
        .take(8)
    if (colors.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToMarkDone(
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onMarkDone()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    color = color,
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(contentAlignment = Alignment.CenterEnd) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Выполнить",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    }
                }
            }
        }
    ) {
        content()
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Поиск задач...") },
        singleLine = true,
        modifier = modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                }
            }
        }
    )
}

@Composable
private fun SearchHistoryList(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("История поиска", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = onClearHistory) { Text("Очистить") }
        }
        history.forEach { item ->
            ListItem(
                headlineContent = { Text(item) },
                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                modifier = Modifier.clickable { onItemClick(item) }
            )
        }
    }
}

@Composable
fun TaskCard(task: Task, onClick: () -> Unit) {
    val priorityColors = LocalPriorityColors.current
    val priorityColor = when (task.priority) {
        "HIGH" -> priorityColors.high
        "MEDIUM" -> priorityColors.medium
        else -> priorityColors.low
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
            ) {
                Surface(color = priorityColor, modifier = Modifier.fillMaxSize()) {}
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!task.relatedTasks.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RelationDots(task.relatedTasks)
                    }
                }
                task.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                task.deadline?.let { deadline ->
                    val formatted = formatDeadline(deadline)
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatDeadline(deadline: String): String {
    return try {
        val date = LocalDate.parse(deadline.take(10))
        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (e: Exception) {
        deadline
    }
}
