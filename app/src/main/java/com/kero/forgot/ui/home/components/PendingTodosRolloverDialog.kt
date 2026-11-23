package com.kero.forgot.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ScheduleSend
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.model.TodoItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val rolloverDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun PendingTodosRolloverDialog(
    pendingTodos: List<TodoItem>,
    today: LocalDate,
    onMoveSingleToToday: (Long) -> Unit,
    onArchiveSingle: (Long) -> Unit,
    onMoveAllToToday: () -> Unit,
    onArchiveAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Unfinished tasks") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingTodos, key = { it.id }) { todo ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = todo.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = todo.targetDate.format(rolloverDateFormatter),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onMoveSingleToToday(todo.id) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ScheduleSend,
                                    contentDescription = "Move to today",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onArchiveSingle(todo.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Archive,
                                    contentDescription = "Archive",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onMoveAllToToday,
                shape = CircleShape
            ) {
                Text("Move all to ${today.format(rolloverDateFormatter)}")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onArchiveAll,
                    shape = CircleShape
                ) {
                    Text("Archive all")
                }
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
