package com.kero.forgot.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.ui.components.EmptyStateView
import com.kero.forgot.ui.components.ScreenTopHeader
import com.kero.forgot.ui.reminders.components.CategoryActivationSheet
import com.kero.forgot.ui.reminders.components.CategoryEditorDialog
import com.kero.forgot.ui.reminders.components.CategoryGroupCard
import com.kero.forgot.ui.reminders.components.ReminderEditorSheet
import java.time.LocalDate

@Composable
fun RemindersScreen(
    uiState: RemindersUiState,
    onSetCategoryActivation: (categoryId: Long, enabled: Boolean, activeFrom: LocalDate?, activeUntil: LocalDate?) -> Unit,
    onSaveCategory: (ReminderCategory) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onSaveReminder: (Reminder) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var creatingReminder by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ReminderCategory?>(null) }
    var creatingCategory by remember { mutableStateOf(false) }
    var activatingCategory by remember { mutableStateOf<ReminderCategory?>(null) }

    val totalReminders = uiState.groups.sumOf { it.reminders.size }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenTopHeader(
                title = "Reminders",
                icon = Icons.Rounded.NotificationsActive,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                badgeCount = totalReminders,
                badgeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                FilledTonalButton(
                    onClick = { creatingCategory = true },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        floatingActionButton = {
            if (uiState.categories.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { creatingReminder = true },
                    shape = RoundedCornerShape(20.dp),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Reminder") }
                )
            }
        }
    ) { innerPadding ->
        if (uiState.groups.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.NotificationsActive,
                title = "No categories yet",
                modifier = Modifier.padding(innerPadding),
                action = {
                    FilledTonalButton(
                        onClick = { creatingCategory = true },
                        shape = CircleShape
                    ) {
                        Text("New category")
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.groups, key = { it.category.id }) { group ->
                    CategoryGroupCard(
                        group = group,
                        onToggleCategory = { enabled ->
                            if (enabled) {
                                activatingCategory = group.category
                            } else {
                                onSetCategoryActivation(
                                    group.category.id,
                                    false,
                                    group.category.activeFrom,
                                    group.category.activeUntil
                                )
                            }
                        },
                        onScheduleCategoryClick = { activatingCategory = group.category },
                        onEditCategory = { editingCategory = group.category },
                        onToggleReminder = onToggleReminder,
                        onEditReminder = { editingReminder = it }
                    )
                }
            }
        }
    }

    activatingCategory?.let { category ->
        CategoryActivationSheet(
            category = category,
            onDismiss = { activatingCategory = null },
            onConfirmAlwaysActive = {
                onSetCategoryActivation(category.id, true, null, null)
                activatingCategory = null
            },
            onConfirmScheduled = { from, until ->
                onSetCategoryActivation(category.id, true, from, until)
                activatingCategory = null
            }
        )
    }

    if (creatingReminder) {
        ReminderEditorSheet(
            initialReminder = null,
            categories = uiState.categories,
            onDismiss = { creatingReminder = false },
            onSave = { reminder ->
                onSaveReminder(reminder)
                creatingReminder = false
            }
        )
    }

    editingReminder?.let { reminder ->
        ReminderEditorSheet(
            initialReminder = reminder,
            categories = uiState.categories,
            onDismiss = { editingReminder = null },
            onSave = { updated ->
                onSaveReminder(updated)
                editingReminder = null
            },
            onDelete = { id ->
                onDeleteReminder(id)
                editingReminder = null
            }
        )
    }

    if (creatingCategory) {
        CategoryEditorDialog(
            initialCategory = null,
            nextSortOrder = uiState.categories.size,
            onDismiss = { creatingCategory = false },
            onSave = { category ->
                onSaveCategory(category)
                creatingCategory = false
            }
        )
    }

    editingCategory?.let { category ->
        CategoryEditorDialog(
            initialCategory = category,
            nextSortOrder = category.sortOrder,
            onDismiss = { editingCategory = null },
            onSave = { updated ->
                onSaveCategory(updated)
                editingCategory = null
            },
            onDelete = if (uiState.categories.size > 1) {
                {
                    onDeleteCategory(category.id)
                    editingCategory = null
                }
            } else {
                null
            }
        )
    }
}
