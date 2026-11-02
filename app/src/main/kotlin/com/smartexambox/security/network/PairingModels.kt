package com.smartexambox.security.network

enum class ConnectionStatus {
    UNPAIRED,
    CONNECTING,
    CONNECTED,
    SYNC_PENDING,
    DISCONNECTED
}

data class PhonePairingState(
    val isPaired: Boolean = false,
    val serverUrl: String = "http://192.168.1.39:8080",
    val authToken: String? = null,
    val deviceId: String = "VELS-EXAM-BOX-SEC-01",
    val model: String = "Android Device",
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNPAIRED,
    val isAwaitingApproval: Boolean = false,
    val statusMessage: String = "Unpaired: Enter laptop IP & pairing code to connect.",
    val lastPingLatencyMs: Long? = null,
    val lastSyncTimestamp: Long = 0L,
    val unsyncedCount: Int = 0
)

data class HeartbeatTelemetry(
    val battery_level: Int,
    val is_charging: Boolean,
    val wifi_ssid: String,
    val latitude: Double?,
    val longitude: Double?,
    val is_inside_geofence: Boolean,
    val threat_risk_score: Int,
    val vault_status: String,
    val unsynced_event_count: Int
)

data class PingResult(
    val success: Boolean,
    val latencyMs: Long = 0L,
    val message: String
)

data class PairingRequestResult(
    val success: Boolean,
    val status: String,
    val message: String
)

data class SyncResult(
    val success: Boolean,
    val acknowledgedIds: List<String> = emptyList(),
    val syncedCount: Int = 0,
    val message: String
)
