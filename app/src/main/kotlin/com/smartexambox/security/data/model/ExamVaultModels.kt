package com.smartexambox.security.data.model

data class ExamPaperPacket(
    val packetId: String,
    val subjectCode: String,
    val subjectName: String,
    val department: String,
    val examDate: String,
    val sessionTime: String,
    val totalCopies: Int,
    val roomNumber: String,
    val sealedEnvelopeHash: String,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long? = null,
    val verificationMethod: String = "Biometric + Supervisor PIN"
)

data class GeofenceZone(
    val zoneId: String = "VELS-CAMPUS-ZONE-1",
    val centerName: String = "VELS University Exam Hall A",
    val latitude: Double = 12.9576,   // VELS College Campus, Pallavaram, Chennai
    val longitude: Double = 80.1444,
    val radiusMeters: Float = 250f,
    val isActive: Boolean = true
)

data class DeviceRegistration(
    val deviceId: String,
    val deviceModel: String,
    val centerName: String,
    val department: String,
    val supervisorName: String,
    val registrationTimestamp: Long = System.currentTimeMillis(),
    val status: String = "REGISTERED_ACTIVE"
)
