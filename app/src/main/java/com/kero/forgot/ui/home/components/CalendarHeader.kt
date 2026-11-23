package com.kero.forgot.ui.home.components

import com.kero.forgot.ui.home.CalendarViewMode
import com.kero.forgot.ui.home.DayMarker

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val weekDayOrder = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

@Composable
fun CalendarHeader(
    today: LocalDate,
    selectedDate: LocalDate,
    calendarMode: CalendarViewMode,
    markerFor: (LocalDate) -> DayMarker,
    onSelectDate: (LocalDate) -> Unit,
    onJumpToToday: () -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val target = when (calendarMode) {
                            CalendarViewMode.WEEK -> selectedDate.minusWeeks(1)
                            CalendarViewMode.MONTH -> selectedDate.minusMonths(1)
                        }
                        onSelectDate(target)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = selectedDate.format(monthYearFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                FilledTonalIconButton(
                    onClick = {
                        val target = when (calendarMode) {
                            CalendarViewMode.WEEK -> selectedDate.plusWeeks(1)
                            CalendarViewMode.MONTH -> selectedDate.plusMonths(1)
                        }
                        onSelectDate(target)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (selectedDate != today) {
                    FilledTonalButton(
                        onClick = onJumpToToday,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onToggleMode,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = when (calendarMode) {
                            CalendarViewMode.WEEK -> Icons.Rounded.CalendarMonth
                            CalendarViewMode.MONTH -> Icons.Rounded.CalendarViewWeek
                        },
                        contentDescription = when (calendarMode) {
                            CalendarViewMode.WEEK -> "Switch to month view"
                            CalendarViewMode.MONTH -> "Switch to week view"
                        },
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDayOrder.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedContent(
                targetState = calendarMode,
                label = "calendar_mode"
            ) { mode ->
                when (mode) {
                    CalendarViewMode.WEEK -> WeekStrip(
                        today = today,
                        selectedDate = selectedDate,
                        markerFor = markerFor,
                        onSelectDate = onSelectDate
                    )
                    CalendarViewMode.MONTH -> MonthGrid(
                        today = today,
                        selectedDate = selectedDate,
                        markerFor = markerFor,
                        onSelectDate = onSelectDate
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(
    today: LocalDate,
    selectedDate: LocalDate,
    markerFor: (LocalDate) -> DayMarker,
    onSelectDate: (LocalDate) -> Unit
) {
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            DayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                isInCurrentMonth = true,
                marker = markerFor(date),
                onClick = { onSelectDate(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    today: LocalDate,
    selectedDate: LocalDate,
    markerFor: (LocalDate) -> DayMarker,
    onSelectDate: (LocalDate) -> Unit
) {
    val yearMonth = YearMonth.from(selectedDate)
    val firstOfMonth = yearMonth.atDay(1)
    val gridStart = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastOfMonth = yearMonth.atEndOfMonth()
    val gridEnd = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(gridStart, gridEnd).toInt() + 1
    val weeks = totalDays / 7

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (week in 0 until weeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dayIndex in 0..6) {
                    val date = gridStart.plusDays((week * 7 + dayIndex).toLong())
                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isInCurrentMonth = date.month == yearMonth.month,
                        marker = markerFor(date),
                        onClick = { onSelectDate(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isInCurrentMonth: Boolean,
    marker: DayMarker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellShape = RoundedCornerShape(14.dp)
    val badgeBg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isInCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .clip(cellShape)
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(badgeBg)
                .then(
                    if (isToday && !isSelected) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.height(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (marker.hasTodos) {
                Box(
                    modifier = Modifier
                        .size(width = 7.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
            if (marker.hasReminders) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
