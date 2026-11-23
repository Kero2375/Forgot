package com.kero.forgot.ui.reminders.components

import com.kero.forgot.ui.reminders.CategoryWithReminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.logic.ReminderScheduleEngine
import com.kero.forgot.domain.model.Reminder
import java.time.format.DateTimeFormatter

private val timeChipFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun CategoryGroupCard(
    group: CategoryWithReminders,
    onToggleCategory: (Boolean) -> Unit,
    onScheduleCategoryClick: () -> Unit,
    onEditCategory: () -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onEditReminder: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = group.category
    val accentColor = Color(category.colorArgb)
    val scheduleLabel = ReminderScheduleEngine.formatCategorySchedule(category)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = if (category.isEnabled) 0.13f else 0.06f))
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditCategory) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit category",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Switch(
                        checked = category.isEnabled,
                        onCheckedChange = onToggleCategory,
                    )
                }

                if (category.isEnabled) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onScheduleCategoryClick),
                        color = accentColor.copy(alpha = 0.16f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (scheduleLabel != null) {
                                    Icons.Rounded.DateRange
                                } else {
                                    Icons.Rounded.AllInclusive
                                },
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = scheduleLabel ?: "Always active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (group.reminders.isEmpty()) {
                Text(
                    text = "No reminders yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            } else {
                group.reminders.forEachIndexed { index, reminder ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                    ReminderItemRow(
                        reminder = reminder,
                        accentColor = accentColor,
                        categoryEnabled = category.isEnabled,
                        onToggle = { enabled -> onToggleReminder(reminder.id, enabled) },
                        onClick = { onEditReminder(reminder) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderItemRow(
    reminder: Reminder,
    accentColor: Color,
    categoryEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val effectiveAlpha = if (categoryEnabled && reminder.isEnabled) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .alpha(effectiveAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (reminder.schedule.isPeriodic) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Periodic",
                        modifier = Modifier.size(15.dp),
                        tint = accentColor
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                reminder.triggerTimes.distinct().sorted().forEach { time ->
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = time.format(timeChipFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = reminder.isEnabled,
            onCheckedChange = onToggle
        )
    }
}
