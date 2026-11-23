package com.kero.forgot.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kero.forgot.domain.model.ReminderOccurrence
import com.kero.forgot.domain.model.TodoItem
import com.kero.forgot.ui.home.components.CalendarHeader
import com.kero.forgot.ui.home.components.PendingTodosBanner
import com.kero.forgot.ui.home.components.PendingTodosRolloverDialog
import com.kero.forgot.ui.home.components.TimelineSection
import com.kero.forgot.ui.home.components.TodoEditorDialog
import com.kero.forgot.ui.home.components.TodoSection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val PAGER_CENTER = 5000
private const val PAGER_COUNT = 10001
private val dayTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onJumpToToday: () -> Unit,
    onToggleCalendarMode: () -> Unit,
    onAddTodo: (title: String, notes: String, date: LocalDate) -> Unit,
    onUpdateTodo: (TodoItem) -> Unit,
    onToggleTodoCompleted: (Long, Boolean) -> Unit,
    onArchiveTodo: (Long) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    onMoveTodoToDate: (Long, LocalDate) -> Unit,
    onMoveAllPendingPastToToday: () -> Unit,
    onArchiveAllPendingPast: () -> Unit,
    onToggleOccurrenceCompleted: (ReminderOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val anchorDate = remember { LocalDate.now() }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = PAGER_CENTER + ChronoUnit.DAYS.between(anchorDate, uiState.selectedDate).toInt(),
        pageCount = { PAGER_COUNT }
    )

    val displayedDate by remember(anchorDate) {
        derivedStateOf { anchorDate.plusDays((pagerState.currentPage - PAGER_CENTER).toLong()) }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                val settledDate = anchorDate.plusDays((settledPage - PAGER_CENTER).toLong())
                onSelectDate(settledDate)
            }
    }

    val scrollToDate: (LocalDate) -> Unit = { targetDate ->
        val targetPage = (PAGER_CENTER + ChronoUnit.DAYS.between(anchorDate, targetDate).toInt())
            .coerceIn(0, PAGER_COUNT - 1)
        if (pagerState.currentPage != targetPage) {
            coroutineScope.launch {
                if (abs(targetPage - pagerState.currentPage) <= 2) {
                    pagerState.animateScrollToPage(targetPage)
                } else {
                    pagerState.scrollToPage(targetPage)
                }
            }
        }
    }

    val navigateToDate: (LocalDate) -> Unit = { targetDate ->
        onSelectDate(targetDate)
        scrollToDate(targetDate)
    }

    val navigateToToday: () -> Unit = {
        onJumpToToday()
        scrollToDate(LocalDate.now())
    }

    var showRolloverDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var addingTodoForDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        CalendarHeader(
            today = uiState.today,
            selectedDate = displayedDate,
            calendarMode = uiState.calendarMode,
            markerFor = uiState::markerFor,
            onSelectDate = navigateToDate,
            onJumpToToday = navigateToToday,
            onToggleMode = onToggleCalendarMode
        )

        if (uiState.pendingPastTodos.isNotEmpty()) {
            PendingTodosBanner(
                count = uiState.pendingPastTodos.size,
                onReviewClick = { showRolloverDialog = true }
            )
        }

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val pageDate = remember(anchorDate, page) {
                anchorDate.plusDays((page - PAGER_CENTER).toLong())
            }
            val snapshot = remember(
                pageDate,
                uiState.reminders,
                uiState.categoriesById,
                uiState.completions,
                uiState.todosByDate
            ) {
                uiState.snapshotFor(pageDate)
            }
            DayPageContent(
                today = uiState.today,
                snapshot = snapshot,
                onAddTodoClick = { addingTodoForDate = pageDate },
                onEditTodoClick = { editingTodo = it },
                onToggleTodoCompleted = onToggleTodoCompleted,
                onMoveTodoToNextDay = { todo ->
                    onMoveTodoToDate(todo.id, todo.targetDate.plusDays(1))
                },
                onArchiveTodo = onArchiveTodo,
                onDeleteTodo = onDeleteTodo,
                onToggleOccurrenceCompleted = onToggleOccurrenceCompleted
            )
        }
    }

    if (showRolloverDialog && uiState.pendingPastTodos.isNotEmpty()) {
        PendingTodosRolloverDialog(
            pendingTodos = uiState.pendingPastTodos,
            today = uiState.today,
            onMoveSingleToToday = { todoId -> onMoveTodoToDate(todoId, uiState.today) },
            onArchiveSingle = onArchiveTodo,
            onMoveAllToToday = {
                onMoveAllPendingPastToToday()
                showRolloverDialog = false
            },
            onArchiveAll = {
                onArchiveAllPendingPast()
                showRolloverDialog = false
            },
            onDismiss = { showRolloverDialog = false }
        )
    }

    addingTodoForDate?.let { targetDate ->
        TodoEditorDialog(
            initialTodo = null,
            targetDate = targetDate,
            onDismiss = { addingTodoForDate = null },
            onSave = { title, notes ->
                onAddTodo(title, notes, targetDate)
                addingTodoForDate = null
            }
        )
    }

    editingTodo?.let { todo ->
        TodoEditorDialog(
            initialTodo = todo,
            targetDate = todo.targetDate,
            onDismiss = { editingTodo = null },
            onSave = { title, notes ->
                onUpdateTodo(todo.copy(title = title, notes = notes))
                editingTodo = null
            }
        )
    }
}

@Composable
private fun DayPageContent(
    today: LocalDate,
    snapshot: DaySnapshot,
    onAddTodoClick: () -> Unit,
    onEditTodoClick: (TodoItem) -> Unit,
    onToggleTodoCompleted: (Long, Boolean) -> Unit,
    onMoveTodoToNextDay: (TodoItem) -> Unit,
    onArchiveTodo: (Long) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    onToggleOccurrenceCompleted: (ReminderOccurrence) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxTodoSectionHeight = maxHeight * 0.44f

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val relativeLabel = when (snapshot.date) {
                    today -> "Today · ${snapshot.date.format(shortDateFormatter)}"
                    today.plusDays(1) -> "Tomorrow · ${snapshot.date.format(shortDateFormatter)}"
                    today.minusDays(1) -> "Yesterday · ${snapshot.date.format(shortDateFormatter)}"
                    else -> snapshot.date.format(dayTitleFormatter)
                }
                Text(
                    text = relativeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FilledTonalButton(
                    onClick = onAddTodoClick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Task", style = MaterialTheme.typography.labelLarge)
                }
            }

            TodoSection(
                todos = snapshot.todos,
                onAddTodoClick = onAddTodoClick,
                onEditTodoClick = onEditTodoClick,
                onToggleTodoCompleted = onToggleTodoCompleted,
                onMoveTodoToNextDay = onMoveTodoToNextDay,
                onArchiveTodo = onArchiveTodo,
                onDeleteTodo = onDeleteTodo,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxTodoSectionHeight)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            TimelineSection(
                snapshot = snapshot,
                onToggleOccurrenceCompleted = onToggleOccurrenceCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
