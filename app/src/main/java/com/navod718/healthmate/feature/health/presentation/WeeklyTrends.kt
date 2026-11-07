package com.navod718.healthmate.feature.health.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.feature.health.data.DailyActivity
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class TrendMetric(val label: String, val unit: String, val color: Color) {
    STEPS("Steps", "steps", Color(0xFF257449)),
    CALORIES("Calories", "kcal", Color(0xFFAE4C27)),
    WATER("Water", "ml", Color(0xFF176CB0));
    fun value(day: DailyActivity): Long = when (this) {
        STEPS -> day.steps; CALORIES -> day.calories; WATER -> day.water
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyTrends(state: WeekState, retry: () -> Unit) {
    var metricIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    val metric = TrendMetric.entries[metricIndex]
    val format = NumberFormat.getIntegerInstance()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("7-day trends", style = MaterialTheme.typography.titleLarge)
            when {
                state.loading -> CircularProgressIndicator(Modifier.size(24.dp))
                state.error != null -> {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = retry) { Text("Retry trends") }
                }
                state.days.isNotEmpty() -> {
                    Text("${displayDate(state.days.first().date)} – ${displayDate(state.days.last().date)}", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TrendMetric.entries.forEachIndexed { index, item ->
                            FilterChip(selected = metricIndex == index, onClick = { metricIndex = index }, label = { Text(item.label) }, modifier = Modifier.testTag("trend-metric-${item.name}"))
                        }
                    }
                    val current = state.days.firstOrNull { it.date == selectedDate } ?: state.days.last()
                    val maximum = state.days.maxOf { metric.value(it) }.coerceAtLeast(1)
                    val total = state.days.sumOf { metric.value(it) }
                    Text("${format.format(total)} ${metric.unit} over 7 days", style = MaterialTheme.typography.titleMedium)
                    val scroll = rememberScrollState()
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val chartWidth = maxOf(maxWidth, 348.dp)
                        Box(Modifier.horizontalScroll(scroll)) {
                            Row(Modifier.width(chartWidth).height(170.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                state.days.forEach { day ->
                                    val value = metric.value(day)
                                    val ratio = (value.toDouble() / maximum).toFloat().coerceIn(0f, 1f)
                                    val chosen = day.date == current.date
                                    Column(Modifier.weight(1f).fillMaxHeight()
                                        .background(if (chosen) metric.color.copy(alpha = .08f) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .testTag("trend-day-${day.date}")
                                        .selectable(chosen, role = Role.Button, onClick = { selectedDate = day.date })
                                        .semantics { contentDescription = "${displayDate(day.date)}: ${format.format(value)} ${metric.unit}" },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                            if (value > 0) Box(Modifier.fillMaxWidth(.65f).fillMaxHeight(ratio.coerceAtLeast(.015f)).background(metric.color.copy(alpha = if (chosen) 1f else .55f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                            else Box(Modifier.fillMaxWidth(.65f).height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                        }
                                        Text(LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEE")), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics {})
                                        Text(LocalDate.parse(day.date).dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.clearAndSetSemantics {})
                                    }
                                }
                            }
                        }
                    }
                    Text("${displayDate(current.date)}: ${format.format(metric.value(current))} ${metric.unit}", modifier = Modifier.testTag("trend-selected-value"))
                    Text("Tap a bar for its daily total. Days without entries count as zero.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
