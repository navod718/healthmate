package com.navod718.healthmate.feature.health.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.feature.health.data.HealthRecord
import java.time.LocalDate

@Composable
fun SearchScreen(
    searchDate: String?,
    query: String?,
    results: List<HealthRecord>,
    busy: Boolean,
    onPickDate: (String) -> Unit,
    onSubmit: () -> Unit,
    onShowAll: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (HealthRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SEARCH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Search health records", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        Text("Find your saved records by date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(enabled = !busy, onClick = { datePickerOpen = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("search-date")) {
            Icon(Icons.Default.CalendarMonth, null)
            Spacer(Modifier.width(8.dp))
            Text(searchDate?.let(::displayDate) ?: "Select date")
        }
        Button(enabled = !busy && searchDate != null, onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("search-submit")) {
            Text("Search records")
        }
        OutlinedButton(enabled = !busy, onClick = onShowAll,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("search-show-all")) {
            Text("Show all records")
        }
        if (query != null) {
            if (results.isEmpty()) {
                Card(
                    Modifier.fillMaxWidth().testTag("search-empty"), shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SearchOff, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.error)
                            Text("No records found", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            "Nothing was logged on ${displayDate(query)}. Pick another date, or add a record for this day.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                if (results.size > 1) DailyTotalCard(results)
                results.forEach { record -> SearchResultCard(record, busy, onEdit, onDelete) }
            }
        }
    }
    if (datePickerOpen) {
        RecordDatePicker(searchDate ?: query ?: LocalDate.now().toString(), onDate = onPickDate, onDismiss = { datePickerOpen = false })
    }
}

@Composable
private fun DailyTotalCard(results: List<HealthRecord>) {
    val steps = results.sumOf { it.steps }
    val calories = results.sumOf { it.calories }
    val water = results.sumOf { it.water }
    Card(
        Modifier.fillMaxWidth().testTag("search-total"), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Daily total", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            MetricRow("Steps walked", "${number(steps.toLong())}", "")
            MetricRow("Calories burned", "${number(calories.toLong())}", " kcal")
            MetricRow("Water intake", "${number(water.toLong())}", " ml")
        }
    }
}

@Composable
private fun SearchResultCard(record: HealthRecord, busy: Boolean, onEdit: (Long) -> Unit, onDelete: (HealthRecord) -> Unit) {
    Card(
        Modifier.fillMaxWidth().testTag("search-record-${record.id}"), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Daily health record", style = MaterialTheme.typography.titleMedium)
            Text(displayDate(record.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MetricRow("Steps walked", "${number(record.steps.toLong())}", "")
            MetricRow("Calories burned", "${number(record.calories.toLong())}", " kcal")
            MetricRow("Water intake", "${number(record.water.toLong())}", " ml")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy, onClick = { onEdit(record.id) }, modifier = Modifier.testTag("search-edit-${record.id}")) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Text(" Edit")
                }
                TextButton(enabled = !busy, onClick = { onDelete(record) }, modifier = Modifier.testTag("search-delete-${record.id}"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Text(" Delete")
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, unit: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text("$value$unit", style = MaterialTheme.typography.bodyMedium)
    }
}
