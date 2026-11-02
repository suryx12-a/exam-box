package com.smartexambox.security.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SecurityEvent(
    val id: String = UUID.randomUUID().toString(),
    val eventType: SecurityEventType,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = formatTimestamp(timestamp),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val authResult: AuthResultType = AuthResultType.NOT_APPLICABLE,
    val severity: SeverityLevel = SeverityLevel.INFO,
    val description: String,
    val isAcknowledged: Boolean = false,
    val isSynced: Boolean = false,
    val isSimulated: Boolean = false
) {
    companion object {
        fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
