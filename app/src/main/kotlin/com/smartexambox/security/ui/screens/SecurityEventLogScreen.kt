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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexambox.security.data.model.SecurityEvent
import com.smartexambox.security.data.model.SeverityLevel
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
import com.smartexambox.security.ui.theme.StatusInfo
import com.smartexambox.security.ui.theme.StatusProtected
import com.smartexambox.security.ui.theme.StatusWarning
import com.smartexambox.security.ui.theme.TextPrimary
import com.smartexambox.security.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SecurityEventLogScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val events by viewModel.eventsFlow.collectAsState()
    var selectedSeverity by remember { mutableStateOf<SeverityLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredEvents = remember(events, selectedSeverity, searchQuery) {
        events.filter { event ->
            val matchSeverity = selectedSeverity == null || event.severity == selectedSeverity
            val matchQuery = searchQuery.isBlank() ||
                    event.description.contains(searchQuery, ignoreCase = true) ||
                    event.eventType.name.contains(searchQuery, ignoreCase = true) ||
                    event.deviceId.contains(searchQuery, ignoreCase = true)
            matchSeverity && matchQuery
        }
    }

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Security Event Log",
                subtitle = "${events.size} Total Audit Records Recorded",
                showBackButton = true,
                onBackClick = { onNavigate(ScreenRoutes.DASHBOARD) },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.exportAuditLogs { file ->
                                Toast.makeText(
                                    context,
                                    "Audit Log Exported: ${file.name} (${file.length()} bytes)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = CyberCyan
                        )
                    }
                }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "Search events, description, device...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = NavyBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSeverity == null,
                        onClick = { selectedSeverity = null },
                        label = { Text("ALL (${events.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = NavyBackground,
                            containerColor = NavySurface,
                            labelColor = TextSecondary
                        )
                    )
                }
                items(SeverityLevel.values()) { sev ->
                    val count = events.count { it.severity == sev }
                    FilterChip(
                        selected = selectedSeverity == sev,
                        onClick = { selectedSeverity = if (selectedSeverity == sev) null else sev },
                        label = { Text("${sev.name} ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (sev) {
                                SeverityLevel.CRITICAL -> StatusCritical
                                SeverityLevel.HIGH -> StatusWarning
                                SeverityLevel.WARNING -> Color(0xFFD97706)
                                SeverityLevel.INFO -> ElectricBlue
                            },
                            selectedLabelColor = Color.White,
                            containerColor = NavySurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No security events matching selected filters.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEvents) { event ->
                        SecurityEventCard(event = event)
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun SecurityEventCard(event: SecurityEvent) {
    val sevColor = when (event.severity) {
        SeverityLevel.CRITICAL -> StatusCritical
        SeverityLevel.HIGH -> StatusCritical.copy(alpha = 0.85f)
        SeverityLevel.WARNING -> StatusWarning
        SeverityLevel.INFO -> StatusInfo
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(sevColor.copy(alpha = 0.15f))
                            .border(1.dp, sevColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = event.severity.name,
                            color = sevColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.eventType.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (event.isSimulated) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF334155))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "DEMO DATA", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = event.description,
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.formattedTime,
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                if (event.latitude != null && event.longitude != null) {
                    Text(
                        text = String.format(Locale.US, "GPS: %.4f°, %.4f°", event.latitude, event.longitude),
                        color = ElectricBlue,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
