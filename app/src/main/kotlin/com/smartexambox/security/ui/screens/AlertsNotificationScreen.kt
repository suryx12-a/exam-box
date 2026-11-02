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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexambox.security.data.model.SecurityEvent
import com.smartexambox.security.data.model.SeverityLevel
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.components.SecurityTopAppBar
import com.smartexambox.security.ui.theme.CyberCyan
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
fun AlertsNotificationScreen(
    viewModel: MainSecurityViewModel,
    onNavigate: (String) -> Unit
) {
    val events by viewModel.eventsFlow.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Active/Unacknowledged, 1 = Resolved/History

    // High and Critical severity events qualify as alerts
    val alertEvents = remember(events) {
        events.filter { it.severity == SeverityLevel.CRITICAL || it.severity == SeverityLevel.HIGH || it.severity == SeverityLevel.WARNING }
    }

    val unacknowledgedAlerts = remember(alertEvents) {
        alertEvents.filter { !it.isAcknowledged }
    }

    val resolvedAlerts = remember(alertEvents) {
        alertEvents.filter { it.isAcknowledged }
    }

    Scaffold(
        topBar = {
            SecurityTopAppBar(
                title = "Security Alert Center",
                subtitle = "${unacknowledgedAlerts.size} Active Incident(s) Requiring Attention",
                showBackButton = true,
                onBackClick = { onNavigate(ScreenRoutes.DASHBOARD) },
                actions = {
                    if (unacknowledgedAlerts.isNotEmpty()) {
                        IconButton(onClick = { viewModel.acknowledgeAllAlerts() }) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Acknowledge All",
                                tint = CyberCyan
                            )
                        }
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
            Spacer(modifier = Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavySurface,
                contentColor = CyberCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "ACTIVE ALERTS (${unacknowledgedAlerts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedTab == 0) CyberCyan else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "RESOLVED (${resolvedAlerts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedTab == 1) CyberCyan else TextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val currentList = if (selectedTab == 0) unacknowledgedAlerts else resolvedAlerts

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusProtected,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "No active security alerts pending." else "No resolved alerts recorded.",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All system sensors, geofence perimeters and auth checkpoints normal.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList) { alert ->
                        AlertItemCard(
                            event = alert,
                            onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun AlertItemCard(
    event: SecurityEvent,
    onAcknowledge: () -> Unit
) {
    val isCritical = event.severity == SeverityLevel.CRITICAL

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCritical) StatusCritical.copy(alpha = 0.8f) else StatusWarning.copy(alpha = 0.7f),
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) Color(0xFF2E0909) else Color(0xFF261904)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (isCritical) StatusCritical else StatusWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.eventType.name,
                        color = if (isCritical) StatusCritical else StatusWarning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (event.isSimulated) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.formattedTime,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                if (!event.isAcknowledged) {
                    Button(
                        onClick = onAcknowledge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCritical) StatusCritical else StatusWarning
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "Acknowledge Incident", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusProtected, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Acknowledged", color = StatusProtected, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
