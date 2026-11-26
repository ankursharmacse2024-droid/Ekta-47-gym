package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.PendingStaffRegistration
import com.example.auth.StaffRegistrationStore
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.model.StaffAccessRole
import com.example.ui.components.EktaBrandMark
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TopBarTurquoise
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.TurquoiseSoft
import com.example.ui.theme.TurquoiseText

private enum class AuthMode(val labelRes: Int, val icon: ImageVector) {
    LOGIN(R.string.auth_tab_login, Icons.AutoMirrored.Rounded.Login),
    REGISTER(R.string.auth_tab_register, Icons.Rounded.PersonAdd)
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSilent by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Login state
    var selectedLoginRole by remember {
        mutableStateOf(StaffRegistrationStore.getSelectedLoginRole(context))
    }

    // Registration state
    var regFullName by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf(StaffAccessRole.ADMIN) }
    var regFacilityName by remember { mutableStateOf("EKTA 47 GYM & FITNESS CENTRE") }
    var regPhone by remember { mutableStateOf("") }
    var regPin by remember { mutableStateOf("") }
    var regPinVisible by remember { mutableStateOf(false) }
    var regAgreedToTerms by remember { mutableStateOf(false) }

    // Validation error highlights
    var regNameError by remember { mutableStateOf<String?>(null) }
    var regPhoneError by remember { mutableStateOf<String?>(null) }
    var regPinError by remember { mutableStateOf<String?>(null) }
    var regTermsError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // EKTA 47 Brand Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TopBarTurquoise)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EktaBrandMark(
                                size = 60.dp,
                                useWhiteSurface = true
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.brand_full_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.brand_established) + " • SECTOR 47 GURUGRAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("auth_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = "Staff Access Help",
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "COMMERCIAL GYM STAFF & ADMIN ACCESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.auth_headline),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.auth_subhead),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode Toggle Tabs: Staff Login vs Staff Registration
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = SurfaceCard,
                        contentColor = RoyalBlueAccent,
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = RoyalBlueAccent,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(color = BorderSubtle)
                        }
                    ) {
                        AuthMode.entries.forEachIndexed { index, mode ->
                            val selected = selectedTabIndex == index
                            Tab(
                                selected = selected,
                                onClick = {
                                    selectedTabIndex = index
                                    errorMessage = null
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = mode.icon,
                                            contentDescription = null,
                                            tint = if (selected) RoyalBlueAccent else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = stringResource(mode.labelRes),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) RoyalBlueAccent else TextMuted
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .testTag("auth_tab_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    ErrorBanner(message = errorMessage!!)
                }

                if (selectedTabIndex == 0) {
                    // STAFF LOGIN MODE
                    StaffLoginCard(
                        selectedRole = selectedLoginRole,
                        onRoleSelect = { role ->
                            selectedLoginRole = role
                            StaffRegistrationStore.saveSelectedLoginRole(context, role)
                        },
                        isLoading = isLoading || isCheckingSilent,
                        onSignInClick = {
                            errorMessage = null
                            isLoading = true
                            StaffRegistrationStore.saveSelectedLoginRole(context, selectedLoginRole)
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        onSwitchToRegister = {
                            selectedTabIndex = 1
                            errorMessage = null
                        }
                    )
                } else {
                    // STAFF REGISTRATION MODE
                    StaffRegistrationCard(
                        fullName = regFullName,
                        onFullNameChange = {
                            regFullName = it
                            regNameError = null
                        },
                        fullNameError = regNameError,
                        role = regRole,
                        onRoleChange = { regRole = it },
                        facilityName = regFacilityName,
                        onFacilityNameChange = { regFacilityName = it },
                        phone = regPhone,
                        onPhoneChange = {
                            regPhone = it
                            regPhoneError = null
                        },
                        phoneError = regPhoneError,
                        pin = regPin,
                        onPinChange = {
                            regPin = it
                            regPinError = null
                        },
                        pinError = regPinError,
                        pinVisible = regPinVisible,
                        onTogglePinVisibility = { regPinVisible = !regPinVisible },
                        agreedToTerms = regAgreedToTerms,
                        onAgreedChange = {
                            regAgreedToTerms = it
                            regTermsError = null
                        },
                        termsError = regTermsError,
                        isLoading = isLoading,
                        onRegisterClick = {
                            focusManager.clearFocus()
                            errorMessage = null

                            // Validate registration input
                            var hasError = false
                            if (regFullName.trim().length < 2) {
                                regNameError = "Please enter staff member's full name (min 2 characters)"
                                hasError = true
                            }
                            if (regPhone.trim().length < 7) {
                                regPhoneError = "Please enter a valid staff contact phone number"
                                hasError = true
                            }
                            val cleanPin = regPin.trim().uppercase()
                            if (cleanPin.length < 4) {
                                regPinError = "Authorization PIN must be at least 4 characters"
                                hasError = true
                            } else if (cleanPin != "EKTA47" && cleanPin != "4747" && cleanPin != "1986" && cleanPin.length < 4) {
                                regPinError = "Invalid gym PIN. Contact management or use EKTA47 / 4747"
                                hasError = true
                            }
                            if (!regAgreedToTerms) {
                                regTermsError = "Please accept the staff member privacy and security terms"
                                hasError = true
                            }

                            if (hasError) return@StaffRegistrationCard

                            // Stage pending registration
                            val pending = PendingStaffRegistration(
                                staffName = regFullName.trim(),
                                role = regRole,
                                facilityName = regFacilityName.trim().ifBlank { "EKTA 47 GYM & FITNESS CENTRE" },
                                phone = regPhone.trim()
                            )
                            StaffRegistrationStore.savePendingRegistration(context, pending)

                            // Launch Google Sign-In with Firebase Auth
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        onSwitchToLogin = {
                            selectedTabIndex = 0
                            errorMessage = null
                        }
                    )
                }

                // Security & Infrastructure Assurance Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TurquoiseSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = TurquoiseText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.auth_security_guarantee),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Zero-trust Google Identity authentication, per-user workspace isolation, and automated turnstile audit trails.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        StaffAuthHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
private fun StaffLoginCard(
    selectedRole: StaffAccessRole,
    onRoleSelect: (StaffAccessRole) -> Unit,
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.auth_login_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.auth_login_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Role selection for shift clearance
            Text(
                text = stringResource(R.string.auth_select_role),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoleSelectionPill(
                    role = StaffAccessRole.ADMIN,
                    selected = selectedRole == StaffAccessRole.ADMIN,
                    icon = Icons.Rounded.AdminPanelSettings,
                    title = "Gym Admin",
                    subtitle = "Full Clearance",
                    onClick = { onRoleSelect(StaffAccessRole.ADMIN) },
                    modifier = Modifier.weight(1f)
                )

                RoleSelectionPill(
                    role = StaffAccessRole.STAFF,
                    selected = selectedRole == StaffAccessRole.STAFF,
                    icon = Icons.Rounded.Badge,
                    title = "Front Desk",
                    subtitle = "Turnstile Ops",
                    onClick = { onRoleSelect(StaffAccessRole.STAFF) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Clearance details summary
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selectedRole == StaffAccessRole.ADMIN) RoyalBlueAccent.copy(alpha = 0.08f) else TurquoiseSoft,
                border = BorderStroke(
                    1.dp,
                    if (selectedRole == StaffAccessRole.ADMIN) RoyalBlueAccent.copy(alpha = 0.25f) else TurquoisePrimary.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedRole == StaffAccessRole.ADMIN) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Badge,
                            contentDescription = null,
                            tint = if (selectedRole == StaffAccessRole.ADMIN) RoyalBlueAccent else TurquoiseText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (selectedRole == StaffAccessRole.ADMIN) "Administrator Clearance Active" else "Front Desk Staff Clearance Active",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRole == StaffAccessRole.ADMIN) RoyalBlueAccent else TurquoiseText
                        )
                    }
                    Text(
                        text = if (selectedRole == StaffAccessRole.ADMIN) {
                            "Unlocks member registration, membership plan pricing, billing accounts, trainer telemetry, and revenue financial reports."
                        } else {
                            "Optimized for high-speed turnstile attendance check-ins, member ID barcode lookups, collecting dues, and issuing WhatsApp receipts."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Capability preview rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AuthFeatureBullet(
                    icon = Icons.Rounded.HowToReg,
                    text = "High-speed attendance turnstile check-ins & pass keypad"
                )
                AuthFeatureBullet(
                    icon = Icons.Rounded.Groups,
                    text = "Instant member search by sequential ID (EKTA-1727)"
                )
                AuthFeatureBullet(
                    icon = Icons.Rounded.Assessment,
                    text = "Dues tracking, WhatsApp reminders & monthly revenue reports"
                )
            }

            HorizontalDivider(color = BorderSubtle)

            // Primary Google Sign-In Button
            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlueAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.signing_in),
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.auth_btn_login),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New staff member?",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                TextButton(
                    onClick = onSwitchToRegister,
                    modifier = Modifier.testTag("switch_to_register_button")
                ) {
                    Text(
                        text = "Register Staff Profile",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlueAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffRegistrationCard(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    fullNameError: String?,
    role: StaffAccessRole,
    onRoleChange: (StaffAccessRole) -> Unit,
    facilityName: String,
    onFacilityNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phoneError: String?,
    pin: String,
    onPinChange: (String) -> Unit,
    pinError: String?,
    pinVisible: Boolean,
    onTogglePinVisibility: () -> Unit,
    agreedToTerms: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    termsError: String?,
    isLoading: Boolean,
    onRegisterClick: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.auth_register_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.auth_register_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Staff Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text(stringResource(R.string.auth_staff_name)) },
                placeholder = { Text("e.g. Rohan Sharma") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = TextMuted)
                },
                isError = fullNameError != null,
                supportingText = {
                    if (fullNameError != null) {
                        Text(text = fullNameError, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(8.dp),
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_staff_name")
            )

            // Staff Role Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.auth_select_role),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoleSelectionPill(
                        role = StaffAccessRole.ADMIN,
                        selected = role == StaffAccessRole.ADMIN,
                        icon = Icons.Rounded.AdminPanelSettings,
                        title = "Gym Admin",
                        subtitle = "Owner / Manager",
                        onClick = { onRoleChange(StaffAccessRole.ADMIN) },
                        modifier = Modifier.weight(1f)
                    )

                    RoleSelectionPill(
                        role = StaffAccessRole.STAFF,
                        selected = role == StaffAccessRole.STAFF,
                        icon = Icons.Rounded.Badge,
                        title = "Front Desk",
                        subtitle = "Operations Staff",
                        onClick = { onRoleChange(StaffAccessRole.STAFF) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Facility / Branch Name
            OutlinedTextField(
                value = facilityName,
                onValueChange = onFacilityNameChange,
                label = { Text(stringResource(R.string.auth_facility_name)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Business, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(8.dp),
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_facility_name")
            )

            // Staff Contact Phone
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.auth_staff_phone)) },
                placeholder = { Text("e.g. +91 98765 43210") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, tint = TextMuted)
                },
                isError = phoneError != null,
                supportingText = {
                    if (phoneError != null) {
                        Text(text = phoneError, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(8.dp),
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_staff_phone")
            )

            // Gym Security Authorization PIN
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                label = { Text(stringResource(R.string.auth_auth_code)) },
                placeholder = { Text("e.g. EKTA47 or 4747") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePinVisibility) {
                        Icon(
                            imageVector = if (pinVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (pinVisible) "Hide PIN" else "Show PIN",
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = pinError != null,
                supportingText = {
                    Text(
                        text = pinError ?: stringResource(R.string.auth_auth_code_hint),
                        color = if (pinError != null) MaterialTheme.colorScheme.error else TextMuted
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(8.dp),
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_auth_pin")
            )

            // Terms & Code of Conduct Checkbox
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAgreedChange(!agreedToTerms) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = onAgreedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = RoyalBlueAccent,
                        uncheckedColor = TextMuted
                    ),
                    modifier = Modifier.testTag("reg_terms_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = stringResource(R.string.auth_terms_agreement),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (termsError != null) MaterialTheme.colorScheme.error else TextPrimary
                    )
                    if (termsError != null) {
                        Text(
                            text = termsError,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle)

            // Complete Registration & Sign In Button
            Button(
                onClick = onRegisterClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlueAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("register_staff_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Registering & Authenticating…",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.auth_btn_register),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have staff access?",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                TextButton(
                    onClick = onSwitchToLogin,
                    modifier = Modifier.testTag("switch_to_login_button")
                ) {
                    Text(
                        text = "Sign In Instead",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlueAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleSelectionPill(
    role: StaffAccessRole,
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) RoyalBlueAccent.copy(alpha = 0.08f) else SurfaceCanvas
        ),
        border = BorderStroke(
            1.5.dp,
            if (selected) RoyalBlueAccent else BorderSubtle
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("role_select_${role.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) RoyalBlueAccent else TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) RoyalBlueAccent else TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = RoyalBlueAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthFeatureBullet(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TurquoisePrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun StaffAuthHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = RoyalBlueAccent
                )
                Text(
                    text = stringResource(R.string.auth_help_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "EKTA 47 GYM & FITNESS CENTRE (Since 1986)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoiseText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Welcome to the EKTA 47 commercial fitness facility workspace. Access is restricted to authorized gym administrators, managers, and front-desk personnel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "Staff Duty Clearances:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "• Gym Administrator: Full privileges including creating/deleting members, setting membership plan prices, modifying UPI accounts, managing trainers, and exporting revenue reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "• Front Desk Staff: Operational turnstile check-in clearance, attendee keypad, member roster lookup, dues renewal, and WhatsApp reminder alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "Staff Onboarding PIN:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "New staff members must enter the facility authorization key (default: EKTA47 or 4747). If you do not have a PIN, contact the General Manager at Sector 47 Gurugram.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent)
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RoyalBlueAccent,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = RoyalBlueAccent,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
