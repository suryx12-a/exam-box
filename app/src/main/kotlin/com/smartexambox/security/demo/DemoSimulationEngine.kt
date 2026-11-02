package com.smartexambox.security.demo

import com.smartexambox.security.data.model.AuthResultType
import com.smartexambox.security.data.model.SecurityEventType
import com.smartexambox.security.data.model.SeverityLevel
import com.smartexambox.security.data.repository.SecurityRepository
import com.smartexambox.security.monitor.LocationMonitor
import com.smartexambox.security.monitor.NetworkConnectivityMonitor
import com.smartexambox.security.notification.SecurityNotificationHelper
import com.smartexambox.security.security.LockoutManager
import kotlinx.coroutines.delay

class DemoSimulationEngine(
    private val repository: SecurityRepository,
    private val lockoutManager: LockoutManager,
    private val locationMonitor: LocationMonitor,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val notificationHelper: SecurityNotificationHelper
) {

    suspend fun simulateFailedAuthentication(onStep: (String) -> Unit) {
        onStep("DEMO DATA: Simulating incorrect PIN attempt...")
        val state = lockoutManager.recordFailedAttempt()

        val severity = if (state.isLockedOut) SeverityLevel.CRITICAL else SeverityLevel.WARNING
        val eventType = if (state.isLockedOut) SecurityEventType.SECURITY_LOCKOUT else SecurityEventType.AUTH_FAILED
        val desc = if (state.isLockedOut) {
            "DEMO DATA: [SIMULATED] 3 consecutive invalid PIN attempts! Temporary 60s application lockout triggered."
        } else {
            "DEMO DATA: [SIMULATED] Incorrect PIN entered (Attempt ${state.failedAttempts}/3)."
        }

        repository.logEvent(
            eventType = eventType,
            description = desc,
            severity = severity,
            authResult = if (state.isLockedOut) AuthResultType.LOCKED else AuthResultType.FAILED,
            isSimulated = true
        )

        notificationHelper.sendSecurityAlertNotification(
            title = if (state.isLockedOut) "Security Lockout Triggered" else "Authentication Failure",
            message = desc,
            severity = severity
        )

        onStep(desc)
    }

    suspend fun simulateGeofenceViolation(onStep: (String) -> Unit) {
        onStep("DEMO DATA: Simulating device movement outside permitted examination zone...")
        // Coordinates outside campus (e.g., Chennai Airport area ~3km away)
        val breachLat = 12.9856
        val breachLng = 80.1633
        locationMonitor.simulateLocation(breachLat, breachLng, accuracy = 4.5f)

        delay(400)
        val desc = "DEMO DATA: [SIMULATED] Geofence Breach Detected! Device is outside permitted zone (3.2 km from VELS Exam Center). Perimeter violation recorded."
        repository.logEvent(
            eventType = SecurityEventType.GEOFENCE_VIOLATION,
            description = desc,
            severity = SeverityLevel.CRITICAL,
            latitude = breachLat,
            longitude = breachLng,
            accuracy = 4.5f,
            isSimulated = true
        )

        notificationHelper.sendSecurityAlertNotification(
            title = "Geofence Perimeter Breach",
            message = desc,
            severity = SeverityLevel.CRITICAL
        )

        onStep("Geofence violation active. Device flagged as CRITICAL risk.")
    }

    suspend fun simulateGeofenceRestore(onStep: (String) -> Unit) {
        onStep("DEMO DATA: Restoring device to inside permitted zone...")
        locationMonitor.resetSimulation()

        val desc = "DEMO DATA: [SIMULATED] Device returned inside permitted examination perimeter. Geofence normalized."
        repository.logEvent(
            eventType = SecurityEventType.GEOFENCE_RESTORED,
            description = desc,
            severity = SeverityLevel.INFO,
            isSimulated = true
        )
        onStep(desc)
    }

    suspend fun simulateNetworkDisconnection(onStep: (String) -> Unit) {
        onStep("DEMO DATA: Simulating Wi-Fi / Cellular network outage...")
        networkMonitor.simulateOfflineMode(true)

        val desc = "DEMO DATA: [SIMULATED] Wi-Fi Connection Lost. Real-time telemetry offline. Automatic local event queueing activated in Room DB."
        repository.logEvent(
            eventType = SecurityEventType.WIFI_DISCONNECTED,
            description = desc,
            severity = SeverityLevel.WARNING,
            isSimulated = true
        )

        notificationHelper.sendSecurityAlertNotification(
            title = "Network Telemetry Lost",
            message = desc,
            severity = SeverityLevel.WARNING
        )
        onStep("Device running in offline buffer mode.")
    }

    suspend fun simulateNetworkRestoration(onStep: (String) -> Unit) {
        onStep("DEMO DATA: Restoring Wi-Fi network connectivity...")
        networkMonitor.simulateOfflineMode(false)

        val desc = "DEMO DATA: [SIMULATED] Wi-Fi Connection Restored. Background sync queue ready for cloud dispatch."
        repository.logEvent(
            eventType = SecurityEventType.WIFI_CONNECTED,
            description = desc,
            severity = SeverityLevel.INFO,
            isSimulated = true
        )
        onStep(desc)
    }

    suspend fun simulateSuccessfulAuthentication(onStep: (String) -> Unit) {
        onStep("DEMO DATA: Simulating multi-factor biometric + supervisor verification...")
        lockoutManager.recordSuccessfulAttempt()

        val desc = "DEMO DATA: [SIMULATED] Multi-Factor Authentication Successful. Examination Paper Digital Vault Unlocked."
        repository.logEvent(
            eventType = SecurityEventType.VAULT_UNLOCKED,
            description = desc,
            severity = SeverityLevel.INFO,
            authResult = AuthResultType.SUCCESS,
            isSimulated = true
        )

        notificationHelper.sendSecurityAlertNotification(
            title = "Exam Vault Unlocked",
            message = desc,
            severity = SeverityLevel.INFO
        )
        onStep(desc)
    }

    suspend fun simulateAlertAcknowledgement(onStep: (String) -> Unit) {
        onStep("DEMO DATA: Administrator acknowledging active security alerts...")
        val count = repository.acknowledgeAllAlerts()
        val desc = "DEMO DATA: [SIMULATED] Administrator acknowledged $count active security alert(s)."
        repository.logEvent(
            eventType = SecurityEventType.ALERT_ACKNOWLEDGED,
            description = desc,
            severity = SeverityLevel.INFO,
            isSimulated = true
        )
        onStep("All alerts marked as ACKNOWLEDGED by Administrator.")
    }
}
