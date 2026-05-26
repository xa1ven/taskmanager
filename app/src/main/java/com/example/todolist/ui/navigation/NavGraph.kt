package com.example.todolist.ui.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.todolist.ui.about.AboutScreen
import com.example.todolist.ui.auth.LoginScreen
import com.example.todolist.ui.auth.RegisterScreen
import com.example.todolist.ui.settings.SettingsScreen
import com.example.todolist.ui.tasks.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object TaskList : Screen("task_list")
    object Archive : Screen("archive")
    object Settings : Screen("settings")
    object About : Screen("about")
    object AddTask : Screen("add_task")
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Int) = "edit_task/$taskId"
    }
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Int) = "task_detail/$taskId"
    }
}

private val drawerRoutes = setOf(
    Screen.TaskList.route,
    Screen.Archive.route,
    Screen.Settings.route,
    Screen.About.route
)

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showDrawer = currentRoute in drawerRoutes

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Screen.TaskList.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) },
            exitTransition = { fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) },
            popExitTransition = { fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) }
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.TaskList.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.TaskList.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.TaskList.route) { backStackEntry ->
                val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh") == true
                TaskListScreen(
                    onOpenDrawer = openDrawer,
                    onAddTask = { navController.navigate(Screen.AddTask.route) },
                    onTaskClick = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    },
                    shouldRefresh = shouldRefresh,
                    onRefreshHandled = {
                        backStackEntry.savedStateHandle.remove<Boolean>("refresh")
                    }
                )
            }

            composable(Screen.Archive.route) {
                ArchiveScreen(
                    onOpenDrawer = openDrawer,
                    onTaskClick = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenDrawer = openDrawer,
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(onOpenDrawer = openDrawer)
            }

            composable(
                route = Screen.AddTask.route,
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(180))
                }
            ) {
                AddEditTaskScreen(
                    taskId = null,
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditTask.route,
                arguments = listOf(navArgument("taskId") { type = NavType.IntType }),
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(180))
                }
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getInt("taskId")
                AddEditTaskScreen(
                    taskId = taskId,
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(navArgument("taskId") { type = NavType.IntType }),
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(180))
                }
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
                TaskDetailScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Screen.EditTask.createRoute(taskId)) },
                    onDeleted = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                        navController.popBackStack()
                    },
                    onRelatedTaskClick = { relatedId ->
                        navController.navigate(Screen.TaskDetail.createRoute(relatedId))
                    }
                )
            }
        }
    }
}
