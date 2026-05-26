package com.example.todolist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.todolist.data.repository.SettingsRepository
import com.example.todolist.data.repository.TokenRepository
import com.example.todolist.ui.navigation.AppNavGraph
import com.example.todolist.ui.navigation.Screen
import com.example.todolist.ui.notifications.NotificationScheduler
import com.example.todolist.ui.settings.SettingsViewModel
import com.example.todolist.ui.theme.ToDoListTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenRepository: TokenRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scheduleNotificationsFromSettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        val startDestination = if (tokenRepository.hasToken()) {
            Screen.TaskList.route
        } else {
            Screen.Login.route
        }

        setContent {
            val navController = rememberNavController()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            ToDoListTheme(
                darkTheme = uiState.isDarkTheme,
                fontScaleIndex = uiState.fontScaleIndex,
                accentScheme = uiState.accentScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    scheduleNotificationsFromSettings()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleNotificationsFromSettings()
        }
    }

    private fun scheduleNotificationsFromSettings() {
        lifecycleScope.launch {
            val morningEnabled = settingsRepository.morningNotifEnabled.first()
            val eveningEnabled = settingsRepository.eveningNotifEnabled.first()
            if (morningEnabled) NotificationScheduler.scheduleMorningNotification(this@MainActivity)
            else NotificationScheduler.cancelMorningNotification(this@MainActivity)
            if (eveningEnabled) NotificationScheduler.scheduleEveningNotification(this@MainActivity)
            else NotificationScheduler.cancelEveningNotification(this@MainActivity)
        }
    }
}
