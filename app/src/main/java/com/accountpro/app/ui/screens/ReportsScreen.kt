package com.accountpro.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.components.StatCard
import com.accountpro.app.ui.components.StatTone
import com.accountpro.app.ui.components.StatusBadge
import com.accountpro.app.ui.viewmodel.AppSessionState
import com.accountpro.app.ui.viewmodel.WorkspaceDataState

private data class ReportTypeUi(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val status: String,
    val summaryLines: List<String>,
)

@Composable
fun ReportsScreen(
    session: AppSessionState,
    data: WorkspaceDataState,
) {
    val context = LocalContext.current
    val baseCode = session.baseCurrency.code
    var range by rememberSaveable { mutableStateOf("this_month") }
    var activeReport by remember { mutableStateOf<ReportTypeUi?>(null) }

    val totalRevenue = data.invoices.filter { it.status == "paid" }
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val outstanding = data.invoices.filter { it.status == "pending" || it.status == "overdue" }
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val totalExpenses = data.expenses
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val netProfit = totalRevenue - totalExpenses
    val fmt = { v: Double -> CurrencyManager.format(v, baseCode, session.language.code) }

    val reportTypes = remember(data, baseCode, totalRevenue, outstanding, totalExpenses, netProfit) {
        listOf(
            ReportTypeUi(
                title = "Revenue Report",
                description = "Paid invoice & POS revenue analysis",
                icon = Icons.Outlined.AttachMoney,
                status = "ready",
                summaryLines = listOf(
                    "Paid Invoices: ${data.invoices.count { it.status == "paid" }}",
                    "Recognized Revenue: ${fmt(totalRevenue)}",
                    "Completed Payments: ${fmt(data.payments.filter { it.status == "completed" }.sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) })}",
                ),
            ),
            ReportTypeUi(
                title = "Invoice Summary",
                description = "Invoice status and aging breakdown",
                icon = Icons.Outlined.Description,
                status = "ready",
                summaryLines = listOf(
                    "Total Invoices: ${data.invoices.size}",
                    "Paid: ${data.invoices.count { it.status == "paid" }}",
                    "Pending / Draft: ${data.invoices.count { it.status == "pending" || it.status == "draft" }}",
                    "Overdue: ${data.invoices.count { it.status == "overdue" }} (${fmt(outstanding)})",
                ),
            ),
            ReportTypeUi(
                title = "Client Analysis",
                description = "Client revenue and account status",
                icon = Icons.Outlined.Group,
                status = "ready",
                summaryLines = data.clients.map {
                    "${it.name}: ${it.totalInvoices} inv · ${fmt(CurrencyManager.convert(it.totalRevenue, "USD", baseCode))}"
                },
            ),
            ReportTypeUi(
                title = "Expense Report",
                description = "Categorized expense breakdown",
                icon = Icons.Outlined.TrendingDown,
                status = "ready",
                summaryLines = data.expenses.groupBy { it.category }.map { (cat, list) ->
                    val sum = list.sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
                    "$cat: ${fmt(sum)} (${list.size})"
                },
            ),
            ReportTypeUi(
                title = "Profit & Loss",
                description = "P&L statement for the current period",
                icon = Icons.Outlined.TrendingUp,
                status = "ready",
                summaryLines = listOf(
                    "Gross Revenue: ${fmt(totalRevenue)}",
                    "Total Operating Expenses: ${fmt(totalExpenses)}",
                    "Net Profit: ${fmt(netProfit)}",
                ),
            ),
            ReportTypeUi(
                title = "Tax Summary",
                description = "Tax collected across invoices",
                icon = Icons.Outlined.BarChart,
                status = "ready",
                summaryLines = listOf(
                    "Taxable Subtotal: ${fmt(data.invoices.sumOf { CurrencyManager.convert(it.subtotal, it.currency, baseCode) })}",
                    "Invoice Tax Total: ${fmt(data.invoices.sumOf { CurrencyManager.convert(it.taxTotal, it.currency, baseCode) })}",
                    "POS Sales Tax Rate: 10%",
                ),
            ),
        )
    }

    fun exportAll() {
        val text = buildString {
            appendLine("RAD Consultancy Financial Summary (${session.organization?.name ?: "Company"})")
            appendLine("Total Revenue: ${fmt(totalRevenue)}")
            appendLine("Outstanding: ${fmt(outstanding)}")
            appendLine("Expenses: ${fmt(totalExpenses)}")
            appendLine("Net Profit: ${fmt(netProfit)}")
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "RAD Consultancy Financial Report")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Financial Report"))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = session.strings.reports,
                subtitle = "Generate and view financial reports and analytics",
                actionLabel = "Export",
                actionIcon = Icons.Outlined.Share,
                onActionClick = ::exportAll,
            )
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "this_month" to "This Month",
                    "quarter" to "This Quarter",
                    "year" to "This Year",
                ),
                selectedKey = range,
                onSelect = { range = it },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Total Revenue",
                    value = fmt(totalRevenue),
                    change = "+12.5%",
                    changePositive = true,
                    icon = Icons.Outlined.AttachMoney,
                    tone = StatTone.REVENUE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Net Profit",
                    value = fmt(netProfit),
                    change = "+15.3%",
                    changePositive = netProfit >= 0,
                    icon = Icons.Outlined.TrendingUp,
                    tone = StatTone.CLIENT,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Outstanding",
                    value = fmt(outstanding),
                    icon = Icons.Outlined.Description,
                    tone = StatTone.INVOICE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Expenses",
                    value = fmt(totalExpenses),
                    icon = Icons.Outlined.TrendingDown,
                    tone = StatTone.EXPENSE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Custom Compose Canvas Chart: Monthly Revenue Trend
        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            val mutedColor = MaterialTheme.colorScheme.surfaceVariant
            val monthlySeries = remember(totalRevenue) {
                val base = totalRevenue.coerceAtLeast(1000.0)
                listOf(0.55f, 0.68f, 0.62f, 0.81f, 0.76f, 1.0f).map { it * base.toFloat() }
            }
            val months = listOf("Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Revenue Trend", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "6-month revenue trajectory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    ) {
                        val maxVal = (monthlySeries.maxOrNull() ?: 1f).coerceAtLeast(1f)
                        val count = monthlySeries.size
                        val slotWidth = size.width / count
                        val barWidth = slotWidth * 0.48f
                        monthlySeries.forEachIndexed { idx, value ->
                            val barHeight = (value / maxVal) * (size.height - 12f)
                            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = mutedColor,
                                topLeft = Offset(left, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(6f, 6f),
                            )
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6f, 6f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        months.forEach { m ->
                            Text(m, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        items(reportTypes, key = { it.title }) { report ->
            EditorialCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { activeReport = report },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(report.icon, contentDescription = report.title, modifier = Modifier.padding(9.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(report.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusBadge(label = "Ready", statusKey = report.status)
                    }
                    OutlinedButton(onClick = { activeReport = report }) {
                        Text("View")
                    }
                }
            }
        }
    }

    activeReport?.let { report ->
        AlertDialog(
            onDismissRequest = { activeReport = null },
            title = { Text(report.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.summaryLines.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeReport = null }) { Text("Close") }
            },
        )
    }
}
