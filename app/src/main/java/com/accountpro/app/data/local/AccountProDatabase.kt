package com.accountpro.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [
        OrganizationEntity::class,
        UserEntity::class,
        UserModuleAccessEntity::class,
        InvitationEntity::class,
        ClientEntity::class,
        InvoiceEntity::class,
        ExpenseEntity::class,
        PaymentEntity::class,
        BankAccountEntity::class,
        BankTransactionEntity::class,
        PosProductEntity::class,
        InventoryItemEntity::class,
        ProjectEntity::class,
        AuditLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AccountProDatabase : RoomDatabase() {
    abstract fun organizationDao(): OrganizationDao
    abstract fun userDao(): UserDao
    abstract fun moduleAccessDao(): ModuleAccessDao
    abstract fun invitationDao(): InvitationDao
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao
    abstract fun bankingDao(): BankingDao
    abstract fun posProductDao(): PosProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun projectDao(): ProjectDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        const val DEMO_ORG_ID = "org-accountpro"

        @Volatile
        private var INSTANCE: AccountProDatabase? = null

        fun getInstance(context: Context): AccountProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AccountProDatabase::class.java,
                    "accountpro.db",
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    seedInitialData(database)
                                }
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        suspend fun seedInitialData(db: AccountProDatabase) {
            val orgId = DEMO_ORG_ID
            if (db.organizationDao().getOrganization(orgId) != null) return

            db.organizationDao().insert(
                OrganizationEntity(
                    id = orgId,
                    name = "RAD Consultancy LLC",
                    baseCurrency = "USD",
                    address = "Business Bay, Dubai, UAE",
                    taxId = "100-456-789",
                )
            )

            // Pre-seeded users across all 3 roles (admin, manager, accountant)
            val now = System.currentTimeMillis()
            db.userDao().insert(
                UserEntity(
                    id = "user-admin",
                    organizationId = orgId,
                    fullName = "Alex Mercer",
                    email = "admin@radconsultancy.com",
                    password = "password123",
                    phone = "+1 (555) 123-4567",
                    role = "admin",
                    isActive = true,
                    createdAt = now - 86_400_000L * 10,
                )
            )
            db.userDao().insert(
                UserEntity(
                    id = "user-manager",
                    organizationId = orgId,
                    fullName = "Jordan Vance",
                    email = "manager@radconsultancy.com",
                    password = "password123",
                    phone = "+1 (555) 234-5678",
                    role = "manager",
                    isActive = true,
                    createdAt = now - 86_400_000L * 5,
                )
            )
            db.userDao().insert(
                UserEntity(
                    id = "user-accountant",
                    organizationId = orgId,
                    fullName = "Elena Rostova",
                    email = "accountant@radconsultancy.com",
                    password = "password123",
                    phone = "+1 (555) 345-6789",
                    role = "accountant",
                    isActive = true,
                    createdAt = now - 86_400_000L * 2,
                )
            )

            // Manager module permissions
            listOf("dashboard", "invoices", "clients", "pos", "inventory", "projects").forEach { key ->
                db.moduleAccessDao().upsert(
                    UserModuleAccessEntity(
                        id = "user-manager-$key",
                        userId = "user-manager",
                        organizationId = orgId,
                        moduleKey = key,
                        enabled = true,
                    )
                )
            }

            seedSampleWorkspaceData(db, orgId)

            db.auditLogDao().insert(
                AuditLogEntity(
                    id = "audit-seed-1",
                    organizationId = orgId,
                    actorId = "user-admin",
                    actorEmail = "admin@radconsultancy.com",
                    action = "sign_in",
                    entityType = "session",
                    detailsText = "email",
                    createdAt = now - 3_600_000L * 4,
                )
            )
            db.auditLogDao().insert(
                AuditLogEntity(
                    id = "audit-seed-2",
                    organizationId = orgId,
                    actorId = "user-admin",
                    actorEmail = "admin@radconsultancy.com",
                    action = "transaction_reconciled",
                    entityType = "bank_transaction",
                    entityId = "TX-1001",
                    detailsText = "TX-1001 matched to INV-001",
                    createdAt = now - 3_600_000L * 2,
                )
            )
        }

        suspend fun seedSampleWorkspaceData(db: AccountProDatabase, orgId: String) {
            db.clientDao().insertAll(
                listOf(
                    ClientEntity(
                        id = "$orgId-cli-1",
                        organizationId = orgId,
                        name = "Tech Solutions Ltd",
                        email = "contact@techsolutions.com",
                        phone = "+1 (555) 123-4567",
                        address = "123 Tech Street, Silicon Valley, CA",
                        totalInvoices = 12,
                        totalRevenue = 45600.0,
                        status = "active",
                    ),
                    ClientEntity(
                        id = "$orgId-cli-2",
                        organizationId = orgId,
                        name = "Marketing Agency Co",
                        email = "hello@marketingagency.com",
                        phone = "+1 (555) 234-5678",
                        address = "456 Marketing Ave, New York, NY",
                        totalInvoices = 8,
                        totalRevenue = 28400.0,
                        status = "active",
                    ),
                    ClientEntity(
                        id = "$orgId-cli-3",
                        organizationId = orgId,
                        name = "Consulting Services",
                        email = "info@consulting.com",
                        phone = "+1 (555) 345-6789",
                        address = "789 Business Blvd, Chicago, IL",
                        totalInvoices = 15,
                        totalRevenue = 67200.0,
                        status = "active",
                    ),
                    ClientEntity(
                        id = "$orgId-cli-4",
                        organizationId = orgId,
                        name = "Design Studio Inc",
                        email = "contact@designstudio.com",
                        phone = "+1 (555) 456-7890",
                        address = "321 Creative Lane, Austin, TX",
                        totalInvoices = 6,
                        totalRevenue = 19800.0,
                        status = "inactive",
                    ),
                    ClientEntity(
                        id = "$orgId-cli-5",
                        organizationId = orgId,
                        name = "E-commerce Co",
                        email = "support@ecommerce.com",
                        phone = "+1 (555) 567-8901",
                        address = "654 Commerce St, Seattle, WA",
                        totalInvoices = 20,
                        totalRevenue = 89500.0,
                        status = "active",
                    ),
                )
            )

            fun itemsJson(desc: String, amount: Double): String = Json.encodeToString(
                listOf(InvoiceLineItem("1", desc, 1.0, amount, 0.0))
            )

            db.invoiceDao().insertAll(
                listOf(
                    InvoiceEntity(
                        id = "$orgId-INV-001",
                        organizationId = orgId,
                        invoiceNumber = "INV-001",
                        clientId = "$orgId-cli-1",
                        clientName = "Tech Solutions Ltd",
                        amount = 15500.0,
                        subtotal = 15500.0,
                        taxTotal = 0.0,
                        currency = "USD",
                        issueDate = "2024-12-15",
                        dueDate = "2024-12-30",
                        status = "paid",
                        itemsJson = itemsJson("Enterprise Cloud Retainer", 15500.0),
                    ),
                    InvoiceEntity(
                        id = "$orgId-INV-002",
                        organizationId = orgId,
                        invoiceNumber = "INV-002",
                        clientId = "$orgId-cli-2",
                        clientName = "Marketing Agency Co",
                        amount = 8250.0,
                        subtotal = 8250.0,
                        taxTotal = 0.0,
                        currency = "USD",
                        issueDate = "2024-12-12",
                        dueDate = "2024-12-27",
                        status = "pending",
                        itemsJson = itemsJson("Q4 Brand Strategy", 8250.0),
                    ),
                    InvoiceEntity(
                        id = "$orgId-INV-003",
                        organizationId = orgId,
                        invoiceNumber = "INV-003",
                        clientId = "$orgId-cli-3",
                        clientName = "Consulting Services",
                        amount = 12800.0,
                        subtotal = 12800.0,
                        taxTotal = 0.0,
                        currency = "USD",
                        issueDate = "2024-12-08",
                        dueDate = "2024-12-23",
                        status = "overdue",
                        itemsJson = itemsJson("Advisory & Audit", 12800.0),
                    ),
                    InvoiceEntity(
                        id = "$orgId-INV-004",
                        organizationId = orgId,
                        invoiceNumber = "INV-004",
                        clientId = "$orgId-cli-4",
                        clientName = "Design Studio Inc",
                        amount = 6750.0,
                        subtotal = 6750.0,
                        taxTotal = 0.0,
                        currency = "USD",
                        issueDate = "2024-12-05",
                        dueDate = "2024-12-20",
                        status = "draft",
                        itemsJson = itemsJson("UI System Sprint", 6750.0),
                    ),
                    InvoiceEntity(
                        id = "$orgId-INV-005",
                        organizationId = orgId,
                        invoiceNumber = "INV-005",
                        clientId = "$orgId-cli-5",
                        clientName = "E-commerce Co",
                        amount = 22300.0,
                        subtotal = 22300.0,
                        taxTotal = 0.0,
                        currency = "USD",
                        issueDate = "2024-12-01",
                        dueDate = "2024-12-16",
                        status = "paid",
                        itemsJson = itemsJson("Platform Checkout Integration", 22300.0),
                    ),
                )
            )

            db.expenseDao().insertAll(
                listOf(
                    ExpenseEntity("$orgId-EXP-001", orgId, "EXP-001", "Office Rent", 2500.0, "USD", "2024-12-01", "Rent", "Metro Properties", "Bank Transfer", "paid", true),
                    ExpenseEntity("$orgId-EXP-002", orgId, "EXP-002", "Software Licenses", 450.0, "USD", "2024-12-03", "Software & Subscriptions", "Adobe Systems", "Credit Card", "paid", true),
                    ExpenseEntity("$orgId-EXP-003", orgId, "EXP-003", "Marketing Campaign", 1200.0, "USD", "2024-12-05", "Marketing", "SocialAds Co", "Credit Card", "pending", false),
                    ExpenseEntity("$orgId-EXP-004", orgId, "EXP-004", "Travel Expenses", 350.0, "USD", "2024-12-08", "Travel", "Delta Airlines", "Credit Card", "paid", true),
                    ExpenseEntity("$orgId-EXP-005", orgId, "EXP-005", "Equipment Purchase", 850.0, "USD", "2024-12-10", "Equipment", "TechSupply Co", "Debit Card", "pending", false),
                )
            )

            db.paymentDao().insertAll(
                listOf(
                    PaymentEntity("$orgId-PAY-001", orgId, "PAY-001", "INV-001", "Tech Solutions Ltd", 15500.0, "USD", "2024-12-15", "credit_card", "completed", "ch_1234567890"),
                    PaymentEntity("$orgId-PAY-002", orgId, "PAY-002", "INV-005", "E-commerce Co", 22300.0, "USD", "2024-12-14", "bank_transfer", "completed", "TXN-9876543210"),
                    PaymentEntity("$orgId-PAY-003", orgId, "PAY-003", "INV-002", "Marketing Agency Co", 8250.0, "USD", "2024-12-12", "mobile_payment", "processing", "MP-5555444433"),
                    PaymentEntity("$orgId-PAY-004", orgId, "PAY-004", "INV-004", "Design Studio Inc", 6750.0, "USD", "2024-12-10", "credit_card", "failed", "ch_0987654321"),
                    PaymentEntity("$orgId-PAY-005", orgId, "PAY-005", "INV-003", "Consulting Services", 12800.0, "USD", "2024-12-08", "bank_transfer", "pending", "TXN-1122334455"),
                )
            )

            db.bankingDao().insertAccounts(
                listOf(
                    BankAccountEntity("$orgId-acc-1", orgId, "Business Checking", "•••• 4821", "Checking", 48250.40),
                    BankAccountEntity("$orgId-acc-2", orgId, "Business Savings", "•••• 7710", "Savings", 122000.0),
                    BankAccountEntity("$orgId-acc-3", orgId, "Company Card", "•••• 3390", "Credit Card", -3420.15),
                )
            )

            db.bankingDao().insertTransactions(
                listOf(
                    BankTransactionEntity("$orgId-TX-1001", orgId, "TX-1001", "$orgId-acc-1", "2024-12-16", "Payment — Tech Solutions Ltd", 15500.0, true, "INV-001"),
                    BankTransactionEntity("$orgId-TX-1002", orgId, "TX-1002", "$orgId-acc-1", "2024-12-14", "Transfer from Stripe payout", 8250.0, false, null),
                    BankTransactionEntity("$orgId-TX-1003", orgId, "TX-1003", "$orgId-acc-3", "2024-12-12", "Adobe Creative Cloud", -450.0, true, "EXP-002"),
                    BankTransactionEntity("$orgId-TX-1004", orgId, "TX-1004", "$orgId-acc-1", "2024-12-10", "Office rent — December", -2500.0, false, null),
                    BankTransactionEntity("$orgId-TX-1005", orgId, "TX-1005", "$orgId-acc-1", "2024-12-08", "Payment — Design Studio Inc", 6750.0, false, null),
                    BankTransactionEntity("$orgId-TX-1006", orgId, "TX-1006", "$orgId-acc-3", "2024-12-05", "Airline tickets", -350.0, false, null),
                )
            )

            db.posProductDao().insertAll(
                listOf(
                    PosProductEntity("$orgId-pos-1", orgId, "Coffee", 4.50, "Beverages", 50),
                    PosProductEntity("$orgId-pos-2", orgId, "Sandwich", 8.99, "Food", 25),
                    PosProductEntity("$orgId-pos-3", orgId, "Juice", 3.25, "Beverages", 30),
                    PosProductEntity("$orgId-pos-4", orgId, "Salad", 12.50, "Food", 15),
                    PosProductEntity("$orgId-pos-5", orgId, "Tea", 3.75, "Beverages", 40),
                    PosProductEntity("$orgId-pos-6", orgId, "Pastry", 5.99, "Food", 20),
                    PosProductEntity("$orgId-pos-7", orgId, "Smoothie", 6.25, "Beverages", 22),
                    PosProductEntity("$orgId-pos-8", orgId, "Burger", 15.99, "Food", 18),
                )
            )

            db.inventoryDao().insertAll(
                listOf(
                    InventoryItemEntity("$orgId-inv-1", orgId, "Wireless Headphones", "WH-001", "Electronics", 45, 10, 50, 79.99, "TechSupply Co.", "Warehouse A", "2024-12-15"),
                    InventoryItemEntity("$orgId-inv-2", orgId, "Office Chair", "OC-002", "Furniture", 8, 15, 25, 199.99, "Office Solutions", "Warehouse B", "2024-12-14"),
                    InventoryItemEntity("$orgId-inv-3", orgId, "Laptop Stand", "LS-003", "Accessories", 0, 5, 20, 39.99, "AccessoryWorld", "Warehouse A", "2024-12-10"),
                    InventoryItemEntity("$orgId-inv-4", orgId, "Desk Lamp", "DL-004", "Lighting", 22, 8, 30, 29.99, "Lighting Pro", "Warehouse B", "2024-12-13"),
                )
            )

            db.projectDao().insertAll(
                listOf(
                    ProjectEntity("$orgId-proj-1", orgId, "Website Redesign", "Tech Solutions Ltd", "Complete redesign of corporate website with new branding", "active", 65, "2024-10-01", "2025-01-15", 25000.0, 16250.0, 120, 200, 24, 16, "John Doe,Jane Smith,Mike Johnson"),
                    ProjectEntity("$orgId-proj-2", orgId, "Mobile App Development", "E-commerce Co", "Native Android & iOS app for customer management", "active", 40, "2024-11-01", "2025-03-30", 50000.0, 20000.0, 180, 400, 35, 14, "Sarah Wilson,Tom Brown,Lisa Chen"),
                    ProjectEntity("$orgId-proj-3", orgId, "Data Migration", "Consulting Services", "Legacy system data migration to cloud platform", "completed", 100, "2024-08-01", "2024-11-15", 15000.0, 14500.0, 95, 100, 18, 18, "David Lee,Emma Davis"),
                    ProjectEntity("$orgId-proj-4", orgId, "Marketing Campaign", "Marketing Agency Co", "Digital marketing campaign for product launch", "on-hold", 25, "2024-11-15", "2025-02-28", 8000.0, 2000.0, 30, 80, 12, 3, "Alex Rodriguez,Rachel Green"),
                )
            )
        }
    }
}
