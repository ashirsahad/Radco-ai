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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accountpro.app.data.local.AuditLogEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.EmptyStateView
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.MonoCodeChip
import com.accountpro.app.ui.components.SearchField
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.viewmodel.AppSessionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ACTION_LABELS = mapOf(
    "sign_in" to "Signed in",
    "role_changed" to "Role changed",
    "permission_updated" to "Access updated",
    "member_status_changed" to "Member status",
    "invoice_created" to "Invoice created",
    "invoice_updated" to "Invoice edited",
    "expense_created" to "Expense added",
    "expense_updated" to "Expense edited",
    "transaction_reconciled" to "Reconciled",
)

private val AUDIT_GROUPS = mapOf(
    "all" to emptySet(),
    "auth" to setOf("sign_in"),
    "access" to setOf("role_changed", "permission_updated", "member_status_changed"),
    "invoices" to setOf("invoice_created", "invoice_updated"),
    "expenses" to setOf("expense_created", "expense_updated"),
    "banking" to setOf("transaction_reconciled"),
)

@Composable
fun AuditLogScreen(
    session: AppSessionState,
    logs: List<AuditLogEntity>,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("all") }
    val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    val filtered = remember(logs, search, group) {
        val allowedActions = AUDIT_GROUPS[group].orEmpty()
        logs.filter { log ->
            val matchGroup = group == "all" || log.action in allowedActions
            val matchSearch = search.isBlank() ||
                log.actorEmail.contains(search, ignoreCase = true) ||
                (log.entityId ?: "").contains(search, ignoreCase = true) ||
                log.detailsText.contains(search, ignoreCase = true)
            matchGroup && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = session.strings.auditLog,
                subtitle = "Who did what, and when, across your company.",
            )
        }

        item {
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search person, record or detail...",
            )
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "all" to "All activity",
                    "auth" to "Sign-ins",
                    "access" to "Roles & access",
                    "invoices" to "Invoices",
                    "expenses" to "Expenses",
                    "banking" to "Banking",
                ),
                selectedKey = group,
                onSelect = { group = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.History,
                    title = "No activity yet",
                    message = "Actions across invoices, expenses, banking, and team access appear here.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { log ->
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MonoCodeChip(ACTION_LABELS[log.action] ?: log.action)
                            Text(
                                text = dateFormatter.format(Date(log.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = log.actorEmail,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (log.detailsText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = log.detailsText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
