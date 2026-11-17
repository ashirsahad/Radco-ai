package com.accountpro.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class InvoiceLineItem(
    val id: String,
    val description: String,
    val quantity: Double,
    val rate: Double,
    val taxRate: Double,
) {
    val amount: Double get() = quantity * rate
    val taxAmount: Double get() = amount * (taxRate / 100.0)
}

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseCurrency: String = "USD",
    val address: String = "123 Business Street, City, State 12345",
    val taxId: String = "123-456-789",
    val acceptCreditCards: Boolean = true,
    val acceptBankTransfers: Boolean = true,
    val latePaymentFeesEnabled: Boolean = false,
    val lateFeePercent: Double = 5.0,
    val emailNotifications: Boolean = true,
    val invoiceReminders: Boolean = true,
    val weeklyReports: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String = "+1 (555) 123-4567",
    val role: String = "admin",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "user_module_access")
data class UserModuleAccessEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val organizationId: String,
    val moduleKey: String,
    val enabled: Boolean,
)

@Entity(tableName = "invitations")
data class InvitationEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val email: String,
    val fullName: String,
    val role: String,
    val modulesCsv: String,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val totalInvoices: Int = 0,
    val totalRevenue: Double = 0.0,
    val status: String = "active",
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val invoiceNumber: String,
    val clientId: String,
    val clientName: String,
    val amount: Double,
    val subtotal: Double,
    val taxTotal: Double,
    val currency: String = "USD",
    val issueDate: String,
    val dueDate: String,
    val status: String = "draft",
    val notes: String = "",
    val terms: String = "",
    val itemsJson: String = "[]",
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val expenseCode: String,
    val description: String,
    val amount: Double,
    val currency: String = "USD",
    val date: String,
    val category: String,
    val vendor: String = "",
    val paymentMethod: String = "Credit Card",
    val status: String = "pending",
    val hasReceipt: Boolean = false,
    val receiptUri: String? = null,
    val billable: Boolean = false,
    val clientName: String = "",
    val projectName: String = "",
    val tagsCsv: String = "",
    val notes: String = "",
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val paymentCode: String,
    val invoiceId: String,
    val clientName: String,
    val amount: Double,
    val currency: String = "USD",
    val date: String,
    val method: String = "credit_card",
    val status: String = "completed",
    val reference: String = "",
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val maskedNumber: String,
    val type: String,
    val balance: Double,
)

@Entity(tableName = "bank_transactions")
data class BankTransactionEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val txCode: String,
    val accountId: String,
    val date: String,
    val description: String,
    val amount: Double,
    val reconciled: Boolean = false,
    val matchedTo: String? = null,
)

@Entity(tableName = "pos_products")
data class PosProductEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val price: Double,
    val category: String,
    val stock: Int,
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val sku: String,
    val category: String,
    val currentStock: Int,
    val reorderLevel: Int,
    val reorderQuantity: Int,
    val unitPrice: Double,
    val supplier: String,
    val location: String,
    val lastUpdated: String,
) {
    val totalValue: Double get() = currentStock * unitPrice
    val status: String
        get() = when {
            currentStock <= 0 -> "out-of-stock"
            currentStock <= reorderLevel -> "low-stock"
            else -> "in-stock"
        }
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val clientName: String,
    val description: String,
    val status: String = "active",
    val progress: Int = 0,
    val startDate: String,
    val endDate: String,
    val budget: Double,
    val spent: Double,
    val timeTrackedHours: Int,
    val estimatedHours: Int,
    val tasksTotal: Int,
    val tasksCompleted: Int,
    val teamMembersCsv: String,
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val actorId: String,
    val actorEmail: String,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val detailsText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
