package com.navod718.healthmate.feature.health.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.feature.health.data.DailyGoals

@Composable
fun GoalsDialog(state: GoalsState, busy: Boolean, onDismiss: () -> Unit, onSave: (DailyGoals) -> Unit, onRetry: () -> Unit) {
    val goals = state.value
    var steps by rememberSaveable(goals) { mutableStateOf(goals.steps.toString()) }
    var calories by rememberSaveable(goals) { mutableStateOf(goals.calories.toString()) }
    var water by rememberSaveable(goals) { mutableStateOf(goals.water.toString()) }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Daily goals") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose daily targets that fit your routine.")
                if (state.loadFailed) {
                    Text("Saved goals could not be refreshed. Showing the last available targets.", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, enabled = !busy) { Text("Retry loading goals") }
                }
                listOf(Triple("Steps goal", steps, { value: String -> steps = value }), Triple("Calories goal (kcal)", calories, { value: String -> calories = value }), Triple("Water goal (ml)", water, { value: String -> water = value })).forEach { (label, value, change) ->
                    OutlinedTextField(value = value, onValueChange = { change(it); error = false }, label = { Text(label) }, enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
                if (error) Text("Each goal must be a positive whole number up to 2,147,483,647.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                val values = listOf(steps, calories, water)
                if (values.any { !it.matches(Regex("[0-9]+")) || (it.toIntOrNull() ?: 0) <= 0 }) error = true
                else onSave(DailyGoals(steps = steps.toInt(), calories = calories.toInt(), water = water.toInt()))
            }) { Text(if (busy) "Saving…" else "Save goals") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}
