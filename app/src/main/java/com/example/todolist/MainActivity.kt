package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.todolist.data.repository.TokenRepository
import com.example.todolist.ui.navigation.AppNavGraph
import com.example.todolist.ui.navigation.Screen
import com.example.todolist.ui.tasks.TaskListViewModel
import com.example.todolist.ui.theme.ToDoListTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenRepository: TokenRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (tokenRepository.hasToken()) {
            Screen.TaskList.route
        } else {
            Screen.Login.route
        }

        setContent {
            val navController = rememberNavController()
            val themeViewModel: TaskListViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()

            ToDoListTheme(darkTheme = isDarkTheme) {
                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { themeViewModel.toggleTheme() }
                )
            }
        }
    }
}
