package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class MembershipTier(
    val label: String,
    val planTitle: String,
    val defaultFeeCents: Int,
    val durationLabel: String
) {
    BLACK_CARD("BLACK CARD", "Annual – 12 Months", 1350000, "12 Months"),
    PERFORMANCE("PERFORMANCE", "Half-Yearly – 6 Months", 750000, "6 Months"),
    FOUNDATION("FOUNDATION", "Quarterly – 3 Months", 420000, "3 Months");

    companion object {
        fun fromRaw(raw: String?): MembershipTier =
            entries.firstOrNull { it.name == raw } ?: FOUNDATION

        fun forDurationMonths(months: Long): MembershipTier = when {
            months >= 12L -> BLACK_CARD
            months >= 6L -> PERFORMANCE
            else -> FOUNDATION
        }
    }
}

enum class MembershipStatus(val label: String) {
    ACTIVE("ACTIVE"),
    EXPIRING("EXPIRING"),
    EXPIRED("EXPIRED");

    companion object {
        fun fromRaw(raw: String?): MembershipStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class CheckInOutcome(val label: String) {
    GRANTED("GRANTED"),
    WARNING("GRACE CHECK-IN"),
    DENIED("DENIED");

    companion object {
        fun fromRaw(raw: String?): CheckInOutcome =
            entries.firstOrNull { it.name == raw } ?: GRANTED
    }
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    ONLINE("Online Payment");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class StaffAccessRole(val label: String) {
    ADMIN("Administrator"),
    STAFF("Front Desk Staff");

    companion object {
        fun fromRaw(raw: String?): StaffAccessRole =
            entries.firstOrNull { it.name == raw } ?: ADMIN
    }
}

@Keep
@IgnoreExtraProperties
data class FacilityProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val facilityName: String = "EKTA 47 GYM",
    val managerName: String = "Facility Administrator",
    val seeded: Boolean = false,
    val dailyCheckInTarget: Long = 180L,
    val activeRole: String = StaffAccessRole.ADMIN.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val normalizedFacilityName: String
        get() = normalizeFacilityName(facilityName)

    val roleEnum: StaffAccessRole
        get() = StaffAccessRole.fromRaw(activeRole)
}

fun normalizeFacilityName(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (trimmed.isEmpty() ||
        trimmed.contains("apex", ignoreCase = true) ||
        trimmed.contains("athletic engine", ignoreCase = true)
    ) {
        "EKTA 47 GYM"
    } else {
        trimmed
    }
}

@Keep
@IgnoreExtraProperties
data class GymSettings(
    @DocumentId val id: String = "default",
    val gymName: String = "EKTA 47 GYM & FITNESS CENTRE",
    val logoUrl: String = "",
    val logoPath: String = "",
    val adminName: String = "Facility Administrator",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val address: String = "",
    val openingHours: String = "Mon–Sat: 5:30 AM – 10:00 PM",
    val currency: String = "INR",
    val memberIdPrefix: String = "",
    val startingMemberId: Long = 1728L,
    val nextMemberId: Long = 1728L,
    val idCardTagline: String = "SINCE 1986 • OFFICIAL DIGITAL PASS",
    val idCardShowLogo: Boolean = true,
    val idCardShowLocker: Boolean = true,
    val idCardShowCoach: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val currencySymbol: String
        get() = when (currency.uppercase(Locale.US)) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
}

@Keep
@IgnoreExtraProperties
data class MembershipPlan(
    @DocumentId val id: String = "",
    val name: String = "",
    val durationMonths: Long = 1L,
    val priceCents: Long = 150000L,
    val description: String = "",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val isDefault: Boolean = false,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val durationDays: Long
        get() = (durationMonths.coerceIn(1L, 60L) * 30L) + if (durationMonths == 12L) 5L else 0L

    val durationDisplayLabel: String
        get() = if (durationMonths == 1L) "1 Month" else "$durationMonths Months"

    val legacyTier: MembershipTier
        get() = MembershipTier.forDurationMonths(durationMonths)
}

@Keep
@IgnoreExtraProperties
data class NotificationSettings(
    @DocumentId val id: String = "default",
    val expiry15Days: Boolean = true,
    val expiry7Days: Boolean = true,
    val expiry3Days: Boolean = true,
    val expiry1Day: Boolean = true,
    val expiredReminder: Boolean = true,
    val whatsappEnabled: Boolean = true,
    val expiringTemplate: String = "Hi {memberName}, your {gymName} membership ({memberId}) expires on {expiryDate}. Renew now for {amount} to continue uninterrupted access.",
    val expiredTemplate: String = "Hi {memberName}, your {gymName} membership ({memberId}) expired on {expiryDate}. Pay {amount} at reception or via UPI to reactivate today.",
    val renewalTemplate: String = "Thank you {memberName}! Your {gymName} membership ({memberId}) has been renewed till {expiryDate}. Amount received: {amount}.",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentSettings(
    @DocumentId val id: String = "default",
    val acceptCash: Boolean = true,
    val acceptUpi: Boolean = true,
    val acceptOnline: Boolean = false,
    val upiId: String = "",
    val upiQrCodeUrl: String = "",
    val upiQrCodePath: String = "",
    val paymentInstructions: String = "Pay via Cash at front desk or scan the official EKTA 47 GYM UPI QR code and enter the UTR/receipt reference.",
    val onlinePaymentEnabled: Boolean = false,
    val onlineGatewayLabel: String = "External Gateway (Server-Verified)",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val acceptedMethods: List<PaymentMethod>
        get() = buildList {
            if (acceptCash) add(PaymentMethod.CASH)
            if (acceptUpi) add(PaymentMethod.UPI)
            if (acceptOnline && onlinePaymentEnabled) add(PaymentMethod.ONLINE)
        }.ifEmpty { listOf(PaymentMethod.CASH) }
}

@Keep
@IgnoreExtraProperties
data class MembershipTransaction(
    @DocumentId val id: String = "",
    val memberId: String = "",
    val memberCode: String = "",
    val memberName: String = "",
    val planId: String = "",
    val planName: String = "",
    val durationMonths: Long = 1L,
    val durationDays: Long = 30L,
    val baseFeeCents: Long = 0L,
    val discountCents: Long = 0L,
    val chargedAmountCents: Long = 0L,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val transactionRef: String = "",
    val transactionType: String = "NEW_MEMBERSHIP",
    val startDateFormatted: String = "",
    val expiryDateFormatted: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val paymentMethodEnum: PaymentMethod
        get() = PaymentMethod.fromRaw(paymentMethod)
}

@Keep
@IgnoreExtraProperties
data class GymMember(
    @DocumentId val id: String = "",
    val memberCode: String = "",
    val fullName: String = "",
    val email: String? = null,
    val phone: String = "",
    val tier: String = MembershipTier.FOUNDATION.name,
    val planId: String? = null,
    val planName: String? = null,
    val durationMonths: Long = 0L,
    val status: String = MembershipStatus.ACTIVE.name,
    val daysRemaining: Long = 30L,
    val monthlyFeeCents: Long = 9900L,
    val lockerNumber: String? = null,
    val trainerName: String? = null,
    val profilePhotoUrl: String? = null,
    val profilePhotoPath: String? = null,
    val accessEnabled: Boolean = true,
    val autoRenew: Boolean = true,
    val totalCheckIns: Long = 0L,
    @ServerTimestamp val lastCheckInAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val tierEnum: MembershipTier get() = MembershipTier.fromRaw(tier)
    val statusEnum: MembershipStatus get() = MembershipStatus.fromRaw(status)

    val displayPlanName: String
        get() = planName?.takeIf { it.isNotBlank() } ?: tierEnum.planTitle

    val hasProfilePhoto: Boolean
        get() = !profilePhotoUrl.isNullOrBlank()

    val numericMemberNumber: Int?
        get() = memberCode.filter { it.isDigit() }.toIntOrNull()

    val displayMemberId: String
        get() = if (memberCode.startsWith("#")) memberCode else "#$memberCode"

    val dueAmountCents: Long
        get() = if (statusEnum == MembershipStatus.EXPIRED) monthlyFeeCents else 0L

    val initials: String
        get() = fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "GM" }

    val startDateFormatted: String
        get() {
            val date = createdAt?.toDate() ?: Date()
            return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }

    val expiryDateFormatted: String
        get() {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysRemaining.toInt())
            return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
        }
}

fun computeNextAutoMemberId(members: List<GymMember>): String {
    val maxId = members.mapNotNull { it.numericMemberNumber }.maxOrNull() ?: 1727
    return (maxOf(1727, maxId) + 1).toString()
}

fun computeNextSequentialMemberCode(
    settings: GymSettings?,
    members: List<GymMember>
): String {
    val prefix = settings?.memberIdPrefix?.trim()?.uppercase(Locale.US).orEmpty()
    val startId = settings?.startingMemberId ?: 1728L
    val configuredNext = settings?.nextMemberId ?: startId
    val maxExisting = members.mapNotNull { it.numericMemberNumber?.toLong() }.maxOrNull() ?: 1727L
    var candidateNum = maxOf(startId, configuredNext, maxExisting + 1L)
    val existingNormalized = members.map { it.memberCode.trim().uppercase(Locale.US).removePrefix("#") }.toSet()
    while ("$prefix$candidateNum" in existingNormalized || candidateNum.toString() in existingNormalized) {
        candidateNum += 1L
    }
    return "$prefix$candidateNum"
}

/**
 * Exact monetary parsing using BigDecimal (avoids floating-point rounding errors).
 */
fun parseMajorCurrencyToCents(raw: String): Long? {
    val trimmed = raw.trim().replace(",", "")
    if (trimmed.isEmpty()) return null
    return try {
        val bd = BigDecimal(trimmed)
        if (bd < BigDecimal.ZERO) return null
        bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (_: Exception) {
        null
    }
}

fun formatMajorAmountInput(cents: Long): String {
    val whole = cents / 100L
    val rem = (cents % 100L).let { if (it < 0) -it else it }
    return if (rem == 0L) whole.toString() else String.format(Locale.US, "%d.%02d", whole, rem)
}

fun formatMoneyCents(cents: Long, currencyCode: String = "INR"): String {
    val symbol = when (currencyCode.uppercase(Locale.US)) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "₹"
    }
    val absCents = kotlin.math.abs(cents)
    val whole = absCents / 100L
    val rem = absCents % 100L
    val nf = NumberFormat.getIntegerInstance(Locale("en", "IN"))
    val formattedWhole = nf.format(whole)
    val prefix = if (cents < 0L) "-$symbol" else symbol
    return if (rem == 0L) {
        "$prefix$formattedWhole"
    } else {
        "$prefix$formattedWhole.${String.format(Locale.US, "%02d", rem)}"
    }
}

fun formatWhatsAppReminderMessage(
    template: String,
    member: GymMember,
    amountCents: Long,
    gymSettings: GymSettings?
): String {
    val currency = gymSettings?.currency ?: "INR"
    val gymName = gymSettings?.gymName?.ifBlank { "EKTA 47 GYM" } ?: "EKTA 47 GYM"
    val formattedAmount = formatMoneyCents(amountCents, currency)
    return template
        .replace("{memberName}", member.fullName)
        .replace("{memberId}", member.displayMemberId)
        .replace("{expiryDate}", member.expiryDateFormatted)
        .replace("{membershipExpiry}", member.expiryDateFormatted)
        .replace("{amount}", formattedAmount)
        .replace("{renewalAmount}", formattedAmount)
        .replace("{gymName}", gymName)
}

@Keep
@IgnoreExtraProperties
data class CheckInLog(
    @DocumentId val id: String = "",
    val memberId: String = "",
    val memberCode: String = "",
    val memberName: String = "",
    val tier: String = MembershipTier.FOUNDATION.name,
    val gateName: String = "Main Turnstile A",
    val outcome: String = CheckInOutcome.GRANTED.name,
    val note: String? = null,
    @ServerTimestamp val checkedInAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val tierEnum: MembershipTier get() = MembershipTier.fromRaw(tier)
    val outcomeEnum: CheckInOutcome get() = CheckInOutcome.fromRaw(outcome)
}

@Keep
@IgnoreExtraProperties
data class TrainerProfile(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val specialty: String = "",
    val activeClients: Long = 0L,
    val sessionsToday: Long = 0L,
    val onFloor: Boolean = true,
    val phone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val initials: String
        get() = fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "TR" }
}

data class BillingDashboardMetrics(
    val monthlyRevenueCents: Long = 0L,
    val potentialMonthlyRevenueCents: Long = 0L,
    val blackCardRevenueCents: Long = 0L,
    val performanceRevenueCents: Long = 0L,
    val foundationRevenueCents: Long = 0L,
    val outstandingOverdueCents: Long = 0L,
    val overdueMemberCount: Int = 0,
    val expiringSoonDuesCents: Long = 0L,
    val expiringMemberCount: Int = 0,
    val autoPayEnrolledCount: Int = 0,
    val autoPayActiveCount: Int = 0,
    val manualBillingCount: Int = 0,
    val autoPayAtRiskCount: Int = 0,
    val totalMembersCount: Int = 0,
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false
) {
    val totalOutstandingCents: Long
        get() = outstandingOverdueCents + expiringSoonDuesCents

    val autoPayCoveragePercent: Int
        get() = if (totalMembersCount == 0) 0
        else ((autoPayEnrolledCount * 100f) / totalMembersCount).toInt().coerceIn(0, 100)
}

@Keep
@IgnoreExtraProperties
data class MonthlyRevenueRecord(
    @DocumentId val id: String = "",
    val monthKey: String = "",
    val monthLabel: String = "",
    val sortOrder: Long = 0L,
    val collectedCents: Long = 0L,
    val outstandingCents: Long = 0L,
    val autoPayCents: Long = 0L,
    val activeMembers: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GymContactDetails(
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val openingHours: String = ""
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
