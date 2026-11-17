package com.accountpro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations WHERE id = :orgId LIMIT 1")
    fun observeOrganization(orgId: String): Flow<OrganizationEntity?>

    @Query("SELECT * FROM organizations WHERE id = :orgId LIMIT 1")
    suspend fun getOrganization(orgId: String): OrganizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(org: OrganizationEntity)

    @Update
    suspend fun update(org: OrganizationEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE organizationId = :orgId ORDER BY createdAt ASC")
    fun observeOrgUsers(orgId: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface ModuleAccessDao {
    @Query("SELECT * FROM user_module_access WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<UserModuleAccessEntity>>

    @Query("SELECT * FROM user_module_access WHERE organizationId = :orgId")
    fun observeForOrg(orgId: String): Flow<List<UserModuleAccessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserModuleAccessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<UserModuleAccessEntity>)
}

@Dao
interface InvitationDao {
    @Query("SELECT * FROM invitations WHERE organizationId = :orgId AND status = 'pending' ORDER BY createdAt DESC")
    fun observePending(orgId: String): Flow<List<InvitationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invitation: InvitationEntity)

    @Query("UPDATE invitations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE organizationId = :orgId ORDER BY name ASC")
    fun observeAll(orgId: String): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<ClientEntity>)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE organizationId = :orgId ORDER BY issueDate DESC, invoiceNumber DESC")
    fun observeAll(orgId: String): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE organizationId = :orgId ORDER BY date DESC, expenseCode DESC")
    fun observeAll(orgId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE organizationId = :orgId ORDER BY date DESC, paymentCode DESC")
    fun observeAll(orgId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)
}

@Dao
interface BankingDao {
    @Query("SELECT * FROM bank_accounts WHERE organizationId = :orgId ORDER BY name ASC")
    fun observeAccounts(orgId: String): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_transactions WHERE organizationId = :orgId ORDER BY date DESC, txCode DESC")
    fun observeTransactions(orgId: String): Flow<List<BankTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<BankAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: BankTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<BankTransactionEntity>)

    @Query("UPDATE bank_transactions SET reconciled = 1, matchedTo = :matchedTo WHERE id = :txId")
    suspend fun reconcileTransaction(txId: String, matchedTo: String)
}

@Dao
interface PosProductDao {
    @Query("SELECT * FROM pos_products WHERE organizationId = :orgId ORDER BY category ASC, name ASC")
    fun observeAll(orgId: String): Flow<List<PosProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<PosProductEntity>)

    @Query("UPDATE pos_products SET stock = MAX(0, stock - :qty) WHERE id = :productId")
    suspend fun decrementStock(productId: String, qty: Int)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE organizationId = :orgId ORDER BY name ASC")
    fun observeAll(orgId: String): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItemEntity>)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE organizationId = :orgId ORDER BY startDate DESC")
    fun observeAll(orgId: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE organizationId = :orgId ORDER BY createdAt DESC LIMIT 500")
    fun observeAll(orgId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)
}
