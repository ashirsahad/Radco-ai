package com.accountpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.ClientEntity
import com.accountpro.app.data.local.InvoiceEntity
import com.accountpro.app.data.local.InvoiceLineItem
import com.accountpro.app.ui.components.EditorialCard
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormDialog(
    orgId: String,
    existing: InvoiceEntity?,
    clients: List<ClientEntity>,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (InvoiceEntity) -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var invoiceNumber by remember {
        mutableStateOf(existing?.invoiceNumber ?: "INV-${(100..999).random()}")
    }
    var selectedClient by remember {
        mutableStateOf(
            clients.firstOrNull { it.name == existing?.clientName || it.id == existing?.clientId }
                ?: clients.firstOrNull()
        )
    }
    var clientExpanded by remember { mutableStateOf(false) }
    var currency by remember { mutableStateOf(existing?.currency ?: defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(existing?.status ?: "draft") }
    var statusExpanded by remember { mutableStateOf(false) }
    var issueDate by remember { mutableStateOf(existing?.issueDate ?: today) }
    var dueDate by remember { mutableStateOf(existing?.dueDate ?: today) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var terms by remember { mutableStateOf(existing?.terms ?: "") }

    val initialItems = remember(existing) {
        val parsed = existing?.itemsJson?.let { json ->
            runCatching { Json.decodeFromString<List<InvoiceLineItem>>(json) }.getOrNull()
        }
        if (!parsed.isNullOrEmpty()) parsed
        else listOf(InvoiceLineItem("1", "Consulting Service", 1.0, existing?.amount ?: 0.0, 0.0))
    }
    val items = remember { mutableStateListOf<InvoiceLineItem>().apply { addAll(initialItems) } }

    val subtotal = items.sumOf { it.amount }
    val taxTotal = items.sumOf { it.taxAmount }
    val total = subtotal + taxTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 20.dp),
        title = {
            Text(if (existing == null) "Create Invoice" else "Edit Invoice")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_number_input"),
                )

                // Client dropdown
                ExposedDropdownMenuBox(
                    expanded = clientExpanded,
                    onExpandedChange = { clientExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedClient?.name ?: "Select client",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Client") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = clientExpanded,
                        onDismissRequest = { clientExpanded = false },
                    ) {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = { Text(client.name) },
                                onClick = {
                                    selectedClient = client
                                    clientExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false },
                        ) {
                            CurrencyManager.supportedCurrencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${curr.symbol} ${curr.code}") },
                                    onClick = {
                                        currency = curr.code
                                        currencyExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = status.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                        ) {
                            listOf("draft", "pending", "paid", "overdue").forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        status = st
                                        statusExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = issueDate,
                        onValueChange = { issueDate = it },
                        label = { Text("Issue Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Line Items", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = {
                            items.add(
                                InvoiceLineItem(
                                    id = UUID.randomUUID().toString(),
                                    description = "",
                                    quantity = 1.0,
                                    rate = 0.0,
                                    taxRate = 0.0,
                                )
                            )
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item")
                    }
                }

                items.forEachIndexed { index, item ->
                    EditorialCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedTextField(
                                    value = item.description,
                                    onValueChange = { items[index] = item.copy(description = it) },
                                    label = { Text("Description") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                if (items.size > 1) {
                                    IconButton(onClick = { items.removeAt(index) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Remove item")
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                                    onValueChange = {
                                        items[index] = item.copy(quantity = it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = if (item.rate == 0.0) "" else item.rate.toString(),
                                    onValueChange = {
                                        items[index] = item.copy(rate = it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("Rate") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                )
                                OutlinedTextField(
                                    value = if (item.taxRate == 0.0) "" else item.taxRate.toString(),
                                    onValueChange = {
                                        items[index] = item.copy(taxRate = it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("Tax %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(CurrencyManager.format(subtotal, currency), style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tax", style = MaterialTheme.typography.bodyMedium)
                    Text(CurrencyManager.format(taxTotal, currency), style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(CurrencyManager.format(total, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = terms,
                    onValueChange = { terms = it },
                    label = { Text("Terms & Conditions") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = InvoiceEntity(
                        id = existing?.id ?: "$orgId-$invoiceNumber-${UUID.randomUUID().toString().take(4)}",
                        organizationId = orgId,
                        invoiceNumber = invoiceNumber.trim(),
                        clientId = selectedClient?.id ?: "",
                        clientName = selectedClient?.name ?: "Unassigned client",
                        amount = total,
                        subtotal = subtotal,
                        taxTotal = taxTotal,
                        currency = currency,
                        issueDate = issueDate,
                        dueDate = dueDate,
                        status = status,
                        notes = notes,
                        terms = terms,
                        itemsJson = Json.encodeToString(items.toList()),
                    )
                    onSave(entity)
                },
                enabled = invoiceNumber.isNotBlank(),
                modifier = Modifier.testTag("save_invoice_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
