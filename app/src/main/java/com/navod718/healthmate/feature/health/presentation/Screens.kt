package com.navod718.healthmate.feature.health.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.navod718.healthmate.feature.health.data.DailyGoals
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.core.validation.RecordInput
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun displayDate(date: String): String = LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
private fun number(value: Long) = NumberFormat.getIntegerInstance().format(value)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(today: String, state: TotalsState, retry: () -> Unit, goals: DailyGoals = DailyGoals(), week: WeekState = WeekState(), busy: Boolean = false, onWater: (Int) -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your day, at a glance", style = MaterialTheme.typography.headlineMedium)
        Text(displayDate(today), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Small steps. Healthier habits.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Goals are personal targets. Change them using Daily goals in the toolbar.", style = MaterialTheme.typography.bodySmall)
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> ErrorPanel(state.error, retry)
            else -> {
                MetricCard("Steps walked", state.totals.steps, goals.steps, "steps", Icons.AutoMirrored.Filled.DirectionsWalk, Color(0xFF257449))
                MetricCard("Calories burned", state.totals.calories, goals.calories, "kcal", Icons.Default.LocalFireDepartment, Color(0xFFAE4C27))
                MetricCard("Water intake", state.totals.water, goals.water, "ml", Icons.Default.WaterDrop, Color(0xFF176CB0))
                Text("Quick water entry", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(250, 500).forEach { amount ->
                        FilledTonalButton(enabled = !busy, onClick = { onWater(amount) }, modifier = Modifier.testTag("water-$amount")) {
                            Icon(Icons.Default.WaterDrop, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+$amount ml")
                        }
                    }
                }
                Text("Each tap saves water for today. You can edit or delete the entry in Records.", style = MaterialTheme.typography.bodySmall)
                Text("Totals include all records you log for today. Add an entry after a walk, workout or water break.", style = MaterialTheme.typography.bodyMedium)
                Text("Your records stay on this device.", style = MaterialTheme.typography.labelLarge)
            }
        }
        WeeklyTrends(week, retry)
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun MetricCard(title: String, value: Long, goal: Int, unit: String, icon: ImageVector, tint: Color) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = .08f))) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp), tint = tint)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text("${number(value)} $unit", style = MaterialTheme.typography.headlineMedium, color = tint)
                val progress = DailyGoals().progress(value, goal)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = tint)
                Text("${(progress * 100).toInt()}% of ${number(goal.toLong())} $unit goal" + if (value >= goal) " · Goal reached" else "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RecordForm(state: EditorState, busy: Boolean, onChange: (RecordInput) -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val context = LocalContext.current
    val input = state.input
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val caloriesFocus = remember { FocusRequester() }
    val waterFocus = remember { FocusRequester() }
    val save = { focus.clearFocus(); keyboard?.hide(); onSave() }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (state.id == null) "Log a healthy moment" else "Update your activity", style = MaterialTheme.typography.headlineMedium)
        Text("Log an activity or your full day. Multiple entries on the same date are added together.")
        OutlinedButton(enabled = !busy, onClick = { pickDate(context, input.date) { onChange(input.copy(date = it)) } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CalendarMonth, null)
            Spacer(Modifier.width(8.dp))
            Text(displayDate(input.date))
        }
        state.errors["date"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        NumberField("Steps walked", "steps", input.steps, state.errors["steps"], !busy, imeAction = ImeAction.Next, keyboardActions = KeyboardActions(onNext = { caloriesFocus.requestFocus() })) { onChange(input.copy(steps = it)) }
        NumberField("Calories burned", "kcal", input.calories, state.errors["calories"], !busy, modifier = Modifier.focusRequester(caloriesFocus), imeAction = ImeAction.Next, keyboardActions = KeyboardActions(onNext = { waterFocus.requestFocus() })) { onChange(input.copy(calories = it)) }
        NumberField("Water intake", "ml", input.water, state.errors["water"], !busy, modifier = Modifier.focusRequester(waterFocus), imeAction = ImeAction.Done, keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() })) { onChange(input.copy(water = it)) }
        state.errors["form"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Use whole numbers. Enter 0 for activities you are not logging.", style = MaterialTheme.typography.bodySmall)
        Button(enabled = !busy, onClick = save, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(if (busy) "Saving…" else if (state.id == null) "Save record" else "Save changes")
        }
    }
}

@Composable
private fun NumberField(label: String, unit: String, value: String, error: String?, enabled: Boolean, modifier: Modifier = Modifier, imeAction: ImeAction = ImeAction.Done, keyboardActions: KeyboardActions = KeyboardActions.Default, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, suffix = { Text(unit) },
        enabled = enabled, isError = error != null, supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction), keyboardActions = keyboardActions, singleLine = true, modifier = modifier.fillMaxWidth())
}

@Composable
private fun ErrorPanel(message: String, retry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = retry) { Text("Retry") }
    }
}

internal fun pickDate(context: android.content.Context, current: String, onDate: (String) -> Unit) {
    val date = runCatching { LocalDate.parse(current) }.getOrDefault(LocalDate.now())
    DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
        datePicker.maxDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.show()
}
