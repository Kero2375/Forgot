package com.kero.forgot.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kero.forgot.di.AppContainer
import com.kero.forgot.ui.archive.ArchiveScreen
import com.kero.forgot.ui.archive.ArchiveViewModel
import com.kero.forgot.ui.home.HomeScreen
import com.kero.forgot.ui.home.HomeViewModel
import com.kero.forgot.ui.reminders.RemindersScreen
import com.kero.forgot.ui.reminders.RemindersViewModel

private enum class AppDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("Day", Icons.Rounded.Today),
    REMINDERS("Reminders", Icons.Rounded.NotificationsActive),
    ARCHIVE("Archive", Icons.Rounded.Archive)
}

@Composable
fun ForgotApp(
    container: AppContainer,
    requestedDestination: String? = null,
    onDestinationConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

    LaunchedEffect(requestedDestination) {
        when (requestedDestination) {
            "home" -> {
                currentDestination = AppDestination.HOME
                onDestinationConsumed()
            }
            "reminders" -> {
                currentDestination = AppDestination.REMINDERS
                onDestinationConsumed()
            }
        }
    }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            categoryRepository = container.categoryRepository,
            reminderRepository = container.reminderRepository,
            todoRepository = container.todoRepository
        )
    )
    val remindersViewModel: RemindersViewModel = viewModel(
        factory = RemindersViewModel.factory(
            categoryRepository = container.categoryRepository,
            reminderRepository = container.reminderRepository,
            reminderScheduler = container.reminderScheduler
        )
    )
    val archiveViewModel: ArchiveViewModel = viewModel(
        factory = ArchiveViewModel.factory(
            todoRepository = container.todoRepository
        )
    )

    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val remindersUiState by remindersViewModel.uiState.collectAsStateWithLifecycle()
    val archiveUiState by archiveViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = {
                            if (destination == AppDestination.HOME) {
                                homeViewModel.refreshToday()
                            }
                            currentDestination = destination
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (currentDestination) {
            AppDestination.HOME -> HomeScreen(
                uiState = homeUiState,
                onSelectDate = homeViewModel::selectDate,
                onJumpToToday = homeViewModel::jumpToToday,
                onToggleCalendarMode = homeViewModel::toggleCalendarMode,
                onAddTodo = homeViewModel::addTodo,
                onUpdateTodo = homeViewModel::updateTodo,
                onToggleTodoCompleted = homeViewModel::toggleTodoCompleted,
                onArchiveTodo = homeViewModel::archiveTodo,
                onDeleteTodo = homeViewModel::deleteTodo,
                onMoveTodoToDate = homeViewModel::moveTodoToDate,
                onMoveAllPendingPastToToday = homeViewModel::moveAllPendingPastToToday,
                onArchiveAllPendingPast = homeViewModel::archiveAllPendingPast,
                onToggleOccurrenceCompleted = homeViewModel::toggleOccurrenceCompleted,
                modifier = screenModifier
            )

            AppDestination.REMINDERS -> RemindersScreen(
                uiState = remindersUiState,
                onSetCategoryActivation = remindersViewModel::setCategoryActivation,
                onSaveCategory = remindersViewModel::saveCategory,
                onDeleteCategory = remindersViewModel::deleteCategory,
                onToggleReminder = remindersViewModel::toggleReminder,
                onSaveReminder = remindersViewModel::saveReminder,
                onDeleteReminder = remindersViewModel::deleteReminder,
                modifier = screenModifier
            )

            AppDestination.ARCHIVE -> ArchiveScreen(
                uiState = archiveUiState,
                onRestoreToToday = archiveViewModel::restoreToToday,
                onRestoreToTomorrow = archiveViewModel::restoreToTomorrow,
                onDeleteTodo = archiveViewModel::deleteTodo,
                modifier = screenModifier
            )
        }
    }
}
