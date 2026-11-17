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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.core.Localization
import com.accountpro.app.data.local.PaymentEntity
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
fun PaymentsScreen(
    session: AppSessionState,
    payments: List<PaymentEntity>,
    onRecordPayment: (invoiceId: String, clientName: String, amount: Double, method: String, status: String) -> Unit,
) {
    val strings = session.strings
    val baseCode = session.baseCurrency.code
    var search by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var showRecordDialog by remember { mutableStateOf(false) }
    var detailPayment by remember { mutableStateOf<PaymentEntity?>(null) }

    val filtered = remember(payments, search, statusFilter) {
        payments.filter { p ->
            val matchesStatus = statusFilter == "all" || p.status == statusFilter
            val matchesSearch = search.isBlank() ||
                p.paymentCode.contains(search, ignoreCase = true) ||
                p.clientName.contains(search, ignoreCase = true) ||
                p.invoiceId.contains(search, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    val totalAmount = payments.sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val completedAmount = payments.filter { it.status == "completed" }
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val pendingAmount = payments.filter { it.status == "pending" || it.status == "processing" }
        .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCode) }
    val fmt = { v: Double -> CurrencyManager.format(v, baseCode, session.language.code) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = strings.payments,
                subtitle = "Track and manage payment transactions",
                actionLabel = "Record",
                actionIcon = Icons.Outlined.Add,
                onActionClick = { showRecordDialog = true },
                actionTestTag = "record_payment_button",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    title = "Total",
                    value = fmt(totalAmount),
                    icon = Icons.Outlined.CreditCard,
                    tone = StatTone.NEUTRAL,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = strings.completed,
                    value = fmt(completedAmount),
                    icon = Icons.Outlined.CreditCard,
                    tone = StatTone.REVENUE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = strings.pending,
                    value = fmt(pendingAmount),
                    icon = Icons.Outlined.CreditCard,
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
                placeholder = "Search payments...",
            )
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "all" to "All",
                    "completed" to strings.completed,
                    "processing" to "Processing",
                    "pending" to strings.pending,
                    "failed" to "Failed",
                ),
                selectedKey = statusFilter,
                onSelect = { statusFilter = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.CreditCard,
                    title = "No payments found",
                    message = "Record a payment or change your filters.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { payment ->
                val methodIcon = when (payment.method) {
                    "bank_transfer" -> Icons.Outlined.AccountBalance
                    "mobile_payment" -> Icons.Outlined.Smartphone
                    "cash" -> Icons.Outlined.AttachMoney
                    else -> Icons.Outlined.CreditCard
                }
                val methodLabel = when (payment.method) {
                    "credit_card" -> "Credit Card"
                    "bank_transfer" -> "Bank Transfer"
                    "mobile_payment" -> "Mobile Payment"
                    "cash" -> "Cash"
                    else -> payment.method
                }

                EditorialCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { detailPayment = payment },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = methodIcon,
                                contentDescription = methodLabel,
                                modifier = Modifier.padding(9.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MonoCodeChip(payment.paymentCode)
                                StatusBadge(
                                    label = Localization.statusLabel(payment.status, strings),
                                    statusKey = payment.status,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${payment.clientName} · ${payment.invoiceId}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "$methodLabel · ${payment.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = CurrencyManager.format(payment.amount, payment.currency, session.language.code),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        var invoiceId by remember { mutableStateOf("INV-002") }
        var clientName by remember { mutableStateOf("Marketing Agency Co") }
        var amountText by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("credit_card") }

        AlertDialog(
            onDismissRequest = { showRecordDialog = false },
            title = { Text("Record Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = invoiceId, onValueChange = { invoiceId = it }, label = { Text("Invoice ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FilterChipsRow(
                        options = listOf(
                            "credit_card" to "Card",
                            "bank_transfer" to "Bank",
                            "mobile_payment" to "Mobile",
                            "cash" to "Cash",
                        ),
                        selectedKey = method,
                        onSelect = { method = it },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordPayment(invoiceId, clientName, amountText.toDoubleOrNull() ?: 0.0, method, "completed")
                        showRecordDialog = false
                    },
                    enabled = clientName.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRecordDialog = false }) { Text("Cancel") }
            },
        )
    }

    detailPayment?.let { p ->
        AlertDialog(
            onDismissRequest = { detailPayment = null },
            title = { Text("${p.paymentCode} Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Client: ${p.clientName}")
                    Text("Invoice: ${p.invoiceId}")
                    Text("Amount: ${CurrencyManager.format(p.amount, p.currency, session.language.code)}")
                    Text("Date: ${p.date}")
                    Text("Status: ${p.status}")
                    Text("Reference: ${p.reference}")
                }
            },
            confirmButton = {
                TextButton(onClick = { detailPayment = null }) { Text("Close") }
            },
        )
    }
}
