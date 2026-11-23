package com.kero.forgot.ui.reminders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.ReminderSchedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class ScheduleKind(val label: String) {
    ONE_SHOT("Once"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

private sealed interface TimePickerMode {
    data object Add : TimePickerMode
    data class Edit(val originalTime: LocalTime) : TimePickerMode
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorSheet(
    initialReminder: Reminder?,
    categories: List<ReminderCategory>,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }

    var title by remember(initialReminder) { mutableStateOf(initialReminder?.title.orEmpty()) }
    var description by remember(initialReminder) { mutableStateOf(initialReminder?.description.orEmpty()) }
    var selectedCategoryId by remember(initialReminder, categories) {
        mutableStateOf(initialReminder?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }
    var isEnabled by remember(initialReminder) { mutableStateOf(initialReminder?.isEnabled ?: true) }

    var scheduleKind by remember(initialReminder) {
        mutableStateOf(
            when (initialReminder?.schedule) {
                is ReminderSchedule.OneShot -> ScheduleKind.ONE_SHOT
                is ReminderSchedule.Daily -> ScheduleKind.DAILY
                is ReminderSchedule.Weekly -> ScheduleKind.WEEKLY
                is ReminderSchedule.Monthly -> ScheduleKind.MONTHLY
                null -> ScheduleKind.DAILY
            }
        )
    }

    var oneShotDate by remember(initialReminder) {
        mutableStateOf((initialReminder?.schedule as? ReminderSchedule.OneShot)?.date ?: today)
    }
    var dailyInterval by remember(initialReminder) {
        mutableIntStateOf((initialReminder?.schedule as? ReminderSchedule.Daily)?.intervalDays ?: 1)
    }
    var weeklyDays by remember(initialReminder) {
        mutableStateOf(
            (initialReminder?.schedule as? ReminderSchedule.Weekly)?.daysOfWeek
                ?: setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                )
        )
    }
    var dayOfMonth by remember(initialReminder) {
        mutableIntStateOf((initialReminder?.schedule as? ReminderSchedule.Monthly)?.dayOfMonth ?: today.dayOfMonth)
    }
    var triggerTimes by remember(initialReminder) {
        mutableStateOf(
            initialReminder?.triggerTimes?.sorted() ?: listOf(LocalTime.of(9, 0))
        )
    }

    var timePickerMode by remember { mutableStateOf<TimePickerMode?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() &&
        selectedCategoryId != 0L &&
        triggerTimes.isNotEmpty() &&
        (scheduleKind != ScheduleKind.WEEKLY || weeklyDays.isNotEmpty())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (initialReminder == null) "New Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEnabled) "Active" else "Off",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (e.g., Brush teeth)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (optional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Category selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(category.colorArgb))
                                )
                            }
                        )
                    }
                }
            }

            // Schedule type selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ScheduleKind.entries.forEachIndexed { index, kind ->
                        SegmentedButton(
                            selected = scheduleKind == kind,
                            onClick = { scheduleKind = kind },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ScheduleKind.entries.size
                            )
                        ) {
                            Text(kind.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                when (scheduleKind) {
                    ScheduleKind.ONE_SHOT -> {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(oneShotDate.format(dateFormatter))
                        }
                    }

                    ScheduleKind.DAILY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (dailyInterval == 1) "Every day" else "Every $dailyInterval days",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (dailyInterval > 1) dailyInterval-- },
                                    enabled = dailyInterval > 1
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease interval")
                                }
                                Text(
                                    text = dailyInterval.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (dailyInterval < 30) dailyInterval++ }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase interval")
                                }
                            }
                        }
                    }

                    ScheduleKind.WEEKLY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DayOfWeek.entries.forEach { day ->
                                val selected = day in weeklyDays
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        weeklyDays = if (selected) {
                                            weeklyDays - day
                                        } else {
                                            weeklyDays + day
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = day.getDisplayName(
                                                TextStyle.NARROW,
                                                Locale.getDefault()
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    ScheduleKind.MONTHLY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Day of month",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (dayOfMonth > 1) dayOfMonth-- },
                                    enabled = dayOfMonth > 1
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Previous day")
                                }
                                Text(
                                    text = dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (dayOfMonth < 31) dayOfMonth++ },
                                    enabled = dayOfMonth < 31
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Next day")
                                }
                            }
                        }
                    }
                }
            }

            // Multiple reminder times section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Reminder times (${triggerTimes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(onClick = { timePickerMode = TimePickerMode.Add }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add time")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    triggerTimes.forEach { time ->
                        InputChip(
                            selected = true,
                            onClick = { timePickerMode = TimePickerMode.Edit(time) },
                            label = { Text(time.format(timeFormatter)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = if (triggerTimes.size > 1) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove time",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { triggerTimes = triggerTimes - time }
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initialReminder != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialReminder.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val existingStart = when (val s = initialReminder?.schedule) {
                            is ReminderSchedule.Daily -> s.startDate
                            is ReminderSchedule.Weekly -> s.startDate
                            is ReminderSchedule.Monthly -> s.startDate
                            else -> today
                        }
                        val schedule = when (scheduleKind) {
                            ScheduleKind.ONE_SHOT -> ReminderSchedule.OneShot(date = oneShotDate)
                            ScheduleKind.DAILY -> ReminderSchedule.Daily(
                                intervalDays = dailyInterval,
                                startDate = existingStart
                            )
                            ScheduleKind.WEEKLY -> ReminderSchedule.Weekly(
                                daysOfWeek = weeklyDays,
                                startDate = existingStart
                            )
                            ScheduleKind.MONTHLY -> ReminderSchedule.Monthly(
                                dayOfMonth = dayOfMonth,
                                startDate = existingStart
                            )
                        }
                        onSave(
                            Reminder(
                                id = initialReminder?.id ?: 0L,
                                title = title.trim(),
                                description = description.trim(),
                                categoryId = selectedCategoryId,
                                isEnabled = isEnabled,
                                schedule = schedule,
                                triggerTimes = triggerTimes.distinct().sorted(),
                                createdAtEpochMillis = initialReminder?.createdAtEpochMillis
                                    ?: System.currentTimeMillis()
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (initialReminder == null) "Create reminder" else "Save changes")
                }
            }
        }
    }

    timePickerMode?.let { mode ->
        val initialTime = when (mode) {
            TimePickerMode.Add -> triggerTimes.lastOrNull()?.plusHours(2) ?: LocalTime.of(9, 0)
            is TimePickerMode.Edit -> mode.originalTime
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )
        val isEditing = mode is TimePickerMode.Edit
        AlertDialog(
            onDismissRequest = { timePickerMode = null },
            title = { Text(if (isEditing) "Edit reminder time" else "Add reminder time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val picked = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        triggerTimes = when (mode) {
                            TimePickerMode.Add -> (triggerTimes + picked).distinct().sorted()
                            is TimePickerMode.Edit -> (triggerTimes - mode.originalTime + picked)
                                .distinct()
                                .sorted()
                        }
                        timePickerMode = null
                    }
                ) {
                    Text(if (isEditing) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerMode = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val initialMillis = oneShotDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            oneShotDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
