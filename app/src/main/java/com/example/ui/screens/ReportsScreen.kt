package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BillingDashboardMetrics
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipStatus
import com.example.model.MembershipTier
import com.example.model.MonthlyRevenueRecord
import com.example.model.NotificationSettings
import com.example.model.UiState
import com.example.model.formatWhatsAppReminderMessage
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.formatCurrencyCents
import com.example.ui.components.launchDialer
import com.example.ui.components.launchWhatsApp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PhoneBg
import com.example.ui.theme.PhoneIcon
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.StatusActiveAccent
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.StatusExpiredBg
import com.example.ui.theme.StatusExpiredText
import com.example.ui.theme.StatusExpiringBg
import com.example.ui.theme.StatusExpiringText
import com.example.ui.theme.SurfaceCanvas
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

enum class ReportTab(val label: String) {
    TRENDS("Trends"),
    SALES("Sales"),
    PLAN_DUE("Plan Due"),
    PT_PLAN_DUE("PT Plan Due"),
    COLLECTION("Collection")
}

enum class ReportPeriod(val label: String, val limitMonths: Int) {
    WEEK("Week", 2),
    QUARTER("Quarter", 3),
    SIX_MONTHS("Six Months", 6),
    YEARLY("Yearly", 12)
}

@Composable
fun ReportsScreen(
    members: List<GymMember>,
    billingState: UiState<BillingDashboardMetrics>,
    revenueHistoryState: UiState<List<MonthlyRevenueRecord>>,
    gymSettings: GymSettings? = null,
    notificationSettings: NotificationSettings? = null,
    onRenewMember: (GymMember) -> Unit,
    onOpenMemberDetail: (GymMember) -> Unit,
    onSyncCurrentMonth: (BillingDashboardMetrics) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ReportTab.TRENDS) }
    var activePeriod by remember { mutableStateOf(ReportPeriod.SIX_MONTHS) }
    var showTableMode by remember { mutableStateOf(false) }

    val metrics = (billingState as? UiState.Success)?.data ?: BillingDashboardMetrics()
    val allHistory = (revenueHistoryState as? UiState.Success)?.data.orEmpty()
    val periodHistory = remember(allHistory, activePeriod) {
        if (allHistory.isEmpty()) emptyList()
        else allHistory.takeLast(activePeriod.limitMonths.coerceAtLeast(1))
    }

    val planDueMembers = remember(members) {
        members.filter { it.statusEnum != MembershipStatus.ACTIVE || !it.accessEnabled }
            .sortedBy { it.daysRemaining }
    }
    val ptPlanDueMembers = remember(members) {
        members.filter {
            !it.trainerName.isNullOrBlank() &&
                (it.statusEnum != MembershipStatus.ACTIVE || it.daysRemaining <= 7L)
        }.sortedBy { it.daysRemaining }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Report Tabs Strip: Trends | Sales | Plan Due | PT Plan Due | Collection
        Surface(
            color = SurfaceCard,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportTab.entries.forEach { tab ->
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
                                .testTag("reports_tab_${tab.name.lowercase()}")
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

                // Period Filter + Graph / Table Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportPeriod.entries.forEach { period ->
                            val selected = period == activePeriod
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (selected) TurquoiseSoft else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) TurquoisePrimary else BorderSubtle
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable { activePeriod = period }
                                    .testTag("reports_period_${period.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) TurquoiseText else TextMuted,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Graph / Table Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCanvas)
                            .padding(2.dp)
                    ) {
                        GraphTableChip(
                            label = "Graph",
                            icon = Icons.Rounded.BarChart,
                            selected = !showTableMode,
                            onClick = { showTableMode = false },
                            tag = "reports_toggle_graph"
                        )
                        GraphTableChip(
                            label = "Table",
                            icon = Icons.Rounded.TableChart,
                            selected = showTableMode,
                            onClick = { showTableMode = true },
                            tag = "reports_toggle_table"
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (billingState is UiState.Error) {
                item { ErrorBanner(message = billingState.message) }
            }
            if (revenueHistoryState is UiState.Error) {
                item { ErrorBanner(message = revenueHistoryState.message) }
            }

            when (activeTab) {
                ReportTab.TRENDS -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Collected MRR",
                                value = formatCurrencyCents(metrics.monthlyRevenueCents),
                                subtitle = "${metrics.autoPayCoveragePercent}% auto-pay",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                accentColor = TurquoisePrimary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Outstanding Dues",
                                value = formatCurrencyCents(metrics.totalOutstandingCents),
                                subtitle = "${metrics.overdueMemberCount + metrics.expiringMemberCount} members",
                                icon = Icons.Rounded.TrendingUp,
                                accentColor = StatusExpiredAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        if (revenueHistoryState is UiState.Loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = TurquoisePrimary)
                            }
                        } else if (periodHistory.isEmpty()) {
                            EmptyStatePanel(
                                title = "No Revenue History Synced",
                                body = "Sync the current billing cycle to record monthly revenue and member registration trends in Firestore.",
                                actionLabel = "Sync Current Month",
                                onAction = { onSyncCurrentMonth(metrics) }
                            )
                        } else if (!showTableMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                VerticalBarReportCard(
                                    title = "Collected Payment",
                                    subtitle = "${activePeriod.label} Firestore Revenue Trend",
                                    barColor = TopBarTurquoise,
                                    labels = periodHistory.map { it.monthLabel },
                                    values = periodHistory.map { it.collectedCents.toFloat() },
                                    valueFormatter = { v -> formatCurrencyCents(v.toLong()) },
                                    onSyncClick = { onSyncCurrentMonth(metrics) },
                                    tag = "report_collected_payment_chart"
                                )
                                VerticalBarReportCard(
                                    title = "New Member",
                                    subtitle = "${activePeriod.label} Active & Registered Growth",
                                    barColor = RoyalBlueAccent,
                                    labels = periodHistory.map { it.monthLabel },
                                    values = periodHistory.map { it.activeMembers.toFloat() },
                                    valueFormatter = { v -> "${v.toInt()} members" },
                                    onSyncClick = null,
                                    tag = "report_new_member_chart"
                                )
                            }
                        } else {
                            HistoryBreakdownTableCard(
                                title = "Revenue & Registration Table (${activePeriod.label})",
                                records = periodHistory,
                                onSyncClick = { onSyncCurrentMonth(metrics) }
                            )
                        }
                    }
                }

                ReportTab.SALES -> {
                    val tierSales = MembershipTier.entries.map { tier ->
                        val tierMembers = members.filter { it.tierEnum == tier }
                        val activeTierRev = tierMembers
                            .filter { it.statusEnum != MembershipStatus.EXPIRED }
                            .sumOf { it.monthlyFeeCents }
                        Triple(tier, tierMembers.size, activeTierRev)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Registered Plans",
                                value = "${members.size}",
                                subtitle = "Across ${MembershipTier.entries.size} tiers",
                                icon = Icons.Rounded.GroupAdd,
                                accentColor = RoyalBlueAccent,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Monthly Sales",
                                value = formatCurrencyCents(metrics.monthlyRevenueCents),
                                subtitle = "Active paid cycle",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                accentColor = StatusActiveAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        if (!showTableMode) {
                            VerticalBarReportCard(
                                title = "Membership Sales by Plan",
                                subtitle = "Collected Revenue per Membership Plan",
                                barColor = TurquoisePrimary,
                                labels = tierSales.map { it.first.label.split(" ").first() },
                                values = tierSales.map { it.third.toFloat() },
                                valueFormatter = { v -> formatCurrencyCents(v.toLong()) },
                                onSyncClick = { onSyncCurrentMonth(metrics) },
                                tag = "report_sales_bar_chart"
                            )
                        } else {
                            PlanSalesTableCard(tierSales = tierSales)
                        }
                    }
                }

                ReportTab.PLAN_DUE -> {
                    item {
                        DueSummaryBanner(
                            title = "Plan Due Members",
                            count = planDueMembers.size,
                            totalDueCents = planDueMembers.sumOf { it.monthlyFeeCents },
                            accentBg = StatusExpiredBg,
                            accentFg = StatusExpiredText
                        )
                    }

                    if (planDueMembers.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "No Pending Plan Dues",
                                body = "All registered members have active membership plans."
                            )
                        }
                    } else if (showTableMode) {
                        item {
                            DueMembersTableCard(
                                title = "Plan Due Table (${planDueMembers.size})",
                                members = planDueMembers,
                                onRenew = onRenewMember,
                                onOpenDetail = onOpenMemberDetail
                            )
                        }
                    } else {
                        items(planDueMembers, key = { it.id }) { member ->
                            ReportDueMemberCard(
                                member = member,
                                gymSettings = gymSettings,
                                notificationSettings = notificationSettings,
                                onOpenDetail = { onOpenMemberDetail(member) },
                                onRenew = { onRenewMember(member) }
                            )
                        }
                    }
                }

                ReportTab.PT_PLAN_DUE -> {
                    item {
                        DueSummaryBanner(
                            title = "PT Plan Due Follow-Ups",
                            count = ptPlanDueMembers.size,
                            totalDueCents = ptPlanDueMembers.sumOf { it.monthlyFeeCents },
                            accentBg = StatusExpiringBg,
                            accentFg = StatusExpiringText
                        )
                    }

                    if (ptPlanDueMembers.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "No PT Plans Due",
                                body = "All personal training assigned memberships are currently in good standing."
                            )
                        }
                    } else if (showTableMode) {
                        item {
                            DueMembersTableCard(
                                title = "PT Plan Due Table (${ptPlanDueMembers.size})",
                                members = ptPlanDueMembers,
                                onRenew = onRenewMember,
                                onOpenDetail = onOpenMemberDetail
                            )
                        }
                    } else {
                        items(ptPlanDueMembers, key = { it.id }) { member ->
                            ReportDueMemberCard(
                                member = member,
                                gymSettings = gymSettings,
                                notificationSettings = notificationSettings,
                                onOpenDetail = { onOpenMemberDetail(member) },
                                onRenew = { onRenewMember(member) }
                            )
                        }
                    }
                }

                ReportTab.COLLECTION -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Total Collected",
                                value = formatCurrencyCents(metrics.monthlyRevenueCents),
                                subtitle = "${metrics.autoPayCoveragePercent}% auto-renew",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                accentColor = StatusActiveAccent,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Manual / At Risk",
                                value = "${metrics.manualBillingCount + metrics.autoPayAtRiskCount}",
                                subtitle = "Requires desk follow-up",
                                icon = Icons.Rounded.Refresh,
                                accentColor = RoyalBlueAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        if (!showTableMode && periodHistory.isNotEmpty()) {
                            VerticalBarReportCard(
                                title = "Payment Collections",
                                subtitle = "${activePeriod.label} Auto-Pay + Manual Collections",
                                barColor = StatusActiveAccent,
                                labels = periodHistory.map { it.monthLabel },
                                values = periodHistory.map { it.collectedCents.toFloat() },
                                valueFormatter = { v -> formatCurrencyCents(v.toLong()) },
                                onSyncClick = { onSyncCurrentMonth(metrics) },
                                tag = "report_collection_bar_chart"
                            )
                        } else {
                            HistoryBreakdownTableCard(
                                title = "Collections Breakdown (${activePeriod.label})",
                                records = periodHistory,
                                onSyncClick = { onSyncCurrentMonth(metrics) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphTableChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) TopBarTurquoise else Color.Transparent,
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VerticalBarReportCard(
    title: String,
    subtitle: String,
    barColor: Color,
    labels: List<String>,
    values: List<Float>,
    valueFormatter: (Float) -> String,
    onSyncClick: (() -> Unit)?,
    tag: String
) {
    var selectedIdx by remember(values.size) { mutableIntStateOf((values.lastIndex).coerceAtLeast(0)) }
    val safeIdx = selectedIdx.coerceIn(0, (values.lastIndex).coerceAtLeast(0))
    val maxVal = remember(values) { (values.maxOrNull() ?: 1f).coerceAtLeast(1f) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                if (onSyncClick != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TurquoiseSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSyncClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = "Sync",
                                tint = TurquoiseText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurquoiseText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (values.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceSubtle,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = labels.getOrElse(safeIdx) { "" },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = valueFormatter(values.getOrElse(safeIdx) { 0f }),
                            style = TabularMetricStyle,
                            color = barColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                ) {
                    val topPad = 10.dp.toPx()
                    val bottomPad = 6.dp.toPx()
                    val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..3) {
                        val y = topPad + plotH * (i / 3f)
                        drawLine(
                            color = BorderSubtle,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (i < 3) dashEffect else null
                        )
                    }

                    val count = values.size.coerceAtLeast(1)
                    val slotW = size.width / count.toFloat()
                    val barW = (slotW * 0.44f).coerceIn(14.dp.toPx(), 34.dp.toPx())
                    values.forEachIndexed { idx, value ->
                        val ratio = (value / maxVal).coerceIn(0f, 1f)
                        val barH = (plotH * ratio).coerceAtLeast(4.dp.toPx())
                        val left = slotW * idx + (slotW - barW) / 2f
                        val top = topPad + plotH - barH
                        drawRoundRect(
                            color = if (idx == safeIdx) barColor else barColor.copy(alpha = 0.55f),
                            topLeft = Offset(left, top),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    labels.forEachIndexed { idx, lbl ->
                        val isSel = idx == safeIdx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) TurquoiseSoft else Color.Transparent)
                                .clickable { selectedIdx = idx }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) TurquoiseText else TextMuted,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryBreakdownTableCard(
    title: String,
    records: List<MonthlyRevenueRecord>,
    onSyncClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_history_table")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onSyncClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TopBarTurquoise),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Sync", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSubtle)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Month", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
                Text("Collected", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
                Text("Due", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(0.9f))
                Text("Members", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(0.8f))
            }
            if (records.isEmpty()) {
                Text(
                    text = "No synced rows yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                records.forEach { rec ->
                    HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rec.monthLabel, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text(formatCurrencyCents(rec.collectedCents), style = TabularMetricStyle, color = StatusActiveText, modifier = Modifier.weight(1f))
                        Text(formatCurrencyCents(rec.outstandingCents), style = TabularMetricStyle, color = StatusExpiredText, modifier = Modifier.weight(0.9f))
                        Text("${rec.activeMembers}", style = TabularMetricStyle, color = TextSecondary, modifier = Modifier.weight(0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanSalesTableCard(
    tierSales: List<Triple<MembershipTier, Int, Long>>
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Membership Plan Breakdown",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSubtle)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Text("Plan", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1.6f))
                Text("Count", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(0.7f))
                Text("Revenue", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
            }
            tierSales.forEach { (tier, count, revCents) ->
                HorizontalDivider(color = BorderSubtle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tier.planTitle, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1.6f))
                    Text("$count", style = TabularMetricStyle, color = TextSecondary, modifier = Modifier.weight(0.7f))
                    Text(formatCurrencyCents(revCents), style = TabularMetricStyle, color = TurquoiseText, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DueSummaryBanner(
    title: String,
    count: Int,
    totalDueCents: Long,
    accentBg: Color,
    accentFg: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentBg.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, accentFg.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentFg,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count members pending payment / renewal",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentFg.copy(alpha = 0.85f)
                )
            }
            Text(
                text = formatCurrencyCents(totalDueCents),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = accentFg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DueMembersTableCard(
    title: String,
    members: List<GymMember>,
    onRenew: (GymMember) -> Unit,
    onOpenDetail: (GymMember) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSubtle)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Text("Member", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1.4f))
                Text("Due", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(0.9f))
                Text("Action", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(0.8f))
            }
            members.forEach { member ->
                HorizontalDivider(color = BorderSubtle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDetail(member) }
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(
                            text = "${member.fullName} (${member.displayMemberId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (member.daysRemaining < 0) "Expired ${-member.daysRemaining}d" else "${member.daysRemaining}d left",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusExpiredText
                        )
                    }
                    Text(
                        text = formatCurrencyCents(member.monthlyFeeCents),
                        style = TabularMetricStyle,
                        color = StatusExpiredText,
                        modifier = Modifier.weight(0.9f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RoyalBlueSoft,
                        modifier = Modifier
                            .weight(0.8f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRenew(member) }
                    ) {
                        Text(
                            text = "Collect",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlueAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportDueMemberCard(
    member: GymMember,
    gymSettings: GymSettings?,
    notificationSettings: NotificationSettings?,
    onOpenDetail: () -> Unit,
    onRenew: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenDetail)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${member.fullName} (${member.displayMemberId})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(member.displayPlanName)
                        if (!member.trainerName.isNullOrBlank()) {
                            append(" • PT: ${member.trainerName}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (member.daysRemaining < 0) {
                        "Expired ${-member.daysRemaining} days ago • Due ${formatCurrencyCents(member.monthlyFeeCents)}"
                    } else {
                        "Expires in ${member.daysRemaining} days • ${formatCurrencyCents(member.monthlyFeeCents)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusExpiredText
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PhoneBg)
                        .clickable { launchDialer(context, member.phone) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = "Call", tint = PhoneIcon, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WhatsAppBg)
                        .clickable {
                            val tpl = if (member.daysRemaining < 0) {
                                notificationSettings?.expiredTemplate
                            } else {
                                notificationSettings?.expiringTemplate
                            }
                            val msg = tpl?.takeIf { it.isNotBlank() }?.let {
                                formatWhatsAppReminderMessage(it, member, member.monthlyFeeCents, gymSettings)
                            }
                            launchWhatsApp(
                                context = context,
                                phone = member.phone,
                                memberName = member.fullName,
                                customMessage = msg,
                                whatsappEnabled = notificationSettings?.whatsappEnabled ?: true
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "WhatsApp", tint = WhatsAppIcon, modifier = Modifier.size(16.dp))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlueAccent,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRenew)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Renew",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
