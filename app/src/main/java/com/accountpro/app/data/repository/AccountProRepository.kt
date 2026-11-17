package com.accountpro.app.data.repository

import com.accountpro.app.core.AppModule
import com.accountpro.app.core.AppRole
import com.accountpro.app.data.local.AccountProDatabase
import com.accountpro.app.data.local.AuditLogEntity
import com.accountpro.app.data.local.BankAccountEntity
import com.accountpro.app.data.local.BankTransactionEntity
import com.accountpro.app.data.local.ClientEntity
import com.accountpro.app.data.local.ExpenseEntity
import com.accountpro.app.data.local.InventoryItemEntity
import com.accountpro.app.data.local.InvitationEntity
import com.accountpro.app.data.local.InvoiceEntity
import com.accountpro.app.data.local.OrganizationEntity
import com.accountpro.app.data.local.PaymentEntity
import com.accountpro.app.data.local.PosProductEntity
import com.accountpro.app.data.local.PreferencesRepository
import com.accountpro.app.data.local.ProjectEntity
import com.accountpro.app.data.local.UserEntity
import com.accountpro.app.data.local.UserModuleAccessEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AccountProRepository(
    private val db: AccountProDatabase,
    private val prefs: PreferencesRepository,
) {
    val loggedInUserId: Flow<String?> = prefs.loggedInUserId
    val languageCode: Flow<String> = prefs.languageCode
    val currencyCode: Flow<String> = prefs.currencyCode
    val darkTheme: Flow<Boolean> = prefs.darkTheme

    fun observeUser(userId: String): Flow<UserEntity?> = db.userDao().observeUser(userId)
    fun observeOrganization(orgId: String): Flow<OrganizationEntity?> = db.organizationDao().observeOrganization(orgId)
    fun observeUserModules(userId: String): Flow<List<UserModuleAccessEntity>> = db.moduleAccessDao().observeForUser(userId)
    fun observeOrgUsers(orgId: String): Flow<List<UserEntity>> = db.userDao().observeOrgUsers(orgId)
    fun observeOrgModuleAccess(orgId: String): Flow<List<UserModuleAccessEntity>> = db.moduleAccessDao().observeForOrg(orgId)
    fun observeInvitations(orgId: String): Flow<List<InvitationEntity>> = db.invitationDao().observePending(orgId)

    fun observeClients(orgId: String): Flow<List<ClientEntity>> = db.clientDao().observeAll(orgId)
    fun observeInvoices(orgId: String): Flow<List<InvoiceEntity>> = db.invoiceDao().observeAll(orgId)
    fun observeExpenses(orgId: String): Flow<List<ExpenseEntity>> = db.expenseDao().observeAll(orgId)
    fun observePayments(orgId: String): Flow<List<PaymentEntity>> = db.paymentDao().observeAll(orgId)
    fun observeBankAccounts(orgId: String): Flow<List<BankAccountEntity>> = db.bankingDao().observeAccounts(orgId)
    fun observeBankTransactions(orgId: String): Flow<List<BankTransactionEntity>> = db.bankingDao().observeTransactions(orgId)
    fun observePosProducts(orgId: String): Flow<List<PosProductEntity>> = db.posProductDao().observeAll(orgId)
    fun observeInventory(orgId: String): Flow<List<InventoryItemEntity>> = db.inventoryDao().observeAll(orgId)
    fun observeProjects(orgId: String): Flow<List<ProjectEntity>> = db.projectDao().observeAll(orgId)
    fun observeAuditLogs(orgId: String): Flow<List<AuditLogEntity>> = db.auditLogDao().observeAll(orgId)

    suspend fun ensureSeeded() {
        AccountProDatabase.seedInitialData(db)
    }

    suspend fun signIn(email: String, password: String): Result<UserEntity> {
        ensureSeeded()
        val user = db.userDao().getUserByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("No account found for $email"))
        if (user.password != password) {
            return Result.failure(IllegalArgumentException("Invalid password"))
        }
        if (!user.isActive) {
            return Result.failure(IllegalStateException("Account deactivated. Contact your company admin."))
        }
        prefs.setLoggedInUserId(user.id)
        logAudit(user.organizationId, user.id, user.email, "sign_in", "session", null, "email")
        return Result.success(user)
    }

    suspend fun quickDemoSignIn(userId: String): Result<UserEntity> {
        ensureSeeded()
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Demo user unavailable"))
        if (!user.isActive) {
            return Result.failure(IllegalStateException("This user is currently deactivated"))
        }
        prefs.setLoggedInUserId(user.id)
        logAudit(user.organizationId, user.id, user.email, "sign_in", "session", null, "demo_${user.role}")
        return Result.success(user)
    }

    suspend fun createCompanyAndAdmin(
        fullName: String,
        companyName: String,
        email: String,
        password: String,
    ): Result<UserEntity> {
        ensureSeeded()
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Use at least 6 characters for the password."))
        }
        if (db.userDao().getUserByEmail(email.trim()) != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }
        val orgId = "org-${UUID.randomUUID().toString().take(8)}"
        val userId = "user-${UUID.randomUUID().toString().take(8)}"
        db.organizationDao().insert(
            OrganizationEntity(id = orgId, name = companyName.trim(), baseCurrency = "USD")
        )
        val newUser = UserEntity(
            id = userId,
            organizationId = orgId,
            fullName = fullName.trim(),
            email = email.trim(),
            password = password,
            role = AppRole.ADMIN.value,
            isActive = true,
        )
        db.userDao().insert(newUser)
        // Seed starter workspace products & accounts so new company has working POS & banking categories
        AccountProDatabase.seedSampleWorkspaceData(db, orgId)
        prefs.setLoggedInUserId(userId)
        logAudit(orgId, userId, newUser.email, "sign_in", "session", null, "company_created")
        return Result.success(newUser)
    }

    suspend fun signOut() {
        prefs.setLoggedInUserId(null)
    }

    suspend fun setLanguage(code: String) = prefs.setLanguageCode(code)
    suspend fun setCurrency(code: String) = prefs.setCurrencyCode(code)
    suspend fun setDarkTheme(enabled: Boolean) = prefs.setDarkTheme(enabled)

    suspend fun saveInvoice(invoice: InvoiceEntity, isEdit: Boolean, actor: UserEntity) {
        db.invoiceDao().insert(invoice)
        logAudit(
            orgId = actor.organizationId,
            actorId = actor.id,
            actorEmail = actor.email,
            action = if (isEdit) "invoice_updated" else "invoice_created",
            entityType = "invoice",
            entityId = invoice.invoiceNumber,
            detailsText = "${invoice.invoiceNumber} · ${invoice.status} · ${invoice.currency} ${String.format(Locale.US, "%.2f", invoice.amount)}",
        )
    }

    suspend fun saveClient(client: ClientEntity) = db.clientDao().insert(client)
    suspend fun deleteClient(id: String) = db.clientDao().deleteById(id)

    suspend fun saveExpense(expense: ExpenseEntity, isEdit: Boolean, actor: UserEntity) {
        db.expenseDao().insert(expense)
        logAudit(
            orgId = actor.organizationId,
            actorId = actor.id,
            actorEmail = actor.email,
            action = if (isEdit) "expense_updated" else "expense_created",
            entityType = "expense",
            entityId = expense.expenseCode,
            detailsText = "${expense.expenseCode} · ${expense.status} · ${expense.currency} ${String.format(Locale.US, "%.2f", expense.amount)}",
        )
    }

    suspend fun recordPayment(payment: PaymentEntity) {
        db.paymentDao().insert(payment)
    }

    suspend fun processPosCheckout(
        orgId: String,
        cartQuantities: Map<PosProductEntity, Int>,
        totalAmount: Double,
        currency: String,
        paymentMethod: String,
        actor: UserEntity,
    ): String {
        cartQuantities.forEach { (product, qty) ->
            db.posProductDao().decrementStock(product.id, qty)
        }
        val code = "PAY-${(100..999).random()}"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        db.paymentDao().insert(
            PaymentEntity(
                id = "$orgId-$code-${System.currentTimeMillis()}",
                organizationId = orgId,
                paymentCode = code,
                invoiceId = "POS-SALE",
                clientName = "Walk-in Customer",
                amount = totalAmount,
                currency = currency,
                date = today,
                method = when (paymentMethod) {
                    "card" -> "credit_card"
                    "mobile" -> "mobile_payment"
                    else -> "cash"
                },
                status = "completed",
                reference = "POS-${System.currentTimeMillis().toString().takeLast(6)}",
            )
        )
        logAudit(
            orgId = orgId,
            actorId = actor.id,
            actorEmail = actor.email,
            action = "invoice_created",
            entityType = "pos_sale",
            entityId = code,
            detailsText = "POS checkout · $currency ${String.format(Locale.US, "%.2f", totalAmount)} via $paymentMethod",
        )
        return code
    }

    suspend fun addBankAccount(account: BankAccountEntity) = db.bankingDao().insertAccount(account)

    suspend fun addBankTransaction(tx: BankTransactionEntity) = db.bankingDao().insertTransaction(tx)

    suspend fun reconcileTransaction(tx: BankTransactionEntity, matchedTo: String, actor: UserEntity) {
        db.bankingDao().reconcileTransaction(tx.id, matchedTo)
        logAudit(
            orgId = actor.organizationId,
            actorId = actor.id,
            actorEmail = actor.email,
            action = "transaction_reconciled",
            entityType = "bank_transaction",
            entityId = tx.txCode,
            detailsText = "${tx.txCode} matched to $matchedTo",
        )
    }

    suspend fun saveInventoryItem(item: InventoryItemEntity) = db.inventoryDao().insert(item)
    suspend fun deleteInventoryItem(id: String) = db.inventoryDao().deleteById(id)

    suspend fun saveProject(project: ProjectEntity) = db.projectDao().insert(project)
    suspend fun deleteProject(id: String) = db.projectDao().deleteById(id)

    suspend fun changeMemberRole(member: UserEntity, newRole: AppRole, actor: UserEntity) {
        val oldRole = member.role
        db.userDao().update(member.copy(role = newRole.value))
        logAudit(
            orgId = actor.organizationId,
            actorId = actor.id,
            actorEmail = actor.email,
            action = "role_changed",
            entityType = "user",
            entityId = member.id,
            detailsText = "${member.email}: $oldRole -> ${newRole.value}",
        )
    }

    suspend fun toggleMemberModule(member: UserEntity, moduleKey: String, enabled: Boolean, actor: UserEntity) {
        db.moduleAccessDao().upsert(
            UserModuleAccessEntity(
                id = "${member.id}-$moduleKey",
                userId = member.id,
                organizationId = actor.organizationId,
                moduleKey = moduleKey,
                enabled = enabled,
            )
        )
        logAudit(
            orgId = actor.organizationId,
            actorId = actor.id,
            actorEmail = actor.email,
            action = "permission_updated",
            entityType = "user",
            entityId = member.id,
            detailsText = "${member.email}: $moduleKey ${if (enabled) "enabled" else "disabled"}",
        )
    }

    suspend fun toggleMemberActive(member: UserEntity, actor: UserEntity) {
        val nextActive = !member.isActive
        db.userDao().update(member.copy(isActive = nextActive))
        logAudit(
            orgId = actor.organizationId,
            actorId = actor.id,
            actorEmail = actor.email,
            action = "member_status_changed",
            entityType = "user",
            entityId = member.id,
            detailsText = "${member.email} ${if (nextActive) "reactivated" else "deactivated"}",
        )
    }

    suspend fun inviteMember(
        fullName: String,
        email: String,
        role: AppRole,
        modules: Set<AppModule>,
        actor: UserEntity,
    ) {
        val orgId = actor.organizationId
        db.invitationDao().insert(
            InvitationEntity(
                id = "inv-${UUID.randomUUID().toString().take(8)}",
                organizationId = orgId,
                email = email.trim(),
                fullName = fullName.trim(),
                role = role.value,
                modulesCsv = modules.joinToString(",") { it.key },
                status = "pending",
            )
        )
        // Also provision a local member record so admins can test/switch permissions immediately if desired
        if (db.userDao().getUserByEmail(email.trim()) == null) {
            val newUserId = "user-${UUID.randomUUID().toString().take(8)}"
            db.userDao().insert(
                UserEntity(
                    id = newUserId,
                    organizationId = orgId,
                    fullName = fullName.trim().ifBlank { email.substringBefore("@") },
                    email = email.trim(),
                    password = "password123",
                    role = role.value,
                    isActive = true,
                )
            )
            if (role == AppRole.MANAGER) {
                db.moduleAccessDao().upsertAll(
                    modules.map { mod ->
                        UserModuleAccessEntity(
                            id = "$newUserId-${mod.key}",
                            userId = newUserId,
                            organizationId = orgId,
                            moduleKey = mod.key,
                            enabled = true,
                        )
                    }
                )
            }
        }
    }

    suspend fun revokeInvitation(id: String) {
        db.invitationDao().updateStatus(id, "revoked")
    }

    suspend fun updateUserProfile(user: UserEntity) = db.userDao().update(user)
    suspend fun updateOrganization(org: OrganizationEntity) {
        db.organizationDao().update(org)
        prefs.setCurrencyCode(org.baseCurrency)
    }

    private suspend fun logAudit(
        orgId: String,
        actorId: String,
        actorEmail: String,
        action: String,
        entityType: String,
        entityId: String?,
        detailsText: String,
    ) {
        db.auditLogDao().insert(
            AuditLogEntity(
                id = "audit-${UUID.randomUUID().toString().take(8)}",
                organizationId = orgId,
                actorId = actorId,
                actorEmail = actorEmail,
                action = action,
                entityType = entityType,
                entityId = entityId,
                detailsText = detailsText,
            )
        )
    }
}
