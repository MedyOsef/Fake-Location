package com.example.fakelocation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MockLocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var mockJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        private const val CHANNEL_ID = "mock_gps_channel"
        private const val NOTIF_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lat = intent?.getDoubleExtra(EXTRA_LAT, 5.3600) ?: 5.3600   // Abidjan par défaut
        val lng = intent?.getDoubleExtra(EXTRA_LNG, -4.0083) ?: -4.0083

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        startForeground(NOTIF_ID, buildNotification(lat, lng))
        setupMockProvider()
        startMocking(lat, lng)

        return START_STICKY
    }

    private fun setupMockProvider() {
        try {
            // Supprimer l'ancien provider si existant
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {}

        try {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                /* requiresNetwork = */ false,
                /* requiresSatellite = */ false,
                /* requiresCell = */ false,
                /* hasMonetaryCost = */ false,
                /* supportsAltitude = */ true,
                /* supportsSpeed = */ true,
                /* supportsBearing = */ true,
                /* powerRequirement = */ Criteria.POWER_LOW,
                /* accuracy = */ Criteria.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun startMocking(lat: Double, lng: Double) {
        mockJob?.cancel()
        mockJob = scope.launch {
            while (isActive) {
                pushLocation(lat, lng)
                delay(1000L)
            }
        }
    }

    private fun pushLocation(lat: Double, lng: Double) {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = lat
            longitude = lng
            altitude = 0.0
            accuracy = 1.0f
            bearing = 0.0f
            speed = 0.0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(lat: Double, lng: Double): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mock GPS",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 Fake Location actif")
            .setContentText("lat: ${"%.4f".format(lat)}, lng: ${"%.4f".format(lng)}")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        mockJob?.cancel()
        scope.cancel()
        try {
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}