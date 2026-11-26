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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BillingDashboardMetrics
import com.example.model.FacilityProfile
import com.example.model.GymContactDetails
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipPlan
import com.example.model.MembershipStatus
import com.example.model.NotificationSettings
import com.example.model.PaymentSettings
import com.example.model.StaffAccessRole
import com.example.model.TrainerProfile
import com.example.model.formatMoneyCents
import com.example.ui.components.EktaBrandMark
import com.example.ui.components.MembershipPlanEditorDialog
import com.example.ui.components.launchDialer
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.InputBorder
import com.example.ui.theme.PhoneBg
import com.example.ui.theme.PhoneIcon
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.StatusExpiredText
import com.example.ui.theme.StatusExpiringBg
import com.example.ui.theme.StatusExpiringText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TabularMetricStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TopBarTurquoise
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.TurquoiseSoft
import com.example.ui.theme.TurquoiseText

private enum class GymSettingsFilter(val label: String) {
    ALL("All"),
    PROFILE("Gym Profile"),
    PLANS("Plans"),
    MEMBER_IDS("Member IDs"),
    REMINDERS("Reminders"),
    PAYMENTS("Payments"),
    STAFF_ACCESS("Staff & Access")
}

@Composable
fun GymSectionScreen(
    facilityProfile: FacilityProfile?,
    gymSettings: GymSettings,
    membershipPlans: List<MembershipPlan>,
    notificationSettings: NotificationSettings,
    paymentSettings: PaymentSettings,
    gymContactDetails: GymContactDetails,
    adminEmail: String?,
    adminDisplayName: String?,
    members: List<GymMember>,
    trainers: List<TrainerProfile>,
    billingMetrics: BillingDashboardMetrics,
    onSaveGymSettings: (GymSettings, Long?, String) -> Unit,
    onUploadGymLogo: (ByteArray) -> Unit,
    onRemoveGymLogo: () -> Unit,
    onSaveGymContact: (phone: String, email: String, address: String, openingHours: String) -> Unit,
    onSaveMembershipPlan: (
        existingPlan: MembershipPlan?,
        name: String,
        durationMonths: Long,
        priceCents: Long,
        description: String,
        isActive: Boolean,
        nextSortOrder: Long
    ) -> Unit,
    onToggleMembershipPlanActive: (MembershipPlan, Boolean) -> Unit,
    onDeleteMembershipPlan: (MembershipPlan) -> Unit,
    onSaveNotificationSettings: (NotificationSettings) -> Unit,
    onSavePaymentSettings: (PaymentSettings) -> Unit,
    onUploadUpiQrCode: (ByteArray) -> Unit,
    onRemoveUpiQrCode: () -> Unit,
    onSwitchWorkspaceRole: (StaffAccessRole) -> Unit,
    onToggleTrainerFloor: (TrainerProfile, Boolean) -> Unit,
    onLogTrainerSession: (TrainerProfile) -> Unit,
    onAddTrainer: () -> Unit,
    onOpenKeypad: () -> Unit,
    onSyncRevenueSnapshot: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeRole = facilityProfile?.roleEnum ?: StaffAccessRole.ADMIN
    val isAdmin = activeRole == StaffAccessRole.ADMIN

    var selectedFilter by remember { mutableStateOf(GymSettingsFilter.ALL) }
    var editingPlan by remember { mutableStateOf<MembershipPlan?>(null) }
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var confirmRoleSwitch by remember { mutableStateOf<StaffAccessRole?>(null) }

    val mergedGymSettings = remember(gymSettings, gymContactDetails, facilityProfile, adminDisplayName) {
        gymSettings.copy(
            gymName = gymSettings.gymName.ifBlank {
                facilityProfile?.normalizedFacilityName ?: "EKTA 47 GYM & FITNESS CENTRE"
            },
            adminName = gymSettings.adminName.ifBlank {
                facilityProfile?.managerName ?: adminDisplayName ?: "Facility Administrator"
            },
            contactPhone = gymSettings.contactPhone.ifBlank { gymContactDetails.phone },
            contactEmail = gymSettings.contactEmail.ifBlank { gymContactDetails.email },
            address = gymSettings.address.ifBlank { gymContactDetails.address },
            openingHours = gymSettings.openingHours.ifBlank { gymContactDetails.openingHours }
        )
    }

    val activeMembersCount = remember(members) {
        members.count { it.statusEnum == MembershipStatus.ACTIVE }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("gym_section_screen"),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overview Card + Role Mode Banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EktaBrandMark(
                            size = 68.dp,
                            useWhiteSurface = true,
                            customLogoUrl = mergedGymSettings.logoUrl
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mergedGymSettings.gymName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.brand_established),
                                style = MaterialTheme.typography.labelSmall,
                                color = TurquoiseText,
                                fontWeight = FontWeight.Bold
                            )
                            if (!adminEmail.isNullOrBlank()) {
                                Text(
                                    text = "Account: $adminEmail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSubtle)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GymMiniStat("ACTIVE MEMBERS", "$activeMembersCount / ${members.size}")
                        GymMiniStat(
                            "MONTHLY MRR",
                            formatMoneyCents(billingMetrics.monthlyRevenueCents, mergedGymSettings.currency)
                        )
                        GymMiniStat("PLANS", "${membershipPlans.count { it.isActive }} Active")
                    }

                    // Role Control Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAdmin) StatusActiveBg.copy(alpha = 0.7f) else StatusExpiringBg,
                        border = BorderStroke(
                            1.dp,
                            if (isAdmin) TurquoisePrimary.copy(alpha = 0.25f) else StatusExpiringText.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = if (isAdmin) StatusActiveText else StatusExpiringText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isAdmin) "Administrator Mode" else "Front Desk Staff Mode",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isAdmin) StatusActiveText else StatusExpiringText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isAdmin) {
                                            "Full control of plans, pricing, logo & settings"
                                        } else {
                                            "Settings & pricing are read-only"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    confirmRoleSwitch = if (isAdmin) StaffAccessRole.STAFF else StaffAccessRole.ADMIN
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (isAdmin) "Switch to Staff" else "Admin Unlock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalBlueAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Filter Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GymSettingsFilter.entries.forEach { filter ->
                    val selected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) TopBarTurquoise else SurfaceCard,
                        border = BorderStroke(1.dp, if (selected) TopBarTurquoise else BorderSubtle),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFilter = filter }
                            .testTag("gym_filter_${filter.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Gym Profile & Logo
        if (selectedFilter == GymSettingsFilter.ALL || selectedFilter == GymSettingsFilter.PROFILE) {
            item {
                GymProfileSettingsCard(
                    gymSettings = mergedGymSettings,
                    facilityProfile = facilityProfile,
                    isAdmin = isAdmin,
                    onUploadLogo = onUploadGymLogo,
                    onRemoveLogo = onRemoveGymLogo,
                    onSaveGymProfile = { updated, target ->
                        onSaveGymSettings(updated, target, "Saved gym profile & contact details")
                    },
                    onSaveLocalContactFallback = onSaveGymContact
                )
            }
        }

        // Section 2: Membership Plans & Pricing
        if (selectedFilter == GymSettingsFilter.ALL || selectedFilter == GymSettingsFilter.PLANS) {
            item {
                MembershipPlansSectionCard(
                    plans = membershipPlans,
                    members = members,
                    currencyCode = mergedGymSettings.currency,
                    isAdmin = isAdmin,
                    onAddPlan = { showCreatePlanDialog = true },
                    onEditPlan = { editingPlan = it },
                    onTogglePlanActive = onToggleMembershipPlanActive,
                    onDeletePlan = onDeleteMembershipPlan
                )
            }
        }

        // Section 3: Member ID & Digital Card Settings
        if (selectedFilter == GymSettingsFilter.ALL || selectedFilter == GymSettingsFilter.MEMBER_IDS) {
            item {
                MemberIdSettingsCard(
                    gymSettings = mergedGymSettings,
                    isAdmin = isAdmin,
                    onSaveIdSettings = { updated ->
                        onSaveGymSettings(updated, null, "Saved Member ID & Digital Card settings")
                    }
                )
            }
        }

        // Section 4: Notifications & WhatsApp
        if (selectedFilter == GymSettingsFilter.ALL || selectedFilter == GymSettingsFilter.REMINDERS) {
            item {
                NotificationsSettingsCard(
                    notificationSettings = notificationSettings,
                    isAdmin = isAdmin,
                    onSaveNotifications = onSaveNotificationSettings
                )
            }
        }

        // Section 5: Payment Settings
        if (selectedFilter == GymSettingsFilter.ALL || selectedFilter == GymSettingsFilter.PAYMENTS) {
            item {
                PaymentSettingsCard(
                    paymentSettings = paymentSettings,
                    isAdmin = isAdmin,
                    onUploadUpiQr = onUploadUpiQrCode,
                    onRemoveUpiQr = onRemoveUpiQrCode,
                    onSavePaymentSettings = onSavePaymentSettings
                )
            }
        }

        // Section 6: General Settings, Currency, Staff & Access Tools
        if (selectedFilter == GymSettingsFilter.ALL || selectedFilter == GymSettingsFilter.STAFF_ACCESS) {
            item {
                GeneralAndCurrencyCard(
                    gymSettings = mergedGymSettings,
                    activeRole = activeRole,
                    isAdmin = isAdmin,
                    onSelectCurrency = { code ->
                        onSaveGymSettings(
                            mergedGymSettings.copy(currency = code),
                            null,
                            "Updated currency to $code"
                        )
                    },
                    onSwitchRoleClick = { role -> confirmRoleSwitch = role }
                )
            }

            item {
                StaffAndCoachesCard(
                    trainers = trainers,
                    isAdmin = isAdmin,
                    onAddTrainer = onAddTrainer,
                    onCall = { phone -> launchDialer(context, phone) },
                    onLogTrainerSession = onLogTrainerSession,
                    onToggleTrainerFloor = onToggleTrainerFloor
                )
            }

            item {
                GymToolsAndAccountCard(
                    onOpenKeypad = onOpenKeypad,
                    onSyncRevenueSnapshot = onSyncRevenueSnapshot,
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (showCreatePlanDialog || editingPlan != null) {
        MembershipPlanEditorDialog(
            existingPlan = editingPlan,
            currencyCode = mergedGymSettings.currency,
            onDismiss = {
                showCreatePlanDialog = false
                editingPlan = null
            },
            onConfirm = { name, durationMonths, priceCents, description, isActive ->
                val nextOrder = (membershipPlans.maxOfOrNull { it.sortOrder } ?: 0L) + 1L
                onSaveMembershipPlan(
                    editingPlan,
                    name,
                    durationMonths,
                    priceCents,
                    description,
                    isActive,
                    editingPlan?.sortOrder ?: nextOrder
                )
            }
        )
    }

    confirmRoleSwitch?.let { targetRole ->
        AlertDialog(
            onDismissRequest = { confirmRoleSwitch = null },
            shape = RoundedCornerShape(12.dp),
            containerColor = SurfaceCard,
            title = { Text("Switch Workspace Role?") },
            text = {
                Text(
                    if (targetRole == StaffAccessRole.STAFF) {
                        "Front Desk Staff mode keeps plans, pricing, logo, payment setup, and deletes locked while allowing check-ins, registrations, and renewals."
                    } else {
                        "Administrator mode unlocks full control over membership pricing, gym branding, payment settings, and roster deletions."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRoleSwitch = null
                        onSwitchWorkspaceRole(targetRole)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Switch to ${targetRole.label}")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRoleSwitch = null }, modifier = Modifier.height(44.dp)) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GeneralAndCurrencyCard(
    gymSettings: GymSettings,
    activeRole: StaffAccessRole,
    isAdmin: Boolean,
    onSelectCurrency: (String) -> Unit,
    onSwitchRoleClick: (StaffAccessRole) -> Unit
) {
    val currencies = listOf("INR" to "INR (₹)", "USD" to "USD ($)", "EUR" to "EUR (€)", "GBP" to "GBP (£)")
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "6. GENERAL & ACCESS ROLE SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Display Currency",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currencies.forEach { (code, label) ->
                    FilterChip(
                        selected = gymSettings.currency.equals(code, ignoreCase = true),
                        onClick = { if (isAdmin) onSelectCurrency(code) },
                        enabled = isAdmin,
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TurquoiseSoft,
                            selectedLabelColor = TurquoiseText
                        )
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle)

            Text(
                text = "Active Workspace Role",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StaffAccessRole.entries.forEach { role ->
                    FilterChip(
                        selected = activeRole == role,
                        onClick = { if (activeRole != role) onSwitchRoleClick(role) },
                        label = { Text(role.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TopBarTurquoise,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffAndCoachesCard(
    trainers: List<TrainerProfile>,
    isAdmin: Boolean,
    onAddTrainer: () -> Unit,
    onCall: (String) -> Unit,
    onLogTrainerSession: (TrainerProfile) -> Unit,
    onToggleTrainerFloor: (TrainerProfile, Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STAFF & COACHES (${trainers.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAdmin) RoyalBlueSoft else SurfaceSubtle,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isAdmin, onClick = onAddTrainer)
                        .testTag("gym_add_staff_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = if (isAdmin) RoyalBlueAccent else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Add Staff",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAdmin) RoyalBlueAccent else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (trainers.isEmpty()) {
                Text(
                    text = "No staff or coaches recorded yet. Tap Add Staff to add team members.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                trainers.forEachIndexed { idx, trainer ->
                    if (idx > 0) HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (trainer.onFloor) StatusActiveBg else SurfaceSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = trainer.initials,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (trainer.onFloor) StatusActiveText else TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = trainer.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${trainer.specialty} • ${trainer.sessionsToday} PT today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PhoneBg)
                                    .clickable { onCall(trainer.phone) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = "Call", tint = PhoneIcon, modifier = Modifier.size(16.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TurquoiseSoft,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onLogTrainerSession(trainer) }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FitnessCenter,
                                    contentDescription = "Log PT Session",
                                    tint = TurquoiseText,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(16.dp)
                                )
                            }
                            Switch(
                                checked = trainer.onFloor,
                                onCheckedChange = { onToggleTrainerFloor(trainer, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TurquoisePrimary,
                                    uncheckedTrackColor = InputBorder
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GymToolsAndAccountCard(
    onOpenKeypad: () -> Unit,
    onSyncRevenueSnapshot: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "OPERATIONAL TOOLS & ACCOUNT",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onOpenKeypad,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TurquoisePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Rounded.Dialpad, contentDescription = null, tint = TurquoiseText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Attendance Pass Keypad", color = TurquoiseText)
            }
            OutlinedButton(
                onClick = onSyncRevenueSnapshot,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Rounded.Sync, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Monthly Revenue Snapshot", color = TextPrimary)
            }
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StatusExpiredAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("gym_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = StatusExpiredText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_sign_out), color = StatusExpiredText)
            }
        }
    }
}

@Composable
private fun GymMiniStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = TabularMetricStyle,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
