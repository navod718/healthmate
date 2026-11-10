package com.navod718.healthmate.feature.health.presentation

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.navod718.healthmate.feature.health.data.DailyGoals
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.navod718.healthmate.core.validation.RecordInput
import com.navod718.healthmate.ui.theme.BrandIndigo
import com.navod718.healthmate.ui.theme.BrandMint
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun displayDate(date: String): String = LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
private fun number(value: Long) = NumberFormat.getIntegerInstance().format(value)

@Composable
fun DashboardScreen(
    state: TotalsState,
    retry: () -> Unit,
    goals: DailyGoals,
    goalsLoadFailed: Boolean,
    retryGoals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BrandIndigo, contentColor = Color.White),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(displayDate(state.date), style = MaterialTheme.typography.labelLarge, color = BrandMint)
                Text("Your day, at a glance", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                Text("A daily view of your movement and hydration.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("Daily activity", style = MaterialTheme.typography.titleLarge)
        if (goalsLoadFailed) {
            Text("Saved goals could not be refreshed. Showing the last available targets.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = retryGoals) { Text("Retry loading goals") }
        }
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> ErrorPanel(state.error, retry)
            else -> {
                val colors = MaterialTheme.colorScheme
                MetricCard("Steps walked", state.totals.steps, goals.steps, goals.progress(state.totals.steps, goals.steps),
                    "steps", Icons.AutoMirrored.Filled.DirectionsWalk, colors.primary, colors.primaryContainer, colors.onPrimaryContainer)
                MetricCard("Calories burned", state.totals.calories, goals.calories, goals.progress(state.totals.calories, goals.calories),
                    "kcal", Icons.Default.LocalFireDepartment, colors.error, colors.errorContainer, colors.onErrorContainer)
                MetricCard("Water intake", state.totals.water, goals.water, goals.progress(state.totals.water, goals.water),
                    "ml", Icons.Default.WaterDrop, colors.secondary, colors.secondaryContainer, colors.onSecondaryContainer)
                Text("Totals include every record you log for today. Your records stay on this device.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String, value: Long, goal: Int, progress: Float, unit: String, icon: ImageVector,
    tint: Color, iconBackground: Color, iconTint: Color,
) {
    val animatedProgress by animateFloatAsState(progress, tween(360), label = "Goal progress")
    OutlinedCard(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).background(iconBackground, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(23.dp), tint = iconTint)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${number(value)} $unit", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth(), color = tint,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                Spacer(Modifier.height(5.dp))
                Text("${(progress * 100).toInt()}% of ${number(goal.toLong())} $unit goal" + if (value >= goal) " · Goal reached" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
