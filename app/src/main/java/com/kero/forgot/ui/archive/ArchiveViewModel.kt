package com.kero.forgot.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kero.forgot.domain.model.TodoItem
import com.kero.forgot.domain.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ArchiveUiState(
    val archivedTodos: List<TodoItem> = emptyList()
)

class ArchiveViewModel(
    private val todoRepository: TodoRepository
) : ViewModel() {

    val uiState: StateFlow<ArchiveUiState> = todoRepository.observeArchivedTodos()
        .map { ArchiveUiState(archivedTodos = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ArchiveUiState()
        )

    fun restoreToToday(todoId: Long) {
        viewModelScope.launch {
            todoRepository.restoreTodo(todoId, LocalDate.now())
        }
    }

    fun restoreToTomorrow(todoId: Long) {
        viewModelScope.launch {
            todoRepository.restoreTodo(todoId, LocalDate.now().plusDays(1))
        }
    }

    fun deleteTodo(todoId: Long) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todoId)
        }
    }

    companion object {
        fun factory(todoRepository: TodoRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ArchiveViewModel(todoRepository) as T
                }
            }
    }
}
