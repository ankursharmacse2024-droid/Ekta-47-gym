package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.StaffRegistrationStore
import com.example.data.ApexRepository
import com.example.data.CloudSyncState
import com.example.data.NetworkMonitor
import com.example.model.BillingDashboardMetrics
import com.example.model.CheckInLog
import com.example.model.CheckInOutcome
import com.example.model.FacilityProfile
import com.example.model.GymContactDetails
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
import com.example.model.UiState
import com.example.model.formatMoneyCents
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RosterFilter(val label: String) {
    ALL("All members"),
    ACTIVE("Active"),
    EXPIRING("Expiring Soon"),
    EXPIRED("Expired"),
    BLOCKED("Blocked")
}

enum class RosterSortOrder(val label: String) {
    NEEDS_ATTENTION("Needs Attention First"),
    NEWEST_ID("Newest ID First"),
    NAME_AZ("Name (A–Z)"),
    DUE_HIGH_LOW("Due Amount High–Low")
}

enum class PlanFilter(val label: String, val tier: MembershipTier?) {
    ALL_PLANS("All Plans", null),
    BLACK_CARD("12M Annual VIP", MembershipTier.BLACK_CARD),
    PERFORMANCE("6M Pro Fitness", MembershipTier.PERFORMANCE),
    FOUNDATION("3M Strength", MembershipTier.FOUNDATION)
}

class ApexViewModel(
    private val repository: ApexRepository,
    private val currentUserId: String,
    managerDisplayName: String?,
    appContext: Context? = null
) : ViewModel() {

    val networkMonitor: NetworkMonitor? = appContext?.let { NetworkMonitor(it) }

    val cloudSyncState: StateFlow<CloudSyncState> = networkMonitor?.syncState
        ?: MutableStateFlow(CloudSyncState.SYNCED).asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor?.isOnline
        ?: MutableStateFlow(true).asStateFlow()

    private val prefs = appContext?.getSharedPreferences(
        "ekta_gym_prefs_$currentUserId",
        Context.MODE_PRIVATE
    )

    val facilityProfile: StateFlow<UiState<FacilityProfile?>> =
        repository.observeFacilityProfile(currentUserId)
            .map<FacilityProfile?, UiState<FacilityProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing facility profile", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load facility profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val membersState: StateFlow<UiState<List<GymMember>>> =
        repository.observeMembers(currentUserId)
            .map<List<GymMember>, UiState<List<GymMember>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing members", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load members"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val checkInsState: StateFlow<UiState<List<CheckInLog>>> =
        repository.observeCheckIns(currentUserId)
            .map<List<CheckInLog>, UiState<List<CheckInLog>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing check-ins", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load check-in feed"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val trainersState: StateFlow<UiState<List<TrainerProfile>>> =
        repository.observeTrainers(currentUserId)
            .map<List<TrainerProfile>, UiState<List<TrainerProfile>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing trainers", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load trainers"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val billingDashboardState: StateFlow<UiState<BillingDashboardMetrics>> =
        repository.observeBillingDashboard(currentUserId)
            .map<BillingDashboardMetrics, UiState<BillingDashboardMetrics>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing billing dashboard", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load billing telemetry"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val revenueHistoryState: StateFlow<UiState<List<MonthlyRevenueRecord>>> =
        repository.observeRevenueHistory(currentUserId)
            .map<List<MonthlyRevenueRecord>, UiState<List<MonthlyRevenueRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing revenue history", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load monthly revenue trends"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val gymSettingsState: StateFlow<UiState<GymSettings>> =
        repository.observeGymSettings(currentUserId)
            .map<GymSettings, UiState<GymSettings>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing gym settings", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load gym settings"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val membershipPlansState: StateFlow<UiState<List<MembershipPlan>>> =
        repository.observeMembershipPlans(currentUserId)
            .map<List<MembershipPlan>, UiState<List<MembershipPlan>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing membership plans", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load membership plans"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val notificationSettingsState: StateFlow<UiState<NotificationSettings>> =
        repository.observeNotificationSettings(currentUserId)
            .map<NotificationSettings, UiState<NotificationSettings>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing notification settings", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load reminder settings"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val paymentSettingsState: StateFlow<UiState<PaymentSettings>> =
        repository.observePaymentSettings(currentUserId)
            .map<PaymentSettings, UiState<PaymentSettings>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing payment settings", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load payment settings"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val transactionsState: StateFlow<UiState<List<MembershipTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<MembershipTransaction>, UiState<List<MembershipTransaction>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing membership transactions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load payment receipts"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _memberIdOnlySearch = MutableStateFlow(false)
    val memberIdOnlySearch: StateFlow<Boolean> = _memberIdOnlySearch.asStateFlow()

    private val _rosterFilter = MutableStateFlow(RosterFilter.ALL)
    val rosterFilter: StateFlow<RosterFilter> = _rosterFilter.asStateFlow()

    private val _rosterSortOrder = MutableStateFlow(RosterSortOrder.NEEDS_ATTENTION)
    val rosterSortOrder: StateFlow<RosterSortOrder> = _rosterSortOrder.asStateFlow()

    private val _planFilter = MutableStateFlow(PlanFilter.ALL_PLANS)
    val planFilter: StateFlow<PlanFilter> = _planFilter.asStateFlow()

    private val _selectedGate = MutableStateFlow("Main Turnstile A")
    val selectedGate: StateFlow<String> = _selectedGate.asStateFlow()

    private val _gymContactDetails = MutableStateFlow(loadGymContactDetails())
    val gymContactDetails: StateFlow<GymContactDetails> = _gymContactDetails.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        val pendingReg = appContext?.let { StaffRegistrationStore.consumePendingRegistration(it) }
        val preferredRole = appContext?.let { StaffRegistrationStore.getSelectedLoginRole(it) }
        val roleToApply = pendingReg?.role ?: preferredRole
        val nameToApply = pendingReg?.staffName?.takeIf { it.isNotBlank() } ?: managerDisplayName
        val facilityToApply = pendingReg?.facilityName

        viewModelScope.launch {
            repository.ensureFacilitySeeded(
                userId = currentUserId,
                managerDisplayName = nameToApply,
                initialRole = roleToApply,
                initialFacilityName = facilityToApply
            ).onFailure { err ->
                _events.tryEmit(err.message ?: "Failed to initialize facility workspace")
            }
        }
    }

    private fun loadGymContactDetails(): GymContactDetails {
        return GymContactDetails(
            phone = prefs?.getString("gym_phone", "").orEmpty(),
            email = prefs?.getString("gym_email", "").orEmpty(),
            address = prefs?.getString("gym_address", "").orEmpty(),
            openingHours = prefs?.getString("gym_hours", "").orEmpty()
        )
    }

    fun saveGymContactDetails(phone: String, email: String, address: String, openingHours: String) {
        val updated = GymContactDetails(
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            openingHours = openingHours.trim()
        )
        prefs?.edit()
            ?.putString("gym_phone", updated.phone)
            ?.putString("gym_email", updated.email)
            ?.putString("gym_address", updated.address)
            ?.putString("gym_hours", updated.openingHours)
            ?.apply()
        _gymContactDetails.value = updated
        _events.tryEmit("Saved gym contact & hours")
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleMemberIdOnlySearch(enabled: Boolean) {
        _memberIdOnlySearch.value = enabled
    }

    fun updateRosterFilter(filter: RosterFilter) {
        _rosterFilter.value = filter
    }

    fun updateRosterSortOrder(sort: RosterSortOrder) {
        _rosterSortOrder.value = sort
    }

    fun updatePlanFilter(plan: PlanFilter) {
        _planFilter.value = plan
    }

    fun selectGate(gate: String) {
        _selectedGate.value = gate
    }

    fun checkInMember(member: GymMember) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.checkInMember(member, _selectedGate.value)
                .onSuccess { outcome ->
                    val message = when (outcome) {
                        CheckInOutcome.GRANTED -> "${member.fullName} (${member.displayMemberId}) checked in at ${_selectedGate.value}"
                        CheckInOutcome.WARNING -> "${member.fullName} checked in (Expires in ${member.daysRemaining}d)"
                        CheckInOutcome.DENIED -> "Access denied for ${member.fullName} — membership expired or blocked"
                    }
                    _events.tryEmit(message)
                }
                .onFailure { err ->
                    _events.tryEmit(err.message ?: "Attendance log failed")
                }
            _isBusy.value = false
        }
    }

    private fun requireAdminAccess(actionLabel: String): Boolean {
        val currentRole = (facilityProfile.value as? UiState.Success)?.data?.roleEnum ?: StaffAccessRole.ADMIN
        if (currentRole != StaffAccessRole.ADMIN) {
            _events.tryEmit("Admin access required to $actionLabel. Switch back to Administrator mode in Gym Settings.")
            return false
        }
        return true
    }

    fun checkInByCode(codeInput: String, members: List<GymMember>) {
        val normalized = codeInput.trim().uppercase().removePrefix("#")
        val digitsOnly = normalized.filter { it.isDigit() }
        val match = members.firstOrNull { member ->
            val memberNorm = member.memberCode.trim().uppercase().removePrefix("#")
            memberNorm == normalized ||
                (digitsOnly.isNotEmpty() && member.numericMemberNumber?.toString() == digitsOnly)
        }
        if (match == null) {
            _events.tryEmit("No member found for ID #$normalized")
        } else {
            checkInMember(match)
        }
    }

    fun renewMember(
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
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.renewMember(
                member = member,
                selectedPlan = selectedPlan,
                fallbackTier = fallbackTier,
                additionalDays = additionalDays,
                chargedAmountCents = chargedAmountCents,
                discountCents = discountCents,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                notes = notes,
                autoRenew = autoRenew
            ).onSuccess {
                val planTitle = selectedPlan?.name ?: fallbackTier.planTitle
                val amtStr = chargedAmountCents?.let { " • ${formatMoneyCents(it)}" }.orEmpty()
                _events.tryEmit("Renewed ${member.fullName} (+${additionalDays}d • $planTitle$amtStr)")
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Renewal failed")
            }
            _isBusy.value = false
        }
    }

    fun addMember(
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
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.addMember(
                memberCode = memberCode.removePrefix("#"),
                fullName = fullName,
                phone = phone,
                email = email,
                selectedPlan = selectedPlan,
                fallbackTier = fallbackTier,
                daysRemaining = daysRemaining,
                chargedAmountCents = chargedAmountCents,
                discountCents = discountCents,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                notes = notes,
                lockerNumber = lockerNumber,
                trainerName = trainerName,
                autoRenew = autoRenew,
                photoBytes = photoBytes
            ).onSuccess {
                _events.tryEmit("Added $fullName (#${memberCode.removePrefix("#")})")
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Failed to register member")
            }
            _isBusy.value = false
        }
    }

    fun updateMemberPhoto(member: GymMember, imageBytes: ByteArray?) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateMemberPhoto(member, imageBytes)
                .onSuccess {
                    _events.tryEmit(
                        if (imageBytes != null) "Updated profile photo for ${member.fullName}"
                        else "Removed profile photo for ${member.fullName}"
                    )
                }
                .onFailure { err ->
                    _events.tryEmit(err.message ?: "Failed to update profile photo")
                }
            _isBusy.value = false
        }
    }

    fun updateMemberDetails(
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
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateMemberDetails(
                member = member,
                fullName = fullName,
                phone = phone,
                email = email,
                selectedPlan = selectedPlan,
                tier = tier,
                lockerNumber = lockerNumber,
                trainerName = trainerName,
                accessEnabled = accessEnabled,
                autoRenew = autoRenew
            ).onSuccess {
                _events.tryEmit("Updated ${member.fullName}")
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Update failed")
            }
            _isBusy.value = false
        }
    }

    fun toggleAccess(member: GymMember, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleMemberAccess(member, enabled)
                .onSuccess {
                    _events.tryEmit(
                        if (enabled) "Unblocked ${member.fullName}"
                        else "Blocked ${member.fullName}"
                    )
                }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to update access") }
        }
    }

    fun toggleAutoRenew(member: GymMember, autoRenew: Boolean) {
        viewModelScope.launch {
            repository.toggleMemberAutoRenew(member, autoRenew)
                .onSuccess {
                    _events.tryEmit(
                        if (autoRenew) "Auto-renew enabled for ${member.fullName}"
                        else "Auto-renew disabled for ${member.fullName}"
                    )
                }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to update auto-renew") }
        }
    }

    fun deleteMember(member: GymMember, onDeleted: () -> Unit = {}) {
        if (!requireAdminAccess("delete member records")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteMember(member.id, member.profilePhotoPath)
                .onSuccess {
                    _events.tryEmit("Removed ${member.fullName} (${member.displayMemberId})")
                    onDeleted()
                }
                .onFailure { err ->
                    _events.tryEmit(err.message ?: "Failed to delete member")
                }
            _isBusy.value = false
        }
    }

    fun toggleTrainerFloor(trainer: TrainerProfile, onFloor: Boolean) {
        viewModelScope.launch {
            repository.toggleTrainerFloor(trainer, onFloor)
                .onSuccess {
                    _events.tryEmit(
                        if (onFloor) "${trainer.fullName} is now on floor"
                        else "${trainer.fullName} marked off floor"
                    )
                }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to update staff status") }
        }
    }

    fun logTrainerSession(trainer: TrainerProfile) {
        viewModelScope.launch {
            repository.logTrainerSession(trainer)
                .onSuccess {
                    _events.tryEmit("Logged PT session for ${trainer.fullName}")
                }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to log session") }
        }
    }

    fun addTrainer(fullName: String, specialty: String, phone: String, activeClients: Long) {
        if (!requireAdminAccess("add staff or trainers")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.addTrainer(fullName, specialty, phone, activeClients)
                .onSuccess {
                    _events.tryEmit("Added staff member $fullName")
                }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to add staff") }
            _isBusy.value = false
        }
    }

    fun syncCurrentMonthRevenue(members: List<GymMember>, metrics: BillingDashboardMetrics) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.syncCurrentMonthRevenueSnapshot(members, metrics)
                .onSuccess { label ->
                    _events.tryEmit("Synced $label snapshot to Firestore reports")
                }
                .onFailure { err ->
                    _events.tryEmit(err.message ?: "Failed to sync monthly revenue snapshot")
                }
            _isBusy.value = false
        }
    }

    fun updateFacilityProfile(
        facilityName: String,
        managerName: String,
        dailyCheckInTarget: Long
    ) {
        if (!requireAdminAccess("update gym profile")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateFacilityProfile(facilityName, managerName, dailyCheckInTarget)
                .onSuccess {
                    _events.tryEmit("Updated gym profile settings")
                }
                .onFailure { err ->
                    _events.tryEmit(err.message ?: "Failed to update gym profile")
                }
            _isBusy.value = false
        }
    }

    fun saveGymSettings(
        settings: GymSettings,
        dailyCheckInTarget: Long? = null,
        successMessage: String = "Saved gym settings to cloud"
    ) {
        if (!requireAdminAccess("modify gym settings")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveGymSettings(settings, dailyCheckInTarget)
                .onSuccess { _events.tryEmit(successMessage) }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to save gym settings") }
            _isBusy.value = false
        }
    }

    fun uploadGymLogo(currentSettings: GymSettings, imageBytes: ByteArray) {
        if (!requireAdminAccess("change the gym logo")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.uploadGymLogo(currentSettings, imageBytes)
                .onSuccess { _events.tryEmit("Updated official gym logo") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to upload gym logo") }
            _isBusy.value = false
        }
    }

    fun removeGymLogo(currentSettings: GymSettings) {
        if (!requireAdminAccess("reset the gym logo")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.removeGymLogo(currentSettings)
                .onSuccess { _events.tryEmit("Restored default EKTA 47 GYM logo") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to reset gym logo") }
            _isBusy.value = false
        }
    }

    fun saveMembershipPlan(
        existingPlan: MembershipPlan?,
        name: String,
        durationMonths: Long,
        priceCents: Long,
        description: String,
        isActive: Boolean,
        nextSortOrder: Long
    ) {
        if (!requireAdminAccess("edit membership plans & pricing")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.upsertMembershipPlan(
                existingPlan = existingPlan,
                name = name,
                durationMonths = durationMonths,
                priceCents = priceCents,
                description = description,
                isActive = isActive,
                sortOrder = nextSortOrder
            ).onSuccess {
                _events.tryEmit(
                    if (existingPlan == null) "Created plan '$name'"
                    else "Updated plan '$name' (${formatMoneyCents(priceCents)})"
                )
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Failed to save membership plan")
            }
            _isBusy.value = false
        }
    }

    fun toggleMembershipPlanActive(plan: MembershipPlan, isActive: Boolean) {
        if (!requireAdminAccess("activate or deactivate plans")) return
        viewModelScope.launch {
            repository.toggleMembershipPlanActive(plan, isActive)
                .onSuccess {
                    _events.tryEmit(
                        if (isActive) "Activated '${plan.name}'"
                        else "Deactivated '${plan.name}'"
                    )
                }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to update plan status") }
        }
    }

    fun deleteMembershipPlan(
        plan: MembershipPlan,
        members: List<GymMember>,
        transactions: List<MembershipTransaction>
    ) {
        if (!requireAdminAccess("delete membership plans")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteMembershipPlanSafely(plan, members, transactions)
                .onSuccess { _events.tryEmit("Deleted plan '${plan.name}'") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Unable to delete plan") }
            _isBusy.value = false
        }
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        if (!requireAdminAccess("update notification & WhatsApp settings")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveNotificationSettings(settings)
                .onSuccess { _events.tryEmit("Saved reminder & WhatsApp templates") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to save reminder settings") }
            _isBusy.value = false
        }
    }

    fun savePaymentSettings(settings: PaymentSettings) {
        if (!requireAdminAccess("update payment settings")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.savePaymentSettings(settings)
                .onSuccess { _events.tryEmit("Saved payment & UPI settings") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to save payment settings") }
            _isBusy.value = false
        }
    }

    fun uploadUpiQrCode(currentSettings: PaymentSettings, imageBytes: ByteArray) {
        if (!requireAdminAccess("upload UPI QR code")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.uploadUpiQrCode(currentSettings, imageBytes)
                .onSuccess { _events.tryEmit("Uploaded UPI QR code") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to upload UPI QR code") }
            _isBusy.value = false
        }
    }

    fun removeUpiQrCode(currentSettings: PaymentSettings) {
        if (!requireAdminAccess("remove UPI QR code")) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.removeUpiQrCode(currentSettings)
                .onSuccess { _events.tryEmit("Removed UPI QR code") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Failed to remove UPI QR code") }
            _isBusy.value = false
        }
    }

    fun switchWorkspaceRole(role: StaffAccessRole) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.setWorkspaceRole(role)
                .onSuccess {
                    _events.tryEmit("Switched workspace mode to ${role.label}")
                }
                .onFailure { err ->
                    _events.tryEmit(err.message ?: "Failed to switch access role")
                }
            _isBusy.value = false
        }
    }

    fun getFormattedLastSync(): String {
        return networkMonitor?.getFormattedLastSyncTime() ?: "Cloud Synced (IST)"
    }

    fun resetGymBusinessData(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            networkMonitor?.markSyncing()
            repository.performFreshGymDataReset(currentUserId, appContext)
                .onSuccess { msg ->
                    networkMonitor?.markSyncSuccess()
                    _events.tryEmit(msg)
                    onComplete(msg)
                }
                .onFailure { err ->
                    networkMonitor?.markSyncFailed()
                    _events.tryEmit(err.message ?: "Failed to reset gym business records")
                }
            _isBusy.value = false
        }
    }

    companion object {
        private const val TAG = "ApexViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun filterMembers(
            members: List<GymMember>,
            query: String,
            filter: RosterFilter,
            planFilter: PlanFilter = PlanFilter.ALL_PLANS,
            sortOrder: RosterSortOrder = RosterSortOrder.NEEDS_ATTENTION,
            memberIdOnly: Boolean = false
        ): List<GymMember> {
            val q = query.trim().lowercase().removePrefix("#")
            val filtered = members.filter { member ->
                val matchesFilter = when (filter) {
                    RosterFilter.ALL -> true
                    RosterFilter.ACTIVE -> member.statusEnum == MembershipStatus.ACTIVE && member.accessEnabled
                    RosterFilter.EXPIRING -> member.statusEnum == MembershipStatus.EXPIRING
                    RosterFilter.EXPIRED -> member.statusEnum == MembershipStatus.EXPIRED
                    RosterFilter.BLOCKED -> !member.accessEnabled
                }
                val matchesPlan = planFilter.tier == null || member.tierEnum == planFilter.tier
                val cleanCode = member.memberCode.lowercase().removePrefix("#")
                val matchesQuery = if (q.isEmpty()) {
                    true
                } else if (memberIdOnly) {
                    cleanCode.contains(q)
                } else {
                    cleanCode.contains(q) ||
                        member.fullName.lowercase().contains(q) ||
                        member.phone.lowercase().contains(q) ||
                        (member.lockerNumber?.lowercase()?.contains(q) == true) ||
                        (member.trainerName?.lowercase()?.contains(q) == true)
                }
                matchesFilter && matchesPlan && matchesQuery
            }

            return when (sortOrder) {
                RosterSortOrder.NEEDS_ATTENTION -> filtered.sortedWith(
                    compareBy<GymMember> {
                        when {
                            !it.accessEnabled || it.statusEnum == MembershipStatus.EXPIRED -> 0
                            it.statusEnum == MembershipStatus.EXPIRING -> 1
                            else -> 2
                        }
                    }.thenBy { it.daysRemaining }
                )
                RosterSortOrder.NEWEST_ID -> filtered.sortedByDescending { it.numericMemberNumber ?: 0 }
                RosterSortOrder.NAME_AZ -> filtered.sortedBy { it.fullName.lowercase() }
                RosterSortOrder.DUE_HIGH_LOW -> filtered.sortedByDescending { it.dueAmountCents }
            }
        }
    }
}
