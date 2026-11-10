package com.navod718.healthmate.feature.health.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.navod718.healthmate.ui.theme.HealthMateMark
import kotlinx.coroutines.delay

private enum class HealthScreen { Dashboard, Records, Editor }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthMateApp(model: HealthViewModel, revealBrand: Boolean = false) {
    val totals by model.totals.collectAsStateWithLifecycle()
    val editor by model.editor.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val goals by model.goals.collectAsStateWithLifecycle()
    val goalsOpen by model.goalsOpen.collectAsStateWithLifecycle()
    val discardPrompt by model.discardPrompt.collectAsStateWithLifecycle()
    val tab by model.tab.collectAsStateWithLifecycle()
    val records by model.records.collectAsStateWithLifecycle()
    val deleteTarget by model.deleteTarget.collectAsStateWithLifecycle()
    val deleteError by model.deleteError.collectAsStateWithLifecycle()
    val samplesOpen by model.samplesOpen.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val screen = when {
        editor.open -> HealthScreen.Editor
        tab == "records" -> HealthScreen.Records
        else -> HealthScreen.Dashboard
    }
    val horizontalDirection = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val brandVisibility = remember {
        MutableTransitionState(!revealBrand).apply { targetState = true }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, model) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                model.refreshToday()
                delay(60_000)
            }
        }
    }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); model.clearMessage() }
    }
    BackHandler(!editor.open && tab == "records") { model.selectTab("dashboard") }
    BackHandler(editor.open) { model.closeEditor() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = {
                if (editor.open) {
                    Text(if (editor.id == null) "Add record" else "Edit record")
                } else {
                    AnimatedVisibility(
                        visibleState = brandVisibility,
                        enter = fadeIn(tween(420)) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.96f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HealthMateMark()
                            Text("HealthMate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (editor.open) IconButton(onClick = model::closeEditor, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }, actions = {
                    if (!editor.open) IconButton(onClick = model::openGoals, enabled = !busy) {
                        Icon(Icons.Default.Flag, "Daily goals")
                    }
                })
        },
        bottomBar = {
            if (!editor.open) Column {
                if (screen == HealthScreen.Dashboard) {
                    // Measuring the action with the bottom bar keeps it outside the dashboard scroll viewport.
                    Box(
                        Modifier.fillMaxWidth()
                            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal))
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        AddRecordButton(onClick = model::add)
                    }
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    NavigationBarItem(selected = tab == "dashboard", onClick = { model.selectTab("dashboard") },
                        icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Dashboard") })
                    NavigationBarItem(selected = tab == "records", onClick = { model.selectTab("records") },
                        icon = { Icon(Icons.Default.History, null) }, label = { Text("Records") })
                }
            }
        },
        floatingActionButton = {
            if (screen == HealthScreen.Records) AddRecordButton(onClick = model::add)
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = {
                val slide = when {
                    targetState == HealthScreen.Editor -> slideInVertically(tween(240)) { it / 24 }
                    initialState == HealthScreen.Editor -> slideInVertically(tween(200)) { -it / 64 }
                    else -> {
                        val direction = if (targetState == HealthScreen.Records) horizontalDirection else -horizontalDirection
                        slideInHorizontally(tween(220)) { direction * it / 12 }
                    }
                }
                (fadeIn(tween(180), initialAlpha = 0.55f) + slide) togetherWith ExitTransition.None
            },
            label = "Health screen",
        ) { destination ->
            // Only the active destination can own focus, gestures or accessibility nodes.
            if (destination == screen) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    val page = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                    when (destination) {
                        HealthScreen.Editor -> RecordForm(editor, busy, model::change, model::save, page)
                        HealthScreen.Records -> RecordsScreen(records, busy, model::filterByDate, model::edit, model::requestDelete, model::openSamples, model::retry, page)
                        HealthScreen.Dashboard -> DashboardScreen(totals, model::retry, goals.value, goals.loadFailed, model::retryGoals, page)
                    }
                }
            }
        }
    }
    if (discardPrompt) AlertDialog(
        onDismissRequest = model::keepEditing,
        title = { Text("Discard this record?") },
        text = { Text("Your unsaved changes will be lost.") },
        confirmButton = { TextButton(onClick = model::discard) { Text("Discard") } },
        dismissButton = { TextButton(onClick = model::keepEditing) { Text("Keep editing") } },
    )
    deleteTarget?.let { record ->
        AlertDialog(onDismissRequest = model::cancelDelete,
            title = { Text("Delete record?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remove the record for ${displayDate(record.date)}? This cannot be undone.")
                    deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = model::confirmDelete, enabled = !busy) { Text(if (busy) "Deleting…" else "Delete record") } },
            dismissButton = { TextButton(onClick = model::cancelDelete, enabled = !busy) { Text("Cancel") } },
        )
    }
    if (samplesOpen) AlertDialog(onDismissRequest = model::closeSamples,
        title = { Text("Load sample records?") },
        text = { Text("Add four example records covering today and the previous three days? They are sample data, not your actual activity. Samples are available only when there are no saved records.") },
        confirmButton = { TextButton(onClick = model::loadSamples, enabled = !busy) { Text(if (busy) "Loading…" else "Load samples") } },
        dismissButton = { TextButton(onClick = model::closeSamples, enabled = !busy) { Text("Cancel") } },
    )
    if (goalsOpen) GoalsDialog(goals, busy, model::closeGoals, model::saveGoals, model::retryGoals)
}

@Composable
private fun AddRecordButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(Icons.Default.Add, null) },
        text = { Text("Add record") },
    )
}
