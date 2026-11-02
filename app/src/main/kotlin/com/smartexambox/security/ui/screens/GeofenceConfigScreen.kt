package com.smartexambox.security.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexambox.security.data.model.GeofenceZone
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.components.SecurityTopAppBar
import com.smartexambox.security.ui.theme.CyberCyan
import com.smartexambox.security.ui.theme.ElectricBlue
import com.smartexambox.security.ui.theme.NavyBackground
import com.smartexambox.security.ui.theme.NavyBorder
import com.smartexambox.security.ui.theme.NavySurface
import com.smartexambox.security.ui.theme.StatusProtected
import com.smartexambox.security.ui.theme.TextPrimary
import com.smartexambox.security.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun GeofenceConfigScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val currentGeofence by viewModel.activeGeofenceFlow.collectAsState()
    val currentLocation by viewModel.locationState.collectAsState()

    var centerName by remember(currentGeofence) { mutableStateOf(currentGeofence.centerName) }
    var latText by remember(currentGeofence) { mutableStateOf(currentGeofence.latitude.toString()) }
    var lngText by remember(currentGeofence) { mutableStateOf(currentGeofence.longitude.toString()) }
    var radius by remember(currentGeofence) { mutableFloatStateOf(currentGeofence.radiusMeters) }
    var isActive by remember(currentGeofence) { mutableStateOf(currentGeofence.isActive) }

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Geofence Configuration",
                subtitle = "Campus Perimeter Parameters",
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
                            text = "PERIMETER MONITORING SETTINGS",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Defines the authorized geographic zone for examination paper custody. Leaving this boundary triggers an instant security breach event and telemetry notification.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                        .background(NavySurface)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Active Perimeter Enforcement", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = if (isActive) "Armed: Breaches trigger Critical alerts" else "Disarmed: Monitoring paused", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = Color(0xFF0F3A5C)
                        )
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = centerName,
                    onValueChange = { centerName = it },
                    label = { Text("Campus / Examination Center Name", color = TextSecondary) },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Center Latitude", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
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

                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Center Longitude", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
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
            }

            // Radius Slider
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Geofence Permitted Radius", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "${radius.toInt()} meters", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Slider(
                            value = radius,
                            onValueChange = { radius = it },
                            valueRange = 50f..1000f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberCyan,
                                activeTrackColor = CyberCyan,
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )

                        Text(
                            text = "Covers examination hall, distribution corridor and administrative building.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Preset Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            latText = String.format(Locale.US, "%.5f", currentLocation.latitude)
                            lngText = String.format(Locale.US, "%.5f", currentLocation.longitude)
                            Toast.makeText(context, "Set to current phone GPS coordinates", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                    ) {
                        Text(text = "Use Current GPS", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            latText = "12.9576"
                            lngText = "80.1444"
                            centerName = "VELS University Exam Hall A"
                            radius = 250f
                            Toast.makeText(context, "Reset to VELS Campus default", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                    ) {
                        Text(text = "Reset to VELS Campus", fontSize = 11.sp)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: currentGeofence.latitude
                        val lng = lngText.toDoubleOrNull() ?: currentGeofence.longitude
                        val updated = GeofenceZone(
                            zoneId = currentGeofence.zoneId,
                            centerName = centerName.trim(),
                            latitude = lat,
                            longitude = lng,
                            radiusMeters = radius,
                            isActive = isActive
                        )
                        viewModel.updateGeofence(updated)
                        Toast.makeText(context, "Geofence Perimeter Updated Successfully!", Toast.LENGTH_SHORT).show()
                        onNavigate(ScreenRoutes.LOCATION)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Save & Apply Geofence Perimeter", color = NavyBackground, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
