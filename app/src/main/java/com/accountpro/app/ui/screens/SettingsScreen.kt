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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.AppLanguage
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.OrganizationEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.viewmodel.AppSessionState
import com.accountpro.app.ui.viewmodel.WorkspaceDataState

@Composable
fun SettingsScreen(
    session: AppSessionState,
    data: WorkspaceDataState,
    onSaveProfile: (fullName: String, email: String, phone: String) -> Unit,
    onSaveOrganization: (OrganizationEntity) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val user = session.currentUser
    val org = session.organization

    var fullName by remember(user?.id) { mutableStateOf(user?.fullName ?: "") }
    var email by remember(user?.id) { mutableStateOf(user?.email ?: "") }
    var phone by remember(user?.id) { mutableStateOf(user?.phone ?: "") }

    var companyName by remember(org?.id) { mutableStateOf(org?.name ?: "") }
    var address by remember(org?.id) { mutableStateOf(org?.address ?: "") }
    var taxId by remember(org?.id) { mutableStateOf(org?.taxId ?: "") }
    var acceptCards by remember(org?.id) { mutableStateOf(org?.acceptCreditCards ?: true) }
    var acceptBank by remember(org?.id) { mutableStateOf(org?.acceptBankTransfers ?: true) }
    var lateFeeEnabled by remember(org?.id) { mutableStateOf(org?.latePaymentFeesEnabled ?: false) }
    var lateFeePct by remember(org?.id) { mutableStateOf((org?.lateFeePercent ?: 5.0).toString()) }
    var emailNotif by remember(org?.id) { mutableStateOf(org?.emailNotifications ?: true) }
    var invoiceRemind by remember(org?.id) { mutableStateOf(org?.invoiceReminders ?: true) }
    var weeklyRep by remember(org?.id) { mutableStateOf(org?.weeklyReports ?: false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = session.strings.settings,
                subtitle = "Manage your account, company, and preferences",
            )
        }

        // Appearance, Language & Currency
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preferences", style = MaterialTheme.typography.titleLarge)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = session.darkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("settings_dark_theme_switch"),
                        )
                    }
                    Text("Language", style = MaterialTheme.typography.labelLarge)
                    FilterChipsRow(
                        options = AppLanguage.entries.map { it.code to "${it.flag} ${it.displayName}" },
                        selectedKey = session.language.code,
                        onSelect = { onSelectLanguage(AppLanguage.fromCode(it)) },
                    )
                    Text("Base Currency", style = MaterialTheme.typography.labelLarge)
                    FilterChipsRow(
                        options = CurrencyManager.supportedCurrencies.map { it.code to "${it.symbol} ${it.code}" },
                        selectedKey = session.baseCurrency.code,
                        onSelect = onSelectCurrency,
                    )
                }
            }
        }

        // Profile
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profile", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { onSaveProfile(fullName.trim(), email.trim(), phone.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save Profile") }
                }
            }
        }

        // Company Info
        if (org != null) {
            item {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Business, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Company Information", style = MaterialTheme.typography.titleLarge)
                        }
                        OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = taxId, onValueChange = { taxId = it }, label = { Text("Tax ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                onSaveOrganization(
                                    org.copy(
                                        name = companyName.trim(),
                                        address = address.trim(),
                                        taxId = taxId.trim(),
                                        baseCurrency = session.baseCurrency.code,
                                        acceptCreditCards = acceptCards,
                                        acceptBankTransfers = acceptBank,
                                        latePaymentFeesEnabled = lateFeeEnabled,
                                        lateFeePercent = lateFeePct.toDoubleOrNull() ?: 5.0,
                                        emailNotifications = emailNotif,
                                        invoiceReminders = invoiceRemind,
                                        weeklyReports = weeklyRep,
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Update Company Info") }
                    }
                }
            }
        }

        // Payment & Notification toggles
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CreditCard, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Payment Settings", style = MaterialTheme.typography.titleLarge)
                    }
                    SettingSwitchRow("Accept Credit Cards", acceptCards) { acceptCards = it }
                    SettingSwitchRow("Accept Bank Transfers", acceptBank) { acceptBank = it }
                    SettingSwitchRow("Late Payment Fees", lateFeeEnabled) { lateFeeEnabled = it }
                    if (lateFeeEnabled) {
                        OutlinedTextField(
                            value = lateFeePct,
                            onValueChange = { lateFeePct = it },
                            label = { Text("Late Fee %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notifications & Data", style = MaterialTheme.typography.titleLarge)
                    }
                    SettingSwitchRow("Email Notifications", emailNotif) { emailNotif = it }
                    SettingSwitchRow("Invoice Reminders", invoiceRemind) { invoiceRemind = it }
                    SettingSwitchRow("Weekly Reports", weeklyRep) { weeklyRep = it }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val exportPayload = "RAD Consultancy Backup\nCompany: ${org?.name}\nInvoices: ${data.invoices.size}\nClients: ${data.clients.size}\nExpenses: ${data.expenses.size}\nPayments: ${data.payments.size}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "RAD Consultancy Data Export")
                                putExtra(Intent.EXTRA_TEXT, exportPayload)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Data"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Workspace Backup")
                    }

                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_signout_button"),
                    ) {
                        Text(session.strings.signOut)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
