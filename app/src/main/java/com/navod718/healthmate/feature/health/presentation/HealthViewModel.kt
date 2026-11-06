package com.navod718.healthmate.feature.health.presentation

import androidx.lifecycle.SavedStateHandle
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
    val input: RecordInput = RecordInput(), val original: RecordInput = input, val errors: Map<String, String> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(private val repository: HealthRepository, private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val _today = MutableStateFlow(LocalDate.now().toString())
    val today = _today.asStateFlow()
    private val retry = MutableStateFlow(0)
    private val _editor = MutableStateFlow(restoreEditor())
    val editor = _editor.asStateFlow()
    private val _discardPrompt = MutableStateFlow(false)
    val discardPrompt = _discardPrompt.asStateFlow()
    private val _goalsOpen = MutableStateFlow(false)
    val goalsOpen = _goalsOpen.asStateFlow()
    val goals = repository.observeGoals().catch {
        if (it is CancellationException) throw it
        _message.value = "Could not load goals. Showing default targets."
        emit(DailyGoals())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyGoals())

    private fun restoreEditor(): EditorState {
        if (savedState.get<Boolean>("open") != true) return EditorState()
        val baseline = RecordInput(date = savedState["originalDate"] ?: LocalDate.now().toString())
        val input = RecordInput(savedState["date"] ?: baseline.date, savedState["steps"] ?: "", savedState["calories"] ?: "", savedState["water"] ?: "")
        return EditorState(open = true, input = input, original = baseline)
    }
    private fun setEditor(state: EditorState) {
        _editor.value = state
        savedState["open"] = state.open
        savedState["date"] = state.input.date
        savedState["originalDate"] = state.original.date
        savedState["steps"] = state.input.steps
        savedState["calories"] = state.input.calories
        savedState["water"] = state.input.water
    }
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
    fun add() { if (!_busy.value) setEditor(EditorState(open = true)) }
    fun closeEditor() {
        if (_busy.value) return
        if (_editor.value.input != _editor.value.original) _discardPrompt.value = true
        else setEditor(EditorState())
    }
    fun keepEditing() { _discardPrompt.value = false }
    fun discard() { if (!_busy.value) { _discardPrompt.value = false; setEditor(EditorState()) } }
    fun openGoals() { if (!_busy.value) _goalsOpen.value = true }
    fun closeGoals() { if (!_busy.value) _goalsOpen.value = false }
    fun saveGoals(value: DailyGoals) {
        if (_busy.value || !value.isValid()) return
        _busy.value = true
        viewModelScope.launch {
            try {
                repository.saveGoals(value)
                _goalsOpen.value = false
                _message.value = "Daily goals updated."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _message.value = "Could not save goals. Please try again."
            } finally { _busy.value = false }
        }
    }
    fun change(input: RecordInput) { if (!_busy.value) setEditor(_editor.value.copy(input = input, errors = emptyMap())) }

    fun save() {
        val state = _editor.value
        if (_busy.value || !state.open) return
        val errors = RecordValidator.validate(state.input)
        if (errors.isNotEmpty()) { _editor.value = state.copy(errors = errors); return }
        _busy.value = true
        viewModelScope.launch {
            try {
                repository.add(state.input)
                setEditor(EditorState())
                _message.value = "Record saved."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _message.value = "Could not save. Your inputs are kept; please try again."
            } finally { _busy.value = false }
        }
    }
}
