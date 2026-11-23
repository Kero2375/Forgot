package com.kero.forgot.ui.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kero.forgot.ui.archive.components.ArchivedTodoRow
import com.kero.forgot.ui.components.TaskListCard
import com.kero.forgot.ui.components.EmptyStateView
import com.kero.forgot.ui.components.ScreenTopHeader

@Composable
fun ArchiveScreen(
    uiState: ArchiveUiState,
    onRestoreToToday: (Long) -> Unit,
    onRestoreToTomorrow: (Long) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenTopHeader(
                title = "Archive",
                icon = Icons.Rounded.Archive,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                badgeCount = uiState.archivedTodos.size
            )
        }
    ) { innerPadding ->
        if (uiState.archivedTodos.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.Inventory2,
                title = "No archived tasks",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TaskListCard(
                    items = uiState.archivedTodos,
                    key = { it.id },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    cornerRadius = 10.dp,
                    tonalElevation = 1.dp
                ) { todo ->
                    ArchivedTodoRow(
                        todo = todo,
                        onRestoreToToday = { onRestoreToToday(todo.id) },
                        onRestoreToTomorrow = { onRestoreToTomorrow(todo.id) },
                        onDelete = { onDeleteTodo(todo.id) }
                    )
                }
            }
        }
    }
}
