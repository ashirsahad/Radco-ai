package com.accountpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.ClientEntity
import com.accountpro.app.data.local.ExpenseEntity
import com.accountpro.app.data.local.ProjectEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val EXPENSE_CATEGORIES = listOf(
    "Office Supplies", "Travel", "Meals & Entertainment", "Professional Services",
    "Software & Subscriptions", "Equipment", "Marketing", "Utilities",
    "Rent", "Insurance", "Legal & Professional", "Other",
)

private val PAYMENT_METHODS = listOf(
    "Credit Card", "Debit Card", "Cash", "Bank Transfer", "Check", "PayPal", "Other",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseFormDialog(
    orgId: String,
    nextIndex: Int,
    existing: ExpenseEntity?,
    clients: List<ClientEntity>,
    projects: List<ProjectEntity>,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amountText by remember { mutableStateOf(if (existing != null) existing.amount.toString() else "") }
    var currency by remember { mutableStateOf(existing?.currency ?: defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(existing?.category ?: EXPENSE_CATEGORIES.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(existing?.date ?: today) }
    var vendor by remember { mutableStateOf(existing?.vendor ?: "") }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: PAYMENT_METHODS.first()) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(existing?.status ?: "pending") }
    var statusExpanded by remember { mutableStateOf(false) }
    var billable by remember { mutableStateOf(existing?.billable ?: false) }
    var clientName by remember { mutableStateOf(existing?.clientName ?: (clients.firstOrNull()?.name ?: "")) }
    var projectName by remember { mutableStateOf(existing?.projectName ?: (projects.firstOrNull()?.name ?: "")) }
    var receiptUri by remember { mutableStateOf(existing?.receiptUri) }
    var newTag by remember { mutableStateOf("") }
    val tags = remember {
        mutableStateListOf<String>().apply {
            existing?.tagsCsv?.split(",")?.filter { it.isNotBlank() }?.let { addAll(it) }
        }
    }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    // Zero-permission Android Photo Picker per Google Play Policy
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 20.dp),
        title = { Text(if (existing == null) "Create Expense" else "Edit Expense") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_description_input"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("expense_amount_input"),
                    )
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
                }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        EXPENSE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
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
                            listOf("pending", "paid").forEach { st ->
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

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = it },
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false },
                    ) {
                        PAYMENT_METHODS.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    paymentExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Billable to client", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = billable, onCheckedChange = { billable = it })
                }

                if (billable) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (receiptUri != null) "Receipt attached (Tap to change)" else "Attach Receipt Image")
                }
                if (receiptUri != null) {
                    AsyncImage(
                        model = receiptUri,
                        contentDescription = "Attached expense receipt preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }

                // Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("Add tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val trimmed = newTag.trim()
                            if (trimmed.isNotEmpty() && trimmed !in tags) {
                                tags.add(trimmed)
                                newTag = ""
                            }
                        },
                    ) { Text("Add") }
                }
                if (tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = { tags.remove(tag) },
                                label = { Text(tag) },
                                trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp)) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val code = existing?.expenseCode ?: "EXP-${nextIndex.toString().padStart(3, '0')}"
                    onSave(
                        ExpenseEntity(
                            id = existing?.id ?: "$orgId-$code-${UUID.randomUUID().toString().take(4)}",
                            organizationId = orgId,
                            expenseCode = code,
                            description = description.trim(),
                            amount = amountText.toDoubleOrNull() ?: 0.0,
                            currency = currency,
                            date = date,
                            category = category,
                            vendor = vendor.trim(),
                            paymentMethod = paymentMethod,
                            status = status,
                            hasReceipt = (existing?.hasReceipt == true) || receiptUri != null,
                            receiptUri = receiptUri,
                            billable = billable,
                            clientName = if (billable) clientName else "",
                            projectName = if (billable) projectName else "",
                            tagsCsv = tags.joinToString(","),
                            notes = notes.trim(),
                        )
                    )
                },
                enabled = description.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.testTag("save_expense_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
