package com.navod718.healthmate.feature.health.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthMateApp(model: HealthViewModel) {
    val totals by model.totals.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val editor by model.editor.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val goals by model.goals.collectAsStateWithLifecycle()
    val goalsOpen by model.goalsOpen.collectAsStateWithLifecycle()
    val discardPrompt by model.discardPrompt.collectAsStateWithLifecycle()
    val tab by model.tab.collectAsStateWithLifecycle()
    val records by model.records.collectAsStateWithLifecycle()
    val filter by model.filter.collectAsStateWithLifecycle()
    val deleteTarget by model.deleteTarget.collectAsStateWithLifecycle()
    val deleteError by model.deleteError.collectAsStateWithLifecycle()
    val samplesOpen by model.samplesOpen.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.refreshToday()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(today) {
        while (true) { delay(60_000); model.refreshToday() }
    }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); model.clearMessage() }
    }
    BackHandler(!editor.open && tab == "records") { model.selectTab("dashboard") }
    BackHandler(editor.open) { model.closeEditor() }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (editor.open) { if (editor.id == null) "Add record" else "Edit record" } else "HealthMate") },
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
            if (!editor.open) NavigationBar {
                NavigationBarItem(selected = tab == "dashboard", onClick = { model.selectTab("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Dashboard") })
                NavigationBarItem(selected = tab == "records", onClick = { model.selectTab("records") },
                    icon = { Icon(Icons.Default.History, null) }, label = { Text("Records") })
            }
        },
        floatingActionButton = {
            if (!editor.open) ExtendedFloatingActionButton(onClick = model::add,
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add record") })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (editor.open) RecordForm(editor, busy, model::change, model::save, Modifier.padding(padding))
        else if (tab == "records") RecordsScreen(records, filter, busy, model::filterByDate, model::edit, model::requestDelete, model::openSamples, model::retry, Modifier.padding(padding))
        else DashboardScreen(today, totals, model::retry, goals, Modifier.padding(padding))
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
    if (goalsOpen) GoalsDialog(goals, busy, model::closeGoals, model::saveGoals)
}
