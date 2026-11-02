package com.smartexambox.security.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartexambox.security.data.model.AuthResultType
import com.smartexambox.security.data.model.DeviceRegistration
import com.smartexambox.security.data.model.ExamPaperPacket
import com.smartexambox.security.data.model.GeofenceZone
import com.smartexambox.security.data.model.SecurityEvent
import com.smartexambox.security.data.model.SecurityEventType
import com.smartexambox.security.data.model.SecurityStatus
import com.smartexambox.security.data.model.SeverityLevel
import com.smartexambox.security.data.repository.SecurityRepository
import com.smartexambox.security.data.repository.SecurityStats
import com.smartexambox.security.demo.DemoSimulationEngine
import com.smartexambox.security.monitor.BatteryMonitor
import com.smartexambox.security.monitor.LocationMonitor
import com.smartexambox.security.monitor.NetworkConnectivityMonitor
import com.smartexambox.security.notification.SecurityNotificationHelper
import com.smartexambox.security.security.BiometricAuthManager
import com.smartexambox.security.security.LockoutManager
import com.smartexambox.security.security.RiskAssessment
import com.smartexambox.security.security.RiskScoringEngine
import com.smartexambox.security.security.SecurityKeystoreManager
import com.smartexambox.security.sync.SyncManager
import com.smartexambox.security.data.db.SecurityDatabaseHelper
import com.smartexambox.security.network.ConnectionStatus
import com.smartexambox.security.network.HeartbeatTelemetry
import com.smartexambox.security.network.PairingClient
import com.smartexambox.security.network.PairingPreferences
import com.smartexambox.security.network.PhonePairingState
import com.smartexambox.security.network.PingResult
import com.smartexambox.security.network.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainSecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val repository = SecurityRepository.getInstance(context)
    val keystoreManager = SecurityKeystoreManager.getInstance(context)
    val lockoutManager = LockoutManager.getInstance(context)
    val locationMonitor = LocationMonitor.getInstance(context)
    val networkMonitor = NetworkConnectivityMonitor.getInstance(context)
    val batteryMonitor = BatteryMonitor.getInstance(context)
    val notificationHelper = SecurityNotificationHelper.getInstance(context)
    val syncManager = SyncManager.getInstance(context)
    val biometricAuthManager = BiometricAuthManager(context)
    private val riskScoringEngine = RiskScoringEngine()

    // Wi-Fi Pairing & Live Telemetry Client
    val pairingPrefs = PairingPreferences(context)
    val pairingClient = PairingClient()

    private val _pairingState = MutableStateFlow(
        PhonePairingState(
            isPaired = pairingPrefs.isPaired,
            serverUrl = pairingPrefs.serverUrl,
            authToken = pairingPrefs.authToken,
            deviceId = "VELS-EXAM-BOX-SEC-01",
            model = "Android Secure Terminal",
            connectionStatus = if (pairingPrefs.isPaired) ConnectionStatus.CONNECTING else ConnectionStatus.UNPAIRED,
            unsyncedCount = SecurityDatabaseHelper.getInstance(context).getPendingSyncCount()
        )
    )
    val pairingState: StateFlow<PhonePairingState> = _pairingState.asStateFlow()

    val demoEngine = DemoSimulationEngine(
        repository = repository,
        lockoutManager = lockoutManager,
        locationMonitor = locationMonitor,
        networkMonitor = networkMonitor,
        notificationHelper = notificationHelper
    )

    // Reactive Data Flows
    val eventsFlow: StateFlow<List<SecurityEvent>> = repository.eventsFlow
    val activeGeofenceFlow: StateFlow<GeofenceZone> = repository.activeGeofenceFlow
    val deviceRegistrationFlow: StateFlow<DeviceRegistration> = repository.deviceRegistrationFlow
    val locationState = locationMonitor.locationState
    val networkState = networkMonitor.networkState
    val batteryState = batteryMonitor.batteryState
    val syncState = syncManager.syncState
    val isLockedOutFlow = lockoutManager.isLockedOutFlow

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _statsFlow = MutableStateFlow(SecurityStats())
    val statsFlow: StateFlow<SecurityStats> = _statsFlow.asStateFlow()

    private val _demoMessage = MutableStateFlow<String?>(null)
    val demoMessage: StateFlow<String?> = _demoMessage.asStateFlow()

    private val _lockoutRemainingSeconds = MutableStateFlow(lockoutManager.getRemainingLockoutSeconds())
    val lockoutRemainingSeconds: StateFlow<Int> = _lockoutRemainingSeconds.asStateFlow()

    private var countdownJob: Job? = null

    // Sample sealed exam paper packets for VELS College
    private val _examPackets = MutableStateFlow(
        listOf(
            ExamPaperPacket(
                packetId = "VELS-CS8001-P1",
                subjectCode = "CS8001",
                subjectName = "Advanced Cryptography & Network Security",
                department = "Computer Science & Engineering",
                examDate = "Nov 02, 2026",
                sessionTime = "10:00 AM - 01:00 PM",
                totalCopies = 60,
                roomNumber = "Exam Hall 101",
                sealedEnvelopeHash = "SHA256:7e8a9f02c4b6e8a1523490fdca903b1e"
            ),
            ExamPaperPacket(
                packetId = "VELS-CS8002-P2",
                subjectCode = "CS8002",
                subjectName = "Cloud Infrastructure & IoT Security",
                department = "Computer Science & Engineering",
                examDate = "Nov 02, 2026",
                sessionTime = "10:00 AM - 01:00 PM",
                totalCopies = 60,
                roomNumber = "Exam Hall 102",
                sealedEnvelopeHash = "SHA256:1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d"
            ),
            ExamPaperPacket(
                packetId = "VELS-IT8003-P3",
                subjectCode = "IT8003",
                subjectName = "Mobile Application Development & Security",
                department = "Information Technology",
                examDate = "Nov 02, 2026",
                sessionTime = "02:00 PM - 05:00 PM",
                totalCopies = 55,
                roomNumber = "Exam Hall 201",
                sealedEnvelopeHash = "SHA256:9f8e7d6c5b4a3928170e1f2a3b4c5d6e"
            ),
            ExamPaperPacket(
                packetId = "VELS-EC8004-P4",
                subjectCode = "EC8004",
                subjectName = "Embedded Systems & Wireless Sensor Networks",
                department = "Electronics & Communication Eng.",
                examDate = "Nov 02, 2026",
                sessionTime = "02:00 PM - 05:00 PM",
                totalCopies = 50,
                roomNumber = "Exam Hall 202",
                sealedEnvelopeHash = "SHA256:4a5b6c7d8e9f0123456789abcdef0123"
            )
        )
    )
    val examPackets: StateFlow<List<ExamPaperPacket>> = _examPackets.asStateFlow()

    // Dynamic Risk Assessment derived from state
    val riskAssessment: StateFlow<RiskAssessment> = combine(
        lockoutManager.failedAttemptsFlow,
        lockoutManager.isLockedOutFlow,
        locationMonitor.locationState,
        networkMonitor.networkState,
        statsFlow
    ) { failed, locked, loc, net, stats ->
        riskScoringEngine.calculateRiskScore(
            failedAttempts = failed,
            isLockedOut = locked,
            isGeofenceViolated = !loc.isInsideGeofence,
            isWifiDisconnected = !net.isConnected,
            unacknowledgedCriticalAlerts = stats.criticalCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RiskAssessment(0, SecurityStatus.PROTECTED, listOf("System Initializing"))
    )

    init {
        viewModelScope.launch {
            repository.loadInitialData()
            refreshStats()
            locationMonitor.setGeofenceZone(repository.activeGeofenceFlow.value)
            locationMonitor.startTracking()
            syncManager.schedulePeriodicSync()
            checkLockoutCountdown()
            startLiveMonitoringLoop()
        }
    }

    fun isPinConfigured(): Boolean = keystoreManager.isPinConfigured()

    fun setupPin(pin: String) {
        keystoreManager.savePin(pin)
        viewModelScope.launch {
            repository.logEvent(
                eventType = SecurityEventType.DEVICE_REGISTERED,
                description = "Master Security PIN initialized in Android Keystore with salted SHA-256 hash",
                severity = SeverityLevel.INFO
            )
            refreshStats()
        }
    }

    fun verifyPinAndAuthenticate(pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (lockoutManager.checkIsLockedOut()) {
            val remaining = lockoutManager.getRemainingLockoutSeconds()
            onError("System locked out due to repeated failed attempts! Cooldown: ${remaining}s")
            return
        }

        if (keystoreManager.verifyPin(pin)) {
            lockoutManager.recordSuccessfulAttempt()
            _isVaultUnlocked.value = true
            viewModelScope.launch {
                val loc = locationMonitor.locationState.value
                repository.logEvent(
                    eventType = SecurityEventType.AUTH_SUCCESS,
                    description = "Successful authentication via Secure PIN. Vault access granted.",
                    severity = SeverityLevel.INFO,
                    authResult = AuthResultType.SUCCESS,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy
                )
                refreshStats()
            }
            onSuccess()
        } else {
            val state = lockoutManager.recordFailedAttempt()
            viewModelScope.launch {
                val loc = locationMonitor.locationState.value
                val isLocked = state.isLockedOut
                val eventType = if (isLocked) SecurityEventType.SECURITY_LOCKOUT else SecurityEventType.AUTH_FAILED
                val severity = if (isLocked) SeverityLevel.CRITICAL else SeverityLevel.WARNING
                val desc = if (isLocked) {
                    "CRITICAL: 3 consecutive failed authentication attempts! System temporarily locked for 60 seconds."
                } else {
                    "Invalid PIN entered (Failed attempt ${state.failedAttempts}/3)."
                }

                repository.logEvent(
                    eventType = eventType,
                    description = desc,
                    severity = severity,
                    authResult = if (isLocked) AuthResultType.LOCKED else AuthResultType.FAILED,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy
                )

                notificationHelper.sendSecurityAlertNotification(
                    title = if (isLocked) "SECURITY LOCKOUT ACTIVATED" else "Authentication Failure",
                    message = desc,
                    severity = severity
                )

                refreshStats()
                if (isLocked) startLockoutCountdown()
            }

            if (state.isLockedOut) {
                onError("Application LOCKED OUT! Exceeded maximum attempts. Cooldown: ${state.remainingSeconds}s")
            } else {
                onError("Incorrect PIN! Attempt ${state.failedAttempts}/3.")
            }
        }
    }

    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (lockoutManager.checkIsLockedOut()) {
            onError("System is currently locked out! Please wait for cooldown.")
            return
        }

        biometricAuthManager.showBiometricPrompt(
            activity = activity,
            title = "Exam Paper Vault Verification",
            subtitle = "Biometric authentication required to access confidential exam packets",
            onSuccess = {
                lockoutManager.recordSuccessfulAttempt()
                _isVaultUnlocked.value = true
                viewModelScope.launch {
                    val loc = locationMonitor.locationState.value
                    repository.logEvent(
                        eventType = SecurityEventType.AUTH_SUCCESS,
                        description = "Biometric identity verified successfully. Question paper vault unlocked.",
                        severity = SeverityLevel.INFO,
                        authResult = AuthResultType.SUCCESS,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy
                    )
                    refreshStats()
                }
                onSuccess()
            },
            onError = { code, err ->
                onError("Biometric authentication error: $err")
            },
            onFailed = {
                val state = lockoutManager.recordFailedAttempt()
                viewModelScope.launch {
                    val desc = "Biometric verification failed (Attempt ${state.failedAttempts}/3)."
                    repository.logEvent(
                        eventType = SecurityEventType.AUTH_FAILED,
                        description = desc,
                        severity = SeverityLevel.WARNING,
                        authResult = AuthResultType.FAILED
                    )
                    refreshStats()
                    if (state.isLockedOut) startLockoutCountdown()
                }
                onError("Biometric verification failed. Please try again.")
            }
        )
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        viewModelScope.launch {
            repository.logEvent(
                eventType = SecurityEventType.DEMO_SIMULATION,
                description = "Digital examination vault sealed and re-locked.",
                severity = SeverityLevel.INFO
            )
            refreshStats()
        }
    }

    fun unlockVaultDirectly(reason: String = "Authorized via Laptop USB Controller") {
        _isVaultUnlocked.value = true
        viewModelScope.launch {
            val loc = locationMonitor.locationState.value
            repository.logEvent(
                eventType = SecurityEventType.AUTH_SUCCESS,
                description = "Vault UNLOCKED: $reason",
                severity = SeverityLevel.INFO,
                authResult = AuthResultType.SUCCESS,
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracy = loc.accuracy
            )
            refreshStats()
        }
    }

    fun acknowledgeAlert(eventId: String) {
        viewModelScope.launch {
            repository.acknowledgeAlert(eventId)
            refreshStats()
        }
    }

    fun acknowledgeAllAlerts() {
        viewModelScope.launch {
            repository.acknowledgeAllAlerts()
            refreshStats()
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            val net = networkMonitor.networkState.value
            syncManager.performImmediateSync(net.isConnected)
            refreshStats()
        }
    }

    fun updateGeofence(zone: GeofenceZone) {
        viewModelScope.launch {
            repository.updateGeofence(zone)
            locationMonitor.setGeofenceZone(zone)
            refreshStats()
        }
    }

    fun updateDeviceRegistration(device: DeviceRegistration) {
        viewModelScope.launch {
            repository.updateDeviceRegistration(device)
            refreshStats()
        }
    }

    fun exportAuditLogs(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            val file = repository.exportEventsToCsv(context)
            onComplete(file)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
            refreshStats()
        }
    }

    private suspend fun refreshStats() {
        _statsFlow.value = repository.getStats()
        syncManager.updatePendingCount()
    }

    private fun checkLockoutCountdown() {
        if (lockoutManager.checkIsLockedOut()) {
            startLockoutCountdown()
        }
    }

    private fun startLockoutCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (lockoutManager.checkIsLockedOut()) {
                val remaining = lockoutManager.getRemainingLockoutSeconds()
                _lockoutRemainingSeconds.value = remaining
                delay(1000)
            }
            _lockoutRemainingSeconds.value = 0
            refreshStats()
        }
    }

    // --- Demo Simulation Actions ---

    fun runSimulatedFailedAuth() {
        viewModelScope.launch {
            demoEngine.simulateFailedAuthentication { msg ->
                _demoMessage.value = msg
            }
            refreshStats()
            if (lockoutManager.checkIsLockedOut()) startLockoutCountdown()
        }
    }

    fun runSimulatedGeofenceViolation() {
        viewModelScope.launch {
            demoEngine.simulateGeofenceViolation { msg ->
                _demoMessage.value = msg
            }
            refreshStats()
        }
    }

    fun runSimulatedGeofenceRestore() {
        viewModelScope.launch {
            demoEngine.simulateGeofenceRestore { msg ->
                _demoMessage.value = msg
            }
            refreshStats()
        }
    }

    fun runSimulatedNetworkDisconnection() {
        viewModelScope.launch {
            demoEngine.simulateNetworkDisconnection { msg ->
                _demoMessage.value = msg
            }
            refreshStats()
        }
    }

    fun runSimulatedNetworkRestoration() {
        viewModelScope.launch {
            demoEngine.simulateNetworkRestoration { msg ->
                _demoMessage.value = msg
            }
            refreshStats()
        }
    }

    fun runSimulatedSuccessfulAuth() {
        viewModelScope.launch {
            demoEngine.simulateSuccessfulAuthentication { msg ->
                _demoMessage.value = msg
            }
            _isVaultUnlocked.value = true
            refreshStats()
        }
    }

    fun runSimulatedAlertAcknowledgement() {
        viewModelScope.launch {
            demoEngine.simulateAlertAcknowledgement { msg ->
                _demoMessage.value = msg
            }
            refreshStats()
        }
    }

    fun clearDemoMessage() {
        _demoMessage.value = null
    }

    // --- Wi-Fi Pairing, Telemetry & Synchronization Methods ---

    private fun startLiveMonitoringLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentPairing = _pairingState.value
                val pendingCount = SecurityDatabaseHelper.getInstance(context).getPendingSyncCount()

                if (currentPairing.isPaired && currentPairing.authToken != null) {
                    val loc = locationMonitor.locationState.value
                    val net = networkMonitor.networkState.value
                    val batt = batteryMonitor.batteryState.value
                    val risk = riskAssessment.value

                    val telemetry = HeartbeatTelemetry(
                        battery_level = batt.percentage,
                        is_charging = batt.isCharging,
                        wifi_ssid = net.networkName,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        is_inside_geofence = loc.isInsideGeofence,
                        threat_risk_score = risk.score,
                        vault_status = if (_isVaultUnlocked.value) "UNLOCKED" else "LOCKED",
                        unsynced_event_count = pendingCount
                    )

                    val ok = pairingClient.sendHeartbeat(currentPairing.serverUrl, currentPairing.authToken, telemetry)
                    if (ok) {
                        val newStatus = if (pendingCount > 0) ConnectionStatus.SYNC_PENDING else ConnectionStatus.CONNECTED
                        _pairingState.value = _pairingState.value.copy(
                            connectionStatus = newStatus,
                            unsyncedCount = pendingCount,
                            statusMessage = if (pendingCount > 0) "$pendingCount offline event(s) queued for sync" else "Connected to Laptop Monitor"
                        )

                        // Auto-sync pending events if any
                        if (pendingCount > 0) {
                            val db = SecurityDatabaseHelper.getInstance(context)
                            val unsynced = db.getUnsyncedEvents()
                            if (unsynced.isNotEmpty()) {
                                val syncRes = pairingClient.syncEvents(currentPairing.serverUrl, currentPairing.authToken, currentPairing.deviceId, unsynced)
                                if (syncRes.success && syncRes.acknowledgedIds.isNotEmpty()) {
                                    db.markEventsAsSynced(syncRes.acknowledgedIds)
                                    val remaining = db.getPendingSyncCount()
                                    _pairingState.value = _pairingState.value.copy(
                                        unsyncedCount = remaining,
                                        lastSyncTimestamp = System.currentTimeMillis(),
                                        connectionStatus = if (remaining > 0) ConnectionStatus.SYNC_PENDING else ConnectionStatus.CONNECTED
                                    )
                                    refreshStats()
                                }
                            }
                        }
                    } else {
                        _pairingState.value = _pairingState.value.copy(
                            connectionStatus = ConnectionStatus.DISCONNECTED,
                            unsyncedCount = pendingCount,
                            statusMessage = "Offline queue active: Laptop unreachable. Events buffering locally in SQLite."
                        )
                    }
                } else {
                    _pairingState.value = _pairingState.value.copy(
                        connectionStatus = ConnectionStatus.UNPAIRED,
                        unsyncedCount = pendingCount
                    )
                }

                delay(8000)
            }
        }
    }

    fun triggerImmediateSyncIfConnected() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPairing = _pairingState.value
            if (currentPairing.isPaired && currentPairing.authToken != null) {
                val db = SecurityDatabaseHelper.getInstance(context)
                val unsynced = db.getUnsyncedEvents()
                if (unsynced.isNotEmpty()) {
                    val res = pairingClient.syncEvents(currentPairing.serverUrl, currentPairing.authToken, currentPairing.deviceId, unsynced)
                    if (res.success && res.acknowledgedIds.isNotEmpty()) {
                        db.markEventsAsSynced(res.acknowledgedIds)
                        _pairingState.value = _pairingState.value.copy(
                            unsyncedCount = db.getPendingSyncCount(),
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        refreshStats()
                    }
                }
            }
        }
    }

    fun testServerPing(url: String, onResult: (PingResult) -> Unit) {
        viewModelScope.launch {
            val res = pairingClient.pingServer(url)
            _pairingState.value = _pairingState.value.copy(
                lastPingLatencyMs = if (res.success) res.latencyMs else null
            )
            onResult(res)
        }
    }

    fun requestPairing(
        serverUrl: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val dev = repository.deviceRegistrationFlow.value
            _pairingState.value = _pairingState.value.copy(
                serverUrl = serverUrl,
                statusMessage = "Verifying pairing code with laptop...",
                connectionStatus = ConnectionStatus.CONNECTING
            )

            val res = pairingClient.requestPairing(serverUrl, code, dev.deviceId, dev.deviceModel)
            if (res.success) {
                _pairingState.value = _pairingState.value.copy(
                    isAwaitingApproval = true,
                    statusMessage = "Code verified! Waiting for laptop administrator to click 'Approve'..."
                )
                onSuccess()
                startPollingForApproval(serverUrl)
            } else {
                _pairingState.value = _pairingState.value.copy(
                    isAwaitingApproval = false,
                    connectionStatus = ConnectionStatus.UNPAIRED,
                    statusMessage = res.message
                )
                onError(res.message)
            }
        }
    }

    private var pollJob: Job? = null
    private fun startPollingForApproval(serverUrl: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            var attempts = 0
            while (attempts < 60 && isActive) {
                delay(2000)
                attempts++
                val json = pairingClient.pollPairingStatus(serverUrl)
                if (json != null) {
                    val status = json.optString("status")
                    if (status == "PAIRED") {
                        val token = json.optString("auth_token")
                        pairingPrefs.serverUrl = serverUrl
                        pairingPrefs.authToken = token
                        pairingPrefs.isPaired = true

                        _pairingState.value = _pairingState.value.copy(
                            isPaired = true,
                            authToken = token,
                            serverUrl = serverUrl,
                            isAwaitingApproval = false,
                            connectionStatus = ConnectionStatus.CONNECTED,
                            statusMessage = "Successfully paired with Laptop Monitor!"
                        )

                        repository.logEvent(
                            eventType = SecurityEventType.DEVICE_REGISTERED,
                            description = "Phone successfully paired with Laptop Monitor at $serverUrl",
                            severity = SeverityLevel.INFO
                        )
                        triggerImmediateSyncIfConnected()
                        break
                    } else if (status == "REJECTED" || status == "UNPAIRED") {
                        _pairingState.value = _pairingState.value.copy(
                            isAwaitingApproval = false,
                            connectionStatus = ConnectionStatus.UNPAIRED,
                            statusMessage = "Pairing was rejected or cancelled."
                        )
                        break
                    }
                }
            }
        }
    }

    fun unpairFromLaptop(onComplete: () -> Unit) {
        viewModelScope.launch {
            val url = _pairingState.value.serverUrl
            val token = _pairingState.value.authToken
            pollJob?.cancel()
            pairingClient.unpair(url, token)
            pairingPrefs.clearPairing()

            _pairingState.value = _pairingState.value.copy(
                isPaired = false,
                authToken = null,
                isAwaitingApproval = false,
                connectionStatus = ConnectionStatus.UNPAIRED,
                statusMessage = "Unpaired from laptop."
            )

            repository.logEvent(
                eventType = SecurityEventType.DEVICE_REGISTERED,
                description = "Phone unpaired from Laptop Monitor.",
                severity = SeverityLevel.WARNING
            )
            onComplete()
        }
    }

    fun forceSyncOfflineEvents(onComplete: (SyncResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _pairingState.value
            if (!state.isPaired || state.authToken == null) {
                onComplete(SyncResult(false, emptyList(), 0, "Phone is not paired with laptop."))
                return@launch
            }
            val db = SecurityDatabaseHelper.getInstance(context)
            val unsynced = db.getUnsyncedEvents()
            val res = pairingClient.syncEvents(state.serverUrl, state.authToken, state.deviceId, unsynced)
            if (res.success && res.acknowledgedIds.isNotEmpty()) {
                db.markEventsAsSynced(res.acknowledgedIds)
                val remaining = db.getPendingSyncCount()
                _pairingState.value = _pairingState.value.copy(
                    unsyncedCount = remaining,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    connectionStatus = if (remaining > 0) ConnectionStatus.SYNC_PENDING else ConnectionStatus.CONNECTED
                )
                refreshStats()
            }
            onComplete(res)
        }
    }
}
