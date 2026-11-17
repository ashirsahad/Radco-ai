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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.BankAccountEntity
import com.accountpro.app.data.local.BankTransactionEntity
import com.accountpro.app.data.local.ExpenseEntity
import com.accountpro.app.data.local.InvoiceEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.EmptyStateView
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.MonoCodeChip
import com.accountpro.app.ui.components.SearchField
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.components.StatCard
import com.accountpro.app.ui.components.StatTone
import com.accountpro.app.ui.components.StatusBadge
import com.accountpro.app.ui.viewmodel.AppSessionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankingScreen(
    session: AppSessionState,
    accounts: List<BankAccountEntity>,
    transactions: List<BankTransactionEntity>,
    invoices: List<InvoiceEntity>,
    expenses: List<ExpenseEntity>,
    onAddAccount: (name: String, last4: String, type: String, balance: Double) -> Unit,
    onAddTransaction: (accountId: String, date: String, description: String, amount: Double) -> Unit,
    onReconcile: (BankTransactionEntity, String) -> Unit,
) {
    val strings = session.strings
    val baseCode = session.baseCurrency.code
    var search by rememberSaveable { mutableStateOf("") }
    var accountFilter by rememberSaveable { mutableStateOf("all") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }

    var matchTx by remember { mutableStateOf<BankTransactionEntity?>(null) }
    var showAddAccount by remember { mutableStateOf(false) }
    var showAddTx by remember { mutableStateOf(false) }

    val totalBalance = accounts.sumOf { CurrencyManager.convert(it.balance, "USD", baseCode) }
    val unreconciledCount = transactions.count { !it.reconciled }
    val reconciledCount = transactions.size - unreconciledCount
    val fmt = { v: Double -> CurrencyManager.format(v, baseCode, session.language.code) }

    val filtered = remember(transactions, search, accountFilter, statusFilter) {
        transactions.filter { tx ->
            val matchAcc = accountFilter == "all" || tx.accountId == accountFilter
            val matchStatus = when (statusFilter) {
                "reconciled" -> tx.reconciled
                "unreconciled" -> !tx.reconciled
                else -> true
            }
            val matchSearch = search.isBlank() ||
                tx.description.contains(search, ignoreCase = true) ||
                tx.txCode.contains(search, ignoreCase = true)
            matchAcc && matchStatus && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = strings.banking,
                subtitle = "Accounts, transactions and reconciliation",
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showAddAccount = true },
                    modifier = Modifier.weight(1f).testTag("add_bank_account_button"),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add account")
                }
                Button(
                    onClick = { showAddTx = true },
                    modifier = Modifier.weight(1f).testTag("add_bank_tx_button"),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add transaction")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Total Balance",
                    value = fmt(totalBalance),
                    subtitle = "${accounts.size} accounts",
                    icon = Icons.Outlined.AccountBalance,
                    tone = StatTone.REVENUE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1.2f),
                )
                StatCard(
                    title = "Unreconciled",
                    value = unreconciledCount.toString(),
                    icon = Icons.Outlined.Link,
                    tone = StatTone.INVOICE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(0.9f),
                )
                StatCard(
                    title = "Reconciled",
                    value = reconciledCount.toString(),
                    icon = Icons.Outlined.CheckCircle,
                    tone = StatTone.CLIENT,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(0.9f),
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(accounts, key = { it.id }) { acc ->
                    EditorialCard(
                        onClick = { accountFilter = if (accountFilter == acc.id) "all" else acc.id },
                        modifier = Modifier.width(210.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(acc.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${acc.type} · ${acc.maskedNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = fmt(CurrencyManager.convert(acc.balance, "USD", baseCode)),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item {
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search transactions...",
            )
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "all" to "All statuses",
                    "unreconciled" to "Unreconciled",
                    "reconciled" to "Reconciled",
                ),
                selectedKey = statusFilter,
                onSelect = { statusFilter = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.AccountBalance,
                    title = "No transactions match",
                    message = "Try clearing your account or status filter.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                val accName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "Account"
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MonoCodeChip(tx.txCode)
                                StatusBadge(
                                    label = if (tx.reconciled) "Reconciled${tx.matchedTo?.let { " · $it" } ?: ""}" else "Unreconciled",
                                    statusKey = if (tx.reconciled) "reconciled" else "unreconciled",
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(tx.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${tx.date} · $accName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = fmt(CurrencyManager.convert(tx.amount, "USD", baseCode)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (!tx.reconciled) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { matchTx = tx },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Match", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Match Reconciliation Dialog
    matchTx?.let { tx ->
        val options = remember(invoices, expenses) {
            invoices.map { it.invoiceNumber to "Invoice ${it.invoiceNumber} — ${it.clientName}" } +
                expenses.map { it.expenseCode to "Expense ${it.expenseCode} — ${it.description}" }
        }
        var selectedMatch by remember { mutableStateOf(options.firstOrNull()?.first ?: "") }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { matchTx = null },
            title = { Text("Match transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tx.description, style = MaterialTheme.typography.bodyMedium)
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = options.firstOrNull { it.first == selectedMatch }?.second ?: selectedMatch,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Match to") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            options.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedMatch = code
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReconcile(tx, selectedMatch)
                        matchTx = null
                    },
                    enabled = selectedMatch.isNotBlank(),
                ) { Text("Reconcile") }
            },
            dismissButton = {
                TextButton(onClick = { matchTx = null }) { Text("Cancel") }
            },
        )
    }

    if (showAddAccount) {
        var name by remember { mutableStateOf("") }
        var last4 by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Checking") }
        var balance by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAccount = false },
            title = { Text("Add bank account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = last4, onValueChange = { last4 = it }, label = { Text("Last 4 digits") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FilterChipsRow(
                        options = listOf("Checking" to "Checking", "Savings" to "Savings", "Credit Card" to "Credit Card"),
                        selectedKey = type,
                        onSelect = { type = it },
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Opening balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddAccount(name.trim(), last4.trim(), type, balance.toDoubleOrNull() ?: 0.0)
                        showAddAccount = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Add account") }
            },
            dismissButton = { TextButton(onClick = { showAddAccount = false }) { Text("Cancel") } },
        )
    }

    if (showAddTx && accounts.isNotEmpty()) {
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        var accountId by remember { mutableStateOf(accounts.first().id) }
        var desc by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(today) }
        var amount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTx = false },
            title = { Text("Add transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChipsRow(
                        options = accounts.map { it.id to it.name },
                        selectedKey = accountId,
                        onSelect = { accountId = it },
                    )
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (negative for outflow)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddTransaction(accountId, date, desc.trim(), amount.toDoubleOrNull() ?: 0.0)
                        showAddTx = false
                    },
                    enabled = desc.isNotBlank(),
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddTx = false }) { Text("Cancel") } },
        )
    }
}
