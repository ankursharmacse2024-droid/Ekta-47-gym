package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.model.CheckInLog
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipPlan
import com.example.model.MembershipStatus
import com.example.model.MembershipTier
import com.example.model.MembershipTransaction
import com.example.model.NotificationSettings
import com.example.model.formatMoneyCents
import com.example.model.formatWhatsAppReminderMessage
import com.example.ui.components.CheckInFeedRow
import com.example.ui.components.EditMemberPhotoDialog
import com.example.ui.components.MemberIdCardDialog
import com.example.ui.components.MemberPhotoAvatar
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrencyCents
import com.example.ui.components.launchDialer
import com.example.ui.components.launchSms
import com.example.ui.components.launchWhatsApp
import com.example.ui.theme.BorderSubtle
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
import java.text.SimpleDateFormat
import java.util.Locale

private enum class MemberDetailTab(val label: String) {
    MEMBERSHIP("Membership"),
    PAYMENTS("Payments"),
    ATTENDANCE("Attendance")
}

@Composable
fun MemberDetailScreen(
    member: GymMember,
    memberCheckIns: List<CheckInLog>,
    plans: List<MembershipPlan>,
    memberTransactions: List<MembershipTransaction>,
    gymSettings: GymSettings?,
    notificationSettings: NotificationSettings?,
    isAdmin: Boolean,
    showTopBackBar: Boolean,
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    onRenew: () -> Unit,
    onToggleAccess: (Boolean) -> Unit,
    onToggleAutoRenew: (Boolean) -> Unit,
    onUpdatePhoto: (ByteArray?) -> Unit,
    onSaveMemberDetails: (
        fullName: String,
        selectedPlan: MembershipPlan?,
        tier: MembershipTier,
        locker: String?,
        trainer: String?,
        phone: String,
        email: String?
    ) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showTopBackBar) {
        BackHandler(onBack = onBack)
    }

    val context = LocalContext.current
    val activePlans = remember(plans) { plans.filter { it.isActive } }
    var activeTab by remember { mutableStateOf(MemberDetailTab.MEMBERSHIP) }
    var fullNameText by remember(member.id, member.fullName) { mutableStateOf(member.fullName) }
    var selectedPlan by remember(member.id, member.planId, activePlans) {
        mutableStateOf(
            activePlans.firstOrNull { it.id == member.planId }
                ?: activePlans.firstOrNull { it.name.equals(member.displayPlanName, ignoreCase = true) }
        )
    }
    var selectedTier by remember(member.id, member.tier) { mutableStateOf(member.tierEnum) }
    var lockerText by remember(member.id, member.lockerNumber) { mutableStateOf(member.lockerNumber ?: "") }
    var trainerText by remember(member.id, member.trainerName) { mutableStateOf(member.trainerName ?: "") }
    var phoneText by remember(member.id, member.phone) { mutableStateOf(member.phone) }
    var emailText by remember(member.id, member.email) { mutableStateOf(member.email ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    var showIdCard by remember { mutableStateOf(false) }
    var showEditPhotoDialog by remember { mutableStateOf(false) }

    val joinedLabel = remember(member.createdAt) {
        member.createdAt?.toDate()?.let {
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(it)
        } ?: "Registered"
    }

    val ringColor = when {
        !member.accessEnabled || member.statusEnum == MembershipStatus.EXPIRED -> StatusExpiredAccent
        member.statusEnum == MembershipStatus.EXPIRING -> StatusExpiringAccent
        else -> StatusActiveAccent
    }

    val (bannerBg, bannerFg, bannerText) = when {
        !member.accessEnabled -> Triple(StatusExpiredBg, StatusExpiredText, "Member access is currently blocked")
        member.statusEnum == MembershipStatus.EXPIRED -> Triple(StatusExpiredBg, StatusExpiredText, "Membership expired ${-member.daysRemaining} days ago • Due ${formatCurrencyCents(member.dueAmountCents)}")
        member.statusEnum == MembershipStatus.EXPIRING -> Triple(StatusExpiringBg, StatusExpiringText, "Expiring in ${member.daysRemaining} days")
        else -> Triple(StatusActiveBg, StatusActiveText, "Active • ${member.daysRemaining} days remaining")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
    ) {
        if (showTopBackBar) {
            Surface(color = TopBarTurquoise, contentColor = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(58.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Member Profile Detail",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Member ID: ${member.displayMemberId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Member",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Identity Card + 3 Stat Boxes
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
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable { showEditPhotoDialog = true }
                                    .testTag("detail_avatar_edit_trigger")
                            ) {
                                MemberPhotoAvatar(
                                    photoUrl = member.profilePhotoUrl,
                                    initials = member.initials,
                                    size = 68.dp,
                                    borderColor = ringColor,
                                    borderWidth = 2.dp,
                                    contentDescription = "${member.fullName} profile photo"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(TopBarTurquoise)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = "Edit profile photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = member.fullName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    StatusPill(status = member.statusEnum)
                                }
                                Text(
                                    text = "Member ID: ${member.displayMemberId} • Joined $joinedLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = listOfNotNull(
                                        member.phone.takeIf { it.isNotBlank() },
                                        member.email?.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { showEditPhotoDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("detail_edit_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PhotoCamera,
                                        contentDescription = null,
                                        tint = TurquoiseText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (member.hasProfilePhoto) "Change / Remove Photo" else "Add Profile Photo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TurquoiseText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Status / Expiry Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bannerBg)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = bannerText,
                                style = MaterialTheme.typography.labelMedium,
                                color = bannerFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 3 Stat Tiles: PLAN | ATTENDANCE | LOCKER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailStatBox(
                                label = "PLAN",
                                value = member.displayPlanName,
                                modifier = Modifier.weight(1f)
                            )
                            DetailStatBox(
                                label = "ATTENDANCE",
                                value = "${member.totalCheckIns} visits",
                                modifier = Modifier.weight(1f)
                            )
                            DetailStatBox(
                                label = "LOCKER",
                                value = member.lockerNumber?.ifBlank { "None" } ?: "None",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Fast Operations 5-tile strip: Call, WhatsApp, Check-In, ID Pass, SMS
                        Text(
                            text = "FAST OPERATIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FastOpTile(
                                icon = Icons.Rounded.Call,
                                label = "Call",
                                bg = PhoneBg,
                                fg = PhoneIcon,
                                onClick = { launchDialer(context, member.phone) },
                                tag = "detail_call_button",
                                modifier = Modifier.weight(1f)
                            )
                            FastOpTile(
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
                                    val formatted = template?.takeIf { it.isNotBlank() }?.let {
                                        formatWhatsAppReminderMessage(it, member, member.monthlyFeeCents, gymSettings)
                                    }
                                    launchWhatsApp(
                                        context = context,
                                        phone = member.phone,
                                        memberName = member.fullName,
                                        customMessage = formatted,
                                        whatsappEnabled = notificationSettings?.whatsappEnabled ?: true
                                    )
                                },
                                tag = "detail_whatsapp_button",
                                modifier = Modifier.weight(1f)
                            )
                            FastOpTile(
                                icon = Icons.Rounded.HowToReg,
                                label = "Check-In",
                                bg = TurquoiseSoft,
                                fg = TurquoiseText,
                                onClick = onCheckIn,
                                tag = "detail_checkin_button",
                                modifier = Modifier.weight(1f)
                            )
                            FastOpTile(
                                icon = Icons.Rounded.Badge,
                                label = "ID Pass",
                                bg = SurfaceSubtle,
                                fg = TextPrimary,
                                onClick = { showIdCard = true },
                                tag = "detail_id_pass_button",
                                modifier = Modifier.weight(1f)
                            )
                            FastOpTile(
                                icon = Icons.Rounded.Sms,
                                label = "SMS",
                                bg = RoyalBlueSoft,
                                fg = RoyalBlueAccent,
                                onClick = { launchSms(context, member.phone, member.fullName) },
                                tag = "detail_sms_button",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = onRenew,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("detail_renew_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Renew Plan / Collect Fee")
                        }
                    }
                }
            }

            // Segmented Tabs: Membership | Payments | Attendance
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemberDetailTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) TopBarTurquoise else SurfaceCard,
                            border = BorderStroke(1.dp, if (selected) TopBarTurquoise else BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeTab = tab }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
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
            }

            when (activeTab) {
                MemberDetailTab.MEMBERSHIP -> {
                    item {
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
                                        text = member.displayPlanName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${member.daysRemaining}d left",
                                        style = TabularMetricStyle,
                                        color = TurquoiseText
                                    )
                                }

                                val progress = (member.daysRemaining.coerceIn(0L, 30L) / 30f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(SurfaceCanvas)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(TurquoisePrimary)
                                    )
                                }

                                // Complete Profile Summary Grid (Start Date, Expiry Date, Payment Status, Coach/Locker)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DetailStatBox(
                                        label = "START DATE",
                                        value = member.startDateFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DetailStatBox(
                                        label = "EXPIRY DATE",
                                        value = member.expiryDateFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DetailStatBox(
                                        label = "PAYMENT STATUS",
                                        value = if (member.dueAmountCents > 0L) {
                                            "Due ${formatCurrencyCents(member.dueAmountCents)}"
                                        } else {
                                            "Paid • ${formatCurrencyCents(member.monthlyFeeCents)}"
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    DetailStatBox(
                                        label = "COACH / PT",
                                        value = member.trainerName?.ifBlank { "Floor Coach" } ?: "Floor Coach",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(color = BorderSubtle)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "EDIT MEMBER PROFILE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = { showEditPhotoDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = "Edit Photo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TurquoiseText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = fullNameText,
                                    onValueChange = { fullNameText = it },
                                    label = { Text("Member Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (activePlans.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        activePlans.forEach { plan ->
                                            val isSelected = selectedPlan?.id == plan.id
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedPlan = plan
                                                    selectedTier = plan.legacyTier
                                                },
                                                label = {
                                                    Text(
                                                        text = plan.name,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = TurquoiseSoft,
                                                    selectedLabelColor = TurquoiseText
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MembershipTier.entries.forEach { tier ->
                                            FilterChip(
                                                selected = selectedTier == tier,
                                                onClick = { selectedTier = tier },
                                                label = {
                                                    Text(
                                                        text = tier.label,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = TurquoiseSoft,
                                                    selectedLabelColor = TurquoiseText
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = lockerText,
                                        onValueChange = { lockerText = it },
                                        label = { Text("Locker #") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = trainerText,
                                        onValueChange = { trainerText = it },
                                        label = { Text("Coach / PT") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f)
                                    )
                                }
                                OutlinedTextField(
                                    value = phoneText,
                                    onValueChange = { phoneText = it },
                                    label = { Text("Phone / WhatsApp") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = emailText,
                                    onValueChange = { emailText = it },
                                    label = { Text("Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onSaveMemberDetails(
                                            fullNameText,
                                            selectedPlan,
                                            selectedPlan?.legacyTier ?: selectedTier,
                                            lockerText.ifBlank { null },
                                            trainerText.ifBlank { null },
                                            phoneText,
                                            emailText.ifBlank { null }
                                        )
                                    },
                                    enabled = fullNameText.trim().length >= 2 && phoneText.trim().length >= 5,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TopBarTurquoise),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("detail_save_button")
                                ) {
                                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }

                MemberDetailTab.PAYMENTS -> {
                    item {
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
                                    text = "FEE & BILLING STATUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Plan Fee", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        Text(
                                            text = formatCurrencyCents(member.monthlyFeeCents),
                                            style = TabularMetricStyle,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Balance Due", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        Text(
                                            text = formatCurrencyCents(member.dueAmountCents),
                                            style = TabularMetricStyle,
                                            color = if (member.dueAmountCents > 0L) StatusExpiredText else StatusActiveText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                HorizontalDivider(color = BorderSubtle)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto-Renew Billing", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            "Automatically charge ${formatCurrencyCents(member.monthlyFeeCents)} each cycle",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = member.autoRenew,
                                        onCheckedChange = onToggleAutoRenew,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = TurquoisePrimary,
                                            uncheckedTrackColor = InputBorder
                                        ),
                                        modifier = Modifier.testTag("detail_autorenew_switch")
                                    )
                                }
                            }
                        }
                    }

                    item {
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
                                    text = "PAYMENT & RENEWAL RECEIPTS (${memberTransactions.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                if (memberTransactions.isEmpty()) {
                                    Text(
                                        text = "No immutable receipt snapshots logged for this member yet. Future additions and renewals will appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                } else {
                                    val currency = gymSettings?.currency ?: "INR"
                                    memberTransactions.forEachIndexed { idx, tx ->
                                        if (idx > 0) HorizontalDivider(color = BorderSubtle)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "${tx.planName} (${tx.durationMonths}m • ${tx.transactionType.replace('_', ' ')})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${tx.startDateFormatted} → ${tx.expiryDateFormatted} • ${tx.paymentMethodEnum.label}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                                if (tx.transactionRef.isNotBlank() || tx.discountCents > 0L) {
                                                    val extra = buildList {
                                                        if (tx.transactionRef.isNotBlank()) add("Ref: ${tx.transactionRef}")
                                                        if (tx.discountCents > 0L) add("Discount: ${formatMoneyCents(tx.discountCents, currency)}")
                                                    }.joinToString(" • ")
                                                    Text(
                                                        text = extra,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TurquoiseText
                                                    )
                                                }
                                            }
                                            Text(
                                                text = formatMoneyCents(tx.chargedAmountCents, currency),
                                                style = TabularMetricStyle,
                                                color = StatusActiveText,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                MemberDetailTab.ATTENDANCE -> {
                    if (memberCheckIns.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No attendance records logged yet for ${member.fullName}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(memberCheckIns, key = { it.id }) { log ->
                            CheckInFeedRow(log = log, onClick = {})
                        }
                    }
                }
            }

            // Administrative Controls
            item {
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
                            text = "ADMINISTRATIVE CONTROLS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Member Access Enabled", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    "Toggle off to block turnstile entry immediately",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = member.accessEnabled,
                                onCheckedChange = onToggleAccess,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TurquoisePrimary,
                                    uncheckedTrackColor = InputBorder
                                ),
                                modifier = Modifier.testTag("detail_access_switch")
                            )
                        }
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            enabled = isAdmin,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isAdmin) StatusExpiredAccent else BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "Delete / Archive Member" else "Delete Member (Admin Required)",
                                color = if (isAdmin) StatusExpiredText else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showIdCard) {
        MemberIdCardDialog(
            member = member,
            gymSettings = gymSettings,
            onDismiss = { showIdCard = false },
            onRecordAttendance = onCheckIn
        )
    }

    if (showEditPhotoDialog) {
        EditMemberPhotoDialog(
            member = member,
            onDismiss = { showEditPhotoDialog = false },
            onSavePhoto = onUpdatePhoto
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = SurfaceCard,
            title = { Text("Delete / Archive Member?") },
            text = {
                Text("Remove ${member.fullName} (${member.displayMemberId}) from your EKTA 47 GYM roster? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusExpiredAccent),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, modifier = Modifier.height(44.dp)) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceSubtle,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = value,
                style = TabularMetricStyle,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FastOpTile(
    icon: ImageVector,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.16f)),
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
