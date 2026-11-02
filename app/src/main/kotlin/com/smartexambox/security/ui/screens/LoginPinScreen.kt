package com.smartexambox.security.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.components.LockoutWarningBanner
import com.smartexambox.security.ui.components.SecurityTopAppBar
import com.smartexambox.security.ui.theme.CyberCyan
import com.smartexambox.security.ui.theme.ElectricBlue
import com.smartexambox.security.ui.theme.NavyBackground
import com.smartexambox.security.ui.theme.NavyBorder
import com.smartexambox.security.ui.theme.NavySurface
import com.smartexambox.security.ui.theme.NavySurfaceVariant
import com.smartexambox.security.ui.theme.StatusCritical
import com.smartexambox.security.ui.theme.StatusProtected
import com.smartexambox.security.ui.theme.TextPrimary
import com.smartexambox.security.ui.theme.TextSecondary

@Composable
fun LoginPinScreen(
    viewModel: MainSecurityViewModel,
    activity: FragmentActivity,
    onNavigate: (String) -> Unit
) {
    val isPinConfigured = remember { viewModel.isPinConfigured() }
    val isLockedOut by viewModel.isLockedOutFlow.collectAsState()
    val lockoutSeconds by viewModel.lockoutRemainingSeconds.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var setupConfirmPin by remember { mutableStateOf("") }
    var isConfirmingSetup by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSetupMode by remember { mutableStateOf(!isPinConfigured) }

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = if (isSetupMode) "Security PIN Setup" else "Vault Authentication",
                subtitle = "VELS Exam Terminal Security"
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = isLockedOut) {
                    LockoutWarningBanner(remainingSeconds = lockoutSeconds)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NavySurfaceVariant)
                        .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = CyberCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        isSetupMode && !isConfirmingSetup -> "Create 6-Digit App PIN"
                        isSetupMode && isConfirmingSetup -> "Confirm 6-Digit PIN"
                        else -> "Enter Security Access PIN"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = if (isSetupMode) "Salted hash will be stored in Android Keystore"
                    else "Authenticate to access dashboard and digital question vault",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 0 until 6) {
                        val isFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) CyberCyan else NavySurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isFilled) CyberCyan else NavyBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                errorMessage?.let {
                    Text(
                        text = it,
                        color = StatusCritical,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            when (key) {
                                "BIO" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .padding(6.dp)
                                            .clip(CircleShape)
                                            .background(NavySurfaceVariant.copy(alpha = 0.5f))
                                            .clickable(enabled = !isLockedOut && !isSetupMode) {
                                                viewModel.authenticateWithBiometrics(
                                                    activity = activity,
                                                    onSuccess = { onNavigate(ScreenRoutes.DASHBOARD) },
                                                    onError = { errorMessage = it }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Biometric Login",
                                            tint = if (!isLockedOut && !isSetupMode) CyberCyan else TextSecondary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .padding(6.dp)
                                            .clip(CircleShape)
                                            .background(NavySurfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                if (pinInput.isNotEmpty()) {
                                                    pinInput = pinInput.dropLast(1)
                                                    errorMessage = null
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .padding(6.dp)
                                            .clip(CircleShape)
                                            .background(NavySurface)
                                            .border(1.dp, NavyBorder, CircleShape)
                                            .clickable(enabled = !isLockedOut && pinInput.length < 6) {
                                                if (pinInput.length < 6) {
                                                    pinInput += key
                                                    errorMessage = null
                                                    if (pinInput.length == 6) {
                                                        handlePinComplete(
                                                            pin = pinInput,
                                                            isSetupMode = isSetupMode,
                                                            isConfirmingSetup = isConfirmingSetup,
                                                            setupConfirmPin = setupConfirmPin,
                                                            onSetupNext = {
                                                                setupConfirmPin = pinInput
                                                                pinInput = ""
                                                                isConfirmingSetup = true
                                                            },
                                                            onSetupSuccess = {
                                                                viewModel.setupPin(pinInput)
                                                                isSetupMode = false
                                                                isConfirmingSetup = false
                                                                onNavigate(ScreenRoutes.DASHBOARD)
                                                            },
                                                            onVerify = {
                                                                viewModel.verifyPinAndAuthenticate(
                                                                    pin = pinInput,
                                                                    onSuccess = { onNavigate(ScreenRoutes.DASHBOARD) },
                                                                    onError = { err ->
                                                                        errorMessage = err
                                                                        pinInput = ""
                                                                    }
                                                                )
                                                            },
                                                            onError = { err ->
                                                                errorMessage = err
                                                                pinInput = ""
                                                            }
                                                        )
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = TextPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bypass for Evaluation Viva / Demo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            viewModel.setupPin("123456")
                            viewModel.verifyPinAndAuthenticate(
                                pin = "123456",
                                onSuccess = { onNavigate(ScreenRoutes.DASHBOARD) },
                                onError = {}
                            )
                        }
                    ) {
                        Text(
                            text = "College Demo Bypass (PIN: 123456)",
                            color = ElectricBlue,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun handlePinComplete(
    pin: String,
    isSetupMode: Boolean,
    isConfirmingSetup: Boolean,
    setupConfirmPin: String,
    onSetupNext: () -> Unit,
    onSetupSuccess: () -> Unit,
    onVerify: () -> Unit,
    onError: (String) -> Unit
) {
    if (isSetupMode) {
        if (!isConfirmingSetup) {
            onSetupNext()
        } else {
            if (pin == setupConfirmPin) {
                onSetupSuccess()
            } else {
                onError("PINs do not match! Please start over.")
            }
        }
    } else {
        onVerify()
    }
}
