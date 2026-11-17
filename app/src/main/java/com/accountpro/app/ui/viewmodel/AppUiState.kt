package com.accountpro.app.ui.viewmodel

import com.accountpro.app.core.AppLanguage
import com.accountpro.app.core.AppModule
import com.accountpro.app.core.AppRole
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.core.Localization
import com.accountpro.app.core.LocalizedStrings
import com.accountpro.app.core.PermissionsHelper
import com.accountpro.app.core.SupportedCurrency
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
import com.accountpro.app.data.local.ProjectEntity
import com.accountpro.app.data.local.UserEntity

data class TeamMemberUi(
    val user: UserEntity,
    val role: AppRole,
    val enabledModules: Set<String>,
)

data class AppSessionState(
    val isLoading: Boolean = true,
    val currentUser: UserEntity? = null,
    val organization: OrganizationEntity? = null,
    val role: AppRole? = null,
    val enabledManagerModules: Set<String> = emptySet(),
    val language: AppLanguage = AppLanguage.EN,
    val baseCurrency: SupportedCurrency = CurrencyManager.supportedCurrencies.first(),
    val darkTheme: Boolean = false,
) {
    val strings: LocalizedStrings get() = Localization.stringsFor(language)
    val allowedModules: List<AppModule> get() = PermissionsHelper.allowedModules(role, enabledManagerModules)
    val isAdmin: Boolean get() = role == AppRole.ADMIN
    fun canAccess(module: AppModule): Boolean = PermissionsHelper.canAccess(role, enabledManagerModules, module)
}

data class WorkspaceDataState(
    val clients: List<ClientEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val bankTransactions: List<BankTransactionEntity> = emptyList(),
    val posProducts: List<PosProductEntity> = emptyList(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val teamMembers: List<TeamMemberUi> = emptyList(),
    val pendingInvites: List<InvitationEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
)
