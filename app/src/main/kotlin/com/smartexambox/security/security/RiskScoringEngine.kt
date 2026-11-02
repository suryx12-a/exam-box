package com.smartexambox.security.security

import com.smartexambox.security.data.model.SecurityStatus

class RiskScoringEngine {

    fun calculateRiskScore(
        failedAttempts: Int,
        isLockedOut: Boolean,
        isGeofenceViolated: Boolean,
        isWifiDisconnected: Boolean,
        unacknowledgedCriticalAlerts: Int = 0
    ): RiskAssessment {
        var score = 0

        // Failed attempts impact
        when {
            failedAttempts >= 3 -> score += 40
            failedAttempts == 2 -> score += 25
            failedAttempts == 1 -> score += 10
        }

        // Lockout condition
        if (isLockedOut) {
            score += 35
        }

        // Geofence violation (High risk: examination papers moving outside campus perimeter)
        if (isGeofenceViolated) {
            score += 45
        }

        // Network disconnection during active tracking
        if (isWifiDisconnected) {
            score += 15
        }

        // Unacknowledged critical alerts
        score += (unacknowledgedCriticalAlerts * 10).coerceAtMost(20)

        // Clamp between 0 and 100
        val finalScore = score.coerceIn(0, 100)

        val status = when {
            finalScore >= 60 || isLockedOut -> SecurityStatus.CRITICAL
            finalScore >= 25 -> SecurityStatus.WARNING
            else -> SecurityStatus.PROTECTED
        }

        val riskFactors = mutableListOf<String>()
        if (isLockedOut) riskFactors.add("System in Active Lockout")
        if (failedAttempts > 0) riskFactors.add("$failedAttempts Failed Authentication Attempt(s)")
        if (isGeofenceViolated) riskFactors.add("Perimeter Geofence Violation Detected")
        if (isWifiDisconnected) riskFactors.add("Network Disconnected (Events Queued Offline)")
        if (unacknowledgedCriticalAlerts > 0) riskFactors.add("$unacknowledgedCriticalAlerts Unresolved Critical Alerts")
        if (riskFactors.isEmpty()) riskFactors.add("All Security Parameters Normal")

        return RiskAssessment(
            score = finalScore,
            status = status,
            factors = riskFactors
        )
    }
}

data class RiskAssessment(
    val score: Int,
    val status: SecurityStatus,
    val factors: List<String>
)
