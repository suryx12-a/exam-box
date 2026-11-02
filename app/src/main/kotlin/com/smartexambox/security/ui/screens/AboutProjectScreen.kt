package com.smartexambox.security.ui.screens

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
import androidx.compose.material.icons.filled.CastForEducation
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.components.SecurityTopAppBar
import com.smartexambox.security.ui.theme.CyberCyan
import com.smartexambox.security.ui.theme.ElectricBlue
import com.smartexambox.security.ui.theme.IndigoShield
import com.smartexambox.security.ui.theme.NavyBackground
import com.smartexambox.security.ui.theme.NavyBorder
import com.smartexambox.security.ui.theme.NavySurface
import com.smartexambox.security.ui.theme.NavySurfaceVariant
import com.smartexambox.security.ui.theme.StatusProtected
import com.smartexambox.security.ui.theme.StatusWarning
import com.smartexambox.security.ui.theme.TextPrimary
import com.smartexambox.security.ui.theme.TextSecondary

@Composable
fun AboutProjectScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "About Project",
                subtitle = "VELS Final-Year Engineering Demonstration",
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

            // College Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .border(1.dp, CyberCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(30.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "VELS Institute of Science, Technology & Advanced Studies",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Text(
                            text = "School of Engineering • Final-Year Engineering Capstone Project",
                            color = ElectricBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Project: Smart Exam Paper Security and Mobile Monitoring System",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Project Background & Problem Statement
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
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "PROJECT MOTIVATION & OBJECTIVES", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "University examination logistics frequently encounter vulnerabilities during paper transportation between confidential printing presses and college exam halls. Traditional physical boxes suffer from lack of live auditability, bulky hardware failures, and high operational costs.\n\nThis system replaces the physical IoT box with a high-assurance smartphone mobile device that acts as the dedicated security sentinel. It incorporates cryptographic vault isolation, GPS geofencing, threat risk scoring, and multi-factor biometric authentication.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Hardware vs Mobile Comparison Table
            item {
                Text(
                    text = "PHYSICAL IOT BOX VS. SMARTPHONE MOBILE SENTINEL",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
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
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ComparisonRow(
                            feature = "Core Processing",
                            iotBox = "8-bit/32-bit Microcontroller (Arduino/ESP32)",
                            mobileSentinel = "64-bit Multi-Core ARM SoC with Hardware TEE"
                        )
                        ComparisonRow(
                            feature = "Vault Locking",
                            iotBox = "Mechanical Solenoid / Relay Switch",
                            mobileSentinel = "Android Keystore AES-256 GCM Digital Enclosure"
                        )
                        ComparisonRow(
                            feature = "Location Tracking",
                            iotBox = "External GPS Module (Slow TTFF)",
                            mobileSentinel = "Fused Location Provider (GPS + Glonass + Galileo)"
                        )
                        ComparisonRow(
                            feature = "Authentication",
                            iotBox = "Basic Membrane Matrix Keypad",
                            mobileSentinel = "Biometric Fingerprint + Salted SHA-256 PIN + 2FA"
                        )
                        ComparisonRow(
                            feature = "Offline Handling",
                            iotBox = "Volatile / Limited EEPROM Buffer",
                            mobileSentinel = "Room SQLite Database with WorkManager Cloud Sync"
                        )
                    }
                }
            }

            // Software Stack Specifications
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
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "TECHNICAL ARCHITECTURE STACK", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "• UI Framework: Jetpack Compose + Material 3 Dark Navy Theme", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Architecture: MVVM with StateFlow & Kotlin Coroutines", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Local Storage: SQLiteOpenHelper / Room Persistence with thread safety", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Cryptography: Android Keystore AES-GCM + Salted PBKDF2/SHA-256", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Biometrics: AndroidX BiometricPrompt Strong Authenticators", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Background Sync: Android Jetpack WorkManager with network constraints", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "• Location & Geofence: Google Play Services Fused Location Provider", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ComparisonRow(
    feature: String,
    iotBox: String,
    mobileSentinel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NavySurfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(text = feature, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Physical IoT Box", color = TextSecondary, fontSize = 10.sp)
                Text(text = iotBox, color = StatusWarning, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Mobile Sentinel", color = TextSecondary, fontSize = 10.sp)
                Text(text = mobileSentinel, color = StatusProtected, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
