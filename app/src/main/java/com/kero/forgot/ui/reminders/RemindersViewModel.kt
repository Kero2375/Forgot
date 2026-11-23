package com.kero.forgot.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kero.forgot.domain.model.Reminder
import com.kero.forgot.domain.model.ReminderCategory
import com.kero.forgot.domain.repository.CategoryRepository
import com.kero.forgot.domain.repository.ReminderRepository
import com.kero.forgot.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CategoryWithReminders(
    val category: ReminderCategory,
    val reminders: List<Reminder>
)

data class RemindersUiState(
    val groups: List<CategoryWithReminders> = emptyList(),
    val categories: List<ReminderCategory> = emptyList()
)

class RemindersViewModel(
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = combine(
        categoryRepository.observeCategories(),
        reminderRepository.observeReminders()
    ) { categories, reminders ->
        val byCategory = reminders.groupBy { it.categoryId }
        val groups = categories.map { category ->
            CategoryWithReminders(
                category = category,
                reminders = byCategory[category.id].orEmpty()
            )
        }
        RemindersUiState(groups = groups, categories = categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = RemindersUiState()
    )

    fun setCategoryActivation(
        categoryId: Long,
        enabled: Boolean,
        activeFrom: LocalDate? = null,
        activeUntil: LocalDate? = null
    ) {
        viewModelScope.launch {
            categoryRepository.setCategoryActivation(
                categoryId = categoryId,
                enabled = enabled,
                activeFrom = activeFrom,
                activeUntil = activeUntil
            )
            reminderScheduler.syncAll()
        }
    }

    fun saveCategory(category: ReminderCategory) {
        if (category.name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.upsertCategory(category.copy(name = category.name.trim()))
            reminderScheduler.syncAll()
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            reminderScheduler.syncAll()
        }
    }

    fun toggleReminder(reminderId: Long, enabled: Boolean) {
        viewModelScope.launch {
            reminderRepository.setReminderEnabled(reminderId, enabled)
            reminderScheduler.syncReminder(reminderId)
        }
    }

    fun saveReminder(reminder: Reminder) {
        if (reminder.title.isBlank() || reminder.triggerTimes.isEmpty()) return
        viewModelScope.launch {
            val savedId = reminderRepository.upsertReminder(
                reminder.copy(
                    title = reminder.title.trim(),
                    description = reminder.description.trim(),
                    triggerTimes = reminder.triggerTimes.distinct().sorted()
                )
            )
            val effectiveId = if (reminder.id != 0L) reminder.id else savedId
            reminderScheduler.syncReminder(effectiveId)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(reminderId)
            reminderRepository.deleteReminder(reminderId)
        }
    }

    companion object {
        fun factory(
            categoryRepository: CategoryRepository,
            reminderRepository: ReminderRepository,
            reminderScheduler: ReminderScheduler
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RemindersViewModel(
                    categoryRepository,
                    reminderRepository,
                    reminderScheduler
                ) as T
            }
        }
    }
}
