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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.core.Localization
import com.accountpro.app.data.local.ClientEntity
import com.accountpro.app.data.local.ExpenseEntity
import com.accountpro.app.data.local.ProjectEntity
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

@Composable
fun ExpensesScreen(
    session: AppSessionState,
    expenses: List<ExpenseEntity>,
    clients: List<ClientEntity>,
    projects: List<ProjectEntity>,
    onSaveExpense: (ExpenseEntity, Boolean) -> Unit,
) {
    val strings = session.strings
    val baseCode = session.baseCurrency.code
    var search by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var showForm by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val categories = remember(expenses) {
        listOf("All" to "All") + expenses.map { it.category }.distinct().map { it to it }
    }

    val filtered = remember(expenses, search, selectedCategory) {
        expenses.filter { e ->
            val matchesCat = selectedCategory == "All" || e.category == selectedCategory
            val matchesSearch = search.isBlank() ||
                e.expenseCode.contains(search, ignoreCase = true) ||
                e.description.contains(search, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    val totalExpenses = expenses.sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val paidExpenses = expenses.filter { it.status == "paid" || it.status == "approved" }
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val pendingExpenses = (totalExpenses - paidExpenses).coerceAtLeast(0.0)
    val fmt = { v: Double -> CurrencyManager.format(v, baseCode, session.language.code) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = strings.expenses,
                subtitle = "Track and manage your business expenses",
                actionLabel = "Add Expense",
                actionIcon = Icons.Outlined.Add,
                onActionClick = {
                    editingExpense = null
                    showForm = true
                },
                actionTestTag = "add_expense_button",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    title = "Total",
                    value = fmt(totalExpenses),
                    icon = Icons.Outlined.ReceiptLong,
                    tone = StatTone.EXPENSE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = strings.paid,
                    value = fmt(paidExpenses),
                    icon = Icons.Outlined.ReceiptLong,
                    tone = StatTone.REVENUE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = strings.pending,
                    value = fmt(pendingExpenses),
                    icon = Icons.Outlined.ReceiptLong,
                    tone = StatTone.INVOICE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search expenses...",
                testTag = "expense_search_input",
            )
        }

        item {
            FilterChipsRow(
                options = categories,
                selectedKey = selectedCategory,
                onSelect = { selectedCategory = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "No expenses found",
                    message = "Add an expense or adjust your filters.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { expense ->
                EditorialCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingExpense = expense
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MonoCodeChip(expense.expenseCode)
                                StatusBadge(
                                    label = Localization.statusLabel(expense.status, strings),
                                    statusKey = expense.status,
                                )
                                if (expense.hasReceipt) {
                                    StatusBadge(label = "Receipt", statusKey = "active")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = expense.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = " · ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = expense.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyManager.format(expense.amount, expense.currency, session.language.code),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(
                                onClick = {
                                    editingExpense = expense
                                    showForm = true
                                },
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit expense", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm && session.currentUser != null) {
        ExpenseFormDialog(
            orgId = session.currentUser.organizationId,
            nextIndex = expenses.size + 1,
            existing = editingExpense,
            clients = clients,
            projects = projects,
            defaultCurrency = session.baseCurrency.code,
            onDismiss = {
                showForm = false
                editingExpense = null
            },
            onSave = { expense ->
                onSaveExpense(expense, editingExpense != null)
                showForm = false
                editingExpense = null
            },
        )
    }
}
