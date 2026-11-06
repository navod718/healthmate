package com.navod718.healthmate.feature.health.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun DashboardScreen(today: String, state: TotalsState, retry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your day, at a glance", style = MaterialTheme.typography.headlineMedium)
        Text(displayDate(today), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Small steps. Healthier habits.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> ErrorPanel(state.error, retry)
            else -> {
                MetricCard("Steps walked", number(state.totals.steps), "steps", Icons.AutoMirrored.Filled.DirectionsWalk, Color(0xFF257449))
                MetricCard("Calories burned", number(state.totals.calories), "kcal", Icons.Default.LocalFireDepartment, Color(0xFFAE4C27))
                MetricCard("Water intake", number(state.totals.water), "ml", Icons.Default.WaterDrop, Color(0xFF176CB0))
                Text("Totals include all records you log for today. Add an entry after a walk, workout or water break.", style = MaterialTheme.typography.bodyMedium)
                Text("Your records stay on this device.", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun MetricCard(title: String, value: String, unit: String, icon: ImageVector, tint: Color) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = .08f))) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp), tint = tint)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text("$value $unit", style = MaterialTheme.typography.headlineMedium, color = tint)
            }
        }
    }
}

@Composable
fun RecordForm(state: EditorState, busy: Boolean, onChange: (RecordInput) -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val input = state.input
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Log a healthy moment", style = MaterialTheme.typography.headlineMedium)
        Text("Log an activity or your full day. Multiple entries on the same date are added together.")
        OutlinedButton(enabled = !busy, onClick = { pickDate(context, input.date) { onChange(input.copy(date = it)) } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CalendarMonth, null)
            Spacer(Modifier.width(8.dp))
            Text(displayDate(input.date))
        }
        state.errors["date"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        NumberField("Steps walked", "steps", input.steps, state.errors["steps"], !busy) { onChange(input.copy(steps = it)) }
        NumberField("Calories burned", "kcal", input.calories, state.errors["calories"], !busy) { onChange(input.copy(calories = it)) }
        NumberField("Water intake", "ml", input.water, state.errors["water"], !busy) { onChange(input.copy(water = it)) }
        state.errors["form"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Use whole numbers. Enter 0 for activities you are not logging.", style = MaterialTheme.typography.bodySmall)
        Button(enabled = !busy, onClick = onSave, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(if (busy) "Saving…" else "Save record")
        }
    }
}

@Composable
private fun NumberField(label: String, unit: String, value: String, error: String?, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, suffix = { Text(unit) },
        enabled = enabled, isError = error != null, supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ErrorPanel(message: String, retry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = retry) { Text("Retry") }
    }
}

private fun pickDate(context: android.content.Context, current: String, onDate: (String) -> Unit) {
    val date = runCatching { LocalDate.parse(current) }.getOrDefault(LocalDate.now())
    DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
        datePicker.maxDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.show()
}
