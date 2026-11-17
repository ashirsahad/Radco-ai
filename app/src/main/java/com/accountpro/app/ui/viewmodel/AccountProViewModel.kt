package com.accountpro.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.accountpro.app.core.AppLanguage
import com.accountpro.app.core.AppModule
import com.accountpro.app.core.AppRole
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.data.local.BankAccountEntity
import com.accountpro.app.data.local.BankTransactionEntity
import com.accountpro.app.data.local.ClientEntity
import com.accountpro.app.data.local.ExpenseEntity
import com.accountpro.app.data.local.InventoryItemEntity
import com.accountpro.app.data.local.InvoiceEntity
import com.accountpro.app.data.local.OrganizationEntity
import com.accountpro.app.data.local.PaymentEntity
import com.accountpro.app.data.local.PosProductEntity
import com.accountpro.app.data.local.ProjectEntity
import com.accountpro.app.data.local.UserEntity
import com.accountpro.app.data.repository.AccountProRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AccountProViewModel(
    private val repository: AccountProRepository,
) : ViewModel() {

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    private val currentUserFlow: Flow<UserEntity?> = repository.loggedInUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(null) else repository.observeUser(userId)
    }

    val sessionState: StateFlow<AppSessionState> = combine(
        repository.loggedInUserId,
        currentUserFlow.flatMapLatest { user ->
            if (user == null) flowOf(Triple<UserEntity?, OrganizationEntity?, Set<String>>(null, null, emptySet()))
            else combine(
                repository.observeOrganization(user.organizationId),
                repository.observeUserModules(user.id),
            ) { org, modules ->
                Triple(user, org, modules.filter { it.enabled }.map { it.moduleKey }.toSet())
            }
        },
        repository.languageCode,
        repository.currencyCode,
        repository.darkTheme,
    ) { userId, triple, langCode, currCode, dark ->
        val (user, org, moduleKeys) = triple
        AppSessionState(
            isLoading = userId != null && user == null,
            currentUser = user,
            organization = org,
            role = user?.let { AppRole.fromValue(it.role) },
            enabledManagerModules = moduleKeys,
            language = AppLanguage.fromCode(langCode),
            baseCurrency = CurrencyManager.byCode(currCode),
            darkTheme = dark,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSessionState(isLoading = true),
    )

    val workspaceData: StateFlow<WorkspaceDataState> = currentUserFlow
        .map { it?.organizationId }
        .flatMapLatest { orgId ->
            if (orgId == null) {
                flowOf(WorkspaceDataState())
            } else {
                val coreFlow = combine(
                    repository.observeClients(orgId),
                    repository.observeInvoices(orgId),
                    repository.observeExpenses(orgId),
                    repository.observePayments(orgId),
                    repository.observePosProducts(orgId),
                ) { clients, invoices, expenses, payments, pos ->
                    CoreData(clients, invoices, expenses, payments, pos)
                }

                val opsFlow = combine(
                    repository.observeBankAccounts(orgId),
                    repository.observeBankTransactions(orgId),
                    repository.observeInventory(orgId),
                    repository.observeProjects(orgId),
                ) { accounts, txs, inventory, projects ->
                    OpsData(accounts, txs, inventory, projects)
                }

                val adminFlow = combine(
                    repository.observeOrgUsers(orgId),
                    repository.observeOrgModuleAccess(orgId),
                    repository.observeInvitations(orgId),
                    repository.observeAuditLogs(orgId),
                ) { users, access, invites, audits ->
                    val members = users.map { u ->
                        TeamMemberUi(
                            user = u,
                            role = AppRole.fromValue(u.role),
                            enabledModules = access.filter { it.userId == u.id && it.enabled }
                                .map { it.moduleKey }
                                .toSet(),
                        )
                    }
                    AdminData(members, invites, audits)
                }

                combine(coreFlow, opsFlow, adminFlow) { core, ops, admin ->
                    WorkspaceDataState(
                        clients = core.clients,
                        invoices = core.invoices,
                        expenses = core.expenses,
                        payments = core.payments,
                        posProducts = core.pos,
                        bankAccounts = ops.accounts,
                        bankTransactions = ops.txs,
                        inventoryItems = ops.inventory,
                        projects = ops.projects,
                        teamMembers = admin.members,
                        pendingInvites = admin.invites,
                        auditLogs = admin.audits,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WorkspaceDataState(),
        )

    private data class CoreData(
        val clients: List<ClientEntity>,
        val invoices: List<InvoiceEntity>,
        val expenses: List<ExpenseEntity>,
        val payments: List<PaymentEntity>,
        val pos: List<PosProductEntity>,
    )

    private data class OpsData(
        val accounts: List<BankAccountEntity>,
        val txs: List<BankTransactionEntity>,
        val inventory: List<InventoryItemEntity>,
        val projects: List<ProjectEntity>,
    )

    private data class AdminData(
        val members: List<TeamMemberUi>,
        val invites: List<com.accountpro.app.data.local.InvitationEntity>,
        val audits: List<com.accountpro.app.data.local.AuditLogEntity>,
    )

    fun formatMoney(amount: Double, currencyCode: String? = null): String {
        val state = sessionState.value
        val code = currencyCode ?: state.baseCurrency.code
        return CurrencyManager.format(amount, code, state.language.code)
    }

    fun formatConvertedMoney(amount: Double, sourceCurrency: String = "USD"): String {
        val state = sessionState.value
        return CurrencyManager.format(
            amount = amount,
            currencyCode = sourceCurrency,
            languageCode = state.language.code,
            convertFromUsd = true,
            targetBaseCode = state.baseCurrency.code,
        )
    }

    fun emitMessage(message: String) {
        _toastEvents.tryEmit(message)
    }

    // Auth
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            repository.signIn(email, password)
                .onFailure { emitMessage(it.message ?: "Could not sign in") }
            _authBusy.value = false
        }
    }

    fun quickDemoSignIn(userId: String) {
        viewModelScope.launch {
            _authBusy.value = true
            repository.quickDemoSignIn(userId)
                .onFailure { emitMessage(it.message ?: "Could not switch account") }
            _authBusy.value = false
        }
    }

    fun createCompany(fullName: String, companyName: String, email: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            repository.createCompanyAndAdmin(fullName, companyName, email, password)
                .onSuccess { emitMessage("Company workspace created") }
                .onFailure { emitMessage(it.message ?: "Could not create company") }
            _authBusy.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language.code) }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { repository.setCurrency(code) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkTheme(enabled) }
    }

    // Invoices
    fun saveInvoice(invoice: InvoiceEntity, isEdit: Boolean) {
        val user = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.saveInvoice(invoice, isEdit, user)
            emitMessage(if (isEdit) "Invoice updated: ${invoice.invoiceNumber}" else "Invoice created: ${invoice.invoiceNumber}")
        }
    }

    // Clients
    fun saveClient(client: ClientEntity, isEdit: Boolean) {
        viewModelScope.launch {
            repository.saveClient(client)
            emitMessage(if (isEdit) "Client updated" else "Client added: ${client.name}")
        }
    }

    fun deleteClient(id: String) {
        viewModelScope.launch {
            repository.deleteClient(id)
            emitMessage("Client removed")
        }
    }

    // Expenses
    fun saveExpense(expense: ExpenseEntity, isEdit: Boolean) {
        val user = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.saveExpense(expense, isEdit, user)
            emitMessage(if (isEdit) "Expense updated" else "Expense added: ${expense.description}")
        }
    }

    // POS
    fun processPosCheckout(cart: Map<PosProductEntity, Int>, totalAmount: Double, paymentMethod: String) {
        val user = sessionState.value.currentUser ?: return
        val currency = sessionState.value.baseCurrency.code
        viewModelScope.launch {
            val code = repository.processPosCheckout(
                orgId = user.organizationId,
                cartQuantities = cart,
                totalAmount = totalAmount,
                currency = currency,
                paymentMethod = paymentMethod,
                actor = user,
            )
            emitMessage("Payment $code processed (${formatMoney(totalAmount, currency)})")
        }
    }

    // Payments
    fun recordPayment(invoiceId: String, clientName: String, amount: Double, method: String, status: String) {
        val user = sessionState.value.currentUser ?: return
        val currency = sessionState.value.baseCurrency.code
        val code = "PAY-${(workspaceData.value.payments.size + 1).toString().padStart(3, '0')}"
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        viewModelScope.launch {
            repository.recordPayment(
                PaymentEntity(
                    id = "${user.organizationId}-$code-${UUID.randomUUID().toString().take(4)}",
                    organizationId = user.organizationId,
                    paymentCode = code,
                    invoiceId = invoiceId,
                    clientName = clientName,
                    amount = amount,
                    currency = currency,
                    date = today,
                    method = method,
                    status = status,
                    reference = "REF-${System.currentTimeMillis().toString().takeLast(6)}",
                )
            )
            emitMessage("Payment $code recorded")
        }
    }

    // Banking
    fun addBankAccount(name: String, last4: String, type: String, balance: Double) {
        val user = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.addBankAccount(
                BankAccountEntity(
                    id = "${user.organizationId}-acc-${UUID.randomUUID().toString().take(6)}",
                    organizationId = user.organizationId,
                    name = name,
                    maskedNumber = "•••• ${last4.ifBlank { "0000" }}",
                    type = type,
                    balance = balance,
                )
            )
            emitMessage("Account added: $name")
        }
    }

    fun addBankTransaction(accountId: String, date: String, description: String, amount: Double) {
        val user = sessionState.value.currentUser ?: return
        val code = "TX-${1000 + workspaceData.value.bankTransactions.size + 1}"
        viewModelScope.launch {
            repository.addBankTransaction(
                BankTransactionEntity(
                    id = "${user.organizationId}-$code-${UUID.randomUUID().toString().take(4)}",
                    organizationId = user.organizationId,
                    txCode = code,
                    accountId = accountId,
                    date = date,
                    description = description,
                    amount = amount,
                    reconciled = false,
                )
            )
            emitMessage("Transaction $code added")
        }
    }

    fun reconcileTransaction(tx: BankTransactionEntity, matchedTo: String) {
        val user = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.reconcileTransaction(tx, matchedTo, user)
            emitMessage("${tx.txCode} reconciled with $matchedTo")
        }
    }

    // Inventory
    fun saveInventoryItem(item: InventoryItemEntity, isEdit: Boolean) {
        viewModelScope.launch {
            repository.saveInventoryItem(item)
            emitMessage(if (isEdit) "Item updated" else "Item added: ${item.name}")
        }
    }

    fun adjustInventoryStock(item: InventoryItemEntity, delta: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        viewModelScope.launch {
            repository.saveInventoryItem(
                item.copy(
                    currentStock = (item.currentStock + delta).coerceAtLeast(0),
                    lastUpdated = today,
                )
            )
        }
    }

    fun deleteInventoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteInventoryItem(id)
            emitMessage("Item deleted")
        }
    }

    // Projects
    fun saveProject(project: ProjectEntity, isEdit: Boolean) {
        viewModelScope.launch {
            repository.saveProject(project)
            emitMessage(if (isEdit) "Project updated" else "Project created: ${project.name}")
        }
    }

    fun logProjectHours(project: ProjectEntity, additionalHours: Int) {
        viewModelScope.launch {
            repository.saveProject(
                project.copy(timeTrackedHours = (project.timeTrackedHours + additionalHours).coerceAtLeast(0))
            )
            emitMessage("Logged +${additionalHours}h to ${project.name}")
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            emitMessage("Project archived")
        }
    }

    // Team & Settings
    fun changeMemberRole(member: UserEntity, newRole: AppRole) {
        val actor = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.changeMemberRole(member, newRole, actor)
            emitMessage("${member.fullName} role updated to ${newRole.displayName}")
        }
    }

    fun toggleMemberModule(member: UserEntity, moduleKey: String, enabled: Boolean) {
        val actor = sessionState.value.currentUser ?: return
        viewModelScope.launch { repository.toggleMemberModule(member, moduleKey, enabled, actor) }
    }

    fun toggleMemberActive(member: UserEntity) {
        val actor = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.toggleMemberActive(member, actor)
            emitMessage(if (member.isActive) "Member deactivated" else "Member reactivated")
        }
    }

    fun inviteMember(fullName: String, email: String, role: AppRole, modules: Set<AppModule>) {
        val actor = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.inviteMember(fullName, email, role, modules, actor)
            emitMessage("Invite sent to $email")
        }
    }

    fun revokeInvitation(id: String) {
        viewModelScope.launch {
            repository.revokeInvitation(id)
            emitMessage("Invite revoked")
        }
    }

    fun updateProfile(fullName: String, email: String, phone: String) {
        val user = sessionState.value.currentUser ?: return
        viewModelScope.launch {
            repository.updateUserProfile(user.copy(fullName = fullName, email = email, phone = phone))
            emitMessage("Profile saved")
        }
    }

    fun updateOrganization(org: OrganizationEntity) {
        viewModelScope.launch {
            repository.updateOrganization(org)
            emitMessage("Company settings updated")
        }
    }

    class Factory(private val repository: AccountProRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountProViewModel(repository) as T
        }
    }
}
