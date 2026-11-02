package com.smartexambox.security.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexambox.security.data.model.SecurityStatus
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.components.SecurityStatusBadge
import com.smartexambox.security.ui.components.SecurityTopAppBar
import com.smartexambox.security.ui.theme.CyberCyan
import com.smartexambox.security.ui.theme.ElectricBlue
import com.smartexambox.security.ui.theme.IndigoShield
import com.smartexambox.security.ui.theme.NavyBackground
import com.smartexambox.security.ui.theme.NavyBorder
import com.smartexambox.security.ui.theme.NavySurface
import com.smartexambox.security.ui.theme.NavySurfaceVariant
import com.smartexambox.security.ui.theme.StatusCritical
import com.smartexambox.security.ui.theme.StatusProtected
import com.smartexambox.security.ui.theme.StatusWarning
import com.smartexambox.security.ui.theme.TextPrimary
import com.smartexambox.security.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var isAdminLoggedIn by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    val device by viewModel.deviceRegistrationFlow.collectAsState()
    val geofence by viewModel.activeGeofenceFlow.collectAsState()
    val riskAssessment by viewModel.riskAssessment.collectAsState()
    val sync by viewModel.syncState.collectAsState()
    val stats by viewModel.statsFlow.collectAsState()
    val location by viewModel.locationState.collectAsState()

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Administrator Portal",
                subtitle = "VELS University Exam Control Cell",
                showBackButton = true,
                onBackClick = { onNavigate(ScreenRoutes.DASHBOARD) }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        if (!isAdminLoggedIn) {
            // Admin Login Gate
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NavySurfaceVariant)
                        .border(1.dp, CyberCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Admin Login",
                        tint = CyberCyan,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Administrator Authorization",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "Enter supervisory credentials to access device fleet, geofence parameters, and audit controls.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = adminPasswordInput,
                    onValueChange = {
                        adminPasswordInput = it
                        loginError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Supervisory Password", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                loginError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = StatusCritical, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        // Default college supervisor demo password: "admin" or "vels2026"
                        if (adminPasswordInput.trim().lowercase() in listOf("admin", "vels2026", "1234")) {
                            isAdminLoggedIn = true
                        } else {
                            loginError = "Invalid Supervisory Password! (Hint: 'admin' or 'vels2026')"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Log In as Administrator", color = NavyBackground, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Evaluation Demo Passcode: 'admin'",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        } else {
            // --- ADMINISTRATOR CONSOLE ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                // Registered Device Overview Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = device.deviceId,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                SecurityStatusBadge(status = riskAssessment.status, isCompact = true)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(text = "Assigned Center: ${device.centerName}", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "Department: ${device.department}", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "Chief Superintendent: ${device.supervisorName}", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = String.format(Locale.US, "Current GPS: %.5f° N, %.5f° E", location.latitude, location.longitude),
                                color = ElectricBlue,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Cloud Synchronization Status
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = IndigoShield, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Cloud Telemetry Synchronization", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                if (sync.isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberCyan, strokeWidth = 2.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = sync.lastSyncResult, color = TextSecondary, fontSize = 11.sp)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.triggerCloudSync() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoShield),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "Force Cloud Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.exportAuditLogs { file ->
                                            Toast.makeText(context, "Exported: ${file.name}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                                ) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Export CSV", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Configuration Quick Links
                item {
                    Text(
                        text = "CONFIGURATION & MANAGEMENT",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdminToolCard(
                            title = "Device Registration",
                            subtitle = "Update ID / Model",
                            icon = Icons.Default.AppRegistration,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(ScreenRoutes.REGISTRATION) }
                        )

                        AdminToolCard(
                            title = "Geofence Perimeter",
                            subtitle = "${geofence.radiusMeters.toInt()}m Campus Radius",
                            icon = Icons.Default.Tune,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(ScreenRoutes.GEOFENCE) }
                        )
                    }
                }

                // --- VIVA DEMONSTRATION SUITE ---
                item {
                    Text(
                        text = "COLLEGE VIVA DEMONSTRATION SUITE (DEMO DATA)",
                        color = StatusWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusWarning.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Simulate real-time security scenarios for college project evaluation:",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            DemoActionButton(
                                title = "1. Simulate 3x Failed PIN (Triggers Lockout)",
                                color = StatusCritical,
                                onClick = { viewModel.runSimulatedFailedAuth() }
                            )

                            DemoActionButton(
                                title = "2. Simulate Geofence Breach (Depart Campus)",
                                color = StatusCritical,
                                onClick = { viewModel.runSimulatedGeofenceViolation() }
                            )

                            DemoActionButton(
                                title = "3. Simulate Wi-Fi Cut (Offline Queue Buffer)",
                                color = StatusWarning,
                                onClick = { viewModel.runSimulatedNetworkDisconnection() }
                            )

                            DemoActionButton(
                                title = "4. Simulate 2FA Biometric Unlock (Open Vault)",
                                color = StatusProtected,
                                onClick = { viewModel.runSimulatedSuccessfulAuth() }
                            )

                            DemoActionButton(
                                title = "5. Simulate Admin Alert Acknowledgement",
                                color = CyberCyan,
                                onClick = { viewModel.runSimulatedAlertAcknowledgement() }
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.clearAllLogs() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF271313)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = StatusCritical)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reset / Clear All Security Audit Records", color = StatusCritical, fontSize = 12.sp)
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun AdminToolCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, NavyBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun DemoActionButton(
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = title, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}
