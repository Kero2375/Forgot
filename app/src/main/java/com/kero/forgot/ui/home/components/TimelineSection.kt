package com.kero.forgot.ui.home.components

import com.kero.forgot.ui.home.DaySnapshot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.logic.ReminderScheduleEngine
import com.kero.forgot.domain.model.ReminderOccurrence
import com.kero.forgot.domain.model.TimelineItem
import com.kero.forgot.ui.components.EmptyStateView
import java.time.format.DateTimeFormatter

private val timelineTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun TimelineSection(
    snapshot: DaySnapshot,
    onToggleOccurrenceCompleted: (ReminderOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        if (snapshot.timelineItems.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.NotificationsNone,
                title = "No reminders scheduled"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = snapshot.timelineItems,
                    key = { item ->
                        when (item) {
                            is TimelineItem.TimeSlot -> "slot_${item.time}"
                            is TimelineItem.TimeGap -> "gap_${item.startTime}_${item.endTime}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is TimelineItem.TimeSlot -> TimelineSlotRow(
                            slot = item,
                            onToggleOccurrenceCompleted = onToggleOccurrenceCompleted
                        )
                        is TimelineItem.TimeGap -> null //TimelineGapRow(gap = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSlotRow(
    slot: TimelineItem.TimeSlot,
    onToggleOccurrenceCompleted: (ReminderOccurrence) -> Unit
) {
    val allDone = slot.occurrences.all { it.isCompleted }
    val firstColor = Color(slot.occurrences.first().categoryColorArgb)
    val railColor = MaterialTheme.colorScheme.outlineVariant
    val dashColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (allDone) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                firstColor.copy(alpha = 0.14f)
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .width(56.dp)
        ) {
            Text(
                text = slot.time.format(timelineTimeFormatter),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (allDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    firstColor
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.width(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
            ){
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    drawLine(
                        color = dashColor,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(
                        if (allDone) MaterialTheme.colorScheme.outline
                        else firstColor
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    drawLine(
                        color = dashColor,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slot.occurrences.forEach { occurrence ->
                TimelineOccurrenceCard(
                    occurrence = occurrence,
                    onClick = { onToggleOccurrenceCompleted(occurrence) }
                )
            }
        }
    }
}

@Composable
private fun TimelineOccurrenceCard(
    occurrence: ReminderOccurrence,
    onClick: () -> Unit
) {
    val accentColor = Color(occurrence.categoryColorArgb)
    val cardAlpha = if (occurrence.isCompleted) 0.58f else 1f
    val containerColor = if (occurrence.isCompleted) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        accentColor.copy(alpha = 0.10f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (occurrence.isCompleted) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = cardAlpha))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
                    textDecoration = if (occurrence.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        color = accentColor.copy(alpha = if (occurrence.isCompleted) 0.10f else 0.18f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = occurrence.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = cardAlpha),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (occurrence.isPeriodic) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Periodic",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha)
                        )
                    }
                    if (occurrence.description.isNotBlank()) {
                        Text(
                            text = occurrence.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (occurrence.isCompleted) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = if (occurrence.isCompleted) "Completed" else "Mark completed",
                    tint = if (occurrence.isCompleted) {
                        accentColor
                    } else {
                        accentColor.copy(alpha = 0.75f)
                    }
                )
            }
        }
    }
}

@Composable
private fun TimelineGapRow(gap: TimelineItem.TimeGap) {
    val dashColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(56.dp))

        Box(
            modifier = Modifier
                .width(22.dp)
                .height(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                drawLine(
                    color = dashColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassEmpty,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${ReminderScheduleEngine.formatDuration(gap.durationMinutes)} free",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
