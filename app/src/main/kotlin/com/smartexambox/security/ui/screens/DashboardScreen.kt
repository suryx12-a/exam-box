package com.smartexambox.security.ui.screens

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexambox.security.data.model.SecurityStatus
import com.smartexambox.security.network.ConnectionStatus
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.components.DemoModeBanner
import com.smartexambox.security.ui.components.LockoutWarningBanner
import com.smartexambox.security.ui.components.MetricCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val riskAssessment by viewModel.riskAssessment.collectAsState()
    val location by viewModel.locationState.collectAsState()
    val network by viewModel.networkState.collectAsState()
    val battery by viewModel.batteryState.collectAsState()
    val stats by viewModel.statsFlow.collectAsState()
    val sync by viewModel.syncState.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val isLockedOut by viewModel.isLockedOutFlow.collectAsState()
    val lockoutSeconds by viewModel.lockoutRemainingSeconds.collectAsState()
    val demoMessage by viewModel.demoMessage.collectAsState()
    val pairingState by viewModel.pairingState.collectAsState()

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Smart Exam Security",
                subtitle = "VELS College Mobile Monitoring",
                status = riskAssessment.status,
                actions = {
                    IconButton(onClick = { viewModel.triggerCloudSync() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = CyberCyan
                        )
                    }
                    IconButton(onClick = { onNavigate(ScreenRoutes.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Laptop Connection Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            when (pairingState.connectionStatus) {
                                ConnectionStatus.CONNECTED -> StatusProtected.copy(alpha = 0.5f)
                                ConnectionStatus.SYNC_PENDING -> StatusWarning.copy(alpha = 0.5f)
                                ConnectionStatus.DISCONNECTED -> StatusCritical.copy(alpha = 0.5f)
                                else -> CyberCyan.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onNavigate(ScreenRoutes.REGISTRATION) },
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        when (pairingState.connectionStatus) {
                                            ConnectionStatus.CONNECTED -> StatusProtected
                                            ConnectionStatus.SYNC_PENDING -> StatusWarning
                                            ConnectionStatus.DISCONNECTED -> StatusCritical
                                            else -> TextSecondary
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when (pairingState.connectionStatus) {
                                        ConnectionStatus.CONNECTED -> "Laptop Monitor Connected"
                                        ConnectionStatus.SYNC_PENDING -> "Laptop: Sync Pending"
                                        ConnectionStatus.DISCONNECTED -> "Laptop Offline (Buffering Locally)"
                                        else -> "Wi-Fi Laptop Link Unpaired"
                                    },
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (pairingState.connectionStatus) {
                                        ConnectionStatus.CONNECTED ->
                                            "${pairingState.serverUrl.removePrefix("http://")} • ${pairingState.lastPingLatencyMs ?: 12}ms"
                                        ConnectionStatus.SYNC_PENDING ->
                                            "${pairingState.unsyncedCount} events queued in local SQLite"
                                        ConnectionStatus.DISCONNECTED ->
                                            "Events buffered locally • Tap to reconnect"
                                        else -> "Tap to pair with Laptop Monitor"
                                    },
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "Manage ›",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Lockout Warning if active
            if (isLockedOut) {
                item {
                    LockoutWarningBanner(remainingSeconds = lockoutSeconds)
                }
            }

            // Demo Mode Banner if active
            demoMessage?.let { msg ->
                item {
                    DemoModeBanner(message = msg, onDismiss = { viewModel.clearDemoMessage() })
                }
            }

            // --- 1. Threat & Security Status Gauge Card ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SYSTEM SECURITY STATUS",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = when (riskAssessment.status) {
                                        SecurityStatus.PROTECTED -> "Fully Protected"
                                        SecurityStatus.WARNING -> "Elevated Security Alert"
                                        SecurityStatus.CRITICAL -> "CRITICAL THREAT LEVEL"
                                    },
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            SecurityStatusBadge(status = riskAssessment.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Threat Meter Score Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Threat Risk Index: ${riskAssessment.score} / 100",
                                color = when (riskAssessment.status) {
                                    SecurityStatus.PROTECTED -> StatusProtected
                                    SecurityStatus.WARNING -> StatusWarning
                                    SecurityStatus.CRITICAL -> StatusCritical
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (riskAssessment.score < 30) "Low Risk" else if (riskAssessment.score < 60) "Medium Risk" else "High Risk",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { riskAssessment.score / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when (riskAssessment.status) {
                                SecurityStatus.PROTECTED -> StatusProtected
                                SecurityStatus.WARNING -> StatusWarning
                                SecurityStatus.CRITICAL -> StatusCritical
                            },
                            trackColor = NavySurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Risk factors chip summary
                        Text(
                            text = "Active Factors: " + riskAssessment.factors.joinToString(" • "),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // --- 2. Live Vault Access Card ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isVaultUnlocked) StatusProtected.copy(alpha = 0.6f) else NavyBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isVaultUnlocked) StatusProtected.copy(alpha = 0.2f) else NavySurfaceVariant)
                                    .border(1.dp, if (isVaultUnlocked) StatusProtected else CyberCyan.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Vault State",
                                    tint = if (isVaultUnlocked) StatusProtected else CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Confidential Exam Vault",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isVaultUnlocked) "VAULT UNLOCKED (4 Sealed Packets Accessible)" else "VAULT SEALED (Authentication Required)",
                                    color = if (isVaultUnlocked) StatusProtected else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigate(ScreenRoutes.VAULT) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVaultUnlocked) StatusProtected else CyberCyan
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isVaultUnlocked) "View" else "Unlock",
                                color = NavyBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // --- 3. Telemetry & Location Overview Card ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                        .clickable { onNavigate(ScreenRoutes.LOCATION) },
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
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "GPS",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Device Real-Time Location (GPS)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (location.isInsideGeofence) StatusProtected.copy(alpha = 0.15f) else StatusCritical.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (location.isInsideGeofence) "INSIDE ZONE" else "GEOFENCE BREACH",
                                    color = if (location.isInsideGeofence) StatusProtected else StatusCritical,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Latitude", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = String.format(Locale.US, "%.5f° N", location.latitude),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text(text = "Longitude", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = String.format(Locale.US, "%.5f° E", location.longitude),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text(text = "Accuracy", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = "±${location.accuracy.toInt()} m",
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Distance to VELS Exam Center: ${location.distanceToCenterMeters.toInt()} meters (Tap to view interactive radar map)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // --- 4. Hardware & Network Status Grid ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Network Connectivity",
                        value = if (network.isConnected) "Connected" else "Offline",
                        subtitle = network.networkName,
                        icon = if (network.isWifi) Icons.Default.Wifi else Icons.Default.WifiOff,
                        accentColor = if (network.isConnected) CyberCyan else StatusCritical,
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Device Battery",
                        value = "${battery.percentage}%",
                        subtitle = if (battery.isCharging) "Charging" else "On Battery",
                        icon = Icons.Default.BatteryFull,
                        accentColor = if (battery.percentage > 20) StatusProtected else StatusCritical,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 5. Security Counters Grid ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Failed Attempts",
                        value = "${stats.failedAttempts}",
                        subtitle = "Lockout at 3 attempts",
                        icon = Icons.Default.Warning,
                        accentColor = if (stats.failedAttempts > 0) StatusCritical else StatusProtected,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenRoutes.EVENT_LOG) }
                    )

                    MetricCard(
                        title = "Cloud Sync Queue",
                        value = "${stats.pendingSync} Pending",
                        subtitle = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sync.lastSyncTimestamp)),
                        icon = Icons.Default.CloudSync,
                        accentColor = if (stats.pendingSync == 0) StatusProtected else StatusWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.triggerCloudSync() }
                    )
                }
            }

            // --- 6. Quick Navigation Section ---
            item {
                Text(
                    text = "APPLICATION MODULES",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardNavButton(
                        title = "Live Radar",
                        subtitle = "Geofence Track",
                        icon = Icons.Default.Map,
                        color = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenRoutes.LOCATION) }
                    )
                    DashboardNavButton(
                        title = "Security Logs",
                        subtitle = "Audit Trail",
                        icon = Icons.Default.History,
                        color = ElectricBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenRoutes.EVENT_LOG) }
                    )
                    DashboardNavButton(
                        title = "Alert Center",
                        subtitle = "${stats.criticalCount + stats.warningCount} Active",
                        icon = Icons.Default.Notifications,
                        color = StatusCritical,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenRoutes.ALERTS) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardNavButton(
                        title = "Admin Portal",
                        subtitle = "Supervisor Tools",
                        icon = Icons.Default.AdminPanelSettings,
                        color = IndigoShield,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenRoutes.ADMIN) }
                    )
                    DashboardNavButton(
                        title = "Biometrics",
                        subtitle = "Fingerprint Auth",
                        icon = Icons.Default.Fingerprint,
                        color = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenRoutes.BIOMETRIC) }
                    )
                    DashboardNavButton(
                        title = "College Demo",
                        subtitle = "Viva Simulation",
                        icon = Icons.Default.PlayCircle,
                        color = StatusWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenRoutes.ADMIN) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun DashboardNavButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
