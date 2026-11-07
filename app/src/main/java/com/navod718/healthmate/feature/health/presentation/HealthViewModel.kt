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

data class RecordsState(val loading: Boolean = true, val records: List<HealthRecord> = emptyList(), val totalCount: Int = 0, val error: String? = null)
data class TotalsState(val loading: Boolean = true, val totals: DailyTotals = DailyTotals(), val error: String? = null)
data class EditorState(
    val open: Boolean = false,
    val input: RecordInput = RecordInput(), val original: RecordInput = input, val errors: Map<String, String> = emptyMap(),
    val id: Long? = null, val loading: Boolean = false,
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
        val baseline = RecordInput(savedState["originalDate"] ?: LocalDate.now().toString(), savedState["originalSteps"] ?: "", savedState["originalCalories"] ?: "", savedState["originalWater"] ?: "")
        val input = RecordInput(savedState["date"] ?: baseline.date, savedState["steps"] ?: "", savedState["calories"] ?: "", savedState["water"] ?: "")
        return EditorState(open = true, input = input, original = baseline, id = savedState["editingId"], loading = savedState["editorLoading"] ?: false)
    }
    private fun setEditor(state: EditorState) {
        _editor.value = state
        savedState["open"] = state.open
        savedState["date"] = state.input.date
        savedState["originalDate"] = state.original.date
        savedState["steps"] = state.input.steps
        savedState["calories"] = state.input.calories
        savedState["water"] = state.input.water
        savedState["originalSteps"] = state.original.steps
        savedState["originalCalories"] = state.original.calories
        savedState["originalWater"] = state.original.water
        savedState["editingId"] = state.id
        savedState["editorLoading"] = state.loading
    }
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _tab = MutableStateFlow(savedState.get<String>("tab") ?: "dashboard")
    val tab = _tab.asStateFlow()
    private val _filter = MutableStateFlow(savedState.get<String>("filter"))
    val filter = _filter.asStateFlow()
    private val _deleteTarget = MutableStateFlow<HealthRecord?>(null)
    val deleteTarget = _deleteTarget.asStateFlow()
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError = _deleteError.asStateFlow()
    private val _samplesOpen = MutableStateFlow(false)
    val samplesOpen = _samplesOpen.asStateFlow()

    val records = combine(_filter, retry) { date, _ -> date }.flatMapLatest { date ->
        combine(repository.observeRecords(date), repository.observeCount()) { entries, count ->
            RecordsState(loading = false, records = entries, totalCount = count)
        }.onStart { emit(RecordsState()) }
            .catch { if (it is CancellationException) throw it; emit(RecordsState(loading = false, error = "Could not load records. Please retry.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsState())

    init { if (_editor.value.loading) _editor.value.id?.let(::loadEdit) }

    fun selectTab(value: String) {
        if (!_editor.value.open && value in listOf("dashboard", "records")) {
            _tab.value = value; savedState["tab"] = value
        }
    }
    fun filterByDate(value: String?) {
        _filter.value = value; savedState["filter"] = value
    }
    fun edit(id: Long) {
        if (_busy.value || _editor.value.open) return
        setEditor(EditorState(open = true, id = id, loading = true))
        loadEdit(id)
    }
    private fun loadEdit(id: Long) {
        viewModelScope.launch {
            try {
                val record = repository.find(id)
                if (!_editor.value.open || _editor.value.id != id || !_editor.value.loading) return@launch
                if (record == null) {
                    setEditor(EditorState()); _message.value = "This record no longer exists."
                } else {
                    val input = RecordInput(record.date, record.steps.toString(), record.calories.toString(), record.water.toString())
                    setEditor(EditorState(open = true, id = id, input = input))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (_editor.value.id == id && _editor.value.loading) {
                    setEditor(EditorState()); _message.value = "Could not open the record. Please try again."
                }
            }
        }
    }
    fun requestDelete(record: HealthRecord) {
        if (!_busy.value) { _deleteError.value = null; _deleteTarget.value = record }
    }
    fun cancelDelete() { if (!_busy.value) { _deleteTarget.value = null; _deleteError.value = null } }
    fun confirmDelete() {
        val target = _deleteTarget.value ?: return
        if (_busy.value) return
        _busy.value = true; _deleteError.value = null
        viewModelScope.launch {
            try {
                val removed = repository.delete(target.id)
                _deleteTarget.value = null
                _message.value = if (removed) "Record deleted." else "This record was already removed."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _deleteError.value = "Could not delete this record. Please retry or cancel."
            } finally { _busy.value = false }
        }
    }
    fun openSamples() { if (!_busy.value) _samplesOpen.value = true }
    fun closeSamples() { if (!_busy.value) _samplesOpen.value = false }
    fun loadSamples() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val loaded = repository.loadSamples()
                _samplesOpen.value = false
                if (loaded) filterByDate(null)
                _message.value = if (loaded) "Four sample records added." else "Samples can only be loaded when no records are saved."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _samplesOpen.value = false
                _message.value = "Could not load samples. Please try again."
            } finally { _busy.value = false }
        }
    }

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
        if (!_editor.value.loading && _editor.value.input != _editor.value.original) _discardPrompt.value = true
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
    fun change(input: RecordInput) { if (!_busy.value && !_editor.value.loading) setEditor(_editor.value.copy(input = input, errors = emptyMap())) }

    fun save() {
        val state = _editor.value
        if (_busy.value || !state.open || state.loading) return
        val errors = RecordValidator.validate(state.input)
        if (errors.isNotEmpty()) { _editor.value = state.copy(errors = errors); return }
        _busy.value = true
        viewModelScope.launch {
            try {
                if (state.id == null) repository.add(state.input)
                else if (!repository.update(state.id, state.input)) {
                    _editor.value = state.copy(errors = mapOf("form" to "This record no longer exists. Your changes were not saved."))
                    return@launch
                }
                setEditor(EditorState())
                _message.value = if (state.id == null) "Record saved." else "Record updated."
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _message.value = "Could not save. Your inputs are kept; please try again."
            } finally { _busy.value = false }
        }
    }
}
