package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.MembershipPlan
import com.example.model.PaymentMethod
import com.example.model.PaymentSettings
import com.example.model.formatMajorAmountInput
import com.example.model.formatMoneyCents
import com.example.model.parseMajorCurrencyToCents
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.InputBorder
import com.example.ui.theme.RoyalBlueAccent
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

@Composable
fun MembershipPlanEditorDialog(
    existingPlan: MembershipPlan?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        durationMonths: Long,
        priceCents: Long,
        description: String,
        isActive: Boolean
    ) -> Unit
) {
    var name by remember(existingPlan?.id) { mutableStateOf(existingPlan?.name.orEmpty()) }
    var durationText by remember(existingPlan?.id) {
        mutableStateOf((existingPlan?.durationMonths ?: 1L).toString())
    }
    var priceText by remember(existingPlan?.id) {
        mutableStateOf(
            if (existingPlan != null) formatMajorAmountInput(existingPlan.priceCents) else "1500"
        )
    }
    var description by remember(existingPlan?.id) {
        mutableStateOf(existingPlan?.description.orEmpty())
    }
    var isActive by remember(existingPlan?.id) {
        mutableStateOf(existingPlan?.isActive ?: true)
    }

    val parsedDuration = durationText.toLongOrNull()
    val parsedPriceCents = parseMajorCurrencyToCents(priceText)
    val durationValid = parsedDuration != null && parsedDuration in 1L..60L
    val priceValid = parsedPriceCents != null && parsedPriceCents >= 0L
    val canSave = name.trim().length >= 2 && durationValid && priceValid

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceCard,
        title = {
            Column {
                Text(
                    text = "MEMBERSHIP PLANS & PRICING",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoisePrimary
                )
                Text(
                    text = if (existingPlan == null) "Add Membership Plan" else "Edit Membership Plan",
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plan Name") },
                    placeholder = { Text("e.g. Quarterly Strength") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_editor_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Duration (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = durationText.isNotEmpty() && !durationValid,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_editor_duration_input")
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Fee ($currencyCode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = priceText.isNotEmpty() && !priceValid,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("plan_editor_price_input")
                    )
                }

                if (parsedPriceCents != null && parsedDuration != null && durationValid) {
                    Text(
                        text = "Live Preview: ${name.ifBlank { "Plan" }} • ${if (parsedDuration == 1L) "1 Month" else "$parsedDuration Months"} • ${formatMoneyCents(parsedPriceCents, currencyCode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TurquoiseText,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Enter 1–60 months and a valid fee amount.",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusExpiredText
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Plan Description & Inclusions") },
                    placeholder = { Text("e.g. Gym floor + cardio + locker access") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_editor_desc_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Plan Active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Only active plans appear during member enrollment and renewal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
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
                    val months = parsedDuration ?: 1L
                    val cents = parsedPriceCents ?: 0L
                    onConfirm(name.trim(), months, cents, description.trim(), isActive)
                    onDismiss()
                },
                enabled = canSave,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("plan_editor_save_button")
            ) {
                Text(if (existingPlan == null) "Create Plan" else "Save Plan")
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
fun PaymentMethodSelectorSection(
    selectedMethod: PaymentMethod,
    onSelectMethod: (PaymentMethod) -> Unit,
    transactionRef: String,
    onTransactionRefChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    paymentSettings: PaymentSettings?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val acceptedMethods = remember(paymentSettings) {
        paymentSettings?.acceptedMethods ?: listOf(PaymentMethod.CASH, PaymentMethod.UPI)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "PAYMENT METHOD",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            acceptedMethods.forEach { method ->
                val selected = selectedMethod == method
                val icon = when (method) {
                    PaymentMethod.CASH -> Icons.Rounded.Payments
                    PaymentMethod.UPI -> Icons.Rounded.QrCode2
                    PaymentMethod.ONLINE -> Icons.Rounded.AccountBalance
                }
                FilterChip(
                    selected = selected,
                    onClick = { onSelectMethod(method) },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = {
                        Text(
                            text = when (method) {
                                PaymentMethod.CASH -> "Cash"
                                PaymentMethod.UPI -> "UPI"
                                PaymentMethod.ONLINE -> "Online"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TopBarTurquoise,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }

        if (selectedMethod == PaymentMethod.UPI && paymentSettings != null) {
            val hasUpiDetails = paymentSettings.upiId.isNotBlank() || paymentSettings.upiQrCodeUrl.isNotBlank()
            if (hasUpiDetails) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TurquoiseSoft.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (paymentSettings.upiQrCodeUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(paymentSettings.upiQrCodeUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Gym UPI QR Code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (paymentSettings.upiId.isNotBlank()) {
                                Text(
                                    text = "UPI ID: ${paymentSettings.upiId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TurquoiseText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (paymentSettings.paymentInstructions.isNotBlank()) {
                                Text(
                                    text = paymentSettings.paymentInstructions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        } else if (selectedMethod == PaymentMethod.ONLINE && paymentSettings != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${paymentSettings.onlineGatewayLabel}: Record confirmed online settlement reference below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        OutlinedTextField(
            value = transactionRef,
            onValueChange = onTransactionRefChange,
            label = {
                Text(
                    when (selectedMethod) {
                        PaymentMethod.UPI -> "UPI UTR / Transaction ID"
                        PaymentMethod.ONLINE -> "Online Payment Reference"
                        PaymentMethod.CASH -> "Receipt / Register Ref (optional)"
                    }
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Payment / Discount Note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
