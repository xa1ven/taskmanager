package com.example.todolist.ui.tasks

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todolist.data.model.RelatedTaskResponse
import com.example.todolist.data.model.Task
import com.example.todolist.ui.theme.LocalPriorityColors
import com.example.todolist.ui.theme.PriorityLow
import com.example.todolist.ui.theme.RelationColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    onRelatedTaskClick: (Int) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddRelationSheet by remember { mutableStateOf(false) }
    var relationSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(taskId) { viewModel.loadTask(taskId) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch { snackbarHostState.showSnackbar(error) }
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            viewModel.clearActionMessage()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить задачу?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTask()
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showAddRelationSheet) {
        val relatedIds = uiState.task?.relatedTasks?.map { it.id } ?: emptyList()
        val filteredTasks = uiState.allTasks.filter { t ->
            t.id != taskId && t.id !in relatedIds &&
                (relationSearchQuery.isBlank() || t.title.contains(relationSearchQuery, ignoreCase = true))
        }

        ModalBottomSheet(onDismissRequest = { showAddRelationSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Добавить связь", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = relationSearchQuery,
                    onValueChange = { relationSearchQuery = it },
                    placeholder = { Text("Поиск задачи...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (filteredTasks.isEmpty()) {
                    Text(
                        "Нет доступных задач",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    filteredTasks.forEach { task ->
                        ListItem(
                            headlineContent = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.clickable {
                                viewModel.addRelation(task.id)
                                showAddRelationSheet = false
                                relationSearchQuery = ""
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.task?.title ?: "Задача", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        Crossfade(
            targetState = uiState.isLoading && uiState.task == null,
            animationSpec = tween(300),
            label = "detail_loading"
        ) { isLoading ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                val task = uiState.task
                if (task == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) { Text("Задача не найдена") }
                } else {
                    var itemsVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { itemsVisible = true }

                    val alpha1 by animateFloatAsState(
                        targetValue = if (itemsVisible) 1f else 0f,
                        animationSpec = tween(350),
                        label = "card1_alpha"
                    )
                    val alpha2 by animateFloatAsState(
                        targetValue = if (itemsVisible) 1f else 0f,
                        animationSpec = tween(350, delayMillis = 100),
                        label = "card2_alpha"
                    )

                    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        if (uiState.isUpdating) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                TaskInfoCard(
                                    task = task,
                                    onMarkDone = { viewModel.markDone() },
                                    modifier = Modifier.graphicsLayer { alpha = alpha1 }
                                )
                            }
                            item {
                                RelatedTasksSection(
                                    relatedTasks = task.relatedTasks ?: emptyList(),
                                    onTaskClick = onRelatedTaskClick,
                                    onRemoveRelation = { viewModel.removeRelation(it) },
                                    onAddRelation = { showAddRelationSheet = true },
                                    modifier = Modifier.graphicsLayer { alpha = alpha2 }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskInfoCard(task: Task, onMarkDone: () -> Unit, modifier: Modifier = Modifier) {
    val priorityColors = LocalPriorityColors.current
    val priorityColor = when (task.priority) {
        "HIGH" -> priorityColors.high
        "MEDIUM" -> priorityColors.medium
        else -> priorityColors.low
    }
    val priorityLabel = when (task.priority) {
        "HIGH" -> "Высокий"
        "MEDIUM" -> "Средний"
        else -> "Низкий"
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(task.title, style = MaterialTheme.typography.headlineSmall)

            task.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(desc, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Приоритет: ", style = MaterialTheme.typography.labelLarge)
                Surface(
                    color = priorityColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = priorityLabel,
                        color = priorityColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            task.deadline?.let { deadline ->
                val formatted = try {
                    LocalDate.parse(deadline.take(10)).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (e: Exception) { deadline }
                Text("Дедлайн: $formatted", style = MaterialTheme.typography.bodyMedium)
            }

            if (task.isDone) {
                Surface(
                    color = PriorityLow.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PriorityLow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Задача выполнена",
                            color = PriorityLow,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Button(
                    onClick = onMarkDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отметить выполненной")
                }
            }
        }
    }
}

@Composable
private fun RelatedTasksSection(
    relatedTasks: List<RelatedTaskResponse>,
    onTaskClick: (Int) -> Unit,
    onRemoveRelation: (Int) -> Unit,
    onAddRelation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Связанные задачи", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddRelation) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить")
                }
            }

            if (relatedTasks.isEmpty()) {
                Text(
                    "Нет связанных задач",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                relatedTasks.forEach { related ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskClick(related.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotColor = RelationColors.colorMap[related.groupColor]
                        if (dotColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = related.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (related.isDone) "Выполнена" else "Не выполнена",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (related.isDone) PriorityLow
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(
                            onClick = { onRemoveRelation(related.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Удалить связь",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
