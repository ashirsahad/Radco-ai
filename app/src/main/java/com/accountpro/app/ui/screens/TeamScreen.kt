package com.accountpro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accountpro.app.core.AppModule
import com.accountpro.app.core.AppRole
import com.accountpro.app.core.PermissionsHelper
import com.accountpro.app.data.local.InvitationEntity
import com.accountpro.app.data.local.UserEntity
import com.accountpro.app.ui.components.EditorialCard
import com.accountpro.app.ui.components.FilterChipsRow
import com.accountpro.app.ui.components.SectionHeader
import com.accountpro.app.ui.components.StatusBadge
import com.accountpro.app.ui.viewmodel.AppSessionState
import com.accountpro.app.ui.viewmodel.TeamMemberUi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeamScreen(
    session: AppSessionState,
    members: List<TeamMemberUi>,
    pendingInvites: List<InvitationEntity>,
    onChangeRole: (UserEntity, AppRole) -> Unit,
    onToggleModule: (UserEntity, String, Boolean) -> Unit,
    onToggleActive: (UserEntity) -> Unit,
    onInviteMember: (fullName: String, email: String, role: AppRole, modules: Set<AppModule>) -> Unit,
    onRevokeInvite: (String) -> Unit,
) {
    val activeCount = members.count { it.user.isActive }
    var showInviteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = session.strings.team,
                subtitle = "$activeCount active ${if (activeCount == 1) "person" else "people"} in your company",
                actionLabel = "Invite",
                actionIcon = Icons.Outlined.Add,
                onActionClick = { showInviteDialog = true },
                actionTestTag = "invite_member_button",
            )
        }

        items(members, key = { it.user.id }) { memberUi ->
            val member = memberUi.user
            val isSelf = member.id == session.currentUser?.id
            var roleExpanded by remember { mutableStateOf(false) }

            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (member.fullName.ifBlank { member.email }).first().uppercase(),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                                if (isSelf) StatusBadge(label = "You", statusKey = "active")
                                if (!member.isActive) StatusBadge(label = "Deactivated", statusKey = "inactive")
                            }
                            Text(
                                member.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = roleExpanded && !isSelf,
                            onExpandedChange = { if (!isSelf) roleExpanded = it },
                            modifier = Modifier.weight(1f),
                        ) {
                            OutlinedTextField(
                                value = memberUi.role.displayName,
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isSelf,
                                label = { Text("Role") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            )
                            ExposedDropdownMenu(
                                expanded = roleExpanded,
                                onDismissRequest = { roleExpanded = false },
                            ) {
                                AppRole.entries.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role.displayName) },
                                        onClick = {
                                            onChangeRole(member, role)
                                            roleExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onToggleActive(member) },
                            enabled = !isSelf,
                            modifier = Modifier.height(54.dp),
                        ) {
                            Icon(
                                imageVector = if (member.isActive) Icons.Outlined.PersonOff else Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (member.isActive) "Deactivate" else "Reactivate")
                        }
                    }

                    HorizontalDivider()

                    when (memberUi.role) {
                        AppRole.MANAGER -> {
                            Text("Sections they can open", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                AppModule.entries.forEach { mod ->
                                    val checked = mod.key in memberUi.enabledModules
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { onToggleModule(member, mod.key, it) },
                                        )
                                        Text(mod.defaultLabel, style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                            }
                        }
                        AppRole.ACCOUNTANT -> {
                            Text(
                                "Fixed access: ${PermissionsHelper.accountantModuleKeys.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppRole.ADMIN -> {
                            Text(
                                "Full access to every section, the team and company settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (pendingInvites.isNotEmpty()) {
            item {
                Text("Pending invites", style = MaterialTheme.typography.titleLarge)
            }
            items(pendingInvites, key = { it.id }) { invite ->
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.MailOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(invite.email, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${invite.fullName.ifBlank { "Invited" }} · ${invite.role}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(onClick = { onRevokeInvite(invite.id) }) {
                            Text("Revoke")
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(AppRole.MANAGER) }
        var selectedModules by remember { mutableStateOf(setOf(AppModule.DASHBOARD)) }

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite a team member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FilterChipsRow(
                        options = AppRole.entries.map { it.value to it.displayName },
                        selectedKey = role.value,
                        onSelect = { role = AppRole.fromValue(it) },
                    )
                    Text(role.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (role == AppRole.MANAGER) {
                        FlowRow {
                            AppModule.entries.forEach { mod ->
                                val checked = mod in selectedModules
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            selectedModules = if (isChecked) selectedModules + mod else selectedModules - mod
                                        },
                                    )
                                    Text(mod.defaultLabel, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onInviteMember(name, email, role, selectedModules)
                        showInviteDialog = false
                    },
                    enabled = email.isNotBlank(),
                ) { Text("Send invite") }
            },
            dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") } },
        )
    }
}
