package com.smartexambox.security.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.smartexambox.security.security.BiometricAuthManager
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.components.SecurityTopAppBar
import com.smartexambox.security.ui.theme.CyberCyan
import com.smartexambox.security.ui.theme.ElectricBlue
import com.smartexambox.security.ui.theme.NavyBackground
import com.smartexambox.security.ui.theme.NavyBorder
import com.smartexambox.security.ui.theme.NavySurface
import com.smartexambox.security.ui.theme.NavySurfaceVariant
import com.smartexambox.security.ui.theme.StatusCritical
import com.smartexambox.security.ui.theme.StatusProtected
import com.smartexambox.security.ui.theme.StatusWarning
import com.smartexambox.security.ui.theme.TextPrimary
import com.smartexambox.security.ui.theme.TextSecondary

@Composable
fun BiometricAuthScreen(
    viewModel: MainSecurityViewModel,
    activity: FragmentActivity,
    onNavigate: (String) -> Unit
) {
    val biometricStatus = remember { viewModel.biometricAuthManager.canAuthenticate() }
    val isLockedOut by viewModel.isLockedOutFlow.collectAsState()
    var authError by remember { mutableStateOf<String?>(null) }
    var authSuccess by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "ringPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    fun launchPrompt() {
        authError = null
        viewModel.authenticateWithBiometrics(
            activity = activity,
            onSuccess = {
                authSuccess = true
                onNavigate(ScreenRoutes.VAULT)
            },
            onError = { authError = it }
        )
    }

    LaunchedEffect(Unit) {
        if (!isLockedOut && biometricStatus == BiometricAuthManager.BiometricStatus.AVAILABLE) {
            launchPrompt()
        }
    }

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Biometric Verification",
                subtitle = "Multi-Factor Paper Vault Access",
                showBackButton = true,
                onBackClick = { onNavigate(ScreenRoutes.DASHBOARD) }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "VAULT MULTI-FACTOR AUTH",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Biometric Identity Scan",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Place registered finger on sensor or verify facial biometric to decrypt and open the sealed question paper vault.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Pulsing Biometric Scanner Visualizer
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.08f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape)
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant)
                            .border(2.dp, if (authSuccess) StatusProtected else CyberCyan, CircleShape)
                            .clickable { launchPrompt() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (authSuccess) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint Sensor",
                            tint = if (authSuccess) StatusProtected else CyberCyan,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Hardware Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (biometricStatus) {
                                        BiometricAuthManager.BiometricStatus.AVAILABLE -> StatusProtected
                                        BiometricAuthManager.BiometricStatus.NOT_ENROLLED -> StatusWarning
                                        else -> StatusCritical
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Biometric Sensor Status",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (biometricStatus) {
                                    BiometricAuthManager.BiometricStatus.AVAILABLE -> "Hardware Available & Enrolled (Strong Security)"
                                    BiometricAuthManager.BiometricStatus.NOT_ENROLLED -> "No Biometrics Enrolled in Android Settings (Use PIN)"
                                    BiometricAuthManager.BiometricStatus.NO_HARDWARE -> "Biometric Hardware Not Present on Device"
                                    else -> "Biometric Sensor Unavailable"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                authError?.let {
                    Text(
                        text = it,
                        color = StatusCritical,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { launchPrompt() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = NavyBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trigger Biometric Prompt",
                        color = NavyBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onNavigate(ScreenRoutes.LOGIN_PIN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                ) {
                    Icon(imageVector = Icons.Default.Pin, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Fallback: Authenticate via PIN", color = ElectricBlue)
                }
            }
        }
    }
}
