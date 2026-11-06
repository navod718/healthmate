package com.navod718.healthmate.feature.health.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.core.validation.RecordValidator
import com.navod718.healthmate.feature.health.data.*
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TotalsState(val loading: Boolean = true, val totals: DailyTotals = DailyTotals(), val error: String? = null)
data class EditorState(
    val open: Boolean = false,
    val input: RecordInput = RecordInput(), val errors: Map<String, String> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(private val repository: HealthRepository) : ViewModel() {
    private val _today = MutableStateFlow(LocalDate.now().toString())
    val today = _today.asStateFlow()
    private val retry = MutableStateFlow(0)
    private val _editor = MutableStateFlow(EditorState())
    val editor = _editor.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val totals = combine(_today, retry) { date, _ -> date }.flatMapLatest { date ->
        repository.observeTotals(date).map { TotalsState(loading = false, totals = it) }
            .onStart { emit(TotalsState()) }
            .catch { if (it is CancellationException) throw it; emit(TotalsState(loading = false, error = "Could not load today's totals. Please retry.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TotalsState())

    fun refreshToday() { _today.value = LocalDate.now().toString() }
    fun retry() { retry.value++ }
    fun clearMessage() { _message.value = null }
    fun add() { if (!_busy.value) _editor.value = EditorState(open = true) }
    fun closeEditor() { if (!_busy.value) _editor.value = EditorState() }
    fun change(input: RecordInput) { _editor.value = _editor.value.copy(input = input, errors = emptyMap()) }

    fun save() {
        val state = _editor.value
        if (_busy.value || !state.open) return
        val errors = RecordValidator.validate(state.input)
        if (errors.isNotEmpty()) { _editor.value = state.copy(errors = errors); return }
        _busy.value = true
        viewModelScope.launch {
            try {
                repository.add(state.input)
                _editor.value = EditorState()
                _message.value = "Record saved."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _message.value = "Could not save. Your inputs are kept; please try again."
            } finally { _busy.value = false }
        }
    }
}
