package com.example.todolist.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todolist.ui.theme.PriorityColors
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ColumnScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) {
            scope.launch { snackbarHostState.showSnackbar("Пароль успешно изменён") }
            viewModel.clearPasswordChangeResult()
        }
    }

    LaunchedEffect(uiState.passwordChangeError) {
        uiState.passwordChangeError?.let { err ->
            scope.launch { snackbarHostState.showSnackbar(err) }
            viewModel.clearPasswordChangeResult()
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = uiState.isChangingPassword,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, new ->
                viewModel.changePassword(current, new)
                showChangePasswordDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы будете перенаправлены на экран входа.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) { Text("Выйти") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // Account section
            SettingsSection(title = "Аккаунт") {
                if (uiState.login.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("Пользователь") },
                        supportingContent = { Text(uiState.login) }
                    )
                    HorizontalDivider()
                }
                ListItem(
                    headlineContent = { Text("Изменить пароль") },
                    trailingContent = {
                        TextButton(onClick = { showChangePasswordDialog = true }) {
                            Text("Изменить")
                        }
                    }
                )
            }

            // Appearance section
            SettingsSection(title = "Внешний вид") {
                ListItem(
                    headlineContent = { Text("Тёмная тема") },
                    trailingContent = {
                        Switch(
                            checked = uiState.isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkTheme() }
                        )
                    }
                )
                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Размер шрифта") },
                    supportingContent = { Text(fontScaleLabel(uiState.fontScaleIndex)) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Мелкий" to 0, "Обычный" to 1, "Крупный" to 2).forEach { (label, index) ->
                        FilterChip(
                            selected = uiState.fontScaleIndex == index,
                            onClick = { viewModel.setFontScaleIndex(index) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Цвет приоритетов") },
                    supportingContent = { Text(accentSchemeLabel(uiState.accentScheme)) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Стандарт" to 0, "Пастель" to 1, "Моно" to 2).forEach { (label, index) ->
                        FilterChip(
                            selected = uiState.accentScheme == index,
                            onClick = { viewModel.setAccentScheme(index) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                AccentColorPreview(uiState.accentScheme)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Notifications section
            SettingsSection(title = "Уведомления") {
                ListItem(
                    headlineContent = { Text("Утреннее напоминание") },
                    supportingContent = { Text("Задачи на сегодня в 8:00") },
                    trailingContent = {
                        Switch(
                            checked = uiState.morningNotifEnabled,
                            onCheckedChange = { viewModel.setMorningNotif(it) }
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Вечернее напоминание") },
                    supportingContent = { Text("Итоги дня в 20:00") },
                    trailingContent = {
                        Switch(
                            checked = uiState.eveningNotifEnabled,
                            onCheckedChange = { viewModel.setEveningNotif(it) }
                        )
                    }
                )
            }

            // Danger zone
            SettingsSection(title = "Выход") {
                ListItem(
                    headlineContent = {
                        Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error)
                    },
                    trailingContent = {
                        TextButton(onClick = { showLogoutDialog = true }) {
                            Text("Выйти", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                content()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AccentColorPreview(scheme: Int) {
    val colors = PriorityColors.fromScheme(scheme)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Приоритеты:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        ColorDot(color = colors.low, label = "Низкий")
        ColorDot(color = colors.medium, label = "Средний")
        ColorDot(color = colors.high, label = "Высокий")
    }
}

@Composable
private fun ColorDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = color,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(12.dp)
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить пароль") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it; error = "" },
                    label = { Text("Текущий пароль") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it; error = "" },
                    label = { Text("Новый пароль") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it; error = "" },
                    label = { Text("Повторите новый пароль") },
                    singleLine = true
                )
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        current.isBlank() -> error = "Введите текущий пароль"
                        newPass.length < 6 -> error = "Новый пароль должен быть не менее 6 символов"
                        newPass != confirmPass -> error = "Пароли не совпадают"
                        else -> onConfirm(current, newPass)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Изменить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun fontScaleLabel(index: Int) = when (index) {
    0 -> "Мелкий"
    2 -> "Крупный"
    else -> "Обычный"
}

private fun accentSchemeLabel(index: Int) = when (index) {
    1 -> "Пастельные"
    2 -> "Монохромные"
    else -> "Стандартные"
}
