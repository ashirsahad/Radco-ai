package com.accountpro.app.core

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    EN("en", "English", "EN"),
    ES("es", "Español", "ES"),
    FR("fr", "Français", "FR");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

data class LocalizedStrings(
    val appName: String,
    val appSubtitle: String,
    val dashboard: String,
    val invoices: String,
    val clients: String,
    val expenses: String,
    val pos: String,
    val payments: String,
    val banking: String,
    val inventory: String,
    val projects: String,
    val reports: String,
    val team: String,
    val auditLog: String,
    val settings: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val add: String,
    val create: String,
    val search: String,
    val filter: String,
    val refresh: String,
    val signOut: String,
    val active: String,
    val inactive: String,
    val pending: String,
    val completed: String,
    val cancelled: String,
    val draft: String,
    val paid: String,
    val overdue: String,
    val welcomeBack: String,
    val noAccessTitle: String,
    val noAccessMessage: String,
)

object Localization {
    fun stringsFor(language: AppLanguage): LocalizedStrings = when (language) {
        AppLanguage.EN -> LocalizedStrings(
            appName = "RAD Consultancy",
            appSubtitle = "Corporate Services | Tech | Advertising",
            dashboard = "Dashboard",
            invoices = "Invoices",
            clients = "Clients",
            expenses = "Expenses",
            pos = "POS",
            payments = "Payments",
            banking = "Banking",
            inventory = "Inventory",
            projects = "Projects",
            reports = "Reports",
            team = "Team",
            auditLog = "Audit log",
            settings = "Settings",
            save = "Save",
            cancel = "Cancel",
            delete = "Delete",
            edit = "Edit",
            add = "Add",
            create = "Create",
            search = "Search",
            filter = "Filter",
            refresh = "Refresh",
            signOut = "Sign out",
            active = "Active",
            inactive = "Inactive",
            pending = "Pending",
            completed = "Completed",
            cancelled = "Cancelled",
            draft = "Draft",
            paid = "Paid",
            overdue = "Overdue",
            welcomeBack = "Welcome back! Here's your business overview.",
            noAccessTitle = "No access",
            noAccessMessage = "You don't have permission to open this section. Ask an admin of your company to grant access.",
        )

        AppLanguage.ES -> LocalizedStrings(
            appName = "RAD Consultancy",
            appSubtitle = "Servicios Corporativos | Tech | Publicidad",
            dashboard = "Panel",
            invoices = "Facturas",
            clients = "Clientes",
            expenses = "Gastos",
            pos = "TPV",
            payments = "Pagos",
            banking = "Banca",
            inventory = "Inventario",
            projects = "Proyectos",
            reports = "Informes",
            team = "Equipo",
            auditLog = "Auditoría",
            settings = "Configuración",
            save = "Guardar",
            cancel = "Cancelar",
            delete = "Eliminar",
            edit = "Editar",
            add = "Añadir",
            create = "Crear",
            search = "Buscar",
            filter = "Filtrar",
            refresh = "Actualizar",
            signOut = "Cerrar sesión",
            active = "Activo",
            inactive = "Inactivo",
            pending = "Pendiente",
            completed = "Completado",
            cancelled = "Cancelado",
            draft = "Borrador",
            paid = "Pagado",
            overdue = "Vencido",
            welcomeBack = "¡Bienvenido de nuevo! Aquí está el resumen de tu negocio.",
            noAccessTitle = "Sin acceso",
            noAccessMessage = "No tienes permiso para abrir esta sección. Pide acceso a un administrador.",
        )

        AppLanguage.FR -> LocalizedStrings(
            appName = "RAD Consultancy",
            appSubtitle = "Services aux Entreprises | Tech | Publicité",
            dashboard = "Tableau de bord",
            invoices = "Factures",
            clients = "Clients",
            expenses = "Dépenses",
            pos = "TPV",
            payments = "Paiements",
            banking = "Banque",
            inventory = "Inventaire",
            projects = "Projets",
            reports = "Rapports",
            team = "Équipe",
            auditLog = "Journal d'audit",
            settings = "Paramètres",
            save = "Enregistrer",
            cancel = "Annuler",
            delete = "Supprimer",
            edit = "Modifier",
            add = "Ajouter",
            create = "Créer",
            search = "Rechercher",
            filter = "Filtrer",
            refresh = "Actualiser",
            signOut = "Déconnexion",
            active = "Actif",
            inactive = "Inactif",
            pending = "En attente",
            completed = "Terminé",
            cancelled = "Annulé",
            draft = "Brouillon",
            paid = "Payé",
            overdue = "En retard",
            welcomeBack = "Bon retour ! Voici l'aperçu de votre entreprise.",
            noAccessTitle = "Accès refusé",
            noAccessMessage = "Vous n'avez pas l'autorisation d'ouvrir cette section. Demandez l'accès à un administrateur.",
        )
    }

    fun moduleLabel(module: AppModule, strings: LocalizedStrings): String = when (module) {
        AppModule.DASHBOARD -> strings.dashboard
        AppModule.INVOICES -> strings.invoices
        AppModule.CLIENTS -> strings.clients
        AppModule.EXPENSES -> strings.expenses
        AppModule.POS -> strings.pos
        AppModule.PAYMENTS -> strings.payments
        AppModule.BANKING -> strings.banking
        AppModule.INVENTORY -> strings.inventory
        AppModule.PROJECTS -> strings.projects
        AppModule.REPORTS -> strings.reports
    }

    fun statusLabel(status: String, strings: LocalizedStrings): String = when (status.lowercase()) {
        "active" -> strings.active
        "inactive" -> strings.inactive
        "pending" -> strings.pending
        "completed" -> strings.completed
        "cancelled" -> strings.cancelled
        "draft" -> strings.draft
        "paid" -> strings.paid
        "overdue" -> strings.overdue
        else -> status.replace("-", " ").replaceFirstChar { it.uppercase() }
    }
}
