package com.kero.forgot.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ScheduleSend
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.VectorProperty.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.model.TodoItem
import com.kero.forgot.ui.components.TaskListCard

@Composable
fun TodoSection(
    todos: List<TodoItem>,
    onAddTodoClick: () -> Unit,
    onEditTodoClick: (TodoItem) -> Unit,
    onToggleTodoCompleted: (Long, Boolean) -> Unit,
    onMoveTodoToNextDay: (TodoItem) -> Unit,
    onArchiveTodo: (Long) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val radius = 10.dp
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        if (todos.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius))
                    .clickable(onClick = onAddTodoClick),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(radius)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Add a task for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            TaskListCard(
                items = todos,
                key = { it.id },
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                cornerRadius = radius
            ) { todo ->
                TodoRow(
                    todo = todo,
                    onToggleCompleted = { onToggleTodoCompleted(todo.id, !todo.isCompleted) },
                    onEdit = { onEditTodoClick(todo) },
                    onMoveNextDay = { onMoveTodoToNextDay(todo) },
                    onArchive = { onArchiveTodo(todo.id) },
                    onDelete = { onDeleteTodo(todo.id) }
                )
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoItem,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onMoveNextDay: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCompleted)
            .padding(start = 4.dp, end = 2.dp, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = { onToggleCompleted() },
            modifier = Modifier.size(38.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 5.dp)
        ) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (todo.isCompleted) FontWeight.Normal else FontWeight.Medium,
                color = if (todo.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (todo.notes.isNotBlank()) {
                Text(
                    text = todo.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Task options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move to next day") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ScheduleSend, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onMoveNextDay()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Archive") },
                    leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onArchive()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
