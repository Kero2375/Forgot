package com.kero.forgot.ui.reminders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.ui.theme.CategoryPalette

@Composable
fun CategoryEditorDialog(
    initialCategory: ReminderCategory?,
    nextSortOrder: Int,
    onDismiss: () -> Unit,
    onSave: (ReminderCategory) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(initialCategory) { mutableStateOf(initialCategory?.name.orEmpty()) }
    var selectedColor by remember(initialCategory) {
        mutableLongStateOf(initialCategory?.colorArgb ?: CategoryPalette.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(if (initialCategory == null) "New category" else "Edit category")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryPalette.forEach { colorArgb ->
                        val isSelected = selectedColor == colorArgb
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(colorArgb))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColor = colorArgb }
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    onSave(
                        ReminderCategory(
                            id = initialCategory?.id ?: 0L,
                            name = name.trim(),
                            colorArgb = selectedColor,
                            isEnabled = initialCategory?.isEnabled ?: true,
                            sortOrder = nextSortOrder,
                            activeFrom = initialCategory?.activeFrom,
                            activeUntil = initialCategory?.activeUntil
                        )
                    )
                },
                enabled = name.isNotBlank(),
                shape = CircleShape
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = CircleShape
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
