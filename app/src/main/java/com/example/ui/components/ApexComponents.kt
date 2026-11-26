package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Sync
import com.example.data.CloudSyncState
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.CheckInLog
import com.example.model.CheckInOutcome
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipStatus
import com.example.model.MembershipTier
import com.example.model.NotificationSettings
import com.example.model.formatMoneyCents
import com.example.model.formatWhatsAppReminderMessage
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EktaNavy
import com.example.ui.theme.InputBorder
import com.example.ui.theme.PhoneBg
import com.example.ui.theme.PhoneIcon
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
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TabularKpiStyle
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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object ActiveGymBranding {
    var logoUrl by mutableStateOf("")
    var currencyCode by mutableStateOf("INR")
    var gymName by mutableStateOf("EKTA 47 GYM & FITNESS CENTRE")
}

/**
 * Official EKTA 47 GYM & FITNESS CENTRE logo (or admin-uploaded custom logo)
 * rendered with original proportions and colors.
 */
@Composable
fun EktaBrandMark(
    size: Dp = 40.dp,
    useWhiteSurface: Boolean = true,
    customLogoUrl: String? = ActiveGymBranding.logoUrl,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (useWhiteSurface) BorderSubtle else TurquoisePrimary.copy(alpha = 0.35f)
        ),
        modifier = modifier.size(size)
    ) {
        val fallbackLogo: @Composable () -> Unit = {
            Image(
                painter = painterResource(id = R.drawable.ekta_47_logo),
                contentDescription = "EKTA 47 GYM & FITNESS CENTRE Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding((size.value * 0.06f).dp)
            )
        }
        if (!customLogoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(customLogoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${ActiveGymBranding.gymName} Logo",
                contentScale = ContentScale.Fit,
                loading = { fallbackLogo() },
                error = { fallbackLogo() },
                modifier = Modifier.padding((size.value * 0.06f).dp)
            )
        } else {
            fallbackLogo()
        }
    }
}

/**
 * Circular member avatar rendering either local preview bytes, remote Firebase Storage photo URL via Coil,
 * or a clean default profile placeholder.
 */
@Composable
fun MemberPhotoAvatar(
    photoUrl: String?,
    initials: String,
    modifier: Modifier = Modifier,
    localPhotoBytes: ByteArray? = null,
    size: Dp = 50.dp,
    borderColor: Color = BorderSubtle,
    borderWidth: Dp = 1.5.dp,
    showPersonPlaceholderWhenEmpty: Boolean = false,
    contentDescription: String = "Member profile photo"
) {
    val context = LocalContext.current
    val imageModel: Any? = when {
        localPhotoBytes != null -> localPhotoBytes
        !photoUrl.isNullOrBlank() -> ImageRequest.Builder(context)
            .data(photoUrl)
            .crossfade(true)
            .build()
        else -> null
    }

    val placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(size)
                .background(SurfaceSubtle),
            contentAlignment = Alignment.Center
        ) {
            if (showPersonPlaceholderWhenEmpty || initials.isBlank()) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(size * 0.52f)
                )
            } else {
                Text(
                    text = initials,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.32f).coerceIn(12f, 26f).sp,
                    color = TextPrimary
                )
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceSubtle)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel != null) {
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                loading = { placeholder() },
                error = { placeholder() },
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            placeholder()
        }
    }
}

@Composable
fun EktaTopBar(
    title: String,
    subtitle: String,
    alertCount: Int,
    showMembersActions: Boolean,
    archiveModeActive: Boolean = false,
    syncState: CloudSyncState = CloudSyncState.SYNCED,
    lastSyncText: String = "",
    onOpenDrawer: () -> Unit,
    onAlertsClick: () -> Unit,
    onToggleArchiveMode: () -> Unit = {},
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        color = TopBarTurquoise,
        contentColor = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(58.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("header_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Open navigation menu",
                    tint = Color.White
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                EktaBrandMark(
                    size = 34.dp,
                    useWhiteSurface = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val dotColor = when (syncState) {
                            CloudSyncState.SYNCED -> Color(0xFF4ADE80)
                            CloudSyncState.SYNCING -> Color(0xFFFDE047)
                            CloudSyncState.OFFLINE -> Color(0xFFFB923C)
                            CloudSyncState.FAILED -> Color(0xFFF87171)
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val msg = if (lastSyncText.isNotBlank()) lastSyncText else syncState.label
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("header_sync_status_button")
                ) {
                    val (icon, tint) = when (syncState) {
                        CloudSyncState.SYNCED -> Icons.Rounded.CloudDone to Color(0xFF86EFAC)
                        CloudSyncState.SYNCING -> Icons.Rounded.Sync to Color(0xFFFEF08A)
                        CloudSyncState.OFFLINE -> Icons.Rounded.CloudOff to Color(0xFFFDBA74)
                        CloudSyncState.FAILED -> Icons.Rounded.CloudOff to Color(0xFFFCA5A5)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = syncState.label,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onAlertsClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("header_notifications_button")
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = "Notifications & Dues Alerts",
                            tint = Color.White
                        )
                        if (alertCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(StatusExpiredAccent)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
                if (showMembersActions) {
                    IconButton(
                        onClick = onToggleArchiveMode,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("header_trash_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Show expired & blocked members",
                            tint = if (archiveModeActive) Color(0xFFFDE047) else Color.White
                        )
                    }
                }
                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("header_help_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = "Help",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TurnstileSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    memberIdOnly: Boolean,
    onToggleMemberIdOnly: (Boolean) -> Unit,
    onSearchAction: () -> Unit,
    placeholder: String = "Search for Member ID",
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) TurquoisePrimary else BorderSubtle,
        label = "search_border"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
                .border(if (isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = if (isFocused) TurquoisePrimary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(TurquoisePrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_search_input")
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TopBarTurquoise,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSearchAction)
                    .testTag("member_search_submit_button")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Member ID Toggle
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (memberIdOnly) TurquoiseSoft else SurfaceCard,
            border = BorderStroke(
                width = 1.dp,
                color = if (memberIdOnly) TurquoisePrimary else BorderSubtle
            ),
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleMemberIdOnly(!memberIdOnly) }
                .testTag("member_id_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tag,
                    contentDescription = "Member ID filter mode",
                    tint = if (memberIdOnly) TurquoiseText else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ID",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (memberIdOnly) TurquoiseText else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 0.dp,
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = value,
                style = TabularKpiStyle,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    status: MembershipStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        MembershipStatus.ACTIVE -> StatusActiveBg to StatusActiveText
        MembershipStatus.EXPIRING -> StatusExpiringBg to StatusExpiringText
        MembershipStatus.EXPIRED -> StatusExpiredBg to StatusExpiredText
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg
        )
    }
}

@Composable
fun TierPill(
    tier: MembershipTier,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tier) {
        MembershipTier.BLACK_CARD -> TurquoiseSoft to TurquoiseText
        MembershipTier.PERFORMANCE -> RoyalBlueSoft to RoyalBlueAccent
        MembershipTier.FOUNDATION -> SurfaceSubtle to TextSecondary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = tier.planTitle,
            style = MaterialTheme.typography.labelSmall,
            color = fg
        )
    }
}

@Composable
fun MemberOperationalCard(
    member: GymMember,
    notificationSettings: NotificationSettings? = null,
    gymSettings: GymSettings? = null,
    onCardClick: () -> Unit,
    onIdCard: () -> Unit,
    onCheckIn: () -> Unit,
    onRenew: () -> Unit,
    onToggleBlock: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val (badgeIcon, badgeColor) = when {
        !member.accessEnabled || member.statusEnum == MembershipStatus.EXPIRED ->
            Icons.Rounded.Close to StatusExpiredAccent
        member.statusEnum == MembershipStatus.EXPIRING ->
            Icons.Rounded.Schedule to StatusExpiringAccent
        member.tierEnum == MembershipTier.BLACK_CARD ->
            Icons.Rounded.Star to StatusActiveAccent
        else ->
            Icons.Rounded.CheckCircle to StatusActiveAccent
    }

    val (stripBg, stripTextColor, expiryLabel) = when {
        !member.accessEnabled ->
            Triple(StatusExpiredBg, StatusExpiredText, "Membership Blocked")
        member.statusEnum == MembershipStatus.EXPIRED ->
            Triple(StatusExpiredBg, StatusExpiredText, "Expired ${-member.daysRemaining} days ago")
        member.statusEnum == MembershipStatus.EXPIRING ->
            Triple(StatusExpiringBg, StatusExpiringText, "Expires in ${member.daysRemaining} days")
        else ->
            Triple(SurfaceSubtle, StatusActiveText, "Valid • ${member.daysRemaining} days left")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onCardClick)
            .testTag("member_card_${member.memberCode}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left circular profile + Right details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.size(54.dp)) {
                    MemberPhotoAvatar(
                        photoUrl = member.profilePhotoUrl,
                        initials = member.initials,
                        size = 50.dp,
                        borderColor = badgeColor.copy(alpha = 0.6f),
                        borderWidth = 1.5.dp,
                        contentDescription = "${member.fullName} profile photo"
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(badgeColor)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = member.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceSubtle,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = member.displayMemberId,
                                    style = TabularMetricStyle,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("member_menu_${member.memberCode}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Member options",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View Profile") },
                                    onClick = {
                                        menuExpanded = false
                                        onCardClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ID Card") },
                                    onClick = {
                                        menuExpanded = false
                                        onIdCard()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (member.accessEnabled) "Block Member" else "Unblock Member") },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleBlock()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete / Archive", color = StatusExpiredText) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = member.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = member.displayPlanName +
                            (member.lockerNumber?.takeIf { it.isNotBlank() }?.let { " • Locker $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = TurquoiseText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Plan Expiry & Due Amount Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(stripBg.copy(alpha = 0.72f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(stripTextColor)
                    )
                    Text(
                        text = expiryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = stripTextColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val hasDue = member.dueAmountCents > 0L
                    Text(
                        text = if (hasDue) "Due Amount" else "Fee Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (hasDue) {
                            "${formatCurrencyCents(member.dueAmountCents)} Due"
                        } else {
                            "Paid ${formatCurrencyCents(member.monthlyFeeCents)}"
                        },
                        style = TabularMetricStyle,
                        color = if (hasDue) StatusExpiredText else StatusActiveText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f))

            // Bottom Action Bar: ID Card, Call, WhatsApp, Attendance, Renew Plan, Block Member
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactCardAction(
                    icon = Icons.Rounded.Badge,
                    label = "ID Card",
                    bg = SurfaceSubtle,
                    fg = TextPrimary,
                    onClick = onIdCard,
                    tag = "id_card_button_${member.memberCode}"
                )
                CompactCardAction(
                    icon = Icons.Rounded.Call,
                    label = "Call",
                    bg = PhoneBg,
                    fg = PhoneIcon,
                    onClick = { launchDialer(context, member.phone) },
                    tag = "call_button_${member.memberCode}"
                )
                CompactCardAction(
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    label = "WhatsApp",
                    bg = WhatsAppBg,
                    fg = WhatsAppIcon,
                    onClick = {
                        val template = if (member.statusEnum == MembershipStatus.EXPIRED || member.daysRemaining < 0L) {
                            notificationSettings?.expiredTemplate
                        } else {
                            notificationSettings?.expiringTemplate
                        }
                        val formattedMessage = template?.takeIf { it.isNotBlank() }?.let {
                            formatWhatsAppReminderMessage(it, member, member.monthlyFeeCents, gymSettings)
                        }
                        launchWhatsApp(
                            context = context,
                            phone = member.phone,
                            memberName = member.fullName,
                            customMessage = formattedMessage,
                            whatsappEnabled = notificationSettings?.whatsappEnabled ?: true
                        )
                    },
                    tag = "whatsapp_button_${member.memberCode}"
                )
                CompactCardAction(
                    icon = Icons.Rounded.HowToReg,
                    label = "Attendance",
                    bg = TurquoiseSoft,
                    fg = TurquoiseText,
                    onClick = onCheckIn,
                    tag = "checkin_button_${member.memberCode}"
                )
                CompactCardAction(
                    icon = Icons.Rounded.Refresh,
                    label = "Renew Plan",
                    bg = RoyalBlueSoft,
                    fg = RoyalBlueAccent,
                    onClick = onRenew,
                    tag = "renew_button_${member.memberCode}"
                )
                CompactCardAction(
                    icon = if (member.accessEnabled) Icons.Rounded.Block else Icons.Rounded.LockOpen,
                    label = if (member.accessEnabled) "Block" else "Unblock",
                    bg = if (member.accessEnabled) StatusExpiredBg.copy(alpha = 0.7f) else StatusActiveBg,
                    fg = if (member.accessEnabled) StatusExpiredText else StatusActiveText,
                    onClick = onToggleBlock,
                    tag = "block_button_${member.memberCode}"
                )
            }
        }
    }
}

@Composable
private fun CompactCardAction(
    icon: ImageVector,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.16f)),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickContactButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun CheckInFeedRow(
    log: CheckInLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint, bg) = when (log.outcomeEnum) {
        CheckInOutcome.GRANTED -> Triple(Icons.Rounded.CheckCircle, StatusActiveText, StatusActiveBg)
        CheckInOutcome.WARNING -> Triple(Icons.Rounded.WarningAmber, StatusExpiringText, StatusExpiringBg)
        CheckInOutcome.DENIED -> Triple(Icons.Rounded.Lock, StatusExpiredText, StatusExpiredBg)
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = log.outcomeEnum.label,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = log.memberName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (log.memberCode.startsWith("#")) log.memberCode else "#${log.memberCode}",
                        style = TabularMetricStyle,
                        color = TextMuted
                    )
                }
                Text(
                    text = "${log.gateName} • ${log.note ?: log.outcomeEnum.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimestamp(log.checkedInAt),
                    style = TabularMetricStyle,
                    color = TextPrimary
                )
                Text(
                    text = log.outcomeEnum.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
            }
        }
    }
}

@Composable
fun EmptyStatePanel(
    title: String,
    body: String,
    icon: ImageVector = Icons.Rounded.FitnessCenter,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(TurquoiseSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TurquoisePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StatusExpiredBg,
        border = BorderStroke(1.dp, StatusExpiredAccent.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = StatusExpiredText
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = StatusExpiredText
            )
        }
    }
}

fun formatCurrencyCents(cents: Long): String =
    formatMoneyCents(cents, ActiveGymBranding.currencyCode)

fun formatTimestamp(ts: Timestamp?): String {
    if (ts == null) return "Just now"
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(ts.toDate())
}

fun launchDialer(context: Context, phone: String) {
    val clean = phone.filter { it.isDigit() || it == '+' }
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No phone dialer available", Toast.LENGTH_SHORT).show()
    }
}

fun launchWhatsApp(
    context: Context,
    phone: String,
    memberName: String,
    customMessage: String? = null,
    whatsappEnabled: Boolean = true
) {
    if (!whatsappEnabled) {
        Toast.makeText(
            context,
            "WhatsApp reminders are disabled in Gym Settings.",
            Toast.LENGTH_SHORT
        ).show()
        return
    }
    val digits = phone.filter { it.isDigit() }
    val rawText = customMessage?.takeIf { it.isNotBlank() }
        ?: "Hi $memberName, checking in from ${ActiveGymBranding.gymName} Front Desk."
    val message = Uri.encode(rawText)
    val url = "https://wa.me/$digits?text=$message"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

fun launchSms(context: Context, phone: String, memberName: String) {
    val clean = phone.filter { it.isDigit() || it == '+' }
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
        putExtra("sms_body", "Hi $memberName, reminder from EKTA 47 GYM & FITNESS CENTRE.")
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open SMS app", Toast.LENGTH_SHORT).show()
    }
}
