package com.navod718.healthmate.feature.health.presentation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.feature.health.data.HealthRecord
import java.text.NumberFormat
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordsScreen(state: RecordsState, busy: Boolean, onFilter: (String?) -> Unit,
                  onEdit: (Long) -> Unit, onDelete: (HealthRecord) -> Unit,
                  onSamples: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val filter = state.date
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    // Scroll the controls with the list on smaller screens.
    key(filter) {
        LazyColumn(modifier.fillMaxSize().testTag("records-list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ACTIVITY LOG", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Your health history", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                    Text("Review and manage your daily activity.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { datePickerOpen = true }) {
                            Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(filter?.let(::displayDate) ?: "Filter by date")
                        }
                        if (filter != null) TextButton(enabled = !busy, onClick = { onFilter(null) }) { Text("Clear") }
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
                    Card(
                        Modifier.fillMaxWidth().testTag(if (filter == null) "empty-records" else "empty-filter-result"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (filter == null) {
                                Text("No records yet", style = MaterialTheme.typography.titleLarge)
                                Text("Add a record to start your health history.")
                                if (state.totalCount == 0) OutlinedButton(enabled = !busy, onClick = onSamples) { Text("Try sample records") }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SearchOff, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.error)
                                    Text("No records found", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    "Nothing was logged on ${displayDate(filter)}. Choose another date, or add a record for this day.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(enabled = !busy, onClick = { onFilter(null) }) { Text("Clear filter") }
                            }
                        }
                    }
                }
                else -> items(state.records, key = { it.id }) { record ->
                    OutlinedCard(
                        Modifier.animateItem(
                            fadeInSpec = tween(160),
                            placementSpec = tween(200),
                            fadeOutSpec = tween(140),
                        ).fillMaxWidth().testTag("record-${record.id}"),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(displayDate(record.date), style = MaterialTheme.typography.titleMedium)
                            RecordMetric(Icons.AutoMirrored.Filled.DirectionsWalk, "${NumberFormat.getIntegerInstance().format(record.steps)} steps")
                            RecordMetric(Icons.Default.LocalFireDepartment, "${NumberFormat.getIntegerInstance().format(record.calories)} kcal burned")
                            RecordMetric(Icons.Default.WaterDrop, "${NumberFormat.getIntegerInstance().format(record.water)} ml water")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(enabled = !busy, onClick = { onEdit(record.id) }, modifier = Modifier.testTag("edit-${record.id}")) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Text(" Edit")
                                }
                                TextButton(enabled = !busy, onClick = { onDelete(record) }, modifier = Modifier.testTag("delete-${record.id}"),
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Text(" Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (datePickerOpen) {
        RecordDatePicker(filter ?: LocalDate.now().toString(), onDate = onFilter, onDismiss = { datePickerOpen = false })
    }
}

@Composable
private fun RecordMetric(icon: ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
