package com.kero.forgot.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.model.TodoItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val editorDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun TodoEditorDialog(
    initialTodo: TodoItem?,
    targetDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String) -> Unit
) {
    var title by remember(initialTodo) { mutableStateOf(initialTodo?.title.orEmpty()) }
    var notes by remember(initialTodo) { mutableStateOf(initialTodo?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (initialTodo == null) {
                    "New task · ${targetDate.format(editorDateFormatter)}"
                } else {
                    "Edit task"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onSave(title, notes) },
                enabled = title.isNotBlank(),
                shape = CircleShape
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
