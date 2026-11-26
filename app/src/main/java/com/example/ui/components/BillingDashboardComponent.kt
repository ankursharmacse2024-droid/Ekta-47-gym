package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BillingDashboardMetrics
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PlusJakartaSansFontFamily
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

@Composable
fun RevenueBillingDashboardCard(
    metrics: BillingDashboardMetrics,
    onFilterOverdue: () -> Unit,
    onFilterExpiring: () -> Unit,
    onFilterManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val autoPayProgress by animateFloatAsState(
        targetValue = metrics.autoPayCoveragePercent / 100f,
        label = "autopay_progress"
    )

    val syncLabel: String
    val syncDotColor: Color
    when {
        metrics.hasPendingWrites -> {
            syncLabel = "SYNCING WRITE"
            syncDotColor = StatusExpiringAccent
        }
        metrics.isFromCache -> {
            syncLabel = "CACHED SNAPSHOT"
            syncDotColor = StatusExpiringAccent
        }
        else -> {
            syncLabel = "LIVE SNAPSHOT"
            syncDotColor = StatusActiveAccent
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("revenue_billing_dashboard_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Clean Turquoise-Accented Light Revenue Summary Band
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TurquoiseSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = TurquoiseText,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EKTA 47 GYM • MONTHLY REVENUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    // Live Firestore Snapshot Status Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SurfaceSubtle)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(syncDotColor)
                        )
                        Text(
                            text = syncLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = formatCurrencyCents(metrics.monthlyRevenueCents),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            color = TextPrimary,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            modifier = Modifier.testTag("dashboard_monthly_revenue_value")
                        )
                        Text(
                            text = "Collected MRR • ${formatCurrencyCents(metrics.potentialMonthlyRevenueCents)} potential",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TurquoiseSoft
                    ) {
                        Text(
                            text = "${metrics.totalMembersCount} MEMBERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = TurquoiseText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Tier Revenue Distribution Stacked Bar
                if (metrics.monthlyRevenueCents > 0L) {
                    val total = metrics.monthlyRevenueCents.toFloat()
                    val blackWeight = (metrics.blackCardRevenueCents / total).coerceAtLeast(0f)
                    val perfWeight = (metrics.performanceRevenueCents / total).coerceAtLeast(0f)
                    val foundWeight = (metrics.foundationRevenueCents / total).coerceAtLeast(0f)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(SurfaceCanvas),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (blackWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(blackWeight)
                                        .fillMaxHeight()
                                        .background(TopBarTurquoise)
                                )
                            }
                            if (perfWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(perfWeight)
                                        .fillMaxHeight()
                                        .background(TurquoisePrimary)
                                )
                            }
                            if (foundWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(foundWeight)
                                        .fillMaxHeight()
                                        .background(RoyalBlueAccent)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TierLegendPill(
                                dotColor = TopBarTurquoise,
                                label = "12M VIP",
                                amount = formatCurrencyCents(metrics.blackCardRevenueCents)
                            )
                            TierLegendPill(
                                dotColor = TurquoisePrimary,
                                label = "6M PRO",
                                amount = formatCurrencyCents(metrics.performanceRevenueCents)
                            )
                            TierLegendPill(
                                dotColor = RoyalBlueAccent,
                                label = "3M BASE",
                                amount = formatCurrencyCents(metrics.foundationRevenueCents)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = BorderSubtle)

            // Outstanding Member Dues Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OUTSTANDING MEMBER DUES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "${formatCurrencyCents(metrics.totalOutstandingCents)} total exposure",
                        style = TabularMetricStyle,
                        color = if (metrics.totalOutstandingCents > 0L) StatusExpiredText else StatusActiveText
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DuesBreakdownTile(
                        title = "OVERDUE DUES",
                        amount = formatCurrencyCents(metrics.outstandingOverdueCents),
                        subtitle = "${metrics.overdueMemberCount} expired",
                        bgColor = StatusExpiredBg,
                        accentColor = StatusExpiredText,
                        icon = Icons.Rounded.WarningAmber,
                        onClick = onFilterOverdue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_overdue_dues_tile")
                    )
                    DuesBreakdownTile(
                        title = "EXPIRING (<7D)",
                        amount = formatCurrencyCents(metrics.expiringSoonDuesCents),
                        subtitle = "${metrics.expiringMemberCount} renewing",
                        bgColor = StatusExpiringBg,
                        accentColor = StatusExpiringText,
                        icon = Icons.Rounded.Schedule,
                        onClick = onFilterExpiring,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_expiring_dues_tile")
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Automated Payment Status Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Autorenew,
                            contentDescription = null,
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AUTOMATED PAYMENT STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = "${metrics.autoPayCoveragePercent}% AUTO-PAY",
                        style = TabularMetricStyle,
                        color = TurquoisePrimary,
                        modifier = Modifier.testTag("dashboard_autopay_percent")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SurfaceCanvas)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(autoPayProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(TurquoisePrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AutoPayStatusBadge(
                        label = "AUTO ACTIVE",
                        count = "${metrics.autoPayActiveCount}",
                        bg = StatusActiveBg,
                        fg = StatusActiveText,
                        icon = Icons.Rounded.CheckCircle,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    AutoPayStatusBadge(
                        label = "MANUAL",
                        count = "${metrics.manualBillingCount}",
                        bg = RoyalBlueSoft,
                        fg = RoyalBlueAccent,
                        icon = Icons.Rounded.Sync,
                        onClick = onFilterManual,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_manual_pay_badge")
                    )
                    AutoPayStatusBadge(
                        label = "AT RISK",
                        count = "${metrics.autoPayAtRiskCount}",
                        bg = if (metrics.autoPayAtRiskCount > 0) StatusExpiredBg else SurfaceCanvas,
                        fg = if (metrics.autoPayAtRiskCount > 0) StatusExpiredText else TextMuted,
                        icon = Icons.Rounded.WarningAmber,
                        onClick = onFilterOverdue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactBillingSummaryCard(
    metrics: BillingDashboardMetrics,
    onOpenBillingDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenBillingDashboard)
            .testTag("compact_billing_summary_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (metrics.hasPendingWrites) StatusExpiringAccent else StatusActiveAccent)
                    )
                    Text(
                        text = "BILLING SNAPSHOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalBlueAccent
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Open billing dashboard",
                        tint = RoyalBlueAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MONTHLY MRR",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = formatCurrencyCents(metrics.monthlyRevenueCents),
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        color = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DUES OWED",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = formatCurrencyCents(metrics.totalOutstandingCents),
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        color = if (metrics.outstandingOverdueCents > 0L) StatusExpiredText else StatusExpiringText
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "AUTO-PAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "${metrics.autoPayCoveragePercent}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        color = TurquoisePrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun TierLegendPill(
    dotColor: Color,
    label: String,
    amount: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = "$label $amount",
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = TextSecondary
        )
    }
}

@Composable
private fun DuesBreakdownTile(
    title: String,
    amount: String,
    subtitle: String,
    bgColor: Color,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amount,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = accentColor,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum")
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun AutoPayStatusBadge(
    label: String,
    count: String,
    bg: Color,
    fg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = count,
                    style = TabularMetricStyle,
                    color = fg
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
