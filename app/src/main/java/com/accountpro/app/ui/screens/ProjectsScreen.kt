package com.accountpro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.ProjectEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.EmptyStateView
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.SearchField
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.components.StatCard
import com.accountpro.app.ui.components.StatTone
import com.accountpro.app.ui.components.StatusBadge
import com.accountpro.app.ui.viewmodel.AppSessionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun ProjectsScreen(
    session: AppSessionState,
    projects: List<ProjectEntity>,
    onSaveProject: (ProjectEntity, Boolean) -> Unit,
    onLogHours: (ProjectEntity, Int) -> Unit,
    onDeleteProject: (String) -> Unit,
) {
    val strings = session.strings
    val baseCode = session.baseCurrency.code
    var search by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var activeTimerProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filtered = remember(projects, search, statusFilter) {
        projects.filter { p ->
            val matchStatus = statusFilter == "all" || p.status == statusFilter
            val matchSearch = search.isBlank() ||
                p.name.contains(search, ignoreCase = true) ||
                p.clientName.contains(search, ignoreCase = true)
            matchStatus && matchSearch
        }
    }

    val totalBudget = projects.sumOf { CurrencyManager.convert(it.budget, "USD", baseCode) }
    val activeCount = projects.count { it.status == "active" }
    val totalHours = projects.sumOf { it.timeTrackedHours }
    val fmt = { v: Double -> CurrencyManager.format(v, baseCode, session.language.code) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = strings.projects,
                subtitle = "Track and manage your projects and time",
                actionLabel = "New Project",
                actionIcon = Icons.Outlined.Add,
                onActionClick = { showCreateDialog = true },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Total Budget",
                    value = fmt(totalBudget),
                    icon = Icons.Outlined.AttachMoney,
                    tone = StatTone.REVENUE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1.2f),
                )
                StatCard(
                    title = "Active",
                    value = activeCount.toString(),
                    subtitle = "${projects.size} total",
                    icon = Icons.Outlined.Folder,
                    tone = StatTone.CLIENT,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(0.9f),
                )
                StatCard(
                    title = "Hours",
                    value = "${totalHours}h",
                    icon = Icons.Outlined.Schedule,
                    tone = StatTone.INVOICE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(0.9f),
                )
            }
        }

        item {
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search projects...",
            )
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "all" to "All Status",
                    "active" to "Active",
                    "completed" to "Completed",
                    "on-hold" to "On Hold",
                    "cancelled" to "Cancelled",
                ),
                selectedKey = statusFilter,
                onSelect = { statusFilter = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.Folder,
                    title = "No projects found",
                    message = "Create a new project or adjust filters.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { project ->
                val isTracking = activeTimerProjectId == project.id
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    project.clientName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusBadge(label = project.status.replace("-", " "), statusKey = project.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            project.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Progress", style = MaterialTheme.typography.labelMedium)
                            Text("${project.progress}% · ${project.tasksCompleted}/${project.tasksTotal} tasks", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (project.progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Budget: ${fmt(CurrencyManager.convert(project.spent, "USD", baseCode))} / ${fmt(CurrencyManager.convert(project.budget, "USD", baseCode))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Time: ${project.timeTrackedHours}h / ${project.estimatedHours}h",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isTracking) {
                                    Button(
                                        onClick = {
                                            activeTimerProjectId = null
                                            onLogHours(project, 1)
                                        },
                                    ) {
                                        Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Stop & Log +1h")
                                    }
                                } else {
                                    OutlinedButton(onClick = { activeTimerProjectId = project.id }) {
                                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Start Timer")
                                    }
                                }
                            }
                            IconButton(onClick = { onDeleteProject(project.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Archive project", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && session.currentUser != null) {
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        var name by remember { mutableStateOf("") }
        var client by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var budget by remember { mutableStateOf("") }
        var hours by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Budget (USD)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Estimated Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveProject(
                            ProjectEntity(
                                id = "${session.currentUser.organizationId}-proj-${UUID.randomUUID().toString().take(6)}",
                                organizationId = session.currentUser.organizationId,
                                name = name.trim(),
                                clientName = client.trim().ifBlank { "Internal" },
                                description = desc.trim(),
                                status = "active",
                                progress = 0,
                                startDate = today,
                                endDate = today,
                                budget = budget.toDoubleOrNull() ?: 0.0,
                                spent = 0.0,
                                timeTrackedHours = 0,
                                estimatedHours = hours.toIntOrNull() ?: 100,
                                tasksTotal = 10,
                                tasksCompleted = 0,
                                teamMembersCsv = session.currentUser.fullName,
                            ),
                            false,
                        )
                        showCreateDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } },
        )
    }
}
