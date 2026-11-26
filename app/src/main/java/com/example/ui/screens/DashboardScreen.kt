package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BillingDashboardMetrics
import com.example.model.CheckInLog
import com.example.model.CheckInOutcome
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipStatus
import com.example.model.NotificationSettings
import com.example.model.UiState
import com.example.model.formatWhatsAppReminderMessage
import com.example.ui.RosterFilter
import com.example.ui.components.CheckInFeedRow
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.RevenueBillingDashboardCard
import com.example.ui.components.formatCurrencyCents
import com.example.ui.components.launchWhatsApp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.StatusActiveAccent
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.StatusExpiredBg
import com.example.ui.theme.StatusExpiredText
import com.example.ui.theme.StatusExpiringAccent
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
import com.example.ui.theme.WhatsAppBg
import com.example.ui.theme.WhatsAppIcon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardSubTab(val label: String) {
    MEMBERSHIP("Membership"),
    PAYMENTS("Payments"),
    ATTENDANCE("Attendance")
}

@Composable
fun DashboardScreen(
    members: List<GymMember>,
    checkIns: List<CheckInLog>,
    billingState: UiState<BillingDashboardMetrics>,
    gymSettings: GymSettings? = null,
    notificationSettings: NotificationSettings? = null,
    selectedGate: String,
    onSelectGate: (String) -> Unit,
    onOpenFilteredMembers: (RosterFilter) -> Unit,
    onOpenMemberDetail: (GymMember) -> Unit,
    onRenewMember: (GymMember) -> Unit,
    onSyncCurrentMonth: (BillingDashboardMetrics) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(DashboardSubTab.MEMBERSHIP) }

    val formattedToday = remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    }

    // Live Firestore statistics (zero hardcoded values)
    val todayCalendar = remember { Calendar.getInstance() }
    val todayCheckIns = remember(checkIns) {
        checkIns.filter { log ->
            val date = log.checkedInAt?.toDate() ?: return@filter false
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
        }
    }
    val monthlyCheckIns = remember(checkIns) {
        checkIns.filter { log ->
            val date = log.checkedInAt?.toDate() ?: return@filter false
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH)
        }
    }
    val uniqueMonthlyMembers = remember(monthlyCheckIns) {
        monthlyCheckIns.map { it.memberId }.distinct().size
    }

    val expiresTodayMembers = remember(members) {
        members.filter { it.daysRemaining == 0L }
    }
    val ptExpiringTodayMembers = remember(members) {
        members.filter { !it.trainerName.isNullOrBlank() && it.daysRemaining in 0L..1L }
    }
    val expiring1to3 = remember(members) {
        members.filter { it.daysRemaining in 1L..3L }
    }
    val expiring4to7 = remember(members) {
        members.filter { it.daysRemaining in 4L..7L }
    }
    val expiring8to15 = remember(members) {
        members.filter { it.daysRemaining in 8L..15L }
    }
    val expiredMembers = remember(members) {
        members.filter { it.statusEnum == MembershipStatus.EXPIRED || it.daysRemaining < 0L }
    }
    val overdueTotalCents = remember(expiredMembers) {
        expiredMembers.sumOf { it.monthlyFeeCents }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tabs (Membership, Payments, Attendance) + Dynamic Date Chip
        Surface(
            color = SurfaceCard,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    DashboardSubTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) TopBarTurquoise else SurfaceSubtle,
                            border = BorderStroke(
                                1.dp,
                                if (selected) TopBarTurquoise else BorderSubtle
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeTab = tab }
                                .testTag("dashboard_tab_${tab.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TurquoiseSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = TurquoiseText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formattedToday,
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        when (activeTab) {
            DashboardSubTab.MEMBERSHIP -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 1: TODAY
                    item {
                        SectionHeading(title = "TODAY", trailing = formattedToday)
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Attendance",
                                    value = "${todayCheckIns.size}",
                                    subtitle = "${todayCheckIns.count { it.outcomeEnum != CheckInOutcome.DENIED }} granted today",
                                    icon = Icons.Rounded.HowToReg,
                                    accentColor = TurquoisePrimary,
                                    onClick = { activeTab = DashboardSubTab.ATTENDANCE },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dash_today_attendance")
                                )
                                KpiMetricCard(
                                    title = "Birthdays",
                                    value = "0",
                                    subtitle = "No birthdays recorded",
                                    icon = Icons.Rounded.Cake,
                                    accentColor = RoyalBlueAccent,
                                    onClick = { onOpenFilteredMembers(RosterFilter.ALL) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dash_today_birthdays")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Expires Today",
                                    value = "${expiresTodayMembers.size}",
                                    subtitle = "Due for renewal today",
                                    icon = Icons.Rounded.NotificationsActive,
                                    accentColor = StatusExpiringAccent,
                                    onClick = { onOpenFilteredMembers(RosterFilter.EXPIRING) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dash_expires_today")
                                )
                                KpiMetricCard(
                                    title = "PT Plan Expiring Today",
                                    value = "${ptExpiringTodayMembers.size}",
                                    subtitle = "Trainer-assigned renewals",
                                    icon = Icons.Rounded.FitnessCenter,
                                    accentColor = StatusExpiredAccent,
                                    onClick = { onOpenFilteredMembers(RosterFilter.EXPIRING) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dash_pt_expiring_today")
                                )
                            }
                        }
                    }

                    // SECTION 2: MONTHLY ATTENDANCE
                    item {
                        SectionHeading(title = "MONTHLY ATTENDANCE")
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Monthly Check-ins",
                                value = "${monthlyCheckIns.size}",
                                subtitle = "Total gate check-ins",
                                icon = Icons.Rounded.EventAvailable,
                                accentColor = StatusActiveAccent,
                                onClick = { activeTab = DashboardSubTab.ATTENDANCE },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dash_monthly_checkins")
                            )
                            KpiMetricCard(
                                title = "Unique Members Attended",
                                value = "$uniqueMonthlyMembers",
                                subtitle = "Of ${members.size} registered",
                                icon = Icons.Rounded.Group,
                                accentColor = RoyalBlueAccent,
                                onClick = { activeTab = DashboardSubTab.ATTENDANCE },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dash_unique_members")
                            )
                        }
                    }

                    // SECTION 3: MEMBERSHIP EXPIRY
                    item {
                        SectionHeading(title = "MEMBERSHIP EXPIRY", trailing = "RENEWAL PIPELINE")
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpiryPipelineRowCard(
                                title = "Expiring (1–3 days)",
                                count = expiring1to3.size,
                                subtitle = "${formatCurrencyCents(expiring1to3.sumOf { it.monthlyFeeCents })} potential renewal",
                                icon = Icons.Rounded.WarningAmber,
                                badgeBg = StatusExpiringBg,
                                badgeFg = StatusExpiringText,
                                actionLabel = if (expiring1to3.isNotEmpty()) "WhatsApp" else "View",
                                onActionClick = {
                                    val first = expiring1to3.firstOrNull()
                                    if (first != null) {
                                        val msg = notificationSettings?.expiringTemplate?.takeIf { it.isNotBlank() }?.let {
                                            formatWhatsAppReminderMessage(it, first, first.monthlyFeeCents, gymSettings)
                                        }
                                        launchWhatsApp(
                                            context = context,
                                            phone = first.phone,
                                            memberName = first.fullName,
                                            customMessage = msg,
                                            whatsappEnabled = notificationSettings?.whatsappEnabled ?: true
                                        )
                                    } else {
                                        onOpenFilteredMembers(RosterFilter.EXPIRING)
                                    }
                                },
                                onCardClick = { onOpenFilteredMembers(RosterFilter.EXPIRING) },
                                tag = "dash_expiring_1_3"
                            )

                            ExpiryPipelineRowCard(
                                title = "Expiring (4–7 days)",
                                count = expiring4to7.size,
                                subtitle = "${formatCurrencyCents(expiring4to7.sumOf { it.monthlyFeeCents })} upcoming dues",
                                icon = Icons.Rounded.Schedule,
                                badgeBg = TurquoiseSoft,
                                badgeFg = TurquoiseText,
                                actionLabel = "View",
                                onActionClick = { onOpenFilteredMembers(RosterFilter.EXPIRING) },
                                onCardClick = { onOpenFilteredMembers(RosterFilter.EXPIRING) },
                                tag = "dash_expiring_4_7"
                            )

                            ExpiryPipelineRowCard(
                                title = "Expiring (8–15 days)",
                                count = expiring8to15.size,
                                subtitle = "Early renewal window",
                                icon = Icons.Rounded.CalendarToday,
                                badgeBg = SurfaceSubtle,
                                badgeFg = TextSecondary,
                                actionLabel = "View",
                                onActionClick = { onOpenFilteredMembers(RosterFilter.ACTIVE) },
                                onCardClick = { onOpenFilteredMembers(RosterFilter.ACTIVE) },
                                tag = "dash_expiring_8_15"
                            )
                        }
                    }

                    // SECTION 4: MEMBERSHIP EXPIRED
                    item {
                        SectionHeading(title = "MEMBERSHIP EXPIRED")
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, StatusExpiredAccent.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenFilteredMembers(RosterFilter.EXPIRED) }
                                .testTag("dash_membership_expired_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StatusExpiredBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Block,
                                            contentDescription = null,
                                            tint = StatusExpiredText
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${expiredMembers.size} Expired Memberships",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Outstanding dues: ${formatCurrencyCents(overdueTotalCents)}",
                                            style = TabularMetricStyle,
                                            color = StatusExpiredText
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusExpiredBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Follow-up",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusExpiredText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            tint = StatusExpiredText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DashboardSubTab.PAYMENTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        when (billingState) {
                            is UiState.Loading -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = TurquoisePrimary)
                            }
                            is UiState.Error -> ErrorBanner(message = billingState.message)
                            is UiState.Success -> {
                                RevenueBillingDashboardCard(
                                    metrics = billingState.data,
                                    onFilterOverdue = { onOpenFilteredMembers(RosterFilter.EXPIRED) },
                                    onFilterExpiring = { onOpenFilteredMembers(RosterFilter.EXPIRING) },
                                    onFilterManual = { onOpenFilteredMembers(RosterFilter.ALL) }
                                )
                            }
                        }
                    }
                    item {
                        val metrics = (billingState as? UiState.Success)?.data
                        if (metrics != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TurquoiseSoft,
                                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSyncCurrentMonth(metrics) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sync current billing snapshot to Reports",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TurquoiseText
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = TurquoiseText
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionHeading(title = "PENDING DUES & RENEWALS")
                    }
                    val dueMembers = members.filter { it.statusEnum != MembershipStatus.ACTIVE }
                    if (dueMembers.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "All Dues Clear",
                                body = "Every registered member is currently paid and active."
                            )
                        }
                    } else {
                        items(dueMembers, key = { it.id }) { member ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenMemberDetail(member) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${member.fullName} (${member.displayMemberId})",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${member.displayPlanName} • ${if (member.daysRemaining < 0) "Expired ${-member.daysRemaining}d ago" else "Expires in ${member.daysRemaining}d"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RoyalBlueAccent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onRenewMember(member) }
                                    ) {
                                        Text(
                                            text = "Collect ${formatCurrencyCents(member.monthlyFeeCents)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DashboardSubTab.ATTENDANCE -> {
                val gates = remember {
                    listOf("Main Turnstile A", "VIP Studio Gate", "Performance Lab")
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionHeading(title = "ACTIVE GATE")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(gates) { gate ->
                                    val selected = gate == selectedGate
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectGate(gate) },
                                        label = { Text(gate) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TopBarTurquoise,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeading(
                            title = "LIVE ATTENDANCE LOG",
                            trailing = "${checkIns.size} RECORDS"
                        )
                    }

                    if (checkIns.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "No Attendance Recorded Yet",
                                body = "Tap Record Attendance below to check in a member by Member ID."
                            )
                        }
                    } else {
                        items(checkIns, key = { it.id }) { log ->
                            CheckInFeedRow(
                                log = log,
                                onClick = {
                                    members.firstOrNull { it.id == log.memberId }?.let(onOpenMemberDetail)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    trailing: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Bold
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = TurquoiseText
            )
        }
    }
}

@Composable
private fun ExpiryPipelineRowCard(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    badgeBg: Color,
    badgeFg: Color,
    actionLabel: String,
    onActionClick: () -> Unit,
    onCardClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onCardClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = "$count Members",
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (actionLabel == "WhatsApp") WhatsAppBg else SurfaceSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (actionLabel == "WhatsApp") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = WhatsAppIcon,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (actionLabel == "WhatsApp") WhatsAppIcon else TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
