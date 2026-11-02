package com.smartexambox.security.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.smartexambox.security.data.model.GeofenceZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationState(
    val latitude: Double = 12.9576,   // Default VELS Campus latitude
    val longitude: Double = 80.1444,  // Default VELS Campus longitude
    val accuracy: Float = 5.0f,
    val speed: Float = 0.0f,
    val isInsideGeofence: Boolean = true,
    val distanceToCenterMeters: Float = 0f,
    val lastUpdateTimestamp: Long = System.currentTimeMillis(),
    val hasPermission: Boolean = false,
    val isSimulated: Boolean = false
)

class LocationMonitor(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private var activeGeofence: GeofenceZone = GeofenceZone()
    private var isSimulating: Boolean = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (isSimulating) return
            val loc = result.lastLocation ?: return
            updateLocation(loc.latitude, loc.longitude, loc.accuracy, loc.speed, false)
        }
    }

    fun setGeofenceZone(zone: GeofenceZone) {
        activeGeofence = zone
        // Re-evaluate distance
        val current = _locationState.value
        updateLocation(current.latitude, current.longitude, current.accuracy, current.speed, current.isSimulated)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        val hasPerm = hasLocationPermission()
        _locationState.value = _locationState.value.copy(hasPermission = hasPerm)
        if (!hasPerm) return

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    if (!isSimulating) {
                        updateLocation(it.latitude, it.longitude, it.accuracy, it.speed, false)
                    }
                }
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(3000L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            _locationState.value = _locationState.value.copy(hasPermission = false)
        }
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun updateLocation(
        lat: Double,
        lng: Double,
        accuracy: Float,
        speed: Float,
        isSimulated: Boolean
    ) {
        val distance = calculateDistance(lat, lng, activeGeofence.latitude, activeGeofence.longitude)
        val inside = !activeGeofence.isActive || distance <= activeGeofence.radiusMeters

        _locationState.value = LocationState(
            latitude = lat,
            longitude = lng,
            accuracy = accuracy,
            speed = speed,
            isInsideGeofence = inside,
            distanceToCenterMeters = distance,
            lastUpdateTimestamp = System.currentTimeMillis(),
            hasPermission = hasLocationPermission(),
            isSimulated = isSimulated
        )
    }

    fun simulateLocation(lat: Double, lng: Double, accuracy: Float = 8.0f) {
        isSimulating = true
        updateLocation(lat, lng, accuracy, 1.2f, true)
    }

    fun resetSimulation() {
        isSimulating = false
        startTracking()
    }

    private fun calculateDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    companion object {
        @Volatile
        private var instance: LocationMonitor? = null

        fun getInstance(context: Context): LocationMonitor {
            return instance ?: synchronized(this) {
                instance ?: LocationMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
}
