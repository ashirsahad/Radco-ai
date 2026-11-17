package com.accountpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.PosProductEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.viewmodel.AppSessionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    session: AppSessionState,
    products: List<PosProductEntity>,
    onProcessCheckout: (Map<PosProductEntity, Int>, Double, String) -> Unit,
) {
    val baseCode = session.baseCurrency.code
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var paymentMethod by rememberSaveable { mutableStateOf("cash") }
    var showCartSheet by remember { mutableStateOf(false) }

    val cart = remember { mutableStateMapOf<String, Int>() }
    val cartProducts = remember(cart.toMap(), products) {
        cart.mapNotNull { (id, qty) ->
            products.firstOrNull { it.id == id }?.let { it to qty }
        }.toMap()
    }

    val categories = remember(products) {
        listOf("All" to "All") + products.map { it.category }.distinct().map { it to it }
    }

    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "All") products else products.filter { it.category == selectedCategory }
    }

    val subtotal = cartProducts.entries.sumOf { (p, qty) ->
        CurrencyManager.convert(p.price, "USD", baseCode) * qty
    }
    val tax = subtotal * 0.10
    val finalTotal = subtotal + tax
    val totalItemCount = cart.values.sum()
    val fmt = { amount: Double -> CurrencyManager.format(amount, baseCode, session.language.code) }

    fun addProduct(product: PosProductEntity) {
        if (product.stock <= 0) return
        val current = cart[product.id] ?: 0
        if (current < product.stock) {
            cart[product.id] = current + 1
        }
    }

    fun updateQty(productId: String, nextQty: Int) {
        if (nextQty <= 0) cart.remove(productId) else cart[productId] = nextQty
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Products Catalog Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
            ) {
                SectionHeader(
                    title = "Point of Sale",
                    subtitle = "Tap products to add to order",
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilterChipsRow(
                    options = categories,
                    selectedKey = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = if (isWide) 16.dp else 80.dp),
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val inCartQty = cart[product.id] ?: 0
                        EditorialCard(
                            onClick = { addProduct(product) },
                            modifier = Modifier.testTag("pos_product_${product.name}"),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ) {
                                        Text("${product.stock}")
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = product.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = fmt(CurrencyManager.convert(product.price, "USD", baseCode)),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (inCartQty > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ) {
                                            Text("x$inCartQty")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isWide && totalItemCount > 0) {
                    Button(
                        onClick = { showCartSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("pos_open_cart_button"),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Current Order ($totalItemCount)", modifier = Modifier.weight(1f))
                        Text(fmt(finalTotal), fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isWide) {
                Surface(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight(),
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    PosCartPanel(
                        cartProducts = cartProducts,
                        subtotal = subtotal,
                        tax = tax,
                        finalTotal = finalTotal,
                        paymentMethod = paymentMethod,
                        fmt = fmt,
                        onPaymentMethodChange = { paymentMethod = it },
                        onUpdateQty = ::updateQty,
                        onClear = { cart.clear() },
                        onCheckout = {
                            onProcessCheckout(cartProducts, finalTotal, paymentMethod)
                            cart.clear()
                        },
                    )
                }
            }
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            PosCartPanel(
                cartProducts = cartProducts,
                subtotal = subtotal,
                tax = tax,
                finalTotal = finalTotal,
                paymentMethod = paymentMethod,
                fmt = fmt,
                onPaymentMethodChange = { paymentMethod = it },
                onUpdateQty = ::updateQty,
                onClear = {
                    cart.clear()
                    showCartSheet = false
                },
                onCheckout = {
                    onProcessCheckout(cartProducts, finalTotal, paymentMethod)
                    cart.clear()
                    showCartSheet = false
                },
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun PosCartPanel(
    cartProducts: Map<PosProductEntity, Int>,
    subtotal: Double,
    tax: Double,
    finalTotal: Double,
    paymentMethod: String,
    fmt: (Double) -> String,
    onPaymentMethodChange: (String) -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Current Order", style = MaterialTheme.typography.titleLarge)
            if (cartProducts.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear cart")
                }
            }
        }

        if (cartProducts.isEmpty()) {
            Text(
                text = "No items in cart",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cartProducts.entries.toList(), key = { it.key.id }) { (product, qty) ->
                    EditorialCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${fmt(product.price)} each",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onUpdateQty(product.id, qty - 1) }) {
                                Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                            }
                            Text(qty.toString(), style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { onUpdateQty(product.id, qty + 1) }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Increase quantity", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                Text(fmt(subtotal), style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tax (10%)", style = MaterialTheme.typography.bodyMedium)
                Text(fmt(tax), style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(fmt(finalTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Text("Payment Method", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Triple("cash", "Cash", Icons.Outlined.AttachMoney),
                    Triple("card", "Card", Icons.Outlined.CreditCard),
                    Triple("mobile", "Mobile", Icons.Outlined.Smartphone),
                ).forEach { (key, label, icon) ->
                    val selected = paymentMethod == key
                    if (selected) {
                        Button(
                            onClick = { onPaymentMethodChange(key) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onPaymentMethodChange(key) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pos_process_payment_button"),
            ) {
                Icon(Icons.Outlined.PointOfSale, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Process Payment")
            }
        }
    }
}
