package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.ApexRepository
import com.example.model.BillingDashboardMetrics
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipStatus
import com.example.model.NotificationSettings
import com.example.model.PaymentSettings
import com.example.model.StaffAccessRole
import com.example.model.UiState
import com.example.model.normalizeFacilityName
import com.example.ui.ApexViewModel
import com.example.ui.RosterFilter
import com.example.ui.components.ActiveGymBranding
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.AddTrainerDialog
import com.example.ui.components.EktaBrandMark
import com.example.ui.components.EktaTopBar
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.PassKeypadDialog
import com.example.ui.components.QuickHelpDialog
import com.example.ui.components.RenewMembershipDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GymSectionScreen
import com.example.ui.screens.MemberDetailScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RosterScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EktaGymTheme
import com.example.ui.theme.NavActiveContent
import com.example.ui.theme.NavHighlightBg
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.StatusExpiredText
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TopBarTurquoise
import com.example.ui.theme.TurquoisePrimary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EktaGymTheme {
                ApexAppRoot()
            }
        }
    }
}

private enum class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    MEMBERS("members", R.string.nav_members, Icons.Rounded.Groups, Icons.Outlined.Groups),
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    REPORTS("reports", R.string.nav_reports, Icons.Rounded.Assessment, Icons.Outlined.Assessment),
    GYM("gym", R.string.nav_gym, Icons.Rounded.FitnessCenter, Icons.Outlined.FitnessCenter)
}

private const val ROUTE_MEMBER_DETAIL = "member_detail/{memberId}"

@Composable
fun ApexAppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(650L)
        showSplash = false
    }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (showSplash) {
        SplashScreen()
    } else if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            ApexAuthenticatedShell(user = user)
        }
    }
}

@Composable
private fun ApexAuthenticatedShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ApexViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ApexViewModel(
                    repository = ApexRepository(db),
                    currentUserId = user.uid,
                    managerDisplayName = user.displayName,
                    appContext = app.applicationContext
                )
            }
        }
    )

    val facilityState by viewModel.facilityProfile.collectAsStateWithLifecycle()
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val checkInsState by viewModel.checkInsState.collectAsStateWithLifecycle()
    val trainersState by viewModel.trainersState.collectAsStateWithLifecycle()
    val billingState by viewModel.billingDashboardState.collectAsStateWithLifecycle()
    val revenueHistoryState by viewModel.revenueHistoryState.collectAsStateWithLifecycle()
    val gymSettingsState by viewModel.gymSettingsState.collectAsStateWithLifecycle()
    val membershipPlansState by viewModel.membershipPlansState.collectAsStateWithLifecycle()
    val notificationSettingsState by viewModel.notificationSettingsState.collectAsStateWithLifecycle()
    val paymentSettingsState by viewModel.paymentSettingsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val memberIdOnly by viewModel.memberIdOnlySearch.collectAsStateWithLifecycle()
    val rosterFilter by viewModel.rosterFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.rosterSortOrder.collectAsStateWithLifecycle()
    val planFilter by viewModel.planFilter.collectAsStateWithLifecycle()
    val selectedGate by viewModel.selectedGate.collectAsStateWithLifecycle()
    val gymContactDetails by viewModel.gymContactDetails.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var showKeypadDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAddTrainerDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var renewTargetMember by remember { mutableStateOf<GymMember?>(null) }
    var tabletSelectedMemberId by remember { mutableStateOf<String?>(null) }

    val facilityProfile = (facilityState as? UiState.Success)?.data
    val members = (membersState as? UiState.Success)?.data.orEmpty()
    val checkIns = (checkInsState as? UiState.Success)?.data.orEmpty()
    val trainers = (trainersState as? UiState.Success)?.data.orEmpty()
    val billingMetrics = (billingState as? UiState.Success)?.data ?: BillingDashboardMetrics()
    val gymSettings = (gymSettingsState as? UiState.Success)?.data ?: GymSettings()
    val membershipPlans = (membershipPlansState as? UiState.Success)?.data.orEmpty()
    val notificationSettings = (notificationSettingsState as? UiState.Success)?.data ?: NotificationSettings()
    val paymentSettings = (paymentSettingsState as? UiState.Success)?.data ?: PaymentSettings()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val isAdmin = (facilityProfile?.roleEnum ?: StaffAccessRole.ADMIN) == StaffAccessRole.ADMIN

    val facilityName = gymSettings.gymName.takeIf { it.isNotBlank() }
        ?: normalizeFacilityName(facilityProfile?.facilityName)

    LaunchedEffect(gymSettings) {
        ActiveGymBranding.logoUrl = gymSettings.logoUrl
        ActiveGymBranding.currencyCode = gymSettings.currency.ifBlank { "INR" }
        ActiveGymBranding.gymName = gymSettings.gymName.ifBlank { "EKTA 47 GYM & FITNESS CENTRE" }
    }

    val alertCount = remember(members) {
        members.count { it.statusEnum != MembershipStatus.ACTIVE || !it.accessEnabled }
    }

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(TopTab.MEMBERS.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute?.startsWith("member_detail") != true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceCard,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                // Turquoise/Blue EKTA 47 GYM Drawer Brand Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TopBarTurquoise)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EktaBrandMark(
                            size = 58.dp,
                            useWhiteSurface = true,
                            customLogoUrl = gymSettings.logoUrl
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.brand_established),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Text(
                        text = facilityName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    val adminLine = listOfNotNull(
                        user.displayName?.takeIf { it.isNotBlank() },
                        user.email?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (adminLine.isNotEmpty()) {
                        Text(
                            text = adminLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TopTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationDrawerItem(
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigateTopLevel(tab.route)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        badge = if (tab == TopTab.DASHBOARD && alertCount > 0) {
                            {
                                Badge(containerColor = StatusExpiredAccent) {
                                    Text("$alertCount", color = Color.White)
                                }
                            }
                        } else null,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = NavHighlightBg,
                            selectedIconColor = NavActiveContent,
                            selectedTextColor = NavActiveContent
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_${tab.route}")
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = BorderSubtle
                )

                NavigationDrawerItem(
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showKeypadDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Dialpad,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.action_record_attendance)) },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("drawer_pass_keypad")
                )

                NavigationDrawerItem(
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        signOutUser(credentialManager, scope)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = StatusExpiredText
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.action_sign_out),
                            color = StatusExpiredText
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("drawer_sign_out")
                )
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCanvas)
        ) {
            val isWideScreen = maxWidth >= 768.dp
            val isDetailRoute = currentRoute?.startsWith("member_detail") == true
            val activeTab = TopTab.entries.firstOrNull { it.route == currentRoute } ?: TopTab.MEMBERS

            Scaffold(
                containerColor = SurfaceCanvas,
                contentWindowInsets = WindowInsets.navigationBars,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (!isDetailRoute) {
                        EktaTopBar(
                            title = stringResource(activeTab.labelRes),
                            subtitle = facilityName,
                            alertCount = alertCount,
                            showMembersActions = activeTab == TopTab.MEMBERS,
                            archiveModeActive = rosterFilter == RosterFilter.BLOCKED || rosterFilter == RosterFilter.EXPIRED,
                            syncState = cloudSyncState,
                            lastSyncText = viewModel.getFormattedLastSync(),
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onAlertsClick = { navigateTopLevel(TopTab.DASHBOARD.route) },
                            onToggleArchiveMode = {
                                if (rosterFilter == RosterFilter.BLOCKED || rosterFilter == RosterFilter.EXPIRED) {
                                    viewModel.updateRosterFilter(RosterFilter.ALL)
                                } else {
                                    viewModel.updateRosterFilter(RosterFilter.EXPIRED)
                                }
                            },
                            onHelpClick = { showHelpDialog = true }
                        )
                    }
                },
                bottomBar = {
                    if (!isWideScreen && !isDetailRoute) {
                        NavigationBar(
                            containerColor = SurfaceCard,
                            tonalElevation = 2.dp
                        ) {
                            TopTab.entries.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NavActiveContent,
                                        selectedTextColor = NavActiveContent,
                                        indicatorColor = NavHighlightBg,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!isDetailRoute && (activeTab == TopTab.MEMBERS || activeTab == TopTab.DASHBOARD)) {
                        val isMembers = activeTab == TopTab.MEMBERS
                        ExtendedFloatingActionButton(
                            onClick = {
                                if (isMembers) showAddMemberDialog = true
                                else showKeypadDialog = true
                            },
                            containerColor = RoyalBlueAccent,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            icon = {
                                Icon(
                                    imageVector = if (isMembers) Icons.Rounded.Add else Icons.Rounded.HowToReg,
                                    contentDescription = null
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(
                                        if (isMembers) R.string.action_add_member
                                        else R.string.action_record_attendance
                                    ),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier.testTag("primary_fab")
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isBusy) {
                        LinearProgressIndicator(
                            color = TurquoisePrimary,
                            trackColor = SurfaceCard,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                    if (isWideScreen && !isDetailRoute) {
                        NavigationRail(
                            containerColor = SurfaceCard,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.padding(top = 8.dp))
                            TopTab.entries.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NavActiveContent,
                                        selectedTextColor = NavActiveContent,
                                        indicatorColor = NavHighlightBg,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                        VerticalDivider(color = BorderSubtle)
                    }

                    val openMemberDetail: (GymMember) -> Unit = { member ->
                        if (isWideScreen) {
                            tabletSelectedMemberId = member.id
                        } else {
                            navController.navigate("member_detail/${member.id}")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(if (isWideScreen && !isDetailRoute) 5f else 1f)
                            .fillMaxHeight()
                    ) {
                        when (val state = membersState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = TurquoisePrimary)
                                }
                            }
                            is UiState.Error -> {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    ErrorBanner(message = state.message)
                                }
                            }
                            is UiState.Success -> {
                                NavHost(
                                    navController = navController,
                                    startDestination = TopTab.MEMBERS.route
                                ) {
                                    composable(TopTab.MEMBERS.route) {
                                        RosterScreen(
                                            members = members,
                                            gymSettings = gymSettings,
                                            notificationSettings = notificationSettings,
                                            searchQuery = searchQuery,
                                            memberIdOnly = memberIdOnly,
                                            activeFilter = rosterFilter,
                                            sortOrder = sortOrder,
                                            planFilter = planFilter,
                                            onSearchChange = viewModel::updateSearchQuery,
                                            onToggleMemberIdOnly = viewModel::toggleMemberIdOnlySearch,
                                            onFilterChange = viewModel::updateRosterFilter,
                                            onSortChange = viewModel::updateRosterSortOrder,
                                            onPlanChange = viewModel::updatePlanFilter,
                                            onAddMember = { showAddMemberDialog = true },
                                            onCheckIn = viewModel::checkInMember,
                                            onRenew = { renewTargetMember = it },
                                            onToggleBlock = { member ->
                                                viewModel.toggleAccess(member, !member.accessEnabled)
                                            },
                                            onDeleteMember = { member -> viewModel.deleteMember(member) },
                                            onMemberClick = openMemberDetail
                                        )
                                    }
                                    composable(TopTab.DASHBOARD.route) {
                                        DashboardScreen(
                                            members = members,
                                            checkIns = checkIns,
                                            billingState = billingState,
                                            gymSettings = gymSettings,
                                            notificationSettings = notificationSettings,
                                            selectedGate = selectedGate,
                                            onSelectGate = viewModel::selectGate,
                                            onOpenFilteredMembers = { filter ->
                                                viewModel.updateRosterFilter(filter)
                                                navigateTopLevel(TopTab.MEMBERS.route)
                                            },
                                            onOpenMemberDetail = openMemberDetail,
                                            onRenewMember = { renewTargetMember = it },
                                            onSyncCurrentMonth = { metrics ->
                                                viewModel.syncCurrentMonthRevenue(members, metrics)
                                            }
                                        )
                                    }
                                    composable(TopTab.REPORTS.route) {
                                        ReportsScreen(
                                            members = members,
                                            billingState = billingState,
                                            revenueHistoryState = revenueHistoryState,
                                            gymSettings = gymSettings,
                                            notificationSettings = notificationSettings,
                                            onRenewMember = { renewTargetMember = it },
                                            onOpenMemberDetail = openMemberDetail,
                                            onSyncCurrentMonth = { metrics ->
                                                viewModel.syncCurrentMonthRevenue(members, metrics)
                                            }
                                        )
                                    }
                                    composable(TopTab.GYM.route) {
                                        GymSectionScreen(
                                            facilityProfile = facilityProfile,
                                            gymSettings = gymSettings,
                                            membershipPlans = membershipPlans,
                                            notificationSettings = notificationSettings,
                                            paymentSettings = paymentSettings,
                                            gymContactDetails = gymContactDetails,
                                            adminEmail = user.email,
                                            adminDisplayName = user.displayName,
                                            members = members,
                                            trainers = trainers,
                                            billingMetrics = billingMetrics,
                                            onSaveGymSettings = { updated, target, msg ->
                                                viewModel.saveGymSettings(updated, target, msg)
                                            },
                                            onUploadGymLogo = { bytes ->
                                                viewModel.uploadGymLogo(gymSettings, bytes)
                                            },
                                            onRemoveGymLogo = {
                                                viewModel.removeGymLogo(gymSettings)
                                            },
                                            onSaveGymContact = viewModel::saveGymContactDetails,
                                            onSaveMembershipPlan = viewModel::saveMembershipPlan,
                                            onToggleMembershipPlanActive = viewModel::toggleMembershipPlanActive,
                                            onDeleteMembershipPlan = { plan ->
                                                viewModel.deleteMembershipPlan(plan, members, transactions)
                                            },
                                            onSaveNotificationSettings = viewModel::saveNotificationSettings,
                                            onSavePaymentSettings = viewModel::savePaymentSettings,
                                            onUploadUpiQrCode = { bytes ->
                                                viewModel.uploadUpiQrCode(paymentSettings, bytes)
                                            },
                                            onRemoveUpiQrCode = {
                                                viewModel.removeUpiQrCode(paymentSettings)
                                            },
                                            onSwitchWorkspaceRole = viewModel::switchWorkspaceRole,
                                            onToggleTrainerFloor = viewModel::toggleTrainerFloor,
                                            onLogTrainerSession = viewModel::logTrainerSession,
                                            onAddTrainer = { showAddTrainerDialog = true },
                                            onOpenKeypad = { showKeypadDialog = true },
                                            onSyncRevenueSnapshot = {
                                                viewModel.syncCurrentMonthRevenue(members, billingMetrics)
                                            },
                                            onSignOut = { signOutUser(credentialManager, scope) }
                                        )
                                    }
                                    composable(
                                        route = ROUTE_MEMBER_DETAIL,
                                        arguments = listOf(navArgument("memberId") { type = NavType.StringType })
                                    ) { entry ->
                                        val memberId = entry.arguments?.getString("memberId").orEmpty()
                                        val member = members.firstOrNull { it.id == memberId }
                                        if (member == null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                EmptyStatePanel(
                                                    title = "Member Unavailable",
                                                    body = "This member record may have been removed.",
                                                    actionLabel = "Return to Members",
                                                    onAction = { navController.popBackStack() }
                                                )
                                            }
                                        } else {
                                            MemberDetailScreen(
                                                member = member,
                                                memberCheckIns = checkIns.filter { it.memberId == member.id },
                                                plans = membershipPlans,
                                                memberTransactions = transactions.filter { it.memberId == member.id },
                                                gymSettings = gymSettings,
                                                notificationSettings = notificationSettings,
                                                isAdmin = isAdmin,
                                                showTopBackBar = true,
                                                onBack = { navController.popBackStack() },
                                                onCheckIn = { viewModel.checkInMember(member) },
                                                onRenew = { renewTargetMember = member },
                                                onToggleAccess = { enabled -> viewModel.toggleAccess(member, enabled) },
                                                onToggleAutoRenew = { auto -> viewModel.toggleAutoRenew(member, auto) },
                                                onUpdatePhoto = { bytes -> viewModel.updateMemberPhoto(member, bytes) },
                                                onSaveMemberDetails = { fullName, selectedPlan, tier, locker, trainer, phone, email ->
                                                    viewModel.updateMemberDetails(
                                                        member = member,
                                                        fullName = fullName,
                                                        phone = phone,
                                                        email = email,
                                                        selectedPlan = selectedPlan,
                                                        tier = tier,
                                                        lockerNumber = locker,
                                                        trainerName = trainer,
                                                        accessEnabled = member.accessEnabled,
                                                        autoRenew = member.autoRenew
                                                    )
                                                },
                                                onDelete = {
                                                    viewModel.deleteMember(member) {
                                                        navController.popBackStack()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tablet / Foldable Supporting Detail Tray
                    if (isWideScreen && !isDetailRoute) {
                        VerticalDivider(color = BorderSubtle)
                        val selectedMember = members.firstOrNull { it.id == tabletSelectedMemberId }
                            ?: members.firstOrNull()
                        Box(
                            modifier = Modifier
                                .weight(3.5f)
                                .fillMaxHeight()
                        ) {
                            if (selectedMember == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStatePanel(
                                        title = "Select a Member",
                                        body = "Tap any member card to inspect details, attendance, and payment status."
                                    )
                                }
                            } else {
                                MemberDetailScreen(
                                    member = selectedMember,
                                    memberCheckIns = checkIns.filter { it.memberId == selectedMember.id },
                                    plans = membershipPlans,
                                    memberTransactions = transactions.filter { it.memberId == selectedMember.id },
                                    gymSettings = gymSettings,
                                    notificationSettings = notificationSettings,
                                    isAdmin = isAdmin,
                                    showTopBackBar = false,
                                    onBack = {},
                                    onCheckIn = { viewModel.checkInMember(selectedMember) },
                                    onRenew = { renewTargetMember = selectedMember },
                                    onToggleAccess = { enabled -> viewModel.toggleAccess(selectedMember, enabled) },
                                    onToggleAutoRenew = { auto -> viewModel.toggleAutoRenew(selectedMember, auto) },
                                    onUpdatePhoto = { bytes -> viewModel.updateMemberPhoto(selectedMember, bytes) },
                                    onSaveMemberDetails = { fullName, selectedPlan, tier, locker, trainer, phone, email ->
                                        viewModel.updateMemberDetails(
                                            member = selectedMember,
                                            fullName = fullName,
                                            phone = phone,
                                            email = email,
                                            selectedPlan = selectedPlan,
                                            tier = tier,
                                            lockerNumber = locker,
                                            trainerName = trainer,
                                            accessEnabled = selectedMember.accessEnabled,
                                            autoRenew = selectedMember.autoRenew
                                        )
                                    },
                                    onDelete = {
                                        viewModel.deleteMember(selectedMember) {
                                            tabletSelectedMemberId = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (showKeypadDialog) {
        PassKeypadDialog(
            members = members,
            onDismiss = { showKeypadDialog = false },
            onCheckInCode = { code -> viewModel.checkInByCode(code, members) }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            existingMembers = members,
            plans = membershipPlans,
            gymSettings = gymSettings,
            paymentSettings = paymentSettings,
            isAdmin = isAdmin,
            trainerNames = trainers.map { it.fullName },
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { memberCode, fullName, phone, email, selectedPlan, fallbackTier, daysRemaining, chargedAmountCents, discountCents, paymentMethod, transactionRef, notes, lockerNumber, trainerName, autoRenew, photoBytes ->
                viewModel.addMember(
                    memberCode = memberCode,
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
                )
            }
        )
    }

    if (showAddTrainerDialog) {
        AddTrainerDialog(
            onDismiss = { showAddTrainerDialog = false },
            onConfirm = viewModel::addTrainer
        )
    }

    if (showHelpDialog) {
        QuickHelpDialog(onDismiss = { showHelpDialog = false })
    }

    renewTargetMember?.let { target ->
        RenewMembershipDialog(
            member = target,
            plans = membershipPlans,
            gymSettings = gymSettings,
            paymentSettings = paymentSettings,
            isAdmin = isAdmin,
            onDismiss = { renewTargetMember = null },
            onConfirm = { selectedPlan, fallbackTier, additionalDays, chargedAmountCents, discountCents, paymentMethod, transactionRef, notes, autoRenew ->
                viewModel.renewMember(
                    member = target,
                    selectedPlan = selectedPlan,
                    fallbackTier = fallbackTier,
                    additionalDays = additionalDays,
                    chargedAmountCents = chargedAmountCents,
                    discountCents = discountCents,
                    paymentMethod = paymentMethod,
                    transactionRef = transactionRef,
                    notes = notes,
                    autoRenew = autoRenew
                )
            }
        )
    }
}
