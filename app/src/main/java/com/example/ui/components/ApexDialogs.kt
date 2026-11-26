package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.media.createTempCameraImageUri
import com.example.media.loadImageBitmap
import com.example.media.loadMemberPhotoBitmap
import com.example.media.saveMemberIdCardToGallery
import com.example.media.shareMemberIdCard
import com.example.media.validateAndPrepareMemberPhoto
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipPlan
import com.example.model.MembershipStatus
import com.example.model.MembershipTier
import com.example.model.PaymentMethod
import com.example.model.PaymentSettings
import com.example.model.computeNextSequentialMemberCode
import com.example.model.formatMajorAmountInput
import com.example.model.formatMoneyCents
import com.example.model.parseMajorCurrencyToCents
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EktaNavy
import com.example.ui.theme.InputBorder
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.StatusExpiredText
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TopBarTurquoise
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.TurquoiseSoft
import com.example.ui.theme.TurquoiseText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PassKeypadDialog(
    members: List<GymMember>,
    onDismiss: () -> Unit,
    onCheckInCode: (String) -> Unit
) {
    var digits by remember { mutableStateOf("") }
    val codePreview = if (digits.isEmpty()) "#____" else "#$digits"
    val matchingMember = remember(digits, members) {
        if (digits.length < 2) null
        else members.firstOrNull { member ->
            val clean = member.memberCode.removePrefix("#").removePrefix("APX-")
            clean.startsWith(digits, ignoreCase = true) || clean.equals(digits, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceCard,
        title = {
            Column {
                Text(
                    text = "ATTENDANCE CHECK-IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoisePrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter Member ID",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TopBarTurquoise,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = codePreview,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = matchingMember?.let {
                                "${it.fullName} • ${it.statusEnum.label}"
                            } ?: "e.g. 1727, 1671, 1670",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLR", "0", "DEL")
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceCanvas)
                                        .clickable {
                                            when (key) {
                                                "CLR" -> digits = ""
                                                "DEL" -> if (digits.isNotEmpty()) digits = digits.dropLast(1)
                                                else -> if (digits.length < 6) digits += key
                                            }
                                        }
                                        .testTag("keypad_key_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                            contentDescription = "Backspace",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (key == "CLR") TextMuted else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = matchingMember?.memberCode ?: digits
                    onCheckInCode(target)
                    onDismiss()
                },
                enabled = digits.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("keypad_checkin_confirm")
            ) {
                Icon(
                    imageVector = Icons.Rounded.HowToReg,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MemberPhotoPickerSection(
    existingPhotoUrl: String?,
    selectedPhotoBytes: ByteArray?,
    removedExisting: Boolean,
    initials: String,
    isProcessing: Boolean,
    errorMessage: String?,
    onProcessingChange: (Boolean) -> Unit,
    onPhotoSelected: (ByteArray) -> Unit,
    onRemovePhoto: () -> Unit,
    onValidationError: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val handlePickedUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch {
                onProcessingChange(true)
                onValidationError(null)
                validateAndPrepareMemberPhoto(context, uri)
                    .onSuccess { prepared ->
                        onPhotoSelected(prepared.bytes)
                    }
                    .onFailure { err ->
                        onValidationError(err.message ?: "Unable to process selected image.")
                    }
                onProcessingChange(false)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        handlePickedUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = pendingCameraUri
        pendingCameraUri = null
        if (success && capturedUri != null) {
            handlePickedUri(capturedUri)
        }
    }

    val launchCameraCapture: () -> Unit = {
        val uri = createTempCameraImageUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            onValidationError("Camera permission is required to take a member photo.")
        }
    }

    val displayedUrl = if (removedExisting) null else existingPhotoUrl
    val hasPhoto = selectedPhotoBytes != null || !displayedUrl.isNullOrBlank()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCanvas,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROFILE PHOTO",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "JPG / PNG / WEBP • Max 5 MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Box(contentAlignment = Alignment.Center) {
                MemberPhotoAvatar(
                    photoUrl = displayedUrl,
                    localPhotoBytes = selectedPhotoBytes,
                    initials = initials,
                    size = 84.dp,
                    borderColor = TurquoisePrimary,
                    borderWidth = 2.dp,
                    showPersonPlaceholderWhenEmpty = true,
                    contentDescription = "Member profile photo preview"
                )
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = TurquoisePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onValidationError(null)
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("photo_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        onValidationError(null)
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            launchCameraCapture()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("photo_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Camera", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }

                if (hasPhoto) {
                    OutlinedButton(
                        onClick = {
                            onValidationError(null)
                            onRemovePhoto()
                        },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StatusExpiredAccent.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("photo_remove_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove photo",
                            tint = StatusExpiredText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusExpiredText
                )
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    existingMembers: List<GymMember>,
    plans: List<MembershipPlan>,
    gymSettings: GymSettings?,
    paymentSettings: PaymentSettings?,
    isAdmin: Boolean,
    trainerNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (
        memberCode: String,
        fullName: String,
        phone: String,
        email: String?,
        selectedPlan: MembershipPlan?,
        fallbackTier: MembershipTier,
        daysRemaining: Long,
        chargedAmountCents: Long?,
        discountCents: Long,
        paymentMethod: PaymentMethod,
        transactionRef: String,
        notes: String,
        lockerNumber: String?,
        trainerName: String?,
        autoRenew: Boolean,
        photoBytes: ByteArray?
    ) -> Unit
) {
    val activePlans = remember(plans) { plans.filter { it.isActive } }
    val currency = gymSettings?.currency ?: "INR"
    val autoMemberId = remember(gymSettings, existingMembers) {
        computeNextSequentialMemberCode(gymSettings, existingMembers)
    }

    var fullName by remember { mutableStateOf("") }
    var memberCode by remember(autoMemberId) { mutableStateOf(autoMemberId) }
    var phone by remember { mutableStateOf("+91 ") }
    var email by remember { mutableStateOf("") }
    var locker by remember { mutableStateOf("L-${(10..59).random()}") }
    var selectedPlan by remember(activePlans) {
        mutableStateOf(activePlans.firstOrNull())
    }
    var selectedTier by remember { mutableStateOf(MembershipTier.PERFORMANCE) }
    var selectedTrainer by remember { mutableStateOf(trainerNames.firstOrNull() ?: "") }
    var daysText by remember(activePlans) {
        mutableStateOf((activePlans.firstOrNull()?.durationDays ?: 30L).toString())
    }
    var feeText by remember(activePlans) {
        val defaultCents = activePlans.firstOrNull()?.priceCents
            ?: MembershipTier.PERFORMANCE.defaultFeeCents.toLong()
        mutableStateOf(formatMajorAmountInput(defaultCents))
    }
    var paymentMethod by remember(paymentSettings) {
        mutableStateOf(paymentSettings?.acceptedMethods?.firstOrNull() ?: PaymentMethod.CASH)
    }
    var transactionRef by remember { mutableStateOf("") }
    var paymentNotes by remember { mutableStateOf("") }
    var autoRenew by remember { mutableStateOf(true) }
    var selectedPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }

    val baseFeeCents = selectedPlan?.priceCents ?: selectedTier.defaultFeeCents.toLong()
    val parsedChargedCents = parseMajorCurrencyToCents(feeText)
    val effectiveChargedCents = parsedChargedCents ?: baseFeeCents
    val discountCents = (baseFeeCents - effectiveChargedCents).coerceAtLeast(0L)

    val cleanCode = memberCode.trim().removePrefix("#")
    val isDuplicateId = remember(cleanCode, existingMembers) {
        cleanCode.isNotEmpty() && existingMembers.any {
            it.memberCode.trim().removePrefix("#").equals(cleanCode, ignoreCase = true)
        }
    }

    val previewInitials = remember(fullName) {
        fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
    }

    val canSubmit = !isProcessingPhoto &&
        fullName.trim().length >= 2 &&
        cleanCode.length >= 2 &&
        !isDuplicateId &&
        phone.trim().length >= 5 &&
        parsedChargedCents != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceCard,
        title = {
            Column {
                Text(
                    text = "${gymSettings?.gymName?.takeIf { it.isNotBlank() } ?: "EKTA 47 GYM"} • REGISTRATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoisePrimary
                )
                Text(
                    text = "Add Member",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MemberPhotoPickerSection(
                    existingPhotoUrl = null,
                    selectedPhotoBytes = selectedPhotoBytes,
                    removedExisting = false,
                    initials = previewInitials,
                    isProcessing = isProcessingPhoto,
                    errorMessage = photoError,
                    onProcessingChange = { isProcessingPhoto = it },
                    onPhotoSelected = { bytes -> selectedPhotoBytes = bytes },
                    onRemovePhoto = { selectedPhotoBytes = null },
                    onValidationError = { photoError = it }
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = memberCode,
                        onValueChange = { memberCode = it },
                        label = { Text("Member ID (Auto)") },
                        singleLine = true,
                        isError = isDuplicateId,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_member_id_input")
                    )
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(88.dp)
                    )
                }
                if (isDuplicateId) {
                    Text(
                        text = "Member ID #$cleanCode already exists. Enter a unique ID.",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusExpiredText
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile / WhatsApp") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = locker,
                        onValueChange = { locker = it },
                        label = { Text("Locker") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = selectedTrainer,
                        onValueChange = { selectedTrainer = it },
                        label = { Text("Trainer") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }

                Text(
                    text = "SELECT ACTIVE MEMBERSHIP PLAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                if (activePlans.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activePlans.forEach { plan ->
                            val isSelected = selectedPlan?.id == plan.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TurquoiseSoft else SurfaceCanvas,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) TurquoisePrimary else BorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedPlan = plan
                                        selectedTier = plan.legacyTier
                                        daysText = plan.durationDays.toString()
                                        feeText = formatMajorAmountInput(plan.priceCents)
                                    }
                                    .testTag("add_member_plan_${plan.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) TurquoiseText else TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${plan.durationDisplayLabel} (${plan.durationDays}d)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Text(
                                        text = formatMoneyCents(plan.priceCents, currency),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) TurquoiseText else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MembershipTier.entries.forEach { tier ->
                            FilterChip(
                                selected = selectedTier == tier,
                                onClick = {
                                    selectedTier = tier
                                    feeText = formatMajorAmountInput(tier.defaultFeeCents.toLong())
                                },
                                label = {
                                    Text(
                                        text = "${tier.planTitle} (${formatMoneyCents(tier.defaultFeeCents.toLong(), currency)})",
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

                // Editable Fee / Discount section
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { if (isAdmin) feeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    enabled = isAdmin,
                    label = {
                        Text(
                            if (isAdmin) "Collected Fee ($currency — edit for discount)"
                            else "Plan Fee ($currency — Admin price)"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = parsedChargedCents == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_fee_input")
                )
                if (discountCents > 0L) {
                    Text(
                        text = "Standard: ${formatMoneyCents(baseFeeCents, currency)} • Discount applied: ${formatMoneyCents(discountCents, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusActiveText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                PaymentMethodSelectorSection(
                    selectedMethod = paymentMethod,
                    onSelectMethod = { paymentMethod = it },
                    transactionRef = transactionRef,
                    onTransactionRefChange = { transactionRef = it },
                    notes = paymentNotes,
                    onNotesChange = { paymentNotes = it },
                    paymentSettings = paymentSettings
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Renew Billing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = autoRenew,
                        onCheckedChange = { autoRenew = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TurquoisePrimary,
                            uncheckedTrackColor = InputBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val planSnapshot = selectedPlan
                    val tierFallback = planSnapshot?.legacyTier ?: selectedTier
                    onConfirm(
                        cleanCode,
                        fullName,
                        phone,
                        email.ifBlank { null },
                        planSnapshot,
                        tierFallback,
                        daysText.toLongOrNull() ?: planSnapshot?.durationDays ?: 30L,
                        effectiveChargedCents,
                        discountCents,
                        paymentMethod,
                        transactionRef,
                        paymentNotes,
                        locker.ifBlank { null },
                        selectedTrainer.ifBlank { null },
                        autoRenew,
                        selectedPhotoBytes
                    )
                    onDismiss()
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_member_submit_button")
            ) {
                Text("Add & Collect ${formatMoneyCents(effectiveChargedCents, currency)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditMemberPhotoDialog(
    member: GymMember,
    onDismiss: () -> Unit,
    onSavePhoto: (ByteArray?) -> Unit
) {
    var selectedPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var removedExisting by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }

    val hasChanges = selectedPhotoBytes != null || (removedExisting && member.hasProfilePhoto)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceCard,
        title = {
            Column {
                Text(
                    text = "MEMBER PROFILE PHOTO",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoisePrimary
                )
                Text(
                    text = "${member.fullName} (${member.displayMemberId})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        },
        text = {
            MemberPhotoPickerSection(
                existingPhotoUrl = member.profilePhotoUrl,
                selectedPhotoBytes = selectedPhotoBytes,
                removedExisting = removedExisting,
                initials = member.initials,
                isProcessing = isProcessing,
                errorMessage = photoError,
                onProcessingChange = { isProcessing = it },
                onPhotoSelected = { bytes ->
                    selectedPhotoBytes = bytes
                    removedExisting = false
                },
                onRemovePhoto = {
                    selectedPhotoBytes = null
                    removedExisting = true
                },
                onValidationError = { photoError = it }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePhoto(selectedPhotoBytes)
                    onDismiss()
                },
                enabled = !isProcessing && hasChanges,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("save_member_photo_button")
            ) {
                Text(if (removedExisting && selectedPhotoBytes == null) "Remove Photo" else "Save Photo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenewMembershipDialog(
    member: GymMember,
    plans: List<MembershipPlan>,
    gymSettings: GymSettings?,
    paymentSettings: PaymentSettings?,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        selectedPlan: MembershipPlan?,
        fallbackTier: MembershipTier,
        additionalDays: Long,
        chargedAmountCents: Long?,
        discountCents: Long,
        paymentMethod: PaymentMethod,
        transactionRef: String,
        notes: String,
        autoRenew: Boolean
    ) -> Unit
) {
    val activePlans = remember(plans) { plans.filter { it.isActive } }
    val currency = gymSettings?.currency ?: "INR"

    val matchedPlan = remember(activePlans, member.planId, member.tier) {
        activePlans.firstOrNull { it.id == member.planId }
            ?: activePlans.firstOrNull { it.legacyTier == member.tierEnum }
            ?: activePlans.firstOrNull()
    }

    var selectedPlan by remember(matchedPlan) { mutableStateOf(matchedPlan) }
    var selectedTier by remember { mutableStateOf(member.tierEnum) }
    var selectedDays by remember(matchedPlan) {
        mutableStateOf(matchedPlan?.durationDays ?: 30L)
    }
    var feeText by remember(matchedPlan) {
        val defaultFee = matchedPlan?.priceCents ?: member.monthlyFeeCents
        mutableStateOf(formatMajorAmountInput(defaultFee))
    }
    var paymentMethod by remember(paymentSettings) {
        mutableStateOf(paymentSettings?.acceptedMethods?.firstOrNull() ?: PaymentMethod.CASH)
    }
    var transactionRef by remember { mutableStateOf("") }
    var paymentNotes by remember { mutableStateOf("") }
    var autoRenew by remember { mutableStateOf(member.autoRenew) }

    val baseFeeCents = selectedPlan?.priceCents ?: selectedTier.defaultFeeCents.toLong()
    val parsedFeeCents = parseMajorCurrencyToCents(feeText)
    val effectiveFeeCents = parsedFeeCents ?: baseFeeCents
    val discountCents = (baseFeeCents - effectiveFeeCents).coerceAtLeast(0L)

    val newTotalDays = (if (member.daysRemaining > 0L) member.daysRemaining else 0L) + selectedDays
    val newExpiryFormatted = remember(newTotalDays) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, newTotalDays.toInt())
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceCard,
        title = {
            Column {
                Text(
                    text = "RENEW PLAN / COLLECT FEE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoisePrimary
                )
                Text(
                    text = "${member.fullName} (${member.displayMemberId})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceSubtle,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Current: ${member.displayPlanName} • ${member.statusEnum.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Current Expiry: ${member.expiryDateFormatted}  →  New Expiry: $newExpiryFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "SELECT ACTIVE PLAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                if (activePlans.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activePlans.forEach { plan ->
                            val selected = selectedPlan?.id == plan.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) TurquoiseSoft else SurfaceCanvas,
                                border = BorderStroke(1.dp, if (selected) TurquoisePrimary else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedPlan = plan
                                        selectedTier = plan.legacyTier
                                        selectedDays = plan.durationDays
                                        feeText = formatMajorAmountInput(plan.priceCents)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) TurquoiseText else TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = plan.durationDisplayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Text(
                                        text = formatMoneyCents(plan.priceCents, currency),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) TurquoiseText else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MembershipTier.entries.forEach { tier ->
                            val selected = selectedTier == tier
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) TurquoiseSoft else SurfaceCanvas,
                                border = BorderStroke(1.dp, if (selected) TurquoisePrimary else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedTier = tier
                                        feeText = formatMajorAmountInput(tier.defaultFeeCents.toLong())
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tier.planTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) TurquoiseText else TextPrimary
                                    )
                                    Text(
                                        text = formatMoneyCents(tier.defaultFeeCents.toLong(), currency),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "EXTENSION DURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                val durationChoices = remember(selectedPlan) {
                    val planDays = selectedPlan?.durationDays
                    buildList {
                        if (planDays != null && planDays !in listOf(30L, 90L, 180L, 365L)) {
                            add(planDays to "${planDays}d Plan")
                        }
                        add(30L to "30d")
                        add(90L to "90d")
                        add(180L to "180d")
                        add(365L to "365d")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationChoices.forEach { (days, label) ->
                        FilterChip(
                            selected = selectedDays == days,
                            onClick = { selectedDays = days },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TopBarTurquoise,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = feeText,
                    onValueChange = { if (isAdmin) feeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    enabled = isAdmin,
                    label = {
                        Text(
                            if (isAdmin) "Renewal Fee ($currency — edit for discount)"
                            else "Renewal Fee ($currency — Admin price)"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = parsedFeeCents == null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (discountCents > 0L) {
                    Text(
                        text = "Standard: ${formatMoneyCents(baseFeeCents, currency)} • Discount: ${formatMoneyCents(discountCents, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusActiveText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                PaymentMethodSelectorSection(
                    selectedMethod = paymentMethod,
                    onSelectMethod = { paymentMethod = it },
                    transactionRef = transactionRef,
                    onTransactionRefChange = { transactionRef = it },
                    notes = paymentNotes,
                    onNotesChange = { paymentNotes = it },
                    paymentSettings = paymentSettings
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Auto-Renew",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = autoRenew,
                        onCheckedChange = { autoRenew = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TurquoisePrimary,
                            uncheckedTrackColor = InputBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tierFallback = selectedPlan?.legacyTier ?: selectedTier
                    onConfirm(
                        selectedPlan,
                        tierFallback,
                        selectedDays,
                        effectiveFeeCents,
                        discountCents,
                        paymentMethod,
                        transactionRef,
                        paymentNotes,
                        autoRenew
                    )
                    onDismiss()
                },
                enabled = parsedFeeCents != null,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("renew_confirm_button")
            ) {
                Text("Collect ${formatMoneyCents(effectiveFeeCents, currency)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddTrainerDialog(
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, specialty: String, phone: String, activeClients: Long) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("Strength & Conditioning") }
    var phone by remember { mutableStateOf("+91 ") }
    var clientsText by remember { mutableStateOf("8") }

    val canSubmit = fullName.trim().length >= 2 &&
        specialty.trim().length >= 2 &&
        phone.trim().length >= 5

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceCard,
        title = {
            Column {
                Text(
                    text = "GYM STAFF ROSTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoisePrimary
                )
                Text(
                    text = "Add Staff / Trainer",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Staff / Trainer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Role / Specialty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = clientsText,
                        onValueChange = { clientsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Clients") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        fullName,
                        specialty,
                        phone,
                        clientsText.toLongOrNull() ?: 0L
                    )
                    onDismiss()
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Add Staff")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MemberIdCardDialog(
    member: GymMember,
    gymSettings: GymSettings? = null,
    onDismiss: () -> Unit,
    onRecordAttendance: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    val gymName = gymSettings?.gymName?.takeIf { it.isNotBlank() } ?: "EKTA 47 GYM & FITNESS CENTRE"
    val tagline = gymSettings?.idCardTagline?.takeIf { it.isNotBlank() } ?: "Digital Member ID Card • Since 1986"
    val showLogo = gymSettings?.idCardShowLogo ?: true
    val showLocker = gymSettings?.idCardShowLocker ?: true
    val showCoach = gymSettings?.idCardShowCoach ?: true

    val validityLine = remember(member.createdAt, member.daysRemaining) {
        if (member.daysRemaining < 0) {
            "Expired on ${member.expiryDateFormatted}"
        } else {
            "${member.startDateFormatted} → ${member.expiryDateFormatted} (${member.daysRemaining}d)"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        containerColor = SurfaceCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showLogo) {
                    EktaBrandMark(
                        size = 46.dp,
                        useWhiteSurface = true,
                        customLogoUrl = gymSettings?.logoUrl?.takeIf { it.isNotBlank() } ?: ActiveGymBranding.logoUrl
                    )
                }
                Column {
                    Text(
                        text = gymName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TurquoiseText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceSubtle,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberPhotoAvatar(
                                photoUrl = member.profilePhotoUrl,
                                initials = member.initials,
                                size = 66.dp,
                                borderColor = TurquoisePrimary,
                                borderWidth = 2.dp,
                                contentDescription = "${member.fullName} ID card photo"
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Member ID: ${member.displayMemberId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TurquoiseText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusPill(status = member.statusEnum)
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Membership Plan: ${member.displayPlanName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Validity: $validityLine",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Mobile: ${member.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            val metaParts = buildList {
                                if (showLocker) {
                                    add("Locker: ${member.lockerNumber?.ifBlank { "Unassigned" } ?: "Unassigned"}")
                                }
                                if (showCoach) {
                                    add("Coach: ${member.trainerName?.ifBlank { "Floor Coach" } ?: "Floor Coach"}")
                                }
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "Gym: $gymName",
                                style = MaterialTheme.typography.labelSmall,
                                color = EktaNavy,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Download & Share Digital ID Card buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val photoBitmap = loadMemberPhotoBitmap(context, member.profilePhotoUrl)
                                val logoBitmap = loadImageBitmap(context, gymSettings?.logoUrl)
                                saveMemberIdCardToGallery(context, member, photoBitmap, logoBitmap, gymSettings)
                                    .onSuccess { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                    .onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            err.message ?: "Failed to save ID card",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                isExporting = false
                            }
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("id_card_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val photoBitmap = loadMemberPhotoBitmap(context, member.profilePhotoUrl)
                                val logoBitmap = loadImageBitmap(context, gymSettings?.logoUrl)
                                shareMemberIdCard(context, member, photoBitmap, logoBitmap, gymSettings)
                                    .onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            err.message ?: "Unable to share ID card",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                isExporting = false
                            }
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("id_card_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRecordAttendance()
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Record Attendance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun QuickHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceCard,
        title = {
            Text(
                text = "EKTA 47 GYM Quick Guide",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "• Members: Search by name, mobile, or toggle 'ID' to match exact Member IDs (#1670, #1671, #1727). New members automatically receive the next sequential ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "• Dashboard: Tap any Today, Monthly Attendance, or Expiry card to view filtered members or attendance logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "• Reports: Switch between Trends, Sales, Plan Due, PT Plan Due, and Collection with Graph/Table toggle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TopBarTurquoise)
            ) {
                Text("Got it")
            }
        }
    )
}
