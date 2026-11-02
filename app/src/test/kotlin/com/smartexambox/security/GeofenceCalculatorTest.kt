package com.smartexambox.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GeofenceCalculatorTest {

    // Haversine formula implementation for JVM unit tests
    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun isInsideGeofence(
        deviceLat: Double,
        deviceLon: Double,
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double,
        isActive: Boolean = true
    ): Boolean {
        if (!isActive) return true
        val distance = calculateDistanceMeters(deviceLat, deviceLon, centerLat, centerLon)
        return distance <= radiusMeters
    }

    @Test
    fun testSamePoint_distanceIsZero() {
        val velsLat = 12.9576
        val velsLon = 80.1444
        val dist = calculateDistanceMeters(velsLat, velsLon, velsLat, velsLon)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun testInsideGeofence_whenWithinRadius() {
        val centerLat = 12.9576
        val centerLon = 80.1444
        val radius = 250.0

        // Point roughly 50m north
        val nearbyLat = 12.9580
        val nearbyLon = 80.1444

        val distance = calculateDistanceMeters(centerLat, centerLon, nearbyLat, nearbyLon)
        assertTrue(distance < radius)
        assertTrue(isInsideGeofence(nearbyLat, nearbyLon, centerLat, centerLon, radius))
    }

    @Test
    fun testOutsideGeofence_generatesBreach() {
        val centerLat = 12.9576
        val centerLon = 80.1444
        val radius = 250.0

        // Point in Chennai Airport (~3.2 km away)
        val breachLat = 12.9856
        val breachLon = 80.1633

        val distance = calculateDistanceMeters(centerLat, centerLon, breachLat, breachLon)
        assertTrue("Distance should be > 2000m, was $distance", distance > 2000.0)
        assertFalse(isInsideGeofence(breachLat, breachLon, centerLat, centerLon, radius))
    }

    @Test
    fun testInactiveGeofence_alwaysPermitsAccess() {
        val centerLat = 12.9576
        val centerLon = 80.1444
        val breachLat = 12.9856
        val breachLon = 80.1633

        val isPermitted = isInsideGeofence(
            deviceLat = breachLat,
            deviceLon = breachLon,
            centerLat = centerLat,
            centerLon = centerLon,
            radiusMeters = 250.0,
            isActive = false // Disabled perimeter check
        )
        assertTrue(isPermitted)
    }
}
