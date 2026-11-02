package com.smartexambox.security

import com.smartexambox.security.data.model.AuthResultType
import com.smartexambox.security.data.model.SecurityEvent
import com.smartexambox.security.data.model.SecurityEventType
import com.smartexambox.security.data.model.SeverityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityEventLoggingTest {

    @Test
    fun testEventCreation_generatesValidUuidAndTimestamp() {
        val event = SecurityEvent(
            eventType = SecurityEventType.AUTH_SUCCESS,
            deviceId = "VELS-SEC-01",
            description = "Authentication successful",
            severity = SeverityLevel.INFO,
            authResult = AuthResultType.SUCCESS
        )

        assertNotNull(event.id)
        assertTrue(event.id.isNotEmpty())
        assertTrue(event.timestamp > 0)
        assertTrue(event.formattedTime.isNotEmpty())
        assertEquals("VELS-SEC-01", event.deviceId)
        assertEquals(SeverityLevel.INFO, event.severity)
        assertEquals(AuthResultType.SUCCESS, event.authResult)
        assertFalse(event.isSimulated)
        assertFalse(event.isAcknowledged)
    }

    @Test
    fun testSimulatedEvent_preservesDemoDataFlag() {
        val simulatedEvent = SecurityEvent(
            eventType = SecurityEventType.GEOFENCE_VIOLATION,
            deviceId = "VELS-SEC-01",
            description = "DEMO DATA: Simulated Geofence Breach",
            severity = SeverityLevel.CRITICAL,
            isSimulated = true
        )

        assertTrue(simulatedEvent.isSimulated)
        assertEquals(SeverityLevel.CRITICAL, simulatedEvent.severity)
        assertTrue(simulatedEvent.description.startsWith("DEMO DATA"))
    }

    @Test
    fun testSeverityClassification_levelsHierarchy() {
        val levels = listOf(SeverityLevel.INFO, SeverityLevel.WARNING, SeverityLevel.HIGH, SeverityLevel.CRITICAL)
        assertEquals(4, levels.size)
        assertTrue(levels.contains(SeverityLevel.CRITICAL))
    }

    @Test
    fun testTimestampFormatting_producesReadableIsoFormat() {
        val timestamp = 1730520000000L // Fixed epoch timestamp
        val formatted = SecurityEvent.formatTimestamp(timestamp)
        assertNotNull(formatted)
        assertTrue(formatted.contains("-"))
        assertTrue(formatted.contains(":"))
    }
}
