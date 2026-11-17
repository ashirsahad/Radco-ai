package com.accountpro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.core.Localization
import com.accountpro.app.data.local.ClientEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.EmptyStateView
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.SearchField
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.components.StatusBadge
import com.accountpro.app.ui.viewmodel.AppSessionState
import java.util.UUID

@Composable
fun ClientsScreen(
    session: AppSessionState,
    clients: List<ClientEntity>,
    onSaveClient: (ClientEntity, Boolean) -> Unit,
    onDeleteClient: (String) -> Unit,
) {
    val strings = session.strings
    var search by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(clients, search, statusFilter) {
        clients.filter { c ->
            val matchesStatus = statusFilter == "all" || c.status == statusFilter
            val matchesSearch = search.isBlank() ||
                c.name.contains(search, ignoreCase = true) ||
                c.email.contains(search, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = strings.clients,
                subtitle = "Manage your client relationships and information",
                actionLabel = "Add Client",
                actionIcon = Icons.Outlined.Add,
                onActionClick = {
                    editingClient = null
                    showDialog = true
                },
                actionTestTag = "add_client_button",
            )
        }

        item {
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search clients...",
                testTag = "client_search_input",
            )
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "all" to "All",
                    "active" to strings.active,
                    "inactive" to strings.inactive,
                ),
                selectedKey = statusFilter,
                onSelect = { statusFilter = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.Group,
                    title = "No clients found",
                    message = "Add a client to start invoicing.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { client ->
                val initials = client.name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }

                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(
                                    label = Localization.statusLabel(client.status, strings),
                                    statusKey = client.status,
                                )
                            }
                            IconButton(onClick = { onDeleteClient(client.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete client", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(client.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(client.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(client.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = client.totalInvoices.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("Invoices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = CurrencyManager.format(client.totalRevenue, "USD", session.language.code, convertFromUsd = true, targetBaseCode = session.baseCurrency.code),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("Revenue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                editingClient = client
                                showDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("View / Edit Details")
                        }
                    }
                }
            }
        }
    }

    if (showDialog && session.currentUser != null) {
        ClientFormDialog(
            orgId = session.currentUser.organizationId,
            existing = editingClient,
            onDismiss = { showDialog = false },
            onSave = { client ->
                onSaveClient(client, editingClient != null)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ClientFormDialog(
    orgId: String,
    existing: ClientEntity?,
    onDismiss: () -> Unit,
    onSave: (ClientEntity) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Client" else "Edit Client") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company / Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                FilterChipsRow(
                    options = listOf("active" to "Active", "inactive" to "Inactive"),
                    selectedKey = status,
                    onSelect = { status = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ClientEntity(
                            id = existing?.id ?: "$orgId-cli-${UUID.randomUUID().toString().take(6)}",
                            organizationId = orgId,
                            name = name.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            totalInvoices = existing?.totalInvoices ?: 0,
                            totalRevenue = existing?.totalRevenue ?: 0.0,
                            status = status,
                        )
                    )
                },
                enabled = name.isNotBlank() && email.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
