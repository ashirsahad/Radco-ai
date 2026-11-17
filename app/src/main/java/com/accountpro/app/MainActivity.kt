package com.accountpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.accountpro.app.core.AppModule
import com.accountpro.app.ui.components.AppDestination
import com.accountpro.app.ui.components.AppShell
import com.accountpro.app.ui.screens.AuditLogScreen
import com.accountpro.app.ui.screens.AuthScreen
import com.accountpro.app.ui.screens.BankingScreen
import com.accountpro.app.ui.screens.ClientsScreen
import com.accountpro.app.ui.screens.DashboardScreen
import com.accountpro.app.ui.screens.ExpensesScreen
import com.accountpro.app.ui.screens.InventoryScreen
import com.accountpro.app.ui.screens.InvoicesScreen
import com.accountpro.app.ui.screens.PaymentsScreen
import com.accountpro.app.ui.screens.PosScreen
import com.accountpro.app.ui.screens.ProjectsScreen
import com.accountpro.app.ui.screens.ReportsScreen
import com.accountpro.app.ui.screens.SettingsScreen
import com.accountpro.app.ui.screens.TeamScreen
import com.accountpro.app.ui.theme.AccountProTheme
import com.accountpro.app.ui.viewmodel.AccountProViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AccountProApplication

        setContent {
            val viewModel: AccountProViewModel = viewModel(
                factory = AccountProViewModel.Factory(app.repository)
            )
            AccountProRoot(viewModel = viewModel)
        }
    }
}

@Composable
private fun AccountProRoot(viewModel: AccountProViewModel) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val data by viewModel.workspaceData.collectAsStateWithLifecycle()
    val authBusy by viewModel.authBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var currentRouteKey by rememberSaveable { mutableStateOf("dashboard") }
    var openInvoiceFormOnInvoices by remember { mutableStateOf(false) }

    val currentDestination: AppDestination = remember(currentRouteKey) {
        when (currentRouteKey) {
            "team" -> AppDestination.Team
            "audit_log" -> AppDestination.AuditLog
            "settings" -> AppDestination.Settings
            else -> AppDestination.Module(AppModule.fromKey(currentRouteKey) ?: AppModule.DASHBOARD)
        }
    }

    // Ensure non-dashboard back navigation returns cleanly to the first allowed module
    val homeKey = session.allowedModules.firstOrNull()?.key ?: "settings"
    if (session.currentUser != null && currentRouteKey != homeKey) {
        BackHandler { currentRouteKey = homeKey }
    }

    AccountProTheme(darkTheme = session.darkTheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { outerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding),
            ) {
                when {
                    session.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    session.currentUser == null -> {
                        AuthScreen(
                            isBusy = authBusy,
                            onSignIn = viewModel::signIn,
                            onCreateCompany = viewModel::createCompany,
                            onDemoSignIn = viewModel::quickDemoSignIn,
                        )
                    }

                    else -> {
                        AppShell(
                            session = session,
                            currentDestination = currentDestination,
                            onNavigate = { dest -> currentRouteKey = dest.routeKey },
                            onSelectLanguage = viewModel::setLanguage,
                            onSelectCurrency = viewModel::setCurrency,
                            onSignOut = {
                                currentRouteKey = "dashboard"
                                viewModel.signOut()
                            },
                        ) {
                            when (currentDestination) {
                                is AppDestination.Module -> when (currentDestination.module) {
                                    AppModule.DASHBOARD -> DashboardScreen(
                                        session = session,
                                        data = data,
                                        onNavigateToModule = { mod -> currentRouteKey = mod.key },
                                        onNewInvoice = {
                                            openInvoiceFormOnInvoices = true
                                            currentRouteKey = AppModule.INVOICES.key
                                        },
                                    )
                                    AppModule.INVOICES -> InvoicesScreen(
                                        session = session,
                                        invoices = data.invoices,
                                        clients = data.clients,
                                        initialShowCreate = openInvoiceFormOnInvoices,
                                        onConsumeInitialShowCreate = { openInvoiceFormOnInvoices = false },
                                        onSaveInvoice = viewModel::saveInvoice,
                                    )
                                    AppModule.CLIENTS -> ClientsScreen(
                                        session = session,
                                        clients = data.clients,
                                        onSaveClient = viewModel::saveClient,
                                        onDeleteClient = viewModel::deleteClient,
                                    )
                                    AppModule.EXPENSES -> ExpensesScreen(
                                        session = session,
                                        expenses = data.expenses,
                                        clients = data.clients,
                                        projects = data.projects,
                                        onSaveExpense = viewModel::saveExpense,
                                    )
                                    AppModule.POS -> PosScreen(
                                        session = session,
                                        products = data.posProducts,
                                        onProcessCheckout = viewModel::processPosCheckout,
                                    )
                                    AppModule.PAYMENTS -> PaymentsScreen(
                                        session = session,
                                        payments = data.payments,
                                        onRecordPayment = viewModel::recordPayment,
                                    )
                                    AppModule.BANKING -> BankingScreen(
                                        session = session,
                                        accounts = data.bankAccounts,
                                        transactions = data.bankTransactions,
                                        invoices = data.invoices,
                                        expenses = data.expenses,
                                        onAddAccount = viewModel::addBankAccount,
                                        onAddTransaction = viewModel::addBankTransaction,
                                        onReconcile = viewModel::reconcileTransaction,
                                    )
                                    AppModule.INVENTORY -> InventoryScreen(
                                        session = session,
                                        items = data.inventoryItems,
                                        onSaveItem = viewModel::saveInventoryItem,
                                        onAdjustStock = viewModel::adjustInventoryStock,
                                        onDeleteItem = viewModel::deleteInventoryItem,
                                    )
                                    AppModule.PROJECTS -> ProjectsScreen(
                                        session = session,
                                        projects = data.projects,
                                        onSaveProject = viewModel::saveProject,
                                        onLogHours = viewModel::logProjectHours,
                                        onDeleteProject = viewModel::deleteProject,
                                    )
                                    AppModule.REPORTS -> ReportsScreen(
                                        session = session,
                                        data = data,
                                    )
                                }
                                AppDestination.Team -> TeamScreen(
                                    session = session,
                                    members = data.teamMembers,
                                    pendingInvites = data.pendingInvites,
                                    onChangeRole = viewModel::changeMemberRole,
                                    onToggleModule = viewModel::toggleMemberModule,
                                    onToggleActive = viewModel::toggleMemberActive,
                                    onInviteMember = viewModel::inviteMember,
                                    onRevokeInvite = viewModel::revokeInvitation,
                                )
                                AppDestination.AuditLog -> AuditLogScreen(
                                    session = session,
                                    logs = data.auditLogs,
                                )
                                AppDestination.Settings -> SettingsScreen(
                                    session = session,
                                    data = data,
                                    onSaveProfile = viewModel::updateProfile,
                                    onSaveOrganization = viewModel::updateOrganization,
                                    onSelectLanguage = viewModel::setLanguage,
                                    onSelectCurrency = viewModel::setCurrency,
                                    onToggleDarkTheme = viewModel::setDarkTheme,
                                    onSignOut = {
                                        currentRouteKey = "dashboard"
                                        viewModel.signOut()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
