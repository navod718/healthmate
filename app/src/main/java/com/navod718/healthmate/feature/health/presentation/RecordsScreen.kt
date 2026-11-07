package com.navod718.healthmate.feature.health.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.feature.health.data.HealthRecord
import java.text.NumberFormat
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordsScreen(state: RecordsState, filter: String?, busy: Boolean, onFilter: (String?) -> Unit,
                  onEdit: (Long) -> Unit, onDelete: (HealthRecord) -> Unit,
                  onSamples: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Header is in the list too, so controls remain reachable on small screens and large fonts.
    LazyColumn(modifier.fillMaxSize().testTag("records-list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your health history", style = MaterialTheme.typography.headlineMedium)
                Text("Review and manage your daily activity.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = {
                        pickDate(context, filter ?: LocalDate.now().toString(), onFilter)
                    }) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(filter?.let(::displayDate) ?: "Filter by date")
                    }
                    if (filter != null) TextButton(onClick = { onFilter(null) }) { Text("Clear") }
                }
                if (!state.loading && state.error == null) Text(
                    "${state.records.size} ${if (state.records.size == 1) "record" else "records"}${if (filter == null) "" else " on this date"}",
                    style = MaterialTheme.typography.labelLarge)
            }
        }
        when {
            state.loading -> item(key = "loading") { CircularProgressIndicator(Modifier.padding(24.dp)) }
            state.error != null -> item(key = "error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
                }
            }
            state.records.isEmpty() -> item(key = "empty") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (filter == null) "No records yet" else "No records for this date", style = MaterialTheme.typography.titleLarge)
                    Text(if (filter == null) "Add a record to start your health history." else "Choose another date or clear the filter.")
                    if (state.totalCount == 0) OutlinedButton(enabled = !busy, onClick = onSamples) { Text("Try sample records") }
                }
            }
            else -> items(state.records, key = { it.id }) { record ->
                Card(Modifier.fillMaxWidth().testTag("record-${record.id}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(displayDate(record.date), style = MaterialTheme.typography.titleMedium)
                        Text("${NumberFormat.getIntegerInstance().format(record.steps)} steps")
                        Text("${NumberFormat.getIntegerInstance().format(record.calories)} kcal burned")
                        Text("${NumberFormat.getIntegerInstance().format(record.water)} ml water", color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(enabled = !busy, onClick = { onEdit(record.id) }, modifier = Modifier.testTag("edit-${record.id}")) {
                                Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Text(" Edit")
                            }
                            TextButton(enabled = !busy, onClick = { onDelete(record) }, modifier = Modifier.testTag("delete-${record.id}")) {
                                Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Text(" Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
