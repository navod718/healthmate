package com.navod718.healthmate.feature.health.presentation

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.navod718.healthmate.feature.health.data.DailyGoals
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.ui.theme.BrandIndigo
import com.navod718.healthmate.ui.theme.BrandMint
import com.navod718.healthmate.ui.theme.CaloriesGradient
import com.navod718.healthmate.ui.theme.StepsGradient
import com.navod718.healthmate.ui.theme.WaterGradient
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun displayDate(date: String): String = LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
internal fun number(value: Long) = NumberFormat.getIntegerInstance().format(value)

@Composable
fun DashboardScreen(
    state: TotalsState,
    retry: () -> Unit,
    goals: DailyGoals,
    goalsLoadFailed: Boolean,
    retryGoals: () -> Unit,
    streak: Int = 0,
    onOpenInsights: () -> Unit = {},
    onQuickAddWater: (Int) -> Unit = {},
    viewingDate: String? = null,
    onClearViewDate: () -> Unit = {},
    onPickViewDate: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var viewPickerOpen by rememberSaveable { mutableStateOf(false) }
    val allGoalsMet = state.totals.steps >= goals.steps && state.totals.calories >= goals.calories && state.totals.water >= goals.water
    val overall = ((goals.progress(state.totals.steps, goals.steps) + goals.progress(state.totals.calories, goals.calories) + goals.progress(state.totals.water, goals.water)) / 3f)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BrandIndigo, contentColor = Color.White),
        ) {
            Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BrandIndigo, Color(0xFF174D5B)))).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp), tint = BrandMint)
                        Text(displayDate(state.date), style = MaterialTheme.typography.labelLarge, color = BrandMint)
                    }
                    Text("Your day, at a glance", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                    AnimatedVisibility(visible = allGoalsMet && !state.loading, enter = fadeIn(tween(240)) + scaleIn(tween(240, easing = FastOutSlowInEasing), initialScale = 0.9f)) {
                        Text("✨ All goals reached!", style = MaterialTheme.typography.titleMedium, color = BrandMint)
                    }
                    AnimatedVisibility(visible = !allGoalsMet || state.loading, enter = fadeIn(tween(240))) {
                        Text("A daily view of your movement and hydration.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OverallRing(overall, Modifier.semantics { contentDescription = "${(overall * 100).toInt()} percent of daily goals" })
            }
        }
        if (viewingDate != null) AssistChip(
            onClick = onClearViewDate,
            label = { Text("Viewing ${displayDate(viewingDate)}") },
            trailingIcon = {
                Icon(Icons.Default.Close, "Show today", Modifier.size(18.dp))
            },
            modifier = Modifier.testTag("view-date-chip"),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = !state.loading, onClick = { viewPickerOpen = true }, modifier = Modifier.testTag("view-date-picker")) {
                Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                Text(" View another day")
            }
            Spacer(Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.WaterDrop, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Log water", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(250, 500, 1000).forEach { amount ->
                FilledTonalButton(onClick = { onQuickAddWater(amount) }, enabled = !state.loading, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("+$amount")
                }
            }
        }
        if (streak > 0) AssistChip(
            onClick = onOpenInsights,
            label = { Text("$streak-day goal streak") },
            leadingIcon = { Icon(Icons.Default.Whatshot, null, Modifier.size(18.dp)) },
        )
        Text("Daily activity", style = MaterialTheme.typography.titleLarge)
        if (goalsLoadFailed) {
            Text("Saved goals could not be refreshed. Showing the last available targets.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = retryGoals) { Text("Retry loading goals") }
        }
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> ErrorPanel(state.error, retry)
            else -> {
                MetricCard("Steps walked", state.totals.steps, goals.steps, goals.progress(state.totals.steps, goals.steps),
                    "steps", Icons.AutoMirrored.Filled.DirectionsWalk, StepsGradient)
                MetricCard("Calories burned", state.totals.calories, goals.calories, goals.progress(state.totals.calories, goals.calories),
                    "kcal", Icons.Default.LocalFireDepartment, CaloriesGradient)
                MetricCard("Water intake", state.totals.water, goals.water, goals.progress(state.totals.water, goals.water),
                    "ml", Icons.Default.WaterDrop, WaterGradient)
            }
        }
    }
    if (viewPickerOpen) {
        RecordDatePicker(viewingDate ?: state.date, onDate = onPickViewDate, onDismiss = { viewPickerOpen = false })
    }
}

@Composable
private fun OverallRing(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress, tween(360), label = "Overall progress")
    Box(modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(Color.White.copy(alpha = 0.3f), 0f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(BrandMint, -90f, 360f * animated, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun MetricCard(
    title: String, value: Long, goal: Int, progress: Float, unit: String, icon: ImageVector,
    gradient: List<Color>,
) {
    val animatedProgress by animateFloatAsState(progress, tween(360), label = "Goal progress")
    val countUp = remember { Animatable(0f) }
    LaunchedEffect(value) { countUp.animateTo(value.toFloat(), tween(600, easing = FastOutSlowInEasing)) }
    // Show the exact target once the animation settles so very large sums stay precise.
    val displayed = if (countUp.isRunning) countUp.value.toLong() else value
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(gradient))) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(44.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, Modifier.size(23.dp), tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                        Text("${number(displayed)} $unit", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f))
                        Spacer(Modifier.height(5.dp))
                        Text("${(progress * 100).toInt()}% of ${number(goal.toLong())} $unit goal" + if (value >= goal) " · Goal reached" else "",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
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
    val input = state.input
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val caloriesFocus = remember { FocusRequester() }
    val waterFocus = remember { FocusRequester() }
    val save = { focus.clearFocus(); keyboard?.hide(); onSave() }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (state.id == null) "Log a healthy moment" else "Update your activity",
            Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        Text("Log an activity or your full day. Entries on the same date are added together.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Activity date", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(enabled = !busy, onClick = { datePickerOpen = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("record-date")) {
            Icon(Icons.Default.CalendarMonth, null)
            Spacer(Modifier.width(8.dp))
            Text(displayDate(input.date))
        }
        state.errors["date"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        NumberField("Steps walked", "steps", input.steps, state.errors["steps"], !busy, imeAction = ImeAction.Next, keyboardActions = KeyboardActions(onNext = { caloriesFocus.requestFocus() })) { onChange(input.copy(steps = it)) }
        NumberField("Calories burned", "kcal", input.calories, state.errors["calories"], !busy, modifier = Modifier.focusRequester(caloriesFocus), imeAction = ImeAction.Next, keyboardActions = KeyboardActions(onNext = { waterFocus.requestFocus() })) { onChange(input.copy(calories = it)) }
        NumberField("Water intake", "ml", input.water, state.errors["water"], !busy, modifier = Modifier.focusRequester(waterFocus), imeAction = ImeAction.Done, keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() })) { onChange(input.copy(water = it)) }
        state.errors["form"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Use whole numbers. Enter 0 for activities you are not logging.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(enabled = !busy, onClick = save, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            else { Icon(Icons.Default.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(if (busy) "Saving…" else if (state.id == null) "Save record" else "Save changes")
        }
    }
    if (datePickerOpen) {
        RecordDatePicker(input.date, onDate = { onChange(input.copy(date = it)) }, onDismiss = { datePickerOpen = false })
    }
}

@Composable
private fun NumberField(label: String, unit: String, value: String, error: String?, enabled: Boolean, modifier: Modifier = Modifier, imeAction: ImeAction = ImeAction.Done, keyboardActions: KeyboardActions = KeyboardActions.Default, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, suffix = { Text(unit) },
        leadingIcon = { Icon(when (unit) {
            "steps" -> Icons.AutoMirrored.Filled.DirectionsWalk
            "kcal" -> Icons.Default.LocalFireDepartment
            else -> Icons.Default.WaterDrop
        }, null) },
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
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

@Composable
internal fun RecordDatePicker(current: String, onDate: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val selectDate by rememberUpdatedState(onDate)
    val dismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(context, current) {
        val date = runCatching { LocalDate.parse(current) }.getOrDefault(LocalDate.now())
        val dialog = DatePickerDialog(context, { _, year, month, day ->
            selectDate(LocalDate.of(year, month + 1, day).toString())
        }, date.year, date.monthValue - 1, date.dayOfMonth)
        dialog.datePicker.maxDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialog.setOnDismissListener { dismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}
