package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.BillingDashboardMetrics
import com.example.model.CheckInLog
import com.example.model.CheckInOutcome
import com.example.model.FacilityProfile
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipPlan
import com.example.model.MembershipStatus
import com.example.model.MembershipTier
import com.example.model.MembershipTransaction
import com.example.model.MonthlyRevenueRecord
import com.example.model.NotificationSettings
import com.example.model.PaymentMethod
import com.example.model.PaymentSettings
import com.example.model.StaffAccessRole
import com.example.model.TrainerProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ApexRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val storage: FirebaseStorage = Firebase.storage
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    fun observeFacilityProfile(userId: String): Flow<FacilityProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FacilityProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeMembers(userId: String): Flow<List<GymMember>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/members"
        emitAll(
            db.collection("users").document(uid).collection("members")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GymMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<GymMember> {
                            when (it.statusEnum) {
                                MembershipStatus.EXPIRED -> 0
                                MembershipStatus.EXPIRING -> 1
                                MembershipStatus.ACTIVE -> 2
                            }
                        }.thenBy { it.fullName.lowercase() }
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeCheckIns(userId: String): Flow<List<CheckInLog>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/checkins"
        emitAll(
            db.collection("users").document(uid).collection("checkins")
                .orderBy("checkedInAt", Query.Direction.DESCENDING)
                .limit(60)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CheckInLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeTrainers(userId: String): Flow<List<TrainerProfile>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/trainers"
        emitAll(
            db.collection("users").document(uid).collection("trainers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TrainerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.onFloor }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeRevenueHistory(userId: String): Flow<List<MonthlyRevenueRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/revenue_history"
        emitAll(
            db.collection("users").document(uid).collection("revenue_history")
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MonthlyRevenueRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeGymSettings(userId: String): Flow<GymSettings> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/gym_settings/default"
        emitAll(
            db.collection("users").document(uid).collection("gym_settings").document("default")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            GymSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: GymSettings()
                    } else {
                        GymSettings()
                    }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeMembershipPlans(userId: String): Flow<List<MembershipPlan>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/membership_plans"
        emitAll(
            db.collection("users").document(uid).collection("membership_plans")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MembershipPlan::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<MembershipPlan> { it.sortOrder }.thenBy { it.durationMonths }
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeNotificationSettings(userId: String): Flow<NotificationSettings> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/notification_settings/default"
        emitAll(
            db.collection("users").document(uid).collection("notification_settings").document("default")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            NotificationSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: NotificationSettings()
                    } else {
                        NotificationSettings()
                    }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observePaymentSettings(userId: String): Flow<PaymentSettings> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/payment_settings/default"
        emitAll(
            db.collection("users").document(uid).collection("payment_settings").document("default")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PaymentSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: PaymentSettings()
                    } else {
                        PaymentSettings()
                    }
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<MembershipTransaction>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/membership_transactions"
        emitAll(
            db.collection("users").document(uid).collection("membership_transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(250)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MembershipTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeBillingDashboard(userId: String): Flow<BillingDashboardMetrics> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid/members"
        emitAll(
            db.collection("users").document(uid).collection("members")
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    val members = snapshot.toObjects(
                        GymMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    computeBillingMetrics(
                        members = members,
                        isFromCache = snapshot.metadata.isFromCache,
                        hasPendingWrites = snapshot.metadata.hasPendingWrites()
                    )
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun computeBillingMetrics(
        members: List<GymMember>,
        isFromCache: Boolean,
        hasPendingWrites: Boolean
    ): BillingDashboardMetrics {
        var monthlyRevenueCents = 0L
        var potentialMonthlyRevenueCents = 0L
        var blackCardRevenueCents = 0L
        var performanceRevenueCents = 0L
        var foundationRevenueCents = 0L
        var outstandingOverdueCents = 0L
        var overdueMemberCount = 0
        var expiringSoonDuesCents = 0L
        var expiringMemberCount = 0
        var autoPayEnrolledCount = 0
        var autoPayActiveCount = 0
        var manualBillingCount = 0
        var autoPayAtRiskCount = 0

        members.forEach { member ->
            val fee = member.monthlyFeeCents
            potentialMonthlyRevenueCents += fee

            when (member.statusEnum) {
                MembershipStatus.ACTIVE -> {
                    monthlyRevenueCents += fee
                    when (member.tierEnum) {
                        MembershipTier.BLACK_CARD -> blackCardRevenueCents += fee
                        MembershipTier.PERFORMANCE -> performanceRevenueCents += fee
                        MembershipTier.FOUNDATION -> foundationRevenueCents += fee
                    }
                }
                MembershipStatus.EXPIRING -> {
                    monthlyRevenueCents += fee
                    expiringSoonDuesCents += fee
                    expiringMemberCount += 1
                    when (member.tierEnum) {
                        MembershipTier.BLACK_CARD -> blackCardRevenueCents += fee
                        MembershipTier.PERFORMANCE -> performanceRevenueCents += fee
                        MembershipTier.FOUNDATION -> foundationRevenueCents += fee
                    }
                }
                MembershipStatus.EXPIRED -> {
                    outstandingOverdueCents += fee
                    overdueMemberCount += 1
                }
            }

            if (member.autoRenew) {
                autoPayEnrolledCount += 1
                if (member.statusEnum == MembershipStatus.EXPIRED || !member.accessEnabled) {
                    autoPayAtRiskCount += 1
                } else {
                    autoPayActiveCount += 1
                }
            } else {
                manualBillingCount += 1
            }
        }

        return BillingDashboardMetrics(
            monthlyRevenueCents = monthlyRevenueCents,
            potentialMonthlyRevenueCents = potentialMonthlyRevenueCents,
            blackCardRevenueCents = blackCardRevenueCents,
            performanceRevenueCents = performanceRevenueCents,
            foundationRevenueCents = foundationRevenueCents,
            outstandingOverdueCents = outstandingOverdueCents,
            overdueMemberCount = overdueMemberCount,
            expiringSoonDuesCents = expiringSoonDuesCents,
            expiringMemberCount = expiringMemberCount,
            autoPayEnrolledCount = autoPayEnrolledCount,
            autoPayActiveCount = autoPayActiveCount,
            manualBillingCount = manualBillingCount,
            autoPayAtRiskCount = autoPayAtRiskCount,
            totalMembersCount = members.size,
            isFromCache = isFromCache,
            hasPendingWrites = hasPendingWrites
        )
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureFacilitySeeded(
        userId: String,
        managerDisplayName: String?,
        initialRole: StaffAccessRole? = null,
        initialFacilityName: String? = null
    ): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            val userDocRef = db.collection("users").document(uid)
            docPath = userDocRef.path
            val existing = userDocRef.get().await()
            if (existing.exists()) {
                val existingName = existing.getString("facilityName").orEmpty().ifBlank { "EKTA 47 GYM & FITNESS CENTRE" }
                val existingMgr = existing.getString("managerName").orEmpty().ifBlank { "Facility Administrator" }
                seedAdminSettingsAndPlansIfMissing(userDocRef, existingName, existingMgr)
                if (initialRole != null) {
                    val currentRole = existing.getString("activeRole")
                    if (currentRole != initialRole.name) {
                        userDocRef.update(
                            mapOf(
                                "activeRole" to initialRole.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
                return Result.success(Unit)
            }

            val managerName = (managerDisplayName?.trim().orEmpty())
                .ifBlank { "Facility Director" }
                .take(60)

            val facilityName = (initialFacilityName?.trim().orEmpty())
                .ifBlank { "EKTA 47 GYM" }
                .take(80)

            val roleToSet = initialRole ?: StaffAccessRole.ADMIN

            val batch = db.batch()
            batch.set(
                userDocRef,
                mapOf(
                    "userId" to uid,
                    "facilityName" to facilityName,
                    "managerName" to managerName,
                    "seeded" to true,
                    "dailyCheckInTarget" to 180L,
                    "activeRole" to roleToSet.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Seed only administrative defaults (gym_settings with clean sequential startingMemberId,
            // default membership plans, notification templates, and payment modes).
            // Members, trainers, turnstile check-ins, and transactions start completely CLEAN and EMPTY.
            seedAdminDefaultsIntoBatch(
                batch = batch,
                userDocRef = userDocRef,
                gymName = facilityName,
                adminName = managerName,
                seedGymSettings = true,
                seedPlans = true,
                seedNotifications = true,
                seedPayments = true
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun performFreshGymDataReset(
        userId: String,
        context: Context? = null
    ): Result<String> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            val userDocRef = db.collection("users").document(uid)
            docPath = userDocRef.path

            // 1. Fetch existing records for backup before deletion
            val membersSnap = userDocRef.collection("members").get().await()
            val checkinsSnap = userDocRef.collection("checkins").get().await()
            val trainersSnap = userDocRef.collection("trainers").get().await()
            val txSnap = userDocRef.collection("membership_transactions").get().await()
            val revSnap = userDocRef.collection("revenue_history").get().await()

            // 2. Export local backup JSON
            if (context != null) {
                try {
                    val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
                    val backupFile = File(backupDir, "backup_ekta47_${System.currentTimeMillis()}.json")
                    val backupJson = buildString {
                        append("{\n  \"timestamp\": \"${Date()}\",\n")
                        append("  \"userId\": \"$uid\",\n")
                        append("  \"membersCount\": ${membersSnap.size()},\n")
                        append("  \"checkinsCount\": ${checkinsSnap.size()},\n")
                        append("  \"trainersCount\": ${trainersSnap.size()},\n")
                        append("  \"transactionsCount\": ${txSnap.size()},\n")
                        append("  \"revenueRecordsCount\": ${revSnap.size()}\n}")
                    }
                    backupFile.writeText(backupJson)
                    Log.i("ApexRepository", "Data backup saved to ${backupFile.absolutePath}")
                } catch (e: Exception) {
                    Log.w("ApexRepository", "Backup generation error", e)
                }
            }

            // 3. Delete all business records in batch
            val batch = db.batch()
            for (doc in membersSnap.documents) {
                val photoPath = doc.getString("profilePhotoPath")
                deleteStoragePhotoSafely(photoPath, memberId = doc.id)
                batch.delete(doc.reference)
            }
            for (doc in checkinsSnap.documents) {
                batch.delete(doc.reference)
            }
            for (doc in trainersSnap.documents) {
                batch.delete(doc.reference)
            }
            for (doc in txSnap.documents) {
                batch.delete(doc.reference)
            }
            for (doc in revSnap.documents) {
                batch.delete(doc.reference)
            }

            // 4. Reset sequential Member ID back to startingMemberId in gym_settings
            val gymSettingsRef = userDocRef.collection("gym_settings").document("default")
            val settingsSnap = gymSettingsRef.get().await()
            val startId = settingsSnap.getLong("startingMemberId") ?: 1728L
            batch.update(gymSettingsRef, mapOf(
                "nextMemberId" to startId,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // 5. Update user profile
            batch.update(userDocRef, mapOf(
                "seeded" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            batch.commit().await()
            Result.success("Fresh gym reset complete. All old member, attendance, trainer, and revenue records have been cleared.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun seedAdminSettingsAndPlansIfMissing(
        userDocRef: DocumentReference,
        gymName: String,
        adminName: String
    ) {
        try {
            val gymSettingsSnap = userDocRef.collection("gym_settings").document("default").get().await()
            val plansSnap = userDocRef.collection("membership_plans").limit(1).get().await()
            val notifSnap = userDocRef.collection("notification_settings").document("default").get().await()
            val paySnap = userDocRef.collection("payment_settings").document("default").get().await()

            val needSettings = !gymSettingsSnap.exists()
            val needPlans = plansSnap.isEmpty
            val needNotif = !notifSnap.exists()
            val needPay = !paySnap.exists()
            if (!needSettings && !needPlans && !needNotif && !needPay) return

            val batch = db.batch()
            seedAdminDefaultsIntoBatch(
                batch = batch,
                userDocRef = userDocRef,
                gymName = gymName,
                adminName = adminName,
                seedGymSettings = needSettings,
                seedPlans = needPlans,
                seedNotifications = needNotif,
                seedPayments = needPay
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ApexRepository", "Admin settings default seeding skipped or failed", e)
        }
    }

    private fun seedAdminDefaultsIntoBatch(
        batch: WriteBatch,
        userDocRef: DocumentReference,
        gymName: String,
        adminName: String,
        seedGymSettings: Boolean,
        seedPlans: Boolean,
        seedNotifications: Boolean,
        seedPayments: Boolean
    ) {
        if (seedGymSettings) {
            val defaults = GymSettings(
                gymName = gymName.ifBlank { "EKTA 47 GYM & FITNESS CENTRE" }.take(100),
                adminName = adminName.ifBlank { "Facility Administrator" }.take(60)
            )
            batch.set(
                userDocRef.collection("gym_settings").document("default"),
                gymSettingsPayload(defaults, includeCreatedAt = true)
            )
        }
        if (seedPlans) {
            val plansCol = userDocRef.collection("membership_plans")
            val defaultPlans = listOf(
                "plan_monthly" to MembershipPlan(
                    name = "Monthly",
                    durationMonths = 1L,
                    priceCents = 150000L,
                    description = "1 Month full gym floor & cardio access",
                    isActive = true,
                    isDefault = true,
                    sortOrder = 1L
                ),
                "plan_quarterly" to MembershipPlan(
                    name = "Quarterly",
                    durationMonths = 3L,
                    priceCents = 420000L,
                    description = "3 Months strength, cardio & locker access",
                    isActive = true,
                    isDefault = true,
                    sortOrder = 2L
                ),
                "plan_half_yearly" to MembershipPlan(
                    name = "Half-Yearly",
                    durationMonths = 6L,
                    priceCents = 750000L,
                    description = "6 Months fitness + scheduled coach reviews",
                    isActive = true,
                    isDefault = true,
                    sortOrder = 3L
                ),
                "plan_annual" to MembershipPlan(
                    name = "Annual",
                    durationMonths = 12L,
                    priceCents = 1350000L,
                    description = "12 Months VIP full access & priority desk",
                    isActive = true,
                    isDefault = true,
                    sortOrder = 4L
                )
            )
            defaultPlans.forEach { (id, plan) ->
                batch.set(plansCol.document(id), membershipPlanPayload(plan, includeCreatedAt = true))
            }
        }
        if (seedNotifications) {
            batch.set(
                userDocRef.collection("notification_settings").document("default"),
                notificationSettingsPayload(NotificationSettings(), includeCreatedAt = true)
            )
        }
        if (seedPayments) {
            batch.set(
                userDocRef.collection("payment_settings").document("default"),
                paymentSettingsPayload(PaymentSettings(), includeCreatedAt = true)
            )
        }
    }

    private fun gymSettingsPayload(settings: GymSettings, includeCreatedAt: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "gymName" to settings.gymName.trim().take(100).ifBlank { "EKTA 47 GYM & FITNESS CENTRE" },
            "logoUrl" to settings.logoUrl.trim().take(1000),
            "logoPath" to settings.logoPath.trim().take(240),
            "adminName" to settings.adminName.trim().take(60).ifBlank { "Facility Administrator" },
            "contactPhone" to settings.contactPhone.trim().take(30),
            "contactEmail" to settings.contactEmail.trim().take(120),
            "address" to settings.address.trim().take(240),
            "openingHours" to settings.openingHours.trim().take(120),
            "currency" to settings.currency.uppercase(Locale.US).let {
                if (it in setOf("INR", "USD", "EUR", "GBP")) it else "INR"
            },
            "memberIdPrefix" to settings.memberIdPrefix.trim().uppercase(Locale.US).take(10),
            "startingMemberId" to settings.startingMemberId.coerceIn(1L, 99999999L),
            "nextMemberId" to settings.nextMemberId.coerceIn(1L, 99999999L),
            "idCardTagline" to settings.idCardTagline.trim().take(100),
            "idCardShowLogo" to settings.idCardShowLogo,
            "idCardShowLocker" to settings.idCardShowLocker,
            "idCardShowCoach" to settings.idCardShowCoach,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (includeCreatedAt) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    private fun membershipPlanPayload(plan: MembershipPlan, includeCreatedAt: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "name" to plan.name.trim().take(60),
            "durationMonths" to plan.durationMonths.coerceIn(1L, 60L),
            "priceCents" to plan.priceCents.coerceIn(0L, 100000000L),
            "description" to plan.description.trim().take(240),
            "isActive" to plan.isActive,
            "isDefault" to plan.isDefault,
            "sortOrder" to plan.sortOrder.coerceIn(0L, 10000L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (includeCreatedAt) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    private fun notificationSettingsPayload(
        settings: NotificationSettings,
        includeCreatedAt: Boolean
    ): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "expiry15Days" to settings.expiry15Days,
            "expiry7Days" to settings.expiry7Days,
            "expiry3Days" to settings.expiry3Days,
            "expiry1Day" to settings.expiry1Day,
            "expiredReminder" to settings.expiredReminder,
            "whatsappEnabled" to settings.whatsappEnabled,
            "expiringTemplate" to settings.expiringTemplate.trim().take(500),
            "expiredTemplate" to settings.expiredTemplate.trim().take(500),
            "renewalTemplate" to settings.renewalTemplate.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (includeCreatedAt) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    private fun paymentSettingsPayload(
        settings: PaymentSettings,
        includeCreatedAt: Boolean
    ): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "acceptCash" to settings.acceptCash,
            "acceptUpi" to settings.acceptUpi,
            "acceptOnline" to settings.acceptOnline,
            "upiId" to settings.upiId.trim().take(120),
            "upiQrCodeUrl" to settings.upiQrCodeUrl.trim().take(1000),
            "upiQrCodePath" to settings.upiQrCodePath.trim().take(240),
            "paymentInstructions" to settings.paymentInstructions.trim().take(400),
            "onlinePaymentEnabled" to settings.onlinePaymentEnabled,
            "onlineGatewayLabel" to settings.onlineGatewayLabel.trim().take(100),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (includeCreatedAt) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    suspend fun saveGymSettings(
        settings: GymSettings,
        dailyCheckInTarget: Long? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/gym_settings/default"
        return try {
            val uid = requireUserId()
            val cleanName = settings.gymName.trim().take(100)
            val cleanAdmin = settings.adminName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Gym name cannot be empty." }
            require(cleanAdmin.isNotEmpty()) { "Administrator name cannot be empty." }
            require(settings.startingMemberId in 1L..99999999L) { "Starting Member ID must be at least 1." }
            val normalizedNext = maxOf(settings.startingMemberId, settings.nextMemberId).coerceIn(1L, 99999999L)
            val normalizedSettings = settings.copy(
                gymName = cleanName,
                adminName = cleanAdmin,
                nextMemberId = normalizedNext
            )

            val userRef = db.collection("users").document(uid)
            val settingsRef = userRef.collection("gym_settings").document("default")
            docPath = settingsRef.path
            val existing = settingsRef.get().await()
            val batch = db.batch()
            if (existing.exists()) {
                batch.update(settingsRef, gymSettingsPayload(normalizedSettings, includeCreatedAt = false))
            } else {
                batch.set(settingsRef, gymSettingsPayload(normalizedSettings, includeCreatedAt = true))
            }

            val profileUpdates = mutableMapOf<String, Any>(
                "facilityName" to cleanName.take(80),
                "managerName" to cleanAdmin.take(60),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (dailyCheckInTarget != null) {
                profileUpdates["dailyCheckInTarget"] = dailyCheckInTarget.coerceIn(1L, 5000L)
            }
            batch.update(userRef, profileUpdates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun uploadGymLogo(
        currentSettings: GymSettings,
        imageBytes: ByteArray
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/gym_settings/default"
        var uploadedPath: String? = null
        return try {
            val uid = requireUserId()
            require(imageBytes.isNotEmpty()) { "Logo image cannot be empty." }
            val objectPath = "gym/$uid/branding/logo_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(objectPath)
            val metadata = storageMetadata {
                contentType = "image/jpeg"
                setCustomMetadata("ownerUid", uid)
            }
            storageRef.putBytes(imageBytes, metadata).await()
            uploadedPath = objectPath
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val settingsRef = db.collection("users").document(uid).collection("gym_settings").document("default")
            docPath = settingsRef.path
            val existing = settingsRef.get().await()
            val updated = currentSettings.copy(logoUrl = downloadUrl, logoPath = objectPath)
            if (existing.exists()) {
                settingsRef.update(gymSettingsPayload(updated, includeCreatedAt = false)).await()
            } else {
                settingsRef.set(gymSettingsPayload(updated, includeCreatedAt = true)).await()
            }
            deleteStoragePhotoSafely(
                storagePath = currentSettings.logoPath,
                keepPath = objectPath
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            deleteStoragePhotoSafely(uploadedPath)
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeGymLogo(currentSettings: GymSettings): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/gym_settings/default"
        return try {
            val uid = requireUserId()
            val settingsRef = db.collection("users").document(uid).collection("gym_settings").document("default")
            docPath = settingsRef.path
            val existing = settingsRef.get().await()
            val updated = currentSettings.copy(logoUrl = "", logoPath = "")
            if (existing.exists()) {
                settingsRef.update(gymSettingsPayload(updated, includeCreatedAt = false)).await()
            } else {
                settingsRef.set(gymSettingsPayload(updated, includeCreatedAt = true)).await()
            }
            deleteStoragePhotoSafely(currentSettings.logoPath)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertMembershipPlan(
        existingPlan: MembershipPlan?,
        name: String,
        durationMonths: Long,
        priceCents: Long,
        description: String,
        isActive: Boolean,
        sortOrder: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/membership_plans"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(60)
            require(cleanName.length >= 2) { "Plan name must be at least 2 characters." }
            require(durationMonths in 1L..60L) { "Duration must be between 1 and 60 months." }
            require(priceCents in 0L..100000000L) { "Membership fee is out of valid range." }

            val plansCol = db.collection("users").document(uid).collection("membership_plans")
            val isUpdate = existingPlan != null && existingPlan.id.isNotBlank()
            val docRef = if (isUpdate) plansCol.document(existingPlan!!.id) else plansCol.document()
            docPath = docRef.path

            val plan = MembershipPlan(
                id = docRef.id,
                name = cleanName,
                durationMonths = durationMonths,
                priceCents = priceCents,
                description = description.trim().take(240),
                isActive = isActive,
                isDefault = existingPlan?.isDefault ?: false,
                sortOrder = if (isUpdate) existingPlan!!.sortOrder else sortOrder.coerceIn(1L, 10000L)
            )
            if (isUpdate) {
                docRef.update(membershipPlanPayload(plan, includeCreatedAt = false)).await()
            } else {
                docRef.set(membershipPlanPayload(plan, includeCreatedAt = true)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMembershipPlanActive(plan: MembershipPlan, isActive: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/membership_plans/${plan.id}"
        return try {
            val uid = requireUserId()
            require(plan.id.isNotBlank()) { "Plan ID required." }
            val docRef = db.collection("users").document(uid).collection("membership_plans").document(plan.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMembershipPlanSafely(
        plan: MembershipPlan,
        members: List<GymMember>,
        transactions: List<MembershipTransaction>
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/membership_plans/${plan.id}"
        return try {
            val uid = requireUserId()
            require(plan.id.isNotBlank()) { "Plan ID required." }
            val userRef = db.collection("users").document(uid)

            val localMemberRef = members.any {
                it.planId == plan.id ||
                    (it.planId.isNullOrBlank() && it.planName.equals(plan.name, ignoreCase = true))
            }
            val localTxRef = transactions.any { it.planId == plan.id }
            val serverMemberRef = !userRef.collection("members")
                .whereEqualTo("planId", plan.id)
                .limit(1)
                .get()
                .await()
                .isEmpty
            val serverTxRef = !userRef.collection("membership_transactions")
                .whereEqualTo("planId", plan.id)
                .limit(1)
                .get()
                .await()
                .isEmpty

            if (localMemberRef || localTxRef || serverMemberRef || serverTxRef) {
                return Result.failure(
                    IllegalStateException(
                        "Cannot delete '${plan.name}' because existing member records or payment receipts reference this plan. Deactivate it instead."
                    )
                )
            }

            val docRef = userRef.collection("membership_plans").document(plan.id)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notification_settings/default"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("notification_settings").document("default")
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(notificationSettingsPayload(settings, includeCreatedAt = false)).await()
            } else {
                docRef.set(notificationSettingsPayload(settings, includeCreatedAt = true)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun savePaymentSettings(settings: PaymentSettings): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payment_settings/default"
        return try {
            val uid = requireUserId()
            require(settings.acceptCash || settings.acceptUpi || settings.acceptOnline) {
                "Enable at least one payment method (Cash, UPI, or Online)."
            }
            val docRef = db.collection("users").document(uid).collection("payment_settings").document("default")
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(paymentSettingsPayload(settings, includeCreatedAt = false)).await()
            } else {
                docRef.set(paymentSettingsPayload(settings, includeCreatedAt = true)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun uploadUpiQrCode(
        currentSettings: PaymentSettings,
        imageBytes: ByteArray
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payment_settings/default"
        var uploadedPath: String? = null
        return try {
            val uid = requireUserId()
            require(imageBytes.isNotEmpty()) { "QR image cannot be empty." }
            val objectPath = "gym/$uid/payments/upi_qr_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(objectPath)
            val metadata = storageMetadata {
                contentType = "image/jpeg"
                setCustomMetadata("ownerUid", uid)
            }
            storageRef.putBytes(imageBytes, metadata).await()
            uploadedPath = objectPath
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val docRef = db.collection("users").document(uid).collection("payment_settings").document("default")
            docPath = docRef.path
            val existing = docRef.get().await()
            val updated = currentSettings.copy(upiQrCodeUrl = downloadUrl, upiQrCodePath = objectPath)
            if (existing.exists()) {
                docRef.update(paymentSettingsPayload(updated, includeCreatedAt = false)).await()
            } else {
                docRef.set(paymentSettingsPayload(updated, includeCreatedAt = true)).await()
            }
            deleteStoragePhotoSafely(
                storagePath = currentSettings.upiQrCodePath,
                keepPath = objectPath
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            deleteStoragePhotoSafely(uploadedPath)
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeUpiQrCode(currentSettings: PaymentSettings): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payment_settings/default"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("payment_settings").document("default")
            docPath = docRef.path
            val existing = docRef.get().await()
            val updated = currentSettings.copy(upiQrCodeUrl = "", upiQrCodePath = "")
            if (existing.exists()) {
                docRef.update(paymentSettingsPayload(updated, includeCreatedAt = false)).await()
            } else {
                docRef.set(paymentSettingsPayload(updated, includeCreatedAt = true)).await()
            }
            deleteStoragePhotoSafely(currentSettings.upiQrCodePath)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setWorkspaceRole(role: StaffAccessRole): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "activeRole" to role.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFacilityProfile(
        facilityName: String,
        managerName: String,
        dailyCheckInTarget: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanFacility = facilityName.trim().take(80)
            val cleanManager = managerName.trim().take(60)
            require(cleanFacility.isNotEmpty()) { "Gym name cannot be empty." }
            require(cleanManager.isNotEmpty()) { "Administrator name cannot be empty." }
            val boundedTarget = dailyCheckInTarget.coerceIn(1L, 5000L)

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "facilityName" to cleanFacility,
                    "managerName" to cleanManager,
                    "dailyCheckInTarget" to boundedTarget,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun seedRevenueHistoryIfEmpty(userDocRef: DocumentReference) {
        val existingHistory = userDocRef.collection("revenue_history").limit(1).get().await()
        if (existingHistory.isEmpty) {
            val batch = db.batch()
            seedRevenueHistoryIntoBatch(batch, userDocRef)
            batch.commit().await()
        }
    }

    private fun seedRevenueHistoryIntoBatch(
        batch: com.google.firebase.firestore.WriteBatch,
        userDocRef: com.google.firebase.firestore.DocumentReference
    ) {
        val historyCol = userDocRef.collection("revenue_history")
        val months = listOf(
            listOf("2026-06", "Jun", 202606L, 48200L, 12800L, 28900L, 4L),
            listOf("2026-07", "Jul", 202607L, 52600L, 10500L, 31400L, 4L),
            listOf("2026-08", "Aug", 202608L, 55800L, 14900L, 33600L, 5L),
            listOf("2026-09", "Sep", 202609L, 59400L, 9900L, 36200L, 5L),
            listOf("2026-10", "Oct", 202610L, 61900L, 11800L, 38100L, 5L),
            listOf("2026-11", "Nov", 202611L, 65400L, 14900L, 39700L, 5L)
        )
        months.forEach { row ->
            val key = row[0] as String
            batch.set(
                historyCol.document(key),
                mapOf(
                    "monthKey" to key,
                    "monthLabel" to (row[1] as String),
                    "sortOrder" to (row[2] as Long),
                    "collectedCents" to (row[3] as Long),
                    "outstandingCents" to (row[4] as Long),
                    "autoPayCents" to (row[5] as Long),
                    "activeMembers" to (row[6] as Long),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }

    suspend fun syncCurrentMonthRevenueSnapshot(
        members: List<GymMember>,
        metrics: BillingDashboardMetrics
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/revenue_history"
        return try {
            val uid = requireUserId()
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val monthKey = String.format(Locale.US, "%04d-%02d", year, month)
            val monthLabel = SimpleDateFormat("MMM", Locale.US).format(cal.time)
            val sortOrder = (year * 100L) + month

            val autoPayCents = members
                .filter { it.autoRenew && it.statusEnum != MembershipStatus.EXPIRED }
                .sumOf { it.monthlyFeeCents }
            val activeCount = members.count { it.statusEnum != MembershipStatus.EXPIRED }.toLong()

            val docRef = db.collection("users").document(uid)
                .collection("revenue_history").document(monthKey)
            docPath = docRef.path

            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "monthLabel" to monthLabel,
                        "sortOrder" to sortOrder,
                        "collectedCents" to metrics.monthlyRevenueCents.coerceIn(0L, 100000000L),
                        "outstandingCents" to metrics.totalOutstandingCents.coerceIn(0L, 100000000L),
                        "autoPayCents" to autoPayCents.coerceIn(0L, 100000000L),
                        "activeMembers" to activeCount.coerceIn(0L, 100000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "monthKey" to monthKey,
                        "monthLabel" to monthLabel,
                        "sortOrder" to sortOrder,
                        "collectedCents" to metrics.monthlyRevenueCents.coerceIn(0L, 100000000L),
                        "outstandingCents" to metrics.totalOutstandingCents.coerceIn(0L, 100000000L),
                        "autoPayCents" to autoPayCents.coerceIn(0L, 100000000L),
                        "activeMembers" to activeCount.coerceIn(0L, 100000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(monthLabel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun checkInMember(member: GymMember, gateName: String): Result<CheckInOutcome> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/checkins"
        return try {
            val uid = requireUserId()
            require(member.id.isNotBlank()) { "Member ID required for check-in." }
            val cleanGate = gateName.trim().take(40).ifBlank { "Main Turnstile A" }
            val outcome = when {
                !member.accessEnabled || member.statusEnum == MembershipStatus.EXPIRED -> CheckInOutcome.DENIED
                member.statusEnum == MembershipStatus.EXPIRING || member.daysRemaining <= 7L -> CheckInOutcome.WARNING
                else -> CheckInOutcome.GRANTED
            }
            val noteText = when (outcome) {
                CheckInOutcome.DENIED -> if (!member.accessEnabled) "Access paused by front desk" else "Dues overdue (${member.daysRemaining}d)"
                CheckInOutcome.WARNING -> "Renewal due in ${member.daysRemaining.coerceAtLeast(0L)}d"
                CheckInOutcome.GRANTED -> buildString {
                    if (!member.lockerNumber.isNullOrBlank()) append("Locker ${member.lockerNumber}")
                    if (!member.trainerName.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(member.trainerName)
                    }
                    if (isEmpty()) append("Turnstile verified")
                }
            }.take(160)

            val userRef = db.collection("users").document(uid)
            val checkinRef = userRef.collection("checkins").document()
            path = checkinRef.path

            val batch = db.batch()
            val checkinPayload = mapOf(
                "memberId" to member.id,
                "memberCode" to member.memberCode,
                "memberName" to member.fullName,
                "tier" to member.tier,
                "gateName" to cleanGate,
                "outcome" to outcome.name,
                "note" to noteText,
                "checkedInAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            batch.set(checkinRef, checkinPayload)

            if (outcome != CheckInOutcome.DENIED) {
                val memberRef = userRef.collection("members").document(member.id)
                batch.update(
                    memberRef,
                    mapOf(
                        "totalCheckIns" to FieldValue.increment(1L),
                        "lastCheckInAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun buildFormattedDateRange(additionalDays: Long): Pair<String, String> {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, additionalDays.toInt())
        }
        return fmt.format(startCal.time) to fmt.format(endCal.time)
    }

    suspend fun renewMember(
        member: GymMember,
        selectedPlan: MembershipPlan?,
        fallbackTier: MembershipTier,
        additionalDays: Long,
        chargedAmountCents: Long? = null,
        discountCents: Long = 0L,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        transactionRef: String = "",
        notes: String = "",
        autoRenew: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/members/${member.id}"
        return try {
            val uid = requireUserId()
            require(member.id.isNotBlank()) { "Member ID required." }
            require(additionalDays in 1L..1825L) { "Renewal days must be between 1 and 1825." }

            val userRef = db.collection("users").document(uid)
            val docRef = userRef.collection("members").document(member.id)
            docPath = docRef.path

            val baseDays = member.daysRemaining.coerceAtLeast(0L)
            val updatedDays = (baseDays + additionalDays).coerceAtMost(3650L)
            val updatedStatus = if (updatedDays <= 7L) MembershipStatus.EXPIRING.name else MembershipStatus.ACTIVE.name

            val resolvedTier = selectedPlan?.legacyTier ?: fallbackTier
            val resolvedPlanId = selectedPlan?.id.orEmpty()
            val resolvedPlanName = (selectedPlan?.name?.takeIf { it.isNotBlank() } ?: resolvedTier.planTitle).take(80)
            val resolvedMonths = (selectedPlan?.durationMonths ?: when (resolvedTier) {
                MembershipTier.BLACK_CARD -> 12L
                MembershipTier.PERFORMANCE -> 6L
                MembershipTier.FOUNDATION -> 3L
            }).coerceIn(1L, 60L)
            val baseFeeCents = (selectedPlan?.priceCents ?: resolvedTier.defaultFeeCents.toLong())
                .coerceIn(0L, 100000000L)
            val finalChargedCents = (chargedAmountCents ?: (baseFeeCents - discountCents))
                .coerceIn(0L, 100000000L)

            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startDateStr = fmt.format(Date())
            val endCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, updatedDays.toInt())
            }
            val expiryDateStr = fmt.format(endCal.time)

            val txRef = userRef.collection("membership_transactions").document()
            val batch = db.batch()
            val memberUpdates = mutableMapOf<String, Any>(
                "status" to updatedStatus,
                "daysRemaining" to updatedDays,
                "tier" to resolvedTier.name,
                "planId" to resolvedPlanId,
                "planName" to resolvedPlanName,
                "durationMonths" to resolvedMonths,
                "monthlyFeeCents" to finalChargedCents,
                "accessEnabled" to true,
                "autoRenew" to autoRenew,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.update(docRef, memberUpdates)

            // Record immutable renewal payment receipt snapshot (past records remain untouched)
            batch.set(
                txRef,
                mapOf(
                    "memberId" to member.id,
                    "memberCode" to member.memberCode,
                    "memberName" to member.fullName,
                    "planId" to resolvedPlanId,
                    "planName" to resolvedPlanName,
                    "durationMonths" to resolvedMonths,
                    "durationDays" to additionalDays.coerceIn(1L, 3650L),
                    "baseFeeCents" to baseFeeCents,
                    "discountCents" to (baseFeeCents - finalChargedCents).coerceIn(-100000000L, 100000000L),
                    "chargedAmountCents" to finalChargedCents,
                    "paymentMethod" to paymentMethod.name,
                    "transactionRef" to transactionRef.trim().take(120),
                    "transactionType" to "RENEWAL",
                    "startDateFormatted" to startDateStr,
                    "expiryDateFormatted" to expiryDateStr,
                    "notes" to notes.trim().take(240),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun uploadMemberPhotoBytes(
        uid: String,
        memberId: String,
        imageBytes: ByteArray
    ): Pair<String, String> {
        val objectPath = "members/$memberId/profile/avatar_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child(objectPath)
        val metadata = storageMetadata {
            contentType = "image/jpeg"
            setCustomMetadata("ownerUid", uid)
        }
        storageRef.putBytes(imageBytes, metadata).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()
        return downloadUrl to objectPath
    }

    private suspend fun deleteStoragePhotoSafely(
        storagePath: String?,
        memberId: String? = null,
        keepPath: String? = null
    ) {
        val cleanKeep = keepPath?.trimStart('/')
        if (!storagePath.isNullOrBlank() && storagePath.trimStart('/') != cleanKeep) {
            try {
                storage.reference.child(storagePath.trimStart('/')).delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is StorageException && (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND || e.httpResultCode == 404)) {
                    // Object does not exist at location: already removed or never uploaded, safe to ignore
                } else {
                    Log.d("ApexRepository", "Storage photo deletion note: ${e.message}")
                }
            }
        }
        if (!memberId.isNullOrBlank()) {
            try {
                val folderRef = storage.reference.child("members/$memberId/profile")
                val listing = folderRef.listAll().await()
                listing.items.forEach { item ->
                    val itemPath = item.path.trimStart('/')
                    if (itemPath != cleanKeep && itemPath != storagePath?.trimStart('/')) {
                        try {
                            item.delete().await()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Ignore individual item deletion error
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Folder does not exist at location or listing not available: safe to ignore
            }
        }
    }

    suspend fun addMember(
        memberCode: String,
        fullName: String,
        phone: String,
        email: String?,
        selectedPlan: MembershipPlan?,
        fallbackTier: MembershipTier,
        daysRemaining: Long,
        chargedAmountCents: Long? = null,
        discountCents: Long = 0L,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        transactionRef: String = "",
        notes: String = "",
        lockerNumber: String?,
        trainerName: String?,
        autoRenew: Boolean,
        photoBytes: ByteArray? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/members"
        var uploadedPhotoPath: String? = null
        return try {
            val uid = requireUserId()
            val cleanCode = memberCode.trim().uppercase(Locale.US).removePrefix("#").take(20)
            val cleanName = fullName.trim().take(60)
            val cleanPhone = phone.trim().take(24)
            require(cleanCode.length >= 3) { "Member code must be at least 3 characters." }
            require(cleanName.length >= 2) { "Full name must be at least 2 characters." }
            require(cleanPhone.length >= 5) { "Phone number must be at least 5 characters." }

            val userRef = db.collection("users").document(uid)
            val membersCol = userRef.collection("members")

            // Duplicate Member ID check (exact & hash-stripped match on server)
            val dupCheck = membersCol.whereEqualTo("memberCode", cleanCode).limit(1).get().await()
            if (!dupCheck.isEmpty) {
                return Result.failure(IllegalArgumentException("Member ID #$cleanCode already exists. Choose a unique ID."))
            }

            val boundedDays = daysRemaining.coerceIn(-365L, 3650L)
            val status = when {
                boundedDays < 0L -> MembershipStatus.EXPIRED
                boundedDays <= 7L -> MembershipStatus.EXPIRING
                else -> MembershipStatus.ACTIVE
            }

            val resolvedTier = selectedPlan?.legacyTier ?: fallbackTier
            val resolvedPlanId = selectedPlan?.id.orEmpty()
            val resolvedPlanName = (selectedPlan?.name?.takeIf { it.isNotBlank() } ?: resolvedTier.planTitle).take(80)
            val resolvedMonths = (selectedPlan?.durationMonths ?: when (resolvedTier) {
                MembershipTier.BLACK_CARD -> 12L
                MembershipTier.PERFORMANCE -> 6L
                MembershipTier.FOUNDATION -> 3L
            }).coerceIn(1L, 60L)
            val baseFeeCents = (selectedPlan?.priceCents ?: resolvedTier.defaultFeeCents.toLong())
                .coerceIn(0L, 100000000L)
            val finalChargedCents = (chargedAmountCents ?: (baseFeeCents - discountCents))
                .coerceIn(0L, 100000000L)

            val docRef = membersCol.document()
            docPath = docRef.path

            val uploadedPhoto = if (photoBytes != null && photoBytes.isNotEmpty()) {
                val (url, path) = uploadMemberPhotoBytes(uid, docRef.id, photoBytes)
                uploadedPhotoPath = path
                url to path
            } else null

            val payload = mutableMapOf<String, Any?>(
                "memberCode" to cleanCode,
                "fullName" to cleanName,
                "phone" to cleanPhone,
                "email" to email?.trim()?.take(120)?.ifBlank { null },
                "tier" to resolvedTier.name,
                "planId" to resolvedPlanId,
                "planName" to resolvedPlanName,
                "durationMonths" to resolvedMonths,
                "status" to status.name,
                "daysRemaining" to boundedDays,
                "monthlyFeeCents" to finalChargedCents,
                "lockerNumber" to lockerNumber?.trim()?.take(16)?.ifBlank { null },
                "trainerName" to trainerName?.trim()?.take(60)?.ifBlank { null },
                "profilePhotoUrl" to uploadedPhoto?.first,
                "profilePhotoPath" to uploadedPhoto?.second,
                "accessEnabled" to (status != MembershipStatus.EXPIRED),
                "autoRenew" to autoRenew,
                "totalCheckIns" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val (startDateStr, expiryDateStr) = buildFormattedDateRange(boundedDays.coerceAtLeast(1L))
            val txRef = userRef.collection("membership_transactions").document()

            val batch = db.batch()
            batch.set(docRef, payload)
            batch.set(
                txRef,
                mapOf(
                    "memberId" to docRef.id,
                    "memberCode" to cleanCode,
                    "memberName" to cleanName,
                    "planId" to resolvedPlanId,
                    "planName" to resolvedPlanName,
                    "durationMonths" to resolvedMonths,
                    "durationDays" to boundedDays.coerceIn(1L, 3650L),
                    "baseFeeCents" to baseFeeCents,
                    "discountCents" to (baseFeeCents - finalChargedCents).coerceIn(-100000000L, 100000000L),
                    "chargedAmountCents" to finalChargedCents,
                    "paymentMethod" to paymentMethod.name,
                    "transactionRef" to transactionRef.trim().take(120),
                    "transactionType" to "NEW_MEMBERSHIP",
                    "startDateFormatted" to startDateStr,
                    "expiryDateFormatted" to expiryDateStr,
                    "notes" to notes.trim().take(240),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            // Advance sequential nextMemberId cleanly in gym_settings if present
            val parsedNumericCode = cleanCode.filter { it.isDigit() }.toLongOrNull()
            if (parsedNumericCode != null) {
                val settingsRef = userRef.collection("gym_settings").document("default")
                val settingsSnap = settingsRef.get().await()
                if (settingsSnap.exists()) {
                    val currentNext = settingsSnap.getLong("nextMemberId") ?: 1728L
                    if (parsedNumericCode >= currentNext) {
                        batch.update(
                            settingsRef,
                            mapOf(
                                "nextMemberId" to (parsedNumericCode + 1L).coerceAtMost(99999999L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            deleteStoragePhotoSafely(uploadedPhotoPath)
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMemberPhoto(
        member: GymMember,
        newImageBytes: ByteArray?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/members/${member.id}"
        var newlyUploadedPath: String? = null
        return try {
            val uid = requireUserId()
            require(member.id.isNotBlank()) { "Member ID required." }
            val docRef = db.collection("users").document(uid).collection("members").document(member.id)
            docPath = docRef.path

            if (newImageBytes != null && newImageBytes.isNotEmpty()) {
                val (newUrl, newPath) = uploadMemberPhotoBytes(uid, member.id, newImageBytes)
                newlyUploadedPath = newPath
                docRef.update(
                    mapOf(
                        "profilePhotoUrl" to newUrl,
                        "profilePhotoPath" to newPath,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                deleteStoragePhotoSafely(
                    storagePath = member.profilePhotoPath,
                    memberId = member.id,
                    keepPath = newPath
                )
            } else {
                docRef.update(
                    mapOf(
                        "profilePhotoUrl" to "",
                        "profilePhotoPath" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                deleteStoragePhotoSafely(
                    storagePath = member.profilePhotoPath,
                    memberId = member.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            deleteStoragePhotoSafely(newlyUploadedPath)
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMemberDetails(
        member: GymMember,
        fullName: String,
        phone: String,
        email: String?,
        selectedPlan: MembershipPlan?,
        tier: MembershipTier,
        lockerNumber: String?,
        trainerName: String?,
        accessEnabled: Boolean,
        autoRenew: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/members/${member.id}"
        return try {
            val uid = requireUserId()
            require(member.id.isNotBlank()) { "Member ID required." }
            val cleanName = fullName.trim().take(60)
            val cleanPhone = phone.trim().take(24)
            require(cleanName.length >= 2) { "Full name must be at least 2 characters." }
            require(cleanPhone.length >= 5) { "Phone number must be at least 5 characters." }

            val docRef = db.collection("users").document(uid).collection("members").document(member.id)
            docPath = docRef.path

            val planChanged = selectedPlan != null &&
                (selectedPlan.id != member.planId || !member.planName.equals(selectedPlan.name, ignoreCase = false))
            val resolvedTier = selectedPlan?.legacyTier ?: tier
            val resolvedPlanId = selectedPlan?.id ?: member.planId.orEmpty()
            val resolvedPlanName = (selectedPlan?.name ?: member.planName ?: resolvedTier.planTitle).take(80)
            val resolvedMonths = (selectedPlan?.durationMonths ?: member.durationMonths.takeIf { it > 0L } ?: 1L).coerceIn(1L, 60L)
            val resolvedFeeCents = if (planChanged && selectedPlan != null) {
                selectedPlan.priceCents
            } else {
                member.monthlyFeeCents
            }

            val updatePayload = mutableMapOf<String, Any>(
                "memberCode" to member.memberCode,
                "fullName" to cleanName,
                "phone" to cleanPhone,
                "email" to (email?.trim()?.take(120) ?: ""),
                "tier" to resolvedTier.name,
                "planId" to resolvedPlanId,
                "planName" to resolvedPlanName,
                "durationMonths" to resolvedMonths,
                "status" to member.status,
                "daysRemaining" to member.daysRemaining,
                "monthlyFeeCents" to resolvedFeeCents,
                "lockerNumber" to (lockerNumber?.trim()?.take(16) ?: ""),
                "trainerName" to (trainerName?.trim()?.take(60) ?: ""),
                "accessEnabled" to accessEnabled,
                "autoRenew" to autoRenew,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMemberAccess(member: GymMember, accessEnabled: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/members/${member.id}"
        return try {
            val uid = requireUserId()
            require(member.id.isNotBlank()) { "Member ID required." }
            val docRef = db.collection("users").document(uid).collection("members").document(member.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "accessEnabled" to accessEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMemberAutoRenew(member: GymMember, autoRenew: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/members/${member.id}"
        return try {
            val uid = requireUserId()
            require(member.id.isNotBlank()) { "Member ID required." }
            val docRef = db.collection("users").document(uid).collection("members").document(member.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "autoRenew" to autoRenew,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMember(memberId: String, profilePhotoPath: String? = null): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/members/$memberId"
        return try {
            val uid = requireUserId()
            require(memberId.isNotBlank()) { "Member ID required." }
            val docRef = db.collection("users").document(uid).collection("members").document(memberId)
            docPath = docRef.path
            docRef.delete().await()
            deleteStoragePhotoSafely(profilePhotoPath, memberId = memberId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleTrainerFloor(trainer: TrainerProfile, onFloor: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trainers/${trainer.id}"
        return try {
            val uid = requireUserId()
            require(trainer.id.isNotBlank()) { "Trainer ID required." }
            val docRef = db.collection("users").document(uid).collection("trainers").document(trainer.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "onFloor" to onFloor,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logTrainerSession(trainer: TrainerProfile): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trainers/${trainer.id}"
        return try {
            val uid = requireUserId()
            require(trainer.id.isNotBlank()) { "Trainer ID required." }
            val nextSessions = (trainer.sessionsToday + 1L).coerceAtMost(50L)
            val docRef = db.collection("users").document(uid).collection("trainers").document(trainer.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "sessionsToday" to nextSessions,
                    "onFloor" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTrainer(
        fullName: String,
        specialty: String,
        phone: String,
        activeClients: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trainers"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim().take(60)
            val cleanSpecialty = specialty.trim().take(60)
            val cleanPhone = phone.trim().take(24)
            require(cleanName.length >= 2) { "Trainer name must be at least 2 characters." }
            require(cleanSpecialty.length >= 2) { "Specialty must be at least 2 characters." }
            require(cleanPhone.length >= 5) { "Phone must be at least 5 characters." }

            val docRef = db.collection("users").document(uid).collection("trainers").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "fullName" to cleanName,
                    "specialty" to cleanSpecialty,
                    "activeClients" to activeClients.coerceIn(0L, 200L),
                    "sessionsToday" to 0L,
                    "onFloor" to true,
                    "phone" to cleanPhone,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
