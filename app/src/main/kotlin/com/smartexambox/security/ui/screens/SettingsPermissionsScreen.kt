package com.smartexambox.security.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SettingsPermissionsScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(viewModel.locationMonitor.hasLocationPermission()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fine || coarse
        if (hasLocationPermission) {
            viewModel.locationMonitor.startTracking()
            Toast.makeText(context, "Location permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission denied! GPS coordinates will use last known position.", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "Security notifications enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Settings & Permissions",
                subtitle = "Hardware & Cryptography Control",
                showBackButton = true,
                onBackClick = { onNavigate(ScreenRoutes.DASHBOARD) }
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Runtime Permissions Section
            item {
                Text(
                    text = "RUNTIME HARDWARE PERMISSIONS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                PermissionItemCard(
                    title = "Precise & Coarse GPS Location",
                    description = "Required for continuous geofence breach detection and coordinates recording.",
                    isGranted = hasLocationPermission,
                    icon = Icons.Default.LocationOn,
                    onRequest = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "High-Priority Notifications",
                    description = "Required for dispatching heads-up security lockout and perimeter violation alerts.",
                    isGranted = true,
                    icon = Icons.Default.Notifications,
                    onRequest = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            Toast.makeText(context, "Notifications enabled by default on this Android version.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Biometric Sensor & Fingerprint",
                    description = "Used for biometric multi-factor authentication to unlock the digital examination vault.",
                    isGranted = true,
                    icon = Icons.Default.Fingerprint,
                    onRequest = {}
                )
            }

            item {
                PermissionItemCard(
                    title = "Network & Wi-Fi State",
                    description = "Monitors internet connectivity for cloud synchronization and offline queueing.",
                    isGranted = true,
                    icon = Icons.Default.Wifi,
                    onRequest = {}
                )
            }

            // Cryptographic Key Management
            item {
                Text(
                    text = "ANDROID KEYSTORE & CRYPTOGRAPHY",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Hardware Key Specifications", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "• Provider: AndroidKeyStore (Hardware Security Module / TEE)", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Master Key Alias: SmartExamBoxMasterKey", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Cipher Algorithm: AES-256 (GCM / NoPadding)", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• PIN Protection: PBKDF2 / Salted SHA-256 with 128-bit Cryptographic Salt", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Plaintext Storage: Zero plaintext passwords stored on disk", color = StatusProtected, fontSize = 11.sp, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { onNavigate(ScreenRoutes.LOGIN_PIN) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                        ) {
                            Text(text = "Reconfigure Master PIN", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Project Specifications Link
            item {
                OutlinedButton(
                    onClick = { onNavigate(ScreenRoutes.ABOUT) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "About VELS Engineering Project & Architecture", fontSize = 12.sp)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onRequest: () -> Unit
) {
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) StatusProtected.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted) StatusProtected else StatusWarning,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(text = description, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = "Grant", color = NavyBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StatusProtected.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(text = "ACTIVE", color = StatusProtected, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
