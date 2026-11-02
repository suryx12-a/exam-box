package com.smartexambox.security.data.repository

import android.content.Context
import com.smartexambox.security.data.db.SecurityDatabaseHelper
import com.smartexambox.security.data.model.AuthResultType
import com.smartexambox.security.data.model.DeviceRegistration
import com.smartexambox.security.data.model.GeofenceZone
import com.smartexambox.security.data.model.SecurityEvent
import com.smartexambox.security.data.model.SecurityEventType
import com.smartexambox.security.data.model.SeverityLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class SecurityRepository(
    private val dbHelper: SecurityDatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _eventsFlow = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val eventsFlow: StateFlow<List<SecurityEvent>> = _eventsFlow.asStateFlow()

    private val _activeGeofenceFlow = MutableStateFlow(dbHelper.getActiveGeofenceZone())
    val activeGeofenceFlow: StateFlow<GeofenceZone> = _activeGeofenceFlow.asStateFlow()

    private val _deviceRegistrationFlow = MutableStateFlow(dbHelper.getDeviceRegistration())
    val deviceRegistrationFlow: StateFlow<DeviceRegistration> = _deviceRegistrationFlow.asStateFlow()

    suspend fun loadInitialData() = withContext(ioDispatcher) {
        refreshEvents()
        _activeGeofenceFlow.value = dbHelper.getActiveGeofenceZone()
        _deviceRegistrationFlow.value = dbHelper.getDeviceRegistration()
    }

    suspend fun logEvent(
        eventType: SecurityEventType,
        description: String,
        severity: SeverityLevel = SeverityLevel.INFO,
        authResult: AuthResultType = AuthResultType.NOT_APPLICABLE,
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Float? = null,
        isSimulated: Boolean = false
    ): SecurityEvent = withContext(ioDispatcher) {
        val deviceId = _deviceRegistrationFlow.value.deviceId
        val event = SecurityEvent(
            eventType = eventType,
            deviceId = deviceId,
            description = description,
            severity = severity,
            authResult = authResult,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            isSimulated = isSimulated
        )
        dbHelper.insertEvent(event)
        refreshEvents()
        event
    }

    suspend fun refreshEvents() = withContext(ioDispatcher) {
        _eventsFlow.value = dbHelper.getAllEvents()
    }

    suspend fun acknowledgeAlert(eventId: String): Boolean = withContext(ioDispatcher) {
        val result = dbHelper.acknowledgeEvent(eventId)
        if (result) refreshEvents()
        result
    }

    suspend fun acknowledgeAllAlerts(): Int = withContext(ioDispatcher) {
        val count = dbHelper.acknowledgeAllEvents()
        if (count > 0) refreshEvents()
        count
    }

    suspend fun updateGeofence(zone: GeofenceZone): Boolean = withContext(ioDispatcher) {
        val ok = dbHelper.updateGeofenceZone(zone)
        if (ok) {
            _activeGeofenceFlow.value = zone
            logEvent(
                eventType = SecurityEventType.DEMO_SIMULATION,
                description = "Geofence updated to ${zone.centerName} (Radius: ${zone.radiusMeters}m)",
                severity = SeverityLevel.INFO
            )
        }
        ok
    }

    suspend fun updateDeviceRegistration(device: DeviceRegistration): Boolean = withContext(ioDispatcher) {
        val ok = dbHelper.updateDeviceRegistration(device)
        if (ok) {
            _deviceRegistrationFlow.value = device
            logEvent(
                eventType = SecurityEventType.DEVICE_REGISTERED,
                description = "Device re-registered: ${device.deviceId} for center ${device.centerName}",
                severity = SeverityLevel.INFO
            )
        }
        ok
    }

    suspend fun getStats(): SecurityStats = withContext(ioDispatcher) {
        SecurityStats(
            failedAttempts = dbHelper.getFailedAttemptsCount(),
            suspiciousEvents = dbHelper.getSuspiciousEventsCount(),
            pendingSync = dbHelper.getPendingSyncCount(),
            criticalCount = dbHelper.getEventCountBySeverity(SeverityLevel.CRITICAL),
            warningCount = dbHelper.getEventCountBySeverity(SeverityLevel.WARNING),
            infoCount = dbHelper.getEventCountBySeverity(SeverityLevel.INFO)
        )
    }

    suspend fun exportEventsToCsv(context: Context): File = withContext(ioDispatcher) {
        val events = dbHelper.getAllEvents()
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "security_events_export_${System.currentTimeMillis()}.csv")

        FileWriter(file).use { writer ->
            writer.append("Event ID,Type,Device ID,Timestamp,Formatted Time,Latitude,Longitude,Accuracy,Auth Result,Severity,Description,Acknowledged,Synced,Simulated\n")
            for (e in events) {
                val line = listOf(
                    e.id,
                    e.eventType.name,
                    e.deviceId,
                    e.timestamp.toString(),
                    "\"${e.formattedTime}\"",
                    e.latitude?.toString() ?: "",
                    e.longitude?.toString() ?: "",
                    e.accuracy?.toString() ?: "",
                    e.authResult.name,
                    e.severity.name,
                    "\"${e.description.replace("\"", "\"\"")}\"",
                    if (e.isAcknowledged) "YES" else "NO",
                    if (e.isSynced) "YES" else "NO",
                    if (e.isSimulated) "DEMO" else "REAL"
                ).joinToString(",")
                writer.append(line).append("\n")
            }
        }
        file
    }

    suspend fun clearAllLogs() = withContext(ioDispatcher) {
        dbHelper.clearAllLogs()
        refreshEvents()
    }

    companion object {
        @Volatile
        private var instance: SecurityRepository? = null

        fun getInstance(context: Context): SecurityRepository {
            return instance ?: synchronized(this) {
                instance ?: SecurityRepository(
                    SecurityDatabaseHelper.getInstance(context)
                ).also { instance = it }
            }
        }
    }
}

data class SecurityStats(
    val failedAttempts: Int = 0,
    val suspiciousEvents: Int = 0,
    val pendingSync: Int = 0,
    val criticalCount: Int = 0,
    val warningCount: Int = 0,
    val infoCount: Int = 0
)
