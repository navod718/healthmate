package com.navod718.healthmate.feature.health.presentation

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navod718.healthmate.core.insights.Achievement
import com.navod718.healthmate.feature.health.data.DailyBreakdown
import com.navod718.healthmate.feature.health.data.DailyGoals
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

private val weekdayLabel = DateTimeFormatter.ofPattern("EE")

private data class TrendMetric(
    val key: String, val label: String, val unit: String,
    val value: (DailyBreakdown) -> Long, val goal: (DailyGoals) -> Int,
)

private val trendMetrics = listOf(
    TrendMetric("steps", "Steps", "steps", { it.steps }, { it.steps }),
    TrendMetric("calories", "Calories", "kcal", { it.calories }, { it.calories }),
    TrendMetric("water", "Water", "ml", { it.water }, { it.water }),
)

private fun metricColor(key: String, primary: Color, error: Color, secondary: Color) = when (key) {
    "calories" -> error; "water" -> secondary; else -> primary
}

// Builds a zero-filled window of consecutive days ending today, oldest first.
private fun window(breakdown: List<DailyBreakdown>, today: LocalDate, count: Int): List<DailyBreakdown> {
    val byDate = breakdown.associateBy(DailyBreakdown::date)
    return ((count - 1).downTo(0)).map { today.minusDays(it.toLong()) }
        .map { byDate[it.toString()] ?: DailyBreakdown(it.toString()) }
}

private fun xLabels(days: List<DailyBreakdown>, dense: Boolean): List<String> = if (dense) {
    days.mapIndexed { index, day -> if (index % 5 == 0 || index == days.lastIndex) LocalDate.parse(day.date).dayOfMonth.toString() else "" }
} else {
    days.map { LocalDate.parse(it.date).format(weekdayLabel) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(state: InsightsState, goals: DailyGoals, today: String, retry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("INSIGHTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Your progress", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        Text("Trends, streaks and milestones from your records.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> RetryPanel(state.error, retry)
            state.breakdown.isEmpty() -> Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No data yet", style = MaterialTheme.typography.titleLarge)
                    Text("Add records or load the samples from the Records tab to see your trends, streak and achievements.")
                }
            }
            else -> {
                TrendCard(state.breakdown, goals, today)
                StreakCard(state.streak, state.bestStreak)
                AchievementsSection(state.achievements)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendCard(breakdown: List<DailyBreakdown>, goals: DailyGoals, today: String) {
    var metricKey by rememberSaveable { mutableStateOf("steps") }
    var range by rememberSaveable { mutableIntStateOf(7) }
    val metric = trendMetrics.first { it.key == metricKey }
    val colors = MaterialTheme.colorScheme
    val barColor = metricColor(metric.key, colors.primary, colors.error, colors.secondary)
    val goal = metric.goal(goals)
    val days = window(breakdown, LocalDate.parse(today), range)
    val values = days.map(metric.value)
    val logged = values.filter { it > 0 }
    val average = if (logged.isEmpty()) 0L else (logged.sum() / logged.size)
    OutlinedCard(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Trends", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trendMetrics.forEach { option ->
                    FilterChip(selected = option.key == metricKey, onClick = { metricKey = option.key }, label = { Text(option.label) })
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = range == 7, onClick = { range = 7 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("7 days") }
                SegmentedButton(selected = range == 30, onClick = { range = 30 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("30 days") }
            }
            TrendChart(values, goal, metric.label, metric.unit, barColor, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth()) {
                xLabels(days, dense = range == 30).forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            Text(
                "${number(values.sum())} ${metric.unit} over ${if (range == 30) "30 days" else "7 days"} · average ${number(average)} · best ${number(values.maxOrNull() ?: 0L)}",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            )
            Text("The dashed line marks your ${number(goal.toLong())} ${metric.unit} daily goal.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrendChart(values: List<Long>, goal: Int, label: String, unit: String, barColor: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val appearance = remember { Animatable(0f) }
    LaunchedEffect(values, goal) {
        appearance.snapTo(0f)
        appearance.animateTo(1f, tween(360))
    }
    val description = "Bar chart of ${label.lowercase()} over the last ${values.size} days. " +
        "Goal ${number(goal.toLong())} $unit. Best day ${number(values.maxOrNull() ?: 0L)} $unit."
    Canvas(modifier.height(190.dp).testTag("trend-chart").semantics { contentDescription = description }) {
        if (values.isEmpty()) return@Canvas
        val left = 46.dp.toPx(); val top = 6.dp.toPx(); val right = 8.dp.toPx(); val bottom = 8.dp.toPx()
        val chartWidth = size.width - left - right
        val chartHeight = size.height - top - bottom
        val ceiling = maxOf(values.maxOrNull() ?: 0L, goal.toLong(), 1L)
        // Round the scale up to one significant figure for tidy gridline labels.
        val magnitude = 10.0.pow(floor(log10(ceiling.toDouble())))
        val scale = ceil(ceiling / magnitude).coerceAtLeast(1.0) * magnitude
        val labelPaint = Paint().apply {
            isAntiAlias = true; textSize = 11.sp.toPx(); color = colors.onSurfaceVariant.toArgb(); textAlign = Paint.Align.RIGHT
        }
        for (line in 1..3) {
            val fraction = line / 3f
            val y = top + chartHeight * (1 - fraction)
            drawLine(colors.outlineVariant, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                number((scale * fraction).toLong()), left - 8.dp.toPx(), y + 4.dp.toPx(), labelPaint,
            )
        }
        val slot = chartWidth / values.size
        val barWidth = slot * 0.62f
        val radius = min(6.dp.toPx(), barWidth / 2)
        values.forEachIndexed { index, value ->
            val height = (value / scale).toFloat() * chartHeight * appearance.value
            val x = left + index * slot + (slot - barWidth) / 2
            drawRoundRect(barColor, topLeft = Offset(x, top + chartHeight - height), size = Size(barWidth, height), cornerRadius = CornerRadius(radius, radius))
        }
        val goalY = top + chartHeight * (1 - (goal / scale).toFloat())
        drawLine(colors.onSurfaceVariant, Offset(left, goalY), Offset(size.width - right, goalY),
            strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    }
}

@Composable
private fun StreakCard(streak: Int, best: Int) {
    OutlinedCard(
        Modifier.fillMaxWidth().testTag("streak-card"), shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Whatshot, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text("Goal streak", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (streak > 0) "$streak-day streak" else "No streak yet", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (streak > 0) "Meet every goal each day to keep it going. Best so far: $best ${if (best == 1) "day" else "days"}."
                    else "Meet all three daily goals to start a streak.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AchievementsSection(achievements: List<Achievement>) {
    val unlocked = achievements.count { it.unlocked }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Achievements", Modifier.semantics { heading() }.testTag("achievements-grid"), style = MaterialTheme.typography.titleLarge)
        Text("$unlocked of ${achievements.size} unlocked", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        achievements.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { achievement -> BadgeCard(achievement, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun badgeIcon(id: String): ImageVector = when (id) {
    "first-record" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "ten-records" -> Icons.Default.Timeline
    "thirty-records" -> Icons.Default.Verified
    "perfect-day" -> Icons.Default.EmojiEvents
    "streak-3" -> Icons.Default.MilitaryTech
    "streak-7" -> Icons.Default.WorkspacePremium
    "streak-14" -> Icons.Default.RocketLaunch
    else -> Icons.AutoMirrored.Filled.DirectionsRun
}

@Composable
private fun BadgeCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val unlocked = achievement.unlocked
    OutlinedCard(
        modifier.testTag("achievement-${achievement.id}"), shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.fillMaxWidth().let { if (unlocked) it else it.alpha(0.6f) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).background(
                    if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(14.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (unlocked) badgeIcon(achievement.id) else Icons.Default.Lock, null,
                    Modifier.size(20.dp),
                    tint = if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                Text(achievement.title, style = MaterialTheme.typography.titleMedium)
                Text(achievement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RetryPanel(message: String, retry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = retry) { Text("Retry") }
    }
}
