package com.smartexambox.security.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexambox.security.data.model.DeviceRegistration
import com.smartexambox.security.network.ConnectionStatus
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
fun DeviceRegistrationScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val currentDevice by viewModel.deviceRegistrationFlow.collectAsState()
    val pairingState by viewModel.pairingState.collectAsState()

    var serverUrlInput by remember(pairingState.serverUrl) { mutableStateOf(pairingState.serverUrl) }
    var pairingCodeInput by remember { mutableStateOf("") }
    var pingMessage by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var isPairingRequesting by remember { mutableStateOf(false) }

    var deviceId by remember(currentDevice) { mutableStateOf(currentDevice.deviceId) }
    var deviceModel by remember(currentDevice) { mutableStateOf(currentDevice.deviceModel) }
    var centerName by remember(currentDevice) { mutableStateOf(currentDevice.centerName) }
    var department by remember(currentDevice) { mutableStateOf(currentDevice.department) }
    var supervisorName by remember(currentDevice) { mutableStateOf(currentDevice.supervisorName) }

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Device Pairing & Registration",
                subtitle = "Physical Phone Security Bridge",
                showBackButton = true,
                onBackClick = { onNavigate(ScreenRoutes.ADMIN) }
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

            // ==========================================
            // SECTION 1: WI-FI MONITORING PAIRING CARD
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = when (pairingState.connectionStatus) {
                                ConnectionStatus.CONNECTED -> StatusProtected
                                ConnectionStatus.SYNC_PENDING -> StatusWarning
                                ConnectionStatus.CONNECTING -> CyberCyan
                                ConnectionStatus.DISCONNECTED -> StatusCritical
                                ConnectionStatus.UNPAIRED -> NavyBorder
                            },
                            shape = RoundedCornerShape(16.dp)
                        ),
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(NavySurfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Laptop,
                                        contentDescription = "Laptop",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "LAPTOP MONITORING LINK",
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Wi-Fi LAN Bridge (Local Network)",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Connection Badge
                            val (badgeBg, badgeTextColor, badgeLabel) = when (pairingState.connectionStatus) {
                                ConnectionStatus.CONNECTED -> Triple(
                                    StatusProtected.copy(alpha = 0.18f),
                                    StatusProtected,
                                    "CONNECTED"
                                )
                                ConnectionStatus.SYNC_PENDING -> Triple(
                                    StatusWarning.copy(alpha = 0.18f),
                                    StatusWarning,
                                    "SYNC PENDING"
                                )
                                ConnectionStatus.CONNECTING -> Triple(
                                    CyberCyan.copy(alpha = 0.18f),
                                    CyberCyan,
                                    "CONNECTING"
                                )
                                ConnectionStatus.DISCONNECTED -> Triple(
                                    StatusCritical.copy(alpha = 0.18f),
                                    StatusCritical,
                                    "DISCONNECTED"
                                )
                                ConnectionStatus.UNPAIRED -> Triple(
                                    Color.Gray.copy(alpha = 0.2f),
                                    TextSecondary,
                                    "UNPAIRED"
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Status message
                        Text(
                            text = pairingState.statusMessage,
                            color = if (pairingState.connectionStatus == ConnectionStatus.CONNECTED) StatusProtected else TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // ------------------------------------------
                        // CASE A: PHONE IS PAIRED
                        // ------------------------------------------
                        if (pairingState.isPaired) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NavySurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Laptop Host IP:", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        pairingState.serverUrl,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ping Latency:", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        "${pairingState.lastPingLatencyMs ?: 12} ms",
                                        color = StatusProtected,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Unsynced Offline Events:", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        "${pairingState.unsyncedCount} queued in SQLite",
                                        color = if (pairingState.unsyncedCount > 0) StatusWarning else StatusProtected,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.forceSyncOfflineEvents { res ->
                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CyberCyan)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.unpairFromLaptop {
                                            Toast.makeText(context, "Unpaired from Laptop Monitor", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical.copy(alpha = 0.85f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unpair", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                        // ------------------------------------------
                        // CASE B: WAITING FOR LAPTOP ADMIN APPROVAL
                        // ------------------------------------------
                        else if (pairingState.isAwaitingApproval) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StatusWarning.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .border(1.dp, StatusWarning.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = StatusWarning,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Waiting for Laptop Confirmation",
                                    color = StatusWarning,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Please look at your laptop web dashboard and click 'Approve Phone'.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        // ------------------------------------------
                        // CASE C: NOT PAIRED (ENTER IP & 6-DIGIT CODE)
                        // ------------------------------------------
                        else {
                            OutlinedTextField(
                                value = serverUrlInput,
                                onValueChange = { serverUrlInput = it },
                                label = { Text("Laptop IP & Port", color = TextSecondary, fontSize = 12.sp) },
                                placeholder = { Text("http://192.168.1.39:8080", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = NavyBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = NavySurfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = NavySurfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = pairingCodeInput,
                                onValueChange = { if (it.length <= 6) pairingCodeInput = it.filter { char -> char.isDigit() } },
                                label = { Text("6-Digit Pairing Code", color = TextSecondary, fontSize = 12.sp) },
                                placeholder = { Text("e.g. 582910", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = NavyBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = NavySurfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = NavySurfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            if (pingMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pingMessage ?: "",
                                    color = if (pingMessage?.startsWith("Connected") == true) StatusProtected else StatusCritical,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isPinging = true
                                        viewModel.testServerPing(serverUrlInput) { res ->
                                            isPinging = false
                                            pingMessage = res.message
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isPinging,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CyberCyan)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isPinging) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberCyan, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test Ping", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (pairingCodeInput.length != 6) {
                                            Toast.makeText(context, "Enter the 6-digit code from the laptop dashboard", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isPairingRequesting = true
                                        viewModel.requestPairing(
                                            serverUrl = serverUrlInput,
                                            code = pairingCodeInput,
                                            onSuccess = {
                                                isPairingRequesting = false
                                                Toast.makeText(context, "Code verified! Now click Approve on Laptop.", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { err ->
                                                isPairingRequesting = false
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isPairingRequesting && pairingCodeInput.length == 6,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isPairingRequesting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NavyBackground, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyBackground)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pair Phone", fontSize = 12.sp, color = NavyBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Note: Prototype HTTP link on local Wi-Fi. No PINs, biometric scans, or exam papers are sent across the network.",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // ==========================================
            // SECTION 2: HARDWARE ENROLLMENT PROFILE
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SMARTPHONE ENROLLMENT DETAILS",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registers this phone as the primary security device for transporting and monitoring semester examination papers.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device Terminal UUID", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
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
            }

            item {
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it },
                    label = { Text("Hardware Model", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
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
            }

            item {
                OutlinedTextField(
                    value = centerName,
                    onValueChange = { centerName = it },
                    label = { Text("Authorized Examination Center", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
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
            }

            item {
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Academic Department", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
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
            }

            item {
                OutlinedTextField(
                    value = supervisorName,
                    onValueChange = { supervisorName = it },
                    label = { Text("Chief Superintendent / In-Charge", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
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
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val updated = DeviceRegistration(
                            deviceId = deviceId.trim(),
                            deviceModel = deviceModel.trim(),
                            centerName = centerName.trim(),
                            department = department.trim(),
                            supervisorName = supervisorName.trim()
                        )
                        viewModel.updateDeviceRegistration(updated)
                        Toast.makeText(context, "Device Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                        onNavigate(ScreenRoutes.ADMIN)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Save Device Profile", color = NavyBackground, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
