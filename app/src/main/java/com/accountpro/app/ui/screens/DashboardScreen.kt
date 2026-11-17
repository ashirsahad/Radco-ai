package com.accountpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.AppModule
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.core.Localization
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.MonoCodeChip
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.components.StatCard
import com.accountpro.app.ui.components.StatTone
import com.accountpro.app.ui.components.StatusBadge
import com.accountpro.app.ui.viewmodel.AppSessionState
import com.accountpro.app.ui.viewmodel.WorkspaceDataState

@Composable
fun DashboardScreen(
    session: AppSessionState,
    data: WorkspaceDataState,
    onNavigateToModule: (AppModule) -> Unit,
    onNewInvoice: () -> Unit,
) {
    val strings = session.strings
    val baseCode = session.baseCurrency.code

    val totalRevenue = data.invoices
        .filter { it.status == "paid" }
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val outstandingAmount = data.invoices
        .filter { it.status == "pending" || it.status == "overdue" }
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val totalExpenses = data.expenses
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val activeClientsCount = data.clients.count { it.status == "active" }

    val fmt = { amount: Double -> CurrencyManager.format(amount, baseCode, session.language.code) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = strings.dashboard,
                subtitle = strings.welcomeBack,
                actionLabel = if (session.canAccess(AppModule.INVOICES)) "New Invoice" else null,
                actionIcon = Icons.Outlined.Add,
                onActionClick = onNewInvoice,
                actionTestTag = "dashboard_new_invoice_button",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    title = "Total Revenue",
                    value = fmt(totalRevenue),
                    change = "+12.5%",
                    changePositive = true,
                    subtitle = "Paid invoices",
                    icon = Icons.Outlined.AttachMoney,
                    tone = StatTone.REVENUE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Outstanding",
                    value = fmt(outstandingAmount),
                    change = "${data.invoices.count { it.status == "pending" || it.status == "overdue" }} open",
                    changePositive = false,
                    subtitle = "Pending / overdue",
                    icon = Icons.Outlined.Description,
                    tone = StatTone.INVOICE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    title = strings.expenses,
                    value = fmt(totalExpenses),
                    change = "${data.expenses.size} items",
                    changePositive = null,
                    subtitle = "This month",
                    icon = Icons.Outlined.TrendingDown,
                    tone = StatTone.EXPENSE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Active Clients",
                    value = activeClientsCount.toString(),
                    change = "${data.clients.size} total",
                    changePositive = true,
                    subtitle = "Directory",
                    icon = Icons.Outlined.Group,
                    tone = StatTone.CLIENT,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Recent Invoices
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Recent Invoices",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Latest invoice activity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (session.canAccess(AppModule.INVOICES)) {
                            TextButton(
                                onClick = { onNavigateToModule(AppModule.INVOICES) },
                                modifier = Modifier.testTag("dashboard_view_all_invoices"),
                            ) {
                                Text("View All")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    data.invoices.take(4).forEachIndexed { index, invoice ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = invoice.clientName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = invoice.issueDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = CurrencyManager.format(invoice.amount, invoice.currency, session.language.code),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Common tasks and shortcuts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    if (session.canAccess(AppModule.INVOICES)) {
                        OutlinedButton(
                            onClick = onNewInvoice,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quick_action_invoice"),
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Create Invoice", modifier = Modifier.weight(1f))
                        }
                    }
                    if (session.canAccess(AppModule.CLIENTS)) {
                        OutlinedButton(
                            onClick = { onNavigateToModule(AppModule.CLIENTS) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quick_action_clients"),
                        ) {
                            Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Manage Clients", modifier = Modifier.weight(1f))
                        }
                    }
                    if (session.canAccess(AppModule.EXPENSES)) {
                        OutlinedButton(
                            onClick = { onNavigateToModule(AppModule.EXPENSES) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quick_action_expenses"),
                        ) {
                            Icon(Icons.Outlined.TrendingDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Record Expense", modifier = Modifier.weight(1f))
                        }
                    }
                    if (session.canAccess(AppModule.REPORTS)) {
                        Button(
                            onClick = { onNavigateToModule(AppModule.REPORTS) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quick_action_reports"),
                        ) {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("View Reports", modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
