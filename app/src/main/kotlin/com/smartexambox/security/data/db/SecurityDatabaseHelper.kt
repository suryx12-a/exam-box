package com.smartexambox.security.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.smartexambox.security.data.model.AuthResultType
import com.smartexambox.security.data.model.DeviceRegistration
import com.smartexambox.security.data.model.GeofenceZone
import com.smartexambox.security.data.model.SecurityEvent
import com.smartexambox.security.data.model.SecurityEventType
import com.smartexambox.security.data.model.SeverityLevel

class SecurityDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_EVENT_TYPE TEXT NOT NULL,
                $COL_DEVICE_ID TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_FORMATTED_TIME TEXT NOT NULL,
                $COL_LATITUDE REAL,
                $COL_LONGITUDE REAL,
                $COL_ACCURACY REAL,
                $COL_AUTH_RESULT TEXT NOT NULL,
                $COL_SEVERITY TEXT NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_IS_ACKNOWLEDGED INTEGER NOT NULL DEFAULT 0,
                $COL_IS_SYNCED INTEGER NOT NULL DEFAULT 0,
                $COL_IS_SIMULATED INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_GEOFENCE (
                $COL_ZONE_ID TEXT PRIMARY KEY,
                $COL_CENTER_NAME TEXT NOT NULL,
                $COL_GEO_LAT REAL NOT NULL,
                $COL_GEO_LNG REAL NOT NULL,
                $COL_RADIUS REAL NOT NULL,
                $COL_GEO_ACTIVE INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_DEVICE (
                $COL_DEV_ID TEXT PRIMARY KEY,
                $COL_DEV_MODEL TEXT NOT NULL,
                $COL_DEV_CENTER TEXT NOT NULL,
                $COL_DEV_DEPT TEXT NOT NULL,
                $COL_DEV_SUPERVISOR TEXT NOT NULL,
                $COL_DEV_TIMESTAMP INTEGER NOT NULL,
                $COL_DEV_STATUS TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Seed default VELS College Geofence Zone
        val cv = ContentValues().apply {
            put(COL_ZONE_ID, "VELS-CAMPUS-ZONE-1")
            put(COL_CENTER_NAME, "VELS University Exam Hall A, Pallavaram")
            put(COL_GEO_LAT, 12.9576)
            put(COL_GEO_LNG, 80.1444)
            put(COL_RADIUS, 250f)
            put(COL_GEO_ACTIVE, 1)
        }
        db.insert(TABLE_GEOFENCE, null, cv)

        // Seed default device registration
        val devCv = ContentValues().apply {
            put(COL_DEV_ID, "VELS-EXAM-BOX-SEC-01")
            put(COL_DEV_MODEL, "Android Secure Terminal")
            put(COL_DEV_CENTER, "VELS University Exam Center A")
            put(COL_DEV_DEPT, "Computer Science & Engineering")
            put(COL_DEV_SUPERVISOR, "Prof. Chief Superintendent")
            put(COL_DEV_TIMESTAMP, System.currentTimeMillis())
            put(COL_DEV_STATUS, "REGISTERED_ACTIVE")
        }
        db.insert(TABLE_DEVICE, null, devCv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GEOFENCE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DEVICE")
        onCreate(db)
    }

    // --- Events DAO Operations ---

    fun insertEvent(event: SecurityEvent): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ID, event.id)
            put(COL_EVENT_TYPE, event.eventType.name)
            put(COL_DEVICE_ID, event.deviceId)
            put(COL_TIMESTAMP, event.timestamp)
            put(COL_FORMATTED_TIME, event.formattedTime)
            put(COL_LATITUDE, event.latitude)
            put(COL_LONGITUDE, event.longitude)
            put(COL_ACCURACY, event.accuracy)
            put(COL_AUTH_RESULT, event.authResult.name)
            put(COL_SEVERITY, event.severity.name)
            put(COL_DESCRIPTION, event.description)
            put(COL_IS_ACKNOWLEDGED, if (event.isAcknowledged) 1 else 0)
            put(COL_IS_SYNCED, if (event.isSynced) 1 else 0)
            put(COL_IS_SIMULATED, if (event.isSimulated) 1 else 0)
        }
        return db.insert(TABLE_EVENTS, null, cv) != -1L
    }

    fun getAllEvents(limit: Int = 200): List<SecurityEvent> {
        val list = mutableListOf<SecurityEvent>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToEvent(it))
            }
        }
        return list
    }

    fun getEventsBySeverity(severity: SeverityLevel): List<SecurityEvent> {
        val list = mutableListOf<SecurityEvent>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$COL_SEVERITY = ?",
            arrayOf(severity.name),
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToEvent(it))
            }
        }
        return list
    }

    fun acknowledgeEvent(eventId: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_IS_ACKNOWLEDGED, 1)
        }
        return db.update(TABLE_EVENTS, cv, "$COL_ID = ?", arrayOf(eventId)) > 0
    }

    fun acknowledgeAllEvents(): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_IS_ACKNOWLEDGED, 1)
        }
        return db.update(TABLE_EVENTS, cv, "$COL_IS_ACKNOWLEDGED = 0", null)
    }

    fun getUnsyncedEvents(): List<SecurityEvent> {
        val list = mutableListOf<SecurityEvent>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$COL_IS_SYNCED = 0",
            null,
            null,
            null,
            "$COL_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToEvent(it))
            }
        }
        return list
    }

    fun markEventsAsSynced(eventIds: List<String>): Int {
        if (eventIds.isEmpty()) return 0
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_IS_SYNCED, 1)
        }
        var updated = 0
        db.beginTransaction()
        try {
            for (id in eventIds) {
                updated += db.update(TABLE_EVENTS, cv, "$COL_ID = ?", arrayOf(id))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return updated
    }

    fun getEventCountBySeverity(severity: SeverityLevel): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_EVENTS WHERE $COL_SEVERITY = ?",
            arrayOf(severity.name)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getFailedAttemptsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_EVENTS WHERE $COL_AUTH_RESULT = ?",
            arrayOf(AuthResultType.FAILED.name)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getSuspiciousEventsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_EVENTS WHERE $COL_SEVERITY IN ('HIGH', 'CRITICAL')",
            null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getPendingSyncCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_EVENTS WHERE $COL_IS_SYNCED = 0",
            null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    // --- Geofence Operations ---

    fun getActiveGeofenceZone(): GeofenceZone {
        val db = readableDatabase
        val cursor = db.query(TABLE_GEOFENCE, null, "$COL_GEO_ACTIVE = 1", null, null, null, null, "1")
        cursor.use {
            if (it.moveToFirst()) {
                return GeofenceZone(
                    zoneId = it.getString(it.getColumnIndexOrThrow(COL_ZONE_ID)),
                    centerName = it.getString(it.getColumnIndexOrThrow(COL_CENTER_NAME)),
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COL_GEO_LAT)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COL_GEO_LNG)),
                    radiusMeters = it.getFloat(it.getColumnIndexOrThrow(COL_RADIUS)),
                    isActive = it.getInt(it.getColumnIndexOrThrow(COL_GEO_ACTIVE)) == 1
                )
            }
        }
        return GeofenceZone()
    }

    fun updateGeofenceZone(zone: GeofenceZone): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ZONE_ID, zone.zoneId)
            put(COL_CENTER_NAME, zone.centerName)
            put(COL_GEO_LAT, zone.latitude)
            put(COL_GEO_LNG, zone.longitude)
            put(COL_RADIUS, zone.radiusMeters)
            put(COL_GEO_ACTIVE, if (zone.isActive) 1 else 0)
        }
        val rows = db.update(TABLE_GEOFENCE, cv, "$COL_ZONE_ID = ?", arrayOf(zone.zoneId))
        return if (rows > 0) true else db.insert(TABLE_GEOFENCE, null, cv) != -1L
    }

    // --- Device Operations ---

    fun getDeviceRegistration(): DeviceRegistration {
        val db = readableDatabase
        val cursor = db.query(TABLE_DEVICE, null, null, null, null, null, null, "1")
        cursor.use {
            if (it.moveToFirst()) {
                return DeviceRegistration(
                    deviceId = it.getString(it.getColumnIndexOrThrow(COL_DEV_ID)),
                    deviceModel = it.getString(it.getColumnIndexOrThrow(COL_DEV_MODEL)),
                    centerName = it.getString(it.getColumnIndexOrThrow(COL_DEV_CENTER)),
                    department = it.getString(it.getColumnIndexOrThrow(COL_DEV_DEPT)),
                    supervisorName = it.getString(it.getColumnIndexOrThrow(COL_DEV_SUPERVISOR)),
                    registrationTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_DEV_TIMESTAMP)),
                    status = it.getString(it.getColumnIndexOrThrow(COL_DEV_STATUS))
                )
            }
        }
        return DeviceRegistration(
            deviceId = "VELS-EXAM-BOX-SEC-01",
            deviceModel = "Android Secure Device",
            centerName = "VELS University Exam Center A",
            department = "Computer Science & Engineering",
            supervisorName = "Prof. Chief Superintendent"
        )
    }

    fun updateDeviceRegistration(device: DeviceRegistration): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_DEV_ID, device.deviceId)
            put(COL_DEV_MODEL, device.deviceModel)
            put(COL_DEV_CENTER, device.centerName)
            put(COL_DEV_DEPT, device.department)
            put(COL_DEV_SUPERVISOR, device.supervisorName)
            put(COL_DEV_TIMESTAMP, device.registrationTimestamp)
            put(COL_DEV_STATUS, device.status)
        }
        val rows = db.update(TABLE_DEVICE, cv, "$COL_DEV_ID = ?", arrayOf(device.deviceId))
        return if (rows > 0) true else db.insert(TABLE_DEVICE, null, cv) != -1L
    }

    fun clearAllLogs() {
        val db = writableDatabase
        db.delete(TABLE_EVENTS, null, null)
    }

    private fun cursorToEvent(c: Cursor): SecurityEvent {
        val latIndex = c.getColumnIndexOrThrow(COL_LATITUDE)
        val lngIndex = c.getColumnIndexOrThrow(COL_LONGITUDE)
        val accIndex = c.getColumnIndexOrThrow(COL_ACCURACY)

        return SecurityEvent(
            id = c.getString(c.getColumnIndexOrThrow(COL_ID)),
            eventType = runCatching {
                SecurityEventType.valueOf(c.getString(c.getColumnIndexOrThrow(COL_EVENT_TYPE)))
            }.getOrDefault(SecurityEventType.DEMO_SIMULATION),
            deviceId = c.getString(c.getColumnIndexOrThrow(COL_DEVICE_ID)),
            timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP)),
            formattedTime = c.getString(c.getColumnIndexOrThrow(COL_FORMATTED_TIME)),
            latitude = if (c.isNull(latIndex)) null else c.getDouble(latIndex),
            longitude = if (c.isNull(lngIndex)) null else c.getDouble(lngIndex),
            accuracy = if (c.isNull(accIndex)) null else c.getFloat(accIndex),
            authResult = runCatching {
                AuthResultType.valueOf(c.getString(c.getColumnIndexOrThrow(COL_AUTH_RESULT)))
            }.getOrDefault(AuthResultType.NOT_APPLICABLE),
            severity = runCatching {
                SeverityLevel.valueOf(c.getString(c.getColumnIndexOrThrow(COL_SEVERITY)))
            }.getOrDefault(SeverityLevel.INFO),
            description = c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)),
            isAcknowledged = c.getInt(c.getColumnIndexOrThrow(COL_IS_ACKNOWLEDGED)) == 1,
            isSynced = c.getInt(c.getColumnIndexOrThrow(COL_IS_SYNCED)) == 1,
            isSimulated = c.getInt(c.getColumnIndexOrThrow(COL_IS_SIMULATED)) == 1
        )
    }

    companion object {
        const val DATABASE_NAME = "smart_exam_security.db"
        const val DATABASE_VERSION = 1

        private const val TABLE_EVENTS = "security_events"
        private const val COL_ID = "id"
        private const val COL_EVENT_TYPE = "event_type"
        private const val COL_DEVICE_ID = "device_id"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_FORMATTED_TIME = "formatted_time"
        private const val COL_LATITUDE = "latitude"
        private const val COL_LONGITUDE = "longitude"
        private const val COL_ACCURACY = "accuracy"
        private const val COL_AUTH_RESULT = "auth_result"
        private const val COL_SEVERITY = "severity"
        private const val COL_DESCRIPTION = "description"
        private const val COL_IS_ACKNOWLEDGED = "is_acknowledged"
        private const val COL_IS_SYNCED = "is_synced"
        private const val COL_IS_SIMULATED = "is_simulated"

        private const val TABLE_GEOFENCE = "geofence_zones"
        private const val COL_ZONE_ID = "zone_id"
        private const val COL_CENTER_NAME = "center_name"
        private const val COL_GEO_LAT = "latitude"
        private const val COL_GEO_LNG = "longitude"
        private const val COL_RADIUS = "radius_meters"
        private const val COL_GEO_ACTIVE = "is_active"

        private const val TABLE_DEVICE = "registered_device"
        private const val COL_DEV_ID = "device_id"
        private const val COL_DEV_MODEL = "device_model"
        private const val COL_DEV_CENTER = "center_name"
        private const val COL_DEV_DEPT = "department"
        private const val COL_DEV_SUPERVISOR = "supervisor_name"
        private const val COL_DEV_TIMESTAMP = "reg_timestamp"
        private const val COL_DEV_STATUS = "status"

        @Volatile
        private var instance: SecurityDatabaseHelper? = null

        fun getInstance(context: Context): SecurityDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: SecurityDatabaseHelper(context).also { instance = it }
            }
        }
    }
}
