package com.smartexambox.security.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LocationTrackingScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val location by viewModel.locationState.collectAsState()
    val geofence by viewModel.activeGeofenceFlow.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Live Location & Geofence",
                subtitle = "GPS Real-Time Telemetry Tracking",
                showBackButton = true,
                onBackClick = { onNavigate(ScreenRoutes.DASHBOARD) },
                actions = {
                    IconButton(onClick = { onNavigate(ScreenRoutes.GEOFENCE) }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Geofence Config",
                            tint = CyberCyan
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

            // Geofence Status Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (location.isInsideGeofence) StatusProtected else StatusCritical,
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (location.isInsideGeofence) Color(0xFF042F2C) else Color(0xFF450A0A)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (location.isInsideGeofence) StatusProtected else StatusCritical).copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (location.isInsideGeofence) Icons.Default.GpsFixed else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (location.isInsideGeofence) StatusProtected else StatusCritical,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (location.isInsideGeofence) "DEVICE INSIDE PERMITTED GEOFENCE" else "CRITICAL: GEOFENCE BREACH DETECTED",
                                color = if (location.isInsideGeofence) StatusProtected else StatusCritical,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (location.isInsideGeofence)
                                    "Box is within the ${geofence.radiusMeters.toInt()}m perimeter of ${geofence.centerName}."
                                else
                                    "Box has departed authorized perimeter! Telemetry alert sent to Examination Controller.",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // --- Radar / Geofence Viewport ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .border(1.dp, NavyBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091024)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val maxRadius = minOf(centerX, centerY) * 0.85f

                            // Concentric Radar Rings
                            for (step in 1..4) {
                                val r = maxRadius * (step / 4f)
                                drawCircle(
                                    color = Color(0xFF1E2D52),
                                    radius = r,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                )
                            }

                            // Crosshair lines
                            drawLine(
                                color = Color(0xFF1E2D52),
                                start = Offset(centerX - maxRadius, centerY),
                                end = Offset(centerX + maxRadius, centerY),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF1E2D52),
                                start = Offset(centerX, centerY - maxRadius),
                                end = Offset(centerX, centerY + maxRadius),
                                strokeWidth = 1f
                            )

                            // Geofence Perimeter Ring
                            val geofenceColor = if (location.isInsideGeofence) StatusProtected else StatusCritical
                            drawCircle(
                                color = geofenceColor.copy(alpha = 0.25f),
                                radius = maxRadius * 0.65f,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 3f)
                            )

                            // Radar Sweep Line
                            val rad = Math.toRadians(sweepAngle.toDouble())
                            val sweepX = centerX + (maxRadius * cos(rad)).toFloat()
                            val sweepY = centerY + (maxRadius * sin(rad)).toFloat()
                            drawLine(
                                color = CyberCyan.copy(alpha = 0.4f),
                                start = Offset(centerX, centerY),
                                end = Offset(sweepX, sweepY),
                                strokeWidth = 2f
                            )

                            // Center Campus Marker (Exam Hall)
                            drawCircle(
                                color = CyberCyan,
                                radius = 6f,
                                center = Offset(centerX, centerY)
                            )

                            // Device Position Dot
                            val deviceOffset = if (location.isInsideGeofence) {
                                Offset(centerX + 24f, centerY - 18f)
                            } else {
                                Offset(centerX + maxRadius * 0.85f, centerY + maxRadius * 0.65f)
                            }

                            // Device Accuracy Halo
                            drawCircle(
                                color = geofenceColor.copy(alpha = 0.25f),
                                radius = 18f,
                                center = deviceOffset
                            )
                            drawCircle(
                                color = geofenceColor,
                                radius = 7f,
                                center = deviceOffset
                            )
                        }

                        // Overlay labels
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "RADAR: VELS CAMPUS GEOFENCE", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(text = "SCALE: 100m / Ring", color = TextSecondary, fontSize = 10.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "Center: ${geofence.centerName}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (location.isSimulated) "DEMO GPS OVERRIDE" else "LIVE GPS TELEMETRY",
                                    color = if (location.isSimulated) StatusWarning else StatusProtected,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // --- GPS Coordinates Card ---
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
                            text = "GPS TELEMETRY METRICS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Latitude", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = String.format(Locale.US, "%.6f° N", location.latitude),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(text = "Longitude", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = String.format(Locale.US, "%.6f° E", location.longitude),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(text = "Accuracy", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = "±${location.accuracy.toInt()} m",
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Distance to Center", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = "${location.distanceToCenterMeters.toInt()} meters",
                                    color = if (location.isInsideGeofence) StatusProtected else StatusCritical,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text(text = "Geofence Radius", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = "${geofence.radiusMeters.toInt()} meters",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text(text = "Last GPS Fix", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(location.lastUpdateTimestamp)),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- College Evaluation Simulator Buttons ---
            item {
                Text(
                    text = "COLLEGE VIVA SIMULATOR CONTROLS",
                    color = StatusWarning,
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
                    Button(
                        onClick = { viewModel.runSimulatedGeofenceViolation() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Simulate Geofence Breach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.runSimulatedGeofenceRestore() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusProtected)
                    ) {
                        Text(text = "Restore Inside Zone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
