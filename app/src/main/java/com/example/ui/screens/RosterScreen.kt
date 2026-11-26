package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.NotificationSettings
import com.example.model.computeNextSequentialMemberCode
import com.example.ui.ApexViewModel
import com.example.ui.PlanFilter
import com.example.ui.RosterFilter
import com.example.ui.RosterSortOrder
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MemberIdCardDialog
import com.example.ui.components.MemberOperationalCard
import com.example.ui.components.TurnstileSearchBar
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.StatusActiveAccent
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TabularMetricStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurquoiseSoft
import com.example.ui.theme.TurquoiseText

@Composable
fun RosterScreen(
    members: List<GymMember>,
    gymSettings: GymSettings?,
    notificationSettings: NotificationSettings?,
    searchQuery: String,
    memberIdOnly: Boolean,
    activeFilter: RosterFilter,
    sortOrder: RosterSortOrder,
    planFilter: PlanFilter,
    onSearchChange: (String) -> Unit,
    onToggleMemberIdOnly: (Boolean) -> Unit,
    onFilterChange: (RosterFilter) -> Unit,
    onSortChange: (RosterSortOrder) -> Unit,
    onPlanChange: (PlanFilter) -> Unit,
    onAddMember: () -> Unit,
    onCheckIn: (GymMember) -> Unit,
    onRenew: (GymMember) -> Unit,
    onToggleBlock: (GymMember) -> Unit,
    onDeleteMember: (GymMember) -> Unit,
    onMemberClick: (GymMember) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(members, searchQuery, activeFilter, planFilter, sortOrder, memberIdOnly) {
        ApexViewModel.filterMembers(
            members = members,
            query = searchQuery,
            filter = activeFilter,
            planFilter = planFilter,
            sortOrder = sortOrder,
            memberIdOnly = memberIdOnly
        )
    }
    val nextAutoId = remember(gymSettings, members) {
        computeNextSequentialMemberCode(gymSettings, members)
    }

    var idCardTarget by remember { mutableStateOf<GymMember?>(null) }
    var deleteTarget by remember { mutableStateOf<GymMember?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Filter Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TurnstileSearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                memberIdOnly = memberIdOnly,
                onToggleMemberIdOnly = onToggleMemberIdOnly,
                onSearchAction = {
                    val exact = filtered.firstOrNull {
                        it.memberCode.removePrefix("#").equals(searchQuery.trim().removePrefix("#"), ignoreCase = true)
                    }
                    if (exact != null) onMemberClick(exact)
                },
                placeholder = if (memberIdOnly) "Enter Member ID (e.g. 1727)" else "Search for Member ID"
            )

            // Filter Dropdown Row: All members, Needs Attention First, All Plans
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdownPill(
                    label = activeFilter.label,
                    isActive = activeFilter != RosterFilter.ALL,
                    options = RosterFilter.entries.map { it.label to it },
                    onSelect = onFilterChange,
                    tag = "dropdown_all_members"
                )
                FilterDropdownPill(
                    label = sortOrder.label,
                    isActive = sortOrder != RosterSortOrder.NEEDS_ATTENTION,
                    options = RosterSortOrder.entries.map { it.label to it },
                    onSelect = onSortChange,
                    tag = "dropdown_sort_order"
                )
                FilterDropdownPill(
                    label = planFilter.label,
                    isActive = planFilter != PlanFilter.ALL_PLANS,
                    options = PlanFilter.entries.map { it.label to it },
                    onSelect = onPlanChange,
                    tag = "dropdown_all_plans"
                )
            }

            // Count + NEXT AUTO ID bar matching Screenshot 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(StatusActiveAccent)
                    )
                    Text(
                        text = "Showing ${filtered.size} registered members",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NEXT AUTO ID:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "#$nextAutoId",
                            style = TabularMetricStyle,
                            color = TurquoiseText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStatePanel(
                        title = "No members match this view",
                        body = "Adjust the search or dropdown filters, or tap + Add Member to register someone new.",
                        actionLabel = "Add Member",
                        onAction = onAddMember
                    )
                }
            } else {
                items(filtered, key = { it.id }) { member ->
                    MemberOperationalCard(
                        member = member,
                        notificationSettings = notificationSettings,
                        gymSettings = gymSettings,
                        onCardClick = { onMemberClick(member) },
                        onIdCard = { idCardTarget = member },
                        onCheckIn = { onCheckIn(member) },
                        onRenew = { onRenew(member) },
                        onToggleBlock = { onToggleBlock(member) },
                        onDelete = { deleteTarget = member }
                    )
                }
            }
        }
    }

    idCardTarget?.let { member ->
        MemberIdCardDialog(
            member = member,
            gymSettings = gymSettings,
            onDismiss = { idCardTarget = null },
            onRecordAttendance = { onCheckIn(member) }
        )
    }

    deleteTarget?.let { member ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = RoundedCornerShape(12.dp),
            containerColor = SurfaceCard,
            title = { Text("Delete / Archive Member?") },
            text = {
                Text(
                    "Remove ${member.fullName} (${member.displayMemberId}) from your EKTA 47 GYM roster? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = member
                        deleteTarget = null
                        onDeleteMember(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusExpiredAccent),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteTarget = null },
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun <T> FilterDropdownPill(
    label: String,
    isActive: Boolean,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
    tag: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isActive) TurquoiseSoft else SurfaceCard,
            border = BorderStroke(1.dp, if (isActive) TurquoiseText.copy(alpha = 0.45f) else BorderSubtle),
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .testTag(tag)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) TurquoiseText else TextPrimary,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isActive) TurquoiseText else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (optionLabel, value) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}
