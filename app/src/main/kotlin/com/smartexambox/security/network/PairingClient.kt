package com.smartexambox.security.network

import com.google.gson.Gson
import com.smartexambox.security.data.model.SecurityEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class PairingClient(private val gson: Gson = Gson()) {

    private val connectTimeoutMs = 6000
    private val readTimeoutMs = 8000

    private fun normalizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        return url
    }

    suspend fun pingServer(rawUrl: String): PingResult = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        val startTime = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/api/ping")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = false
            }

            val code = conn.responseCode
            val latency = System.currentTimeMillis() - startTime
            if (code == 200) {
                PingResult(
                    success = true,
                    latencyMs = latency,
                    message = "Connected to Laptop Monitor ($latency ms latency)"
                )
            } else {
                PingResult(
                    success = false,
                    message = "Server returned HTTP error $code"
                )
            }
        } catch (e: SocketTimeoutException) {
            PingResult(
                success = false,
                message = "Connection timed out. Check: 1) Is Laptop IP correct? 2) Are phone and laptop on the same Wi-Fi?"
            )
        } catch (e: ConnectException) {
            PingResult(
                success = false,
                message = "Connection refused. Ensure 'python server.py' is running on laptop and Windows Firewall allows port 8080."
            )
        } catch (e: UnknownHostException) {
            PingResult(
                success = false,
                message = "Unknown host. Ensure you entered a valid IP address (e.g., 192.168.1.39:8080)."
            )
        } catch (e: Exception) {
            PingResult(
                success = false,
                message = "Network error: ${e.localizedMessage ?: "Cannot reach server"}"
            )
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun requestPairing(
        rawUrl: String,
        pairingCode: String,
        deviceId: String,
        model: String
    ): PairingRequestResult = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/api/pairing/request")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
                put("device_id", deviceId)
                put("model", model)
                put("pairing_code", pairingCode.trim())
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val respStr = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            val json = JSONObject(respStr)

            if (code in 200..299 && json.optBoolean("success", false)) {
                PairingRequestResult(
                    success = true,
                    status = json.optString("status", "PENDING_CONFIRMATION"),
                    message = json.optString("message", "Pairing code verified! Awaiting laptop administrator confirmation.")
                )
            } else {
                PairingRequestResult(
                    success = false,
                    status = "ERROR",
                    message = json.optString("error", "Pairing rejected by server (HTTP $code)")
                )
            }
        } catch (e: SocketTimeoutException) {
            PairingRequestResult(false, "TIMEOUT", "Timeout contacting server. Check Wi-Fi connection.")
        } catch (e: ConnectException) {
            PairingRequestResult(false, "REFUSED", "Server unreachable on port 8080. Check firewall and server status.")
        } catch (e: Exception) {
            PairingRequestResult(false, "ERROR", "Pairing request failed: ${e.localizedMessage}")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun pollPairingStatus(rawUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/api/pairing/status")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
            }
            if (conn.responseCode == 200) {
                val respStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                JSONObject(respStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun sendHeartbeat(
        rawUrl: String,
        authToken: String,
        telemetry: HeartbeatTelemetry
    ): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/api/heartbeat")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $authToken")
            }

            val payload = JSONObject().apply {
                put("battery_level", telemetry.battery_level)
                put("is_charging", telemetry.is_charging)
                put("wifi_ssid", telemetry.wifi_ssid)
                put("latitude", telemetry.latitude ?: JSONObject.NULL)
                put("longitude", telemetry.longitude ?: JSONObject.NULL)
                put("is_inside_geofence", telemetry.is_inside_geofence)
                put("threat_risk_score", telemetry.threat_risk_score)
                put("vault_status", telemetry.vault_status)
                put("unsynced_event_count", telemetry.unsynced_event_count)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun syncEvents(
        rawUrl: String,
        authToken: String,
        deviceId: String,
        events: List<SecurityEvent>
    ): SyncResult = withContext(Dispatchers.IO) {
        if (events.isEmpty()) {
            return@withContext SyncResult(true, emptyList(), 0, "No pending events to sync")
        }

        val baseUrl = normalizeUrl(rawUrl)
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/api/events/sync")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $authToken")
            }

            val eventsArray = JSONArray()
            events.forEach { ev ->
                val obj = JSONObject().apply {
                    put("id", ev.id)
                    put("eventType", ev.eventType.name)
                    put("deviceId", ev.deviceId)
                    put("timestamp", ev.timestamp)
                    put("formattedTime", ev.formattedTime)
                    put("description", ev.description)
                    put("severity", ev.severity.name)
                    put("latitude", ev.latitude ?: JSONObject.NULL)
                    put("longitude", ev.longitude ?: JSONObject.NULL)
                    put("isSimulated", ev.isSimulated)
                }
                eventsArray.put(obj)
            }

            val payload = JSONObject().apply {
                put("device_id", deviceId)
                put("events", eventsArray)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val respStr = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            val json = JSONObject(respStr)

            if (code in 200..299 && json.optBoolean("success", false)) {
                val ackArray = json.optJSONArray("acknowledged_ids")
                val ackList = mutableListOf<String>()
                if (ackArray != null) {
                    for (i in 0 until ackArray.length()) {
                        ackList.add(ackArray.getString(i))
                    }
                }
                val syncedCount = json.optInt("synced_count", ackList.size)
                SyncResult(
                    success = true,
                    acknowledgedIds = ackList,
                    syncedCount = syncedCount,
                    message = "Synchronized $syncedCount event(s) to laptop dashboard"
                )
            } else {
                SyncResult(
                    success = false,
                    message = json.optString("error", "Sync failed with HTTP $code")
                )
            }
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Sync failed: ${e.localizedMessage ?: "Network error"}"
            )
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun unpair(rawUrl: String, authToken: String?): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = normalizeUrl(rawUrl)
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl/api/pairing/unpair")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                if (authToken != null) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
            }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
