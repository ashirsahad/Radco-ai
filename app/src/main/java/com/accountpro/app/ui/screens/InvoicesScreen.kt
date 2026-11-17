package com.accountpro.app.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.core.Localization
import com.accountpro.app.data.local.ClientEntity
import com.accountpro.app.data.local.InvoiceEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.EmptyStateView
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.MonoCodeChip
import com.accountpro.app.ui.components.SearchField
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.components.StatusBadge
import com.accountpro.app.ui.viewmodel.AppSessionState

@Composable
fun InvoicesScreen(
    session: AppSessionState,
    invoices: List<InvoiceEntity>,
    clients: List<ClientEntity>,
    initialShowCreate: Boolean = false,
    onConsumeInitialShowCreate: () -> Unit = {},
    onSaveInvoice: (InvoiceEntity, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val strings = session.strings
    var search by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var showForm by remember { mutableStateOf(initialShowCreate) }
    var editingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }

    if (initialShowCreate && !showForm) {
        showForm = true
        onConsumeInitialShowCreate()
    } else if (initialShowCreate) {
        onConsumeInitialShowCreate()
    }

    val filtered = remember(invoices, search, statusFilter) {
        invoices.filter { inv ->
            val matchesStatus = statusFilter == "all" || inv.status == statusFilter
            val matchesSearch = search.isBlank() ||
                inv.invoiceNumber.contains(search, ignoreCase = true) ||
                inv.clientName.contains(search, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    fun exportInvoices(list: List<InvoiceEntity>) {
        val summary = buildString {
            appendLine("RAD Consultancy Invoices Export")
            list.forEach {
                appendLine("${it.invoiceNumber} | ${it.clientName} | ${it.currency} ${it.amount} | ${it.status} | Due: ${it.dueDate}")
            }
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "RAD Consultancy Invoices")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Invoices"))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = strings.invoices,
                subtitle = "Manage your invoices and billing",
                actionLabel = "New Invoice",
                actionIcon = Icons.Outlined.Add,
                onActionClick = {
                    editingInvoice = null
                    showForm = true
                },
                actionTestTag = "new_invoice_button",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Search invoices...",
                    modifier = Modifier.weight(1f),
                    testTag = "invoice_search_input",
                )
                OutlinedButton(
                    onClick = { exportInvoices(filtered) },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("invoice_export_button"),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Export invoices", modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "all" to "All",
                    "draft" to strings.draft,
                    "pending" to strings.pending,
                    "paid" to strings.paid,
                    "overdue" to strings.overdue,
                ),
                selectedKey = statusFilter,
                onSelect = { statusFilter = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.Description,
                    title = "No invoices found",
                    message = "Try adjusting your search or create a new invoice.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { invoice ->
                EditorialCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_card_${invoice.invoiceNumber}"),
                    onClick = {
                        editingInvoice = invoice
                        showForm = true
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MonoCodeChip(invoice.invoiceNumber)
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(
                                    label = Localization.statusLabel(invoice.status, strings),
                                    statusKey = invoice.status,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = invoice.clientName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Issued ${invoice.issueDate} · Due ${invoice.dueDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyManager.format(invoice.amount, invoice.currency, session.language.code),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        editingInvoice = invoice
                                        showForm = true
                                    },
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit invoice", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { exportInvoices(listOf(invoice)) }) {
                                    Icon(Icons.Outlined.Share, contentDescription = "Share invoice", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm && session.currentUser != null) {
        InvoiceFormDialog(
            orgId = session.currentUser.organizationId,
            existing = editingInvoice,
            clients = clients,
            defaultCurrency = session.baseCurrency.code,
            onDismiss = {
                showForm = false
                editingInvoice = null
            },
            onSave = { invoice ->
                onSaveInvoice(invoice, editingInvoice != null)
                showForm = false
                editingInvoice = null
            },
        )
    }
}
