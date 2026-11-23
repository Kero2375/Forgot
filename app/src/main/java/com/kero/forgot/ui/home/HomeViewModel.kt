package com.kero.forgot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kero.forgot.domain.logic.ReminderScheduleEngine
import com.kero.forgot.domain.model.CompletionKey
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.model.ReminderOccurrence
import com.kero.forgot.domain.model.TimelineItem
import com.kero.forgot.domain.model.TodoItem
import com.kero.forgot.domain.repository.CategoryRepository
import com.kero.forgot.domain.repository.ReminderRepository
import com.kero.forgot.domain.repository.TodoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarViewMode {
    WEEK,
    MONTH
}

data class DaySnapshot(
    val date: LocalDate,
    val todos: List<TodoItem>,
    val timelineItems: List<TimelineItem>,
    val totalOccurrences: Int,
    val completedOccurrences: Int
)

data class DayMarker(
    val hasTodos: Boolean,
    val hasReminders: Boolean
)

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val calendarMode: CalendarViewMode = CalendarViewMode.WEEK,
    val categoriesById: Map<Long, ReminderCategory> = emptyMap(),
    val reminders: List<Reminder> = emptyList(),
    val completions: Set<CompletionKey> = emptySet(),
    val todosByDate: Map<LocalDate, List<TodoItem>> = emptyMap(),
    val pendingPastTodos: List<TodoItem> = emptyList()
) {
    fun snapshotFor(date: LocalDate): DaySnapshot {
        val occurrences = ReminderScheduleEngine.occurrencesForDate(
            reminders = reminders,
            categoriesById = categoriesById,
            completions = completions,
            date = date
        )
        val timelineItems = ReminderScheduleEngine.buildTimeline(occurrences)
        return DaySnapshot(
            date = date,
            todos = todosByDate[date].orEmpty(),
            timelineItems = timelineItems,
            totalOccurrences = occurrences.size,
            completedOccurrences = occurrences.count { it.isCompleted }
        )
    }

    fun markerFor(date: LocalDate): DayMarker {
        val hasTodos = todosByDate[date]?.any { !it.isCompleted } == true
        val hasReminders = reminders.any { reminder ->
            val categoryActive = categoriesById[reminder.categoryId]?.isActiveOn(date) != false
            reminder.isEnabled && categoryActive && ReminderScheduleEngine.occursOn(reminder, date)
        }
        return DayMarker(hasTodos = hasTodos, hasReminders = hasReminders)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val todoRepository: TodoRepository
) : ViewModel() {

    private val todayFlow = MutableStateFlow(LocalDate.now())
    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val calendarModeFlow = MutableStateFlow(CalendarViewMode.WEEK)

    private val observedMonthFlow = selectedDateFlow
        .map { YearMonth.from(it) }
        .distinctUntilChanged()

    private val activeTodosFlow = observedMonthFlow.flatMapLatest { month ->
        val start = month.minusMonths(2).atDay(1)
        val end = month.plusMonths(2).atEndOfMonth()
        todoRepository.observeActiveTodosInRange(start, end)
    }

    private val completionsFlow = observedMonthFlow.flatMapLatest { month ->
        val start = month.minusMonths(2).atDay(1)
        val end = month.plusMonths(2).atEndOfMonth()
        reminderRepository.observeCompletionsInRange(start, end)
    }

    private val pendingPastTodosFlow = todayFlow.flatMapLatest { today ->
        todoRepository.observePendingPastTodos(today)
    }

    private val dataFlow = combine(
        categoryRepository.observeCategories(),
        reminderRepository.observeReminders(),
        completionsFlow,
        activeTodosFlow,
        pendingPastTodosFlow
    ) { categories, reminders, completions, todos, pendingPast ->
        DataBundle(
            categoriesById = categories.associateBy { it.id },
            reminders = reminders,
            completions = completions,
            todosByDate = todos.groupBy { it.targetDate },
            pendingPastTodos = pendingPast
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        todayFlow,
        selectedDateFlow,
        calendarModeFlow,
        dataFlow
    ) { today, selectedDate, calendarMode, bundle ->
        HomeUiState(
            today = today,
            selectedDate = selectedDate,
            calendarMode = calendarMode,
            categoriesById = bundle.categoriesById,
            reminders = bundle.reminders,
            completions = bundle.completions,
            todosByDate = bundle.todosByDate,
            pendingPastTodos = bundle.pendingPastTodos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    fun refreshToday() {
        val now = LocalDate.now()
        if (todayFlow.value != now) {
            todayFlow.value = now
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun jumpToToday() {
        val now = LocalDate.now()
        todayFlow.value = now
        selectedDateFlow.value = now
    }

    fun toggleCalendarMode() {
        calendarModeFlow.value = when (calendarModeFlow.value) {
            CalendarViewMode.WEEK -> CalendarViewMode.MONTH
            CalendarViewMode.MONTH -> CalendarViewMode.WEEK
        }
    }

    fun setCalendarMode(mode: CalendarViewMode) {
        calendarModeFlow.value = mode
    }

    fun addTodo(title: String, notes: String, targetDate: LocalDate) {
        if (title.isBlank()) return
        viewModelScope.launch {
            todoRepository.addTodo(title = title, notes = notes, targetDate = targetDate)
        }
    }

    fun updateTodo(todo: TodoItem) {
        if (todo.title.isBlank()) return
        viewModelScope.launch {
            todoRepository.updateTodo(todo)
        }
    }

    fun toggleTodoCompleted(todoId: Long, completed: Boolean) {
        viewModelScope.launch {
            todoRepository.setTodoCompleted(todoId, completed)
        }
    }

    fun archiveTodo(todoId: Long) {
        viewModelScope.launch {
            todoRepository.archiveTodo(todoId)
        }
    }

    fun deleteTodo(todoId: Long) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todoId)
        }
    }

    fun moveTodoToDate(todoId: Long, newDate: LocalDate) {
        viewModelScope.launch {
            todoRepository.moveTodoToDate(todoId, newDate)
        }
    }

    fun moveAllPendingPastToToday() {
        val pendingIds = uiState.value.pendingPastTodos.map { it.id }
        val today = LocalDate.now()
        viewModelScope.launch {
            todoRepository.moveTodosToDate(pendingIds, today)
        }
    }

    fun archiveAllPendingPast() {
        val pendingIds = uiState.value.pendingPastTodos.map { it.id }
        viewModelScope.launch {
            todoRepository.archiveTodos(pendingIds)
        }
    }

    fun toggleOccurrenceCompleted(occurrence: ReminderOccurrence) {
        viewModelScope.launch {
            reminderRepository.toggleOccurrenceCompletion(
                reminderId = occurrence.reminderId,
                date = occurrence.date,
                time = occurrence.time,
                completed = !occurrence.isCompleted
            )
        }
    }

    private data class DataBundle(
        val categoriesById: Map<Long, ReminderCategory>,
        val reminders: List<Reminder>,
        val completions: Set<CompletionKey>,
        val todosByDate: Map<LocalDate, List<TodoItem>>,
        val pendingPastTodos: List<TodoItem>
    )

    companion object {
        fun factory(
            categoryRepository: CategoryRepository,
            reminderRepository: ReminderRepository,
            todoRepository: TodoRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(categoryRepository, reminderRepository, todoRepository) as T
            }
        }
    }
}
