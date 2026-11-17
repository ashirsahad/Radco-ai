package com.accountpro.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.accountpro.app.R
import com.accountpro.app.core.AppLanguage
import com.accountpro.app.core.AppModule
import com.accountpro.app.core.CurrencyManager
import com.accountpro.app.core.Localization
import com.accountpro.app.ui.theme.PureWhite
import com.accountpro.app.ui.theme.SidebarDark
import com.accountpro.app.ui.theme.SidebarDarkAccent
import com.accountpro.app.ui.theme.SidebarDarkText
import com.accountpro.app.ui.theme.TuscanRed
import com.accountpro.app.ui.viewmodel.AppSessionState
import kotlinx.coroutines.launch

sealed interface AppDestination {
    val routeKey: String
    data class Module(val module: AppModule) : AppDestination {
        override val routeKey: String get() = module.key
    }
    data object Team : AppDestination { override val routeKey = "team" }
    data object AuditLog : AppDestination { override val routeKey = "audit_log" }
    data object Settings : AppDestination { override val routeKey = "settings" }
}

private data class NavEntry(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
)

fun iconForModule(module: AppModule): ImageVector = when (module) {
    AppModule.DASHBOARD -> Icons.Outlined.Dashboard
    AppModule.INVOICES -> Icons.Outlined.Description
    AppModule.CLIENTS -> Icons.Outlined.Group
    AppModule.EXPENSES -> Icons.Outlined.TrendingDown
    AppModule.POS -> Icons.Outlined.PointOfSale
    AppModule.PAYMENTS -> Icons.Outlined.CreditCard
    AppModule.BANKING -> Icons.Outlined.AccountBalance
    AppModule.INVENTORY -> Icons.Outlined.Inventory2
    AppModule.PROJECTS -> Icons.Outlined.Folder
    AppModule.REPORTS -> Icons.Outlined.BarChart
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    session: AppSessionState,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit,
) {
    val strings = session.strings
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    val navEntries = remember(session.allowedModules, session.isAdmin, strings) {
        buildList {
            session.allowedModules.forEach { mod ->
                add(NavEntry(AppDestination.Module(mod), Localization.moduleLabel(mod, strings), iconForModule(mod)))
            }
            if (session.isAdmin) {
                add(NavEntry(AppDestination.Team, strings.team, Icons.Outlined.Groups))
                add(NavEntry(AppDestination.AuditLog, strings.auditLog, Icons.Outlined.History))
            }
            add(NavEntry(AppDestination.Settings, strings.settings, Icons.Outlined.Settings))
        }
    }

    val currentTitle = navEntries.firstOrNull { it.destination == currentDestination }?.label ?: strings.appName

    val isCurrentAllowed = when (currentDestination) {
        is AppDestination.Module -> session.canAccess(currentDestination.module)
        AppDestination.Team, AppDestination.AuditLog -> session.isAdmin
        AppDestination.Settings -> true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isWide,
            drawerContent = {
                DrawerMenuSheet(
                    session = session,
                    navEntries = navEntries,
                    currentDestination = currentDestination,
                    onSelectDestination = { dest ->
                        scope.launch { drawerState.close() }
                        onNavigate(dest)
                    },
                    onSignOut = {
                        scope.launch { drawerState.close() }
                        onSignOut()
                    },
                )
            },
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        containerColor = SidebarDark,
                        contentColor = SidebarDarkText,
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Image(
                                painter = painterResource(id = R.drawable.logo_rad_mark_dark),
                                contentDescription = "RAD Consultancy",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(28.dp),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            navEntries.forEach { entry ->
                                val selected = entry.destination == currentDestination
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { onNavigate(entry.destination) },
                                    icon = { Icon(entry.icon, contentDescription = entry.label) },
                                    label = { Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                )
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = currentTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = session.organization?.name ?: strings.appSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            navigationIcon = {
                                if (!isWide) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_button"),
                                    ) {
                                        Icon(Icons.Outlined.Menu, contentDescription = "Open navigation menu")
                                    }
                                }
                            },
                            actions = {
                                // Language quick switcher
                                Box {
                                    IconButton(
                                        onClick = { showLanguageMenu = true },
                                        modifier = Modifier.testTag("topbar_language_button"),
                                    ) {
                                        Icon(Icons.Outlined.Language, contentDescription = "Switch language")
                                    }
                                    DropdownMenu(
                                        expanded = showLanguageMenu,
                                        onDismissRequest = { showLanguageMenu = false },
                                    ) {
                                        AppLanguage.entries.forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text("${lang.flag} · ${lang.displayName}") },
                                                onClick = {
                                                    onSelectLanguage(lang)
                                                    showLanguageMenu = false
                                                },
                                            )
                                        }
                                    }
                                }

                                // Currency quick switcher
                                Box {
                                    IconButton(
                                        onClick = { showCurrencyMenu = true },
                                        modifier = Modifier.testTag("topbar_currency_button"),
                                    ) {
                                        Icon(Icons.Outlined.AttachMoney, contentDescription = "Switch currency")
                                    }
                                    DropdownMenu(
                                        expanded = showCurrencyMenu,
                                        onDismissRequest = { showCurrencyMenu = false },
                                    ) {
                                        CurrencyManager.supportedCurrencies.forEach { curr ->
                                            DropdownMenuItem(
                                                text = { Text("${curr.symbol} ${curr.code} - ${curr.name}") },
                                                onClick = {
                                                    onSelectCurrency(curr.code)
                                                    showCurrencyMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    },
                    bottomBar = {
                        if (!isWide) {
                            val primaryTabs = navEntries.take(4)
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                            ) {
                                primaryTabs.forEach { entry ->
                                    val selected = entry.destination == currentDestination
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { onNavigate(entry.destination) },
                                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                                        label = { Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    )
                                }
                                if (navEntries.size > 4) {
                                    NavigationBarItem(
                                        selected = primaryTabs.none { it.destination == currentDestination },
                                        onClick = { scope.launch { drawerState.open() } },
                                        icon = { Icon(Icons.Outlined.Menu, contentDescription = "More modules") },
                                        label = { Text("More", maxLines = 1) },
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        if (!isCurrentAllowed) {
                            NoAccessView(
                                title = strings.noAccessTitle,
                                message = strings.noAccessMessage,
                            )
                        } else {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuSheet(
    session: AppSessionState,
    navEntries: List<NavEntry>,
    currentDestination: AppDestination,
    onSelectDestination: (AppDestination) -> Unit,
    onSignOut: () -> Unit,
) {
    val user = session.currentUser
    val displayName = user?.fullName?.ifBlank { user.email } ?: "Member"
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"

    ModalDrawerSheet(
        drawerContainerColor = SidebarDark,
        drawerContentColor = SidebarDarkText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_rad_mark_dark),
                    contentDescription = "RAD Consultancy",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(124.dp)
                        .height(48.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = session.organization?.name ?: session.strings.appSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SidebarDarkText.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            HorizontalDivider(color = SidebarDarkAccent)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                navEntries.forEach { entry ->
                    val selected = entry.destination == currentDestination
                    NavigationDrawerItem(
                        selected = selected,
                        onClick = { onSelectDestination(entry.destination) },
                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                        label = { Text(entry.label) },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("drawer_nav_${entry.destination.routeKey}"),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = TuscanRed,
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            unselectedContainerColor = SidebarDark,
                            unselectedIconColor = SidebarDarkText.copy(alpha = 0.78f),
                            unselectedTextColor = SidebarDarkText.copy(alpha = 0.78f),
                        ),
                    )
                }
            }

            HorizontalDivider(color = SidebarDarkAccent)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = SidebarDarkAccent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(initial, color = SidebarDarkText, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = SidebarDarkText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${user?.email ?: ""} · ${session.role?.displayName ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SidebarDarkText.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("drawer_signout_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = session.strings.signOut,
                        tint = SidebarDarkText,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoAccessView(
    title: String,
    message: String,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
