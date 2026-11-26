package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.media.validateAndPrepareMemberPhoto
import com.example.model.FacilityProfile
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipPlan
import com.example.model.NotificationSettings
import com.example.model.PaymentSettings
import com.example.model.formatMoneyCents
import com.example.model.normalizeFacilityName
import com.example.ui.components.EktaBrandMark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.InputBorder
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.StatusExpiredText
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
import kotlinx.coroutines.launch

@Composable
fun GymProfileSettingsCard(
    gymSettings: GymSettings,
    facilityProfile: FacilityProfile?,
    isAdmin: Boolean,
    onUploadLogo: (ByteArray) -> Unit,
    onRemoveLogo: () -> Unit,
    onSaveGymProfile: (GymSettings, Long) -> Unit,
    onSaveLocalContactFallback: (phone: String, email: String, address: String, hours: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var gymName by remember(gymSettings.gymName, facilityProfile?.facilityName) {
        mutableStateOf(
            gymSettings.gymName.ifBlank { normalizeFacilityName(facilityProfile?.facilityName) }
        )
    }
    var adminName by remember(gymSettings.adminName, facilityProfile?.managerName) {
        mutableStateOf(gymSettings.adminName.ifBlank { facilityProfile?.managerName ?: "Facility Administrator" })
    }
    var contactPhone by remember(gymSettings.contactPhone) { mutableStateOf(gymSettings.contactPhone) }
    var contactEmail by remember(gymSettings.contactEmail) { mutableStateOf(gymSettings.contactEmail) }
    var address by remember(gymSettings.address) { mutableStateOf(gymSettings.address) }
    var openingHours by remember(gymSettings.openingHours) { mutableStateOf(gymSettings.openingHours) }
    var targetText by remember(facilityProfile?.dailyCheckInTarget) {
        mutableStateOf((facilityProfile?.dailyCheckInTarget ?: 180L).toString())
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                validateAndPrepareMemberPhoto(context, uri)
                    .onSuccess { prepared -> onUploadLogo(prepared.bytes) }
                    .onFailure { err ->
                        Toast.makeText(context, err.message ?: "Invalid logo image", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

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
                text = "1. GYM PROFILE & OFFICIAL LOGO",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EktaBrandMark(
                    size = 64.dp,
                    useWhiteSurface = true,
                    customLogoUrl = gymSettings.logoUrl
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (gymSettings.logoUrl.isNotBlank()) "Custom Gym Logo Active" else "Default EKTA 47 GYM Logo",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                logoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = isAdmin,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("gym_upload_logo_button")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Logo", style = MaterialTheme.typography.labelSmall)
                        }
                        if (gymSettings.logoUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = onRemoveLogo,
                                enabled = isAdmin,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, StatusExpiredAccent.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = "Reset Logo", tint = StatusExpiredText, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = gymName,
                onValueChange = { gymName = it },
                enabled = isAdmin,
                label = { Text("Gym Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    enabled = isAdmin,
                    label = { Text("Admin Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter(Char::isDigit) },
                    enabled = isAdmin,
                    label = { Text("Daily Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                enabled = isAdmin,
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                enabled = isAdmin,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                enabled = isAdmin,
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = openingHours,
                onValueChange = { openingHours = it },
                enabled = isAdmin,
                label = { Text("Opening Hours") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val updated = gymSettings.copy(
                        gymName = gymName.trim().ifBlank { "EKTA 47 GYM & FITNESS CENTRE" },
                        adminName = adminName.trim().ifBlank { "Facility Administrator" },
                        contactPhone = contactPhone.trim(),
                        contactEmail = contactEmail.trim(),
                        address = address.trim(),
                        openingHours = openingHours.trim()
                    )
                    onSaveGymProfile(updated, targetText.toLongOrNull()?.coerceAtLeast(10L) ?: 180L)
                    onSaveLocalContactFallback(contactPhone, contactEmail, address, openingHours)
                },
                enabled = isAdmin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TopBarTurquoise),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("gym_save_profile_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Gym Profile")
            }
        }
    }
}

@Composable
fun MembershipPlansSectionCard(
    plans: List<MembershipPlan>,
    members: List<GymMember>,
    currencyCode: String,
    isAdmin: Boolean,
    onAddPlan: () -> Unit,
    onEditPlan: (MembershipPlan) -> Unit,
    onTogglePlanActive: (MembershipPlan, Boolean) -> Unit,
    onDeletePlan: (MembershipPlan) -> Unit
) {
    var pendingDeletePlan by remember { mutableStateOf<MembershipPlan?>(null) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. MEMBERSHIP PLANS & PRICING",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${plans.count { it.isActive }} active of ${plans.size} plans",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAdmin) RoyalBlueSoft else SurfaceSubtle,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isAdmin, onClick = onAddPlan)
                        .testTag("gym_add_plan_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                            text = "Add Plan",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAdmin) RoyalBlueAccent else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            plans.forEachIndexed { index, plan ->
                if (index > 0) HorizontalDivider(color = BorderSubtle)
                val enrolledCount = remember(members, plan.id, plan.name) {
                    members.count { it.planId == plan.id || (it.planId.isNullOrBlank() && it.displayPlanName.equals(plan.name, ignoreCase = true)) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = plan.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (plan.isActive) StatusActiveBg else SurfaceSubtle
                                ) {
                                    Text(
                                        text = if (plan.isActive) "ACTIVE" else "INACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (plan.isActive) StatusActiveText else TextMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${plan.durationDisplayLabel} • $enrolledCount members",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (plan.description.isNotBlank()) {
                                Text(
                                    text = plan.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TurquoiseSoft
                        ) {
                            Text(
                                text = formatMoneyCents(plan.priceCents, currencyCode),
                                style = TabularMetricStyle,
                                color = TurquoiseText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Switch(
                                checked = plan.isActive,
                                onCheckedChange = { if (isAdmin) onTogglePlanActive(plan, it) },
                                enabled = isAdmin,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TurquoisePrimary,
                                    uncheckedTrackColor = InputBorder
                                )
                            )
                            Text(
                                text = if (plan.isActive) "Visible in registration" else "Hidden",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onEditPlan(plan) },
                                enabled = isAdmin,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("edit_plan_${plan.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit ${plan.name}",
                                    tint = if (isAdmin) RoyalBlueAccent else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { pendingDeletePlan = plan },
                                enabled = isAdmin,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("delete_plan_${plan.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ${plan.name}",
                                    tint = if (isAdmin) StatusExpiredText else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeletePlan?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeletePlan = null },
            shape = RoundedCornerShape(12.dp),
            containerColor = SurfaceCard,
            title = { Text("Delete '${target.name}'?") },
            text = {
                Text(
                    "Plans referenced by existing members or payment receipts are protected and cannot be deleted. You can deactivate them anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeletePlan = null
                        onDeletePlan(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusExpiredAccent),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Delete Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlan = null }, modifier = Modifier.height(44.dp)) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MemberIdSettingsCard(
    gymSettings: GymSettings,
    isAdmin: Boolean,
    onSaveIdSettings: (GymSettings) -> Unit
) {
    var prefix by remember(gymSettings.memberIdPrefix) { mutableStateOf(gymSettings.memberIdPrefix) }
    var startingIdText by remember(gymSettings.startingMemberId) {
        mutableStateOf(gymSettings.startingMemberId.toString())
    }
    var nextIdText by remember(gymSettings.nextMemberId) {
        mutableStateOf(gymSettings.nextMemberId.toString())
    }
    var tagline by remember(gymSettings.idCardTagline) { mutableStateOf(gymSettings.idCardTagline) }
    var showLogo by remember(gymSettings.idCardShowLogo) { mutableStateOf(gymSettings.idCardShowLogo) }
    var showLocker by remember(gymSettings.idCardShowLocker) { mutableStateOf(gymSettings.idCardShowLocker) }
    var showCoach by remember(gymSettings.idCardShowCoach) { mutableStateOf(gymSettings.idCardShowCoach) }

    val cleanPrefix = prefix.trim().uppercase()
    val previewNext = nextIdText.toLongOrNull() ?: gymSettings.nextMemberId

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
                text = "3. MEMBER ID & DIGITAL CARD SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TurquoiseSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next Auto-Generated ID Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = TurquoiseText
                    )
                    Text(
                        text = "#$cleanPrefix$previewNext",
                        style = TabularMetricStyle,
                        color = TurquoiseText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it.take(10) },
                    enabled = isAdmin,
                    label = { Text("ID Prefix") },
                    placeholder = { Text("e.g. EKTA-") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startingIdText,
                    onValueChange = { startingIdText = it.filter(Char::isDigit).take(7) },
                    enabled = isAdmin,
                    label = { Text("Start ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = nextIdText,
                    onValueChange = { nextIdText = it.filter(Char::isDigit).take(7) },
                    enabled = isAdmin,
                    label = { Text("Next ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                enabled = isAdmin,
                label = { Text("Digital ID Card Tagline") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SettingsToggleRow("Show Gym Logo on ID Card", showLogo, isAdmin) { showLogo = it }
            SettingsToggleRow("Show Locker Number on ID Card", showLocker, isAdmin) { showLocker = it }
            SettingsToggleRow("Show Assigned Coach on ID Card", showCoach, isAdmin) { showCoach = it }

            Button(
                onClick = {
                    val startNum = (startingIdText.toLongOrNull() ?: 1728L).coerceAtLeast(1L)
                    val nextNum = (nextIdText.toLongOrNull() ?: startNum).coerceAtLeast(startNum)
                    onSaveIdSettings(
                        gymSettings.copy(
                            memberIdPrefix = cleanPrefix,
                            startingMemberId = startNum,
                            nextMemberId = nextNum,
                            idCardTagline = tagline.trim().ifBlank { "SINCE 1986 • OFFICIAL DIGITAL PASS" },
                            idCardShowLogo = showLogo,
                            idCardShowLocker = showLocker,
                            idCardShowCoach = showCoach
                        )
                    )
                },
                enabled = isAdmin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TopBarTurquoise),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("gym_save_id_settings_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save ID & Card Settings")
            }
        }
    }
}

@Composable
fun NotificationsSettingsCard(
    notificationSettings: NotificationSettings,
    isAdmin: Boolean,
    onSaveNotifications: (NotificationSettings) -> Unit
) {
    var d15 by remember(notificationSettings.expiry15Days) { mutableStateOf(notificationSettings.expiry15Days) }
    var d7 by remember(notificationSettings.expiry7Days) { mutableStateOf(notificationSettings.expiry7Days) }
    var d3 by remember(notificationSettings.expiry3Days) { mutableStateOf(notificationSettings.expiry3Days) }
    var d1 by remember(notificationSettings.expiry1Day) { mutableStateOf(notificationSettings.expiry1Day) }
    var expired by remember(notificationSettings.expiredReminder) { mutableStateOf(notificationSettings.expiredReminder) }
    var whatsappEnabled by remember(notificationSettings.whatsappEnabled) { mutableStateOf(notificationSettings.whatsappEnabled) }
    var expiringTpl by remember(notificationSettings.expiringTemplate) { mutableStateOf(notificationSettings.expiringTemplate) }
    var expiredTpl by remember(notificationSettings.expiredTemplate) { mutableStateOf(notificationSettings.expiredTemplate) }
    var renewalTpl by remember(notificationSettings.renewalTemplate) { mutableStateOf(notificationSettings.renewalTemplate) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "4. NOTIFICATIONS & WHATSAPP REMINDERS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            SettingsToggleRow("15 Days Before Expiry Alert", d15, isAdmin) { d15 = it }
            SettingsToggleRow("7 Days Before Expiry Alert", d7, isAdmin) { d7 = it }
            SettingsToggleRow("3 Days Before Expiry Alert", d3, isAdmin) { d3 = it }
            SettingsToggleRow("1 Day Before Expiry Alert", d1, isAdmin) { d1 = it }
            SettingsToggleRow("After Expiry Follow-Up Alert", expired, isAdmin) { expired = it }
            HorizontalDivider(color = BorderSubtle)
            SettingsToggleRow("Enable WhatsApp Reminder Launch", whatsappEnabled, isAdmin) { whatsappEnabled = it }

            Text(
                text = "Placeholders: {memberName}, {memberId}, {expiryDate}, {amount}, {gymName}",
                style = MaterialTheme.typography.labelSmall,
                color = TurquoiseText
            )

            OutlinedTextField(
                value = expiringTpl,
                onValueChange = { expiringTpl = it },
                enabled = isAdmin,
                label = { Text("Expiring Reminder Template") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = expiredTpl,
                onValueChange = { expiredTpl = it },
                enabled = isAdmin,
                label = { Text("Expired Reminder Template") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = renewalTpl,
                onValueChange = { renewalTpl = it },
                enabled = isAdmin,
                label = { Text("Renewal Confirmation Template") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSaveNotifications(
                        notificationSettings.copy(
                            expiry15Days = d15,
                            expiry7Days = d7,
                            expiry3Days = d3,
                            expiry1Day = d1,
                            expiredReminder = expired,
                            whatsappEnabled = whatsappEnabled,
                            expiringTemplate = expiringTpl.trim(),
                            expiredTemplate = expiredTpl.trim(),
                            renewalTemplate = renewalTpl.trim()
                        )
                    )
                },
                enabled = isAdmin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TopBarTurquoise),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("gym_save_notifications_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Reminder & WhatsApp Settings")
            }
        }
    }
}

@Composable
fun PaymentSettingsCard(
    paymentSettings: PaymentSettings,
    isAdmin: Boolean,
    onUploadUpiQr: (ByteArray) -> Unit,
    onRemoveUpiQr: () -> Unit,
    onSavePaymentSettings: (PaymentSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var acceptCash by remember(paymentSettings.acceptCash) { mutableStateOf(paymentSettings.acceptCash) }
    var acceptUpi by remember(paymentSettings.acceptUpi) { mutableStateOf(paymentSettings.acceptUpi) }
    var acceptOnline by remember(paymentSettings.acceptOnline) { mutableStateOf(paymentSettings.acceptOnline) }
    var upiId by remember(paymentSettings.upiId) { mutableStateOf(paymentSettings.upiId) }
    var instructions by remember(paymentSettings.paymentInstructions) { mutableStateOf(paymentSettings.paymentInstructions) }
    var onlineEnabled by remember(paymentSettings.onlinePaymentEnabled) { mutableStateOf(paymentSettings.onlinePaymentEnabled) }
    var gatewayLabel by remember(paymentSettings.onlineGatewayLabel) { mutableStateOf(paymentSettings.onlineGatewayLabel) }

    val qrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                validateAndPrepareMemberPhoto(context, uri)
                    .onSuccess { prepared -> onUploadUpiQr(prepared.bytes) }
                    .onFailure { err ->
                        Toast.makeText(context, err.message ?: "Invalid QR image", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

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
                text = "5. PAYMENT & UPI SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            SettingsToggleRow("Accept Cash Payments", acceptCash, isAdmin) { acceptCash = it }
            SettingsToggleRow("Accept UPI / QR Payments", acceptUpi, isAdmin) { acceptUpi = it }
            SettingsToggleRow("Accept Online Payments", acceptOnline, isAdmin) { acceptOnline = it }

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                enabled = isAdmin,
                label = { Text("Official Gym UPI ID") },
                placeholder = { Text("e.g. ekta47gym@upi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // UPI QR Preview + Upload/Remove
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCanvas)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (paymentSettings.upiQrCodeUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(paymentSettings.upiQrCodeUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Official UPI QR Code",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoiseSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = TurquoiseText, modifier = Modifier.size(30.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (paymentSettings.upiQrCodeUrl.isNotBlank()) "UPI QR Code Uploaded" else "No UPI QR Code Uploaded",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                qrPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = isAdmin,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("gym_upload_qr_button")
                        ) {
                            Text(
                                text = if (paymentSettings.upiQrCodeUrl.isNotBlank()) "Replace QR" else "Upload QR",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (paymentSettings.upiQrCodeUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = onRemoveUpiQr,
                                enabled = isAdmin,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, StatusExpiredAccent.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove QR", tint = StatusExpiredText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                enabled = isAdmin,
                label = { Text("Desk & Member Payment Instructions") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            SettingsToggleRow("Enable External Online Reference Tracking", onlineEnabled, isAdmin) {
                onlineEnabled = it
            }
            if (onlineEnabled) {
                OutlinedTextField(
                    value = gatewayLabel,
                    onValueChange = { gatewayLabel = it },
                    enabled = isAdmin,
                    label = { Text("Online Payment Label (Public Display Only)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "For security, private payment gateway secret keys are never stored in the mobile app.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Button(
                onClick = {
                    onSavePaymentSettings(
                        paymentSettings.copy(
                            acceptCash = acceptCash,
                            acceptUpi = acceptUpi,
                            acceptOnline = acceptOnline,
                            upiId = upiId.trim(),
                            paymentInstructions = instructions.trim(),
                            onlinePaymentEnabled = onlineEnabled,
                            onlineGatewayLabel = gatewayLabel.trim().ifBlank { "External Gateway (Server-Verified)" }
                        )
                    )
                },
                enabled = isAdmin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TopBarTurquoise),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("gym_save_payment_settings_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Payment Settings")
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) TextPrimary else TextMuted,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TurquoisePrimary,
                uncheckedTrackColor = InputBorder
            )
        )
    }
}
