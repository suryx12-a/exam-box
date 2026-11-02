package com.smartexambox.security

import com.smartexambox.security.data.model.SecurityStatus
import com.smartexambox.security.security.RiskScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RiskScoringEngineTest {

    private lateinit var engine: RiskScoringEngine

    @Before
    fun setUp() {
        engine = RiskScoringEngine()
    }

    @Test
    fun testNormalState_returnsProtectedStatusAndZeroScore() {
        val result = engine.calculateRiskScore(
            failedAttempts = 0,
            isLockedOut = false,
            isGeofenceViolated = false,
            isWifiDisconnected = false,
            unacknowledgedCriticalAlerts = 0
        )

        assertEquals(0, result.score)
        assertEquals(SecurityStatus.PROTECTED, result.status)
        assertTrue(result.factors.contains("All Security Parameters Normal"))
    }

    @Test
    fun testSingleFailedAttempt_remainsProtectedWithMinorRisk() {
        val result = engine.calculateRiskScore(
            failedAttempts = 1,
            isLockedOut = false,
            isGeofenceViolated = false,
            isWifiDisconnected = false
        )

        assertEquals(10, result.score)
        assertEquals(SecurityStatus.PROTECTED, result.status)
        assertTrue(result.factors.any { it.contains("1 Failed Authentication") })
    }

    @Test
    fun testTwoFailedAttempts_escalatesToWarning() {
        val result = engine.calculateRiskScore(
            failedAttempts = 2,
            isLockedOut = false,
            isGeofenceViolated = false,
            isWifiDisconnected = false
        )

        assertEquals(25, result.score)
        assertEquals(SecurityStatus.WARNING, result.status)
        assertTrue(result.factors.any { it.contains("2 Failed Authentication") })
    }

    @Test
    fun testThreeFailedAttemptsWithLockout_escalatesToCritical() {
        val result = engine.calculateRiskScore(
            failedAttempts = 3,
            isLockedOut = true,
            isGeofenceViolated = false,
            isWifiDisconnected = false
        )

        // 40 (attempts) + 35 (lockout) = 75
        assertEquals(75, result.score)
        assertEquals(SecurityStatus.CRITICAL, result.status)
        assertTrue(result.factors.any { it.contains("Active Lockout") })
    }

    @Test
    fun testGeofenceBreach_triggersWarningOrCritical() {
        val result = engine.calculateRiskScore(
            failedAttempts = 0,
            isLockedOut = false,
            isGeofenceViolated = true,
            isWifiDisconnected = false
        )

        assertEquals(45, result.score)
        assertEquals(SecurityStatus.WARNING, result.status)
        assertTrue(result.factors.any { it.contains("Geofence Violation") })
    }

    @Test
    fun testCompoundThreats_clampedAtOneHundred() {
        val result = engine.calculateRiskScore(
            failedAttempts = 5,
            isLockedOut = true,
            isGeofenceViolated = true,
            isWifiDisconnected = true,
            unacknowledgedCriticalAlerts = 5
        )

        assertEquals(100, result.score)
        assertEquals(SecurityStatus.CRITICAL, result.status)
    }
}
