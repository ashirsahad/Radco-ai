package com.accountpro.app.core

enum class AppRole(val value: String, val displayName: String, val hint: String) {
    ADMIN("admin", "Admin", "Full access, including team and company settings"),
    MANAGER("manager", "Manager", "Only the sections you tick below"),
    ACCOUNTANT("accountant", "Accountant", "Dashboard, Invoices, Expenses, Payments, Banking, Reports");

    companion object {
        fun fromValue(value: String): AppRole =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: MANAGER
    }
}

enum class AppModule(val key: String, val defaultLabel: String) {
    DASHBOARD("dashboard", "Dashboard"),
    INVOICES("invoices", "Invoices"),
    CLIENTS("clients", "Clients"),
    EXPENSES("expenses", "Expenses"),
    POS("pos", "POS"),
    PAYMENTS("payments", "Payments"),
    BANKING("banking", "Banking"),
    INVENTORY("inventory", "Inventory"),
    PROJECTS("projects", "Projects"),
    REPORTS("reports", "Reports");

    companion object {
        fun fromKey(key: String): AppModule? = entries.firstOrNull { it.key == key }
    }
}

object PermissionsHelper {
    val accountantModuleKeys: Set<String> = setOf(
        AppModule.DASHBOARD.key,
        AppModule.INVOICES.key,
        AppModule.EXPENSES.key,
        AppModule.PAYMENTS.key,
        AppModule.BANKING.key,
        AppModule.REPORTS.key,
    )

    fun canAccess(role: AppRole?, enabledManagerModules: Set<String>, module: AppModule): Boolean {
        if (role == null) return false
        return when (role) {
            AppRole.ADMIN -> true
            AppRole.ACCOUNTANT -> module.key in accountantModuleKeys
            AppRole.MANAGER -> module.key in enabledManagerModules
        }
    }

    fun allowedModules(role: AppRole?, enabledManagerModules: Set<String>): List<AppModule> =
        AppModule.entries.filter { canAccess(role, enabledManagerModules, it) }
}
