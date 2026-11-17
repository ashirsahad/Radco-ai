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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.InventoryItemEntity
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
import java.util.UUID

@Composable
fun InventoryScreen(
    session: AppSessionState,
    items: List<InventoryItemEntity>,
    onSaveItem: (InventoryItemEntity, Boolean) -> Unit,
    onAdjustStock: (InventoryItemEntity, Int) -> Unit,
    onDeleteItem: (String) -> Unit,
) {
    val strings = session.strings
    val baseCode = session.baseCurrency.code
    var search by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(items, search, statusFilter) {
        items.filter { item ->
            val matchStatus = statusFilter == "all" || item.status == statusFilter
            val matchSearch = search.isBlank() ||
                item.name.contains(search, ignoreCase = true) ||
                item.sku.contains(search, ignoreCase = true)
            matchStatus && matchSearch
        }
    }

    val totalValue = items.sumOf { CurrencyManager.convert(it.totalValue, "USD", baseCode) }
    val lowStockCount = items.count { it.status == "low-stock" }
    val outOfStockCount = items.count { it.status == "out-of-stock" }
    val fmt = { v: Double -> CurrencyManager.format(v, baseCode, session.language.code) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = strings.inventory,
                subtitle = "Track and manage your product inventory",
                actionLabel = "Add Item",
                actionIcon = Icons.Outlined.Add,
                onActionClick = { showAddDialog = true },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Total Value",
                    value = fmt(totalValue),
                    icon = Icons.Outlined.BarChart,
                    tone = StatTone.REVENUE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Total Items",
                    value = items.size.toString(),
                    icon = Icons.Outlined.Inventory2,
                    tone = StatTone.CLIENT,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Low Stock",
                    value = lowStockCount.toString(),
                    icon = Icons.Outlined.TrendingDown,
                    tone = StatTone.INVOICE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Out of Stock",
                    value = outOfStockCount.toString(),
                    icon = Icons.Outlined.WarningAmber,
                    tone = StatTone.EXPENSE,
                    isDark = session.darkTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search items or SKU...",
            )
        }

        item {
            FilterChipsRow(
                options = listOf(
                    "all" to "All Status",
                    "in-stock" to "In Stock",
                    "low-stock" to "Low Stock",
                    "out-of-stock" to "Out of Stock",
                ),
                selectedKey = statusFilter,
                onSelect = { statusFilter = it },
            )
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.Inventory2,
                    title = "No inventory items",
                    message = "Add an item or clear your filters.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                MonoCodeChip(item.sku)
                                StatusBadge(label = item.status.replace("-", " "), statusKey = item.status)
                            }
                            IconButton(onClick = { onDeleteItem(item.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete item", modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${item.category} · ${item.location} · ${item.supplier}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Unit: ${fmt(CurrencyManager.convert(item.unitPrice, "USD", baseCode))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "Value: ${fmt(CurrencyManager.convert(item.totalValue, "USD", baseCode))}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onAdjustStock(item, -1) }) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease stock")
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(item.currentStock.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("min ${item.reorderLevel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onAdjustStock(item, +1) }) {
                                    Icon(Icons.Outlined.Add, contentDescription = "Increase stock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && session.currentUser != null) {
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        var name by remember { mutableStateOf("") }
        var sku by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Electronics") }
        var stock by remember { mutableStateOf("10") }
        var reorderLevel by remember { mutableStateOf("5") }
        var unitPrice by remember { mutableStateOf("") }
        var supplier by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("Warehouse A") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Inventory Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = reorderLevel, onValueChange = { reorderLevel = it }, label = { Text("Reorder At") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Unit Price (USD)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveItem(
                            InventoryItemEntity(
                                id = "${session.currentUser.organizationId}-inv-${UUID.randomUUID().toString().take(6)}",
                                organizationId = session.currentUser.organizationId,
                                name = name.trim(),
                                sku = sku.trim().ifBlank { "SKU-${(100..999).random()}" },
                                category = category.trim(),
                                currentStock = stock.toIntOrNull() ?: 0,
                                reorderLevel = reorderLevel.toIntOrNull() ?: 5,
                                reorderQuantity = 20,
                                unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                                supplier = supplier.trim().ifBlank { "Default Supplier" },
                                location = location.trim(),
                                lastUpdated = today,
                            ),
                            false,
                        )
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
        )
    }
}
