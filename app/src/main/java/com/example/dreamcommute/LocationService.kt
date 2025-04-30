package com.example.dreamcommute

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var wakeLock: PowerManager.WakeLock
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "LocationAlarmChannel"

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for foreground service
        createNotificationChannel()

        // Keep device from going into deep sleep
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CommuterAlarm::LocationServiceWakeLock"
        )
        wakeLock.acquire(6 * 60 * 60 * 1000L) // 6 hours max

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    checkProximityToDestination(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as a foreground service to prevent system killing it
        startForeground(NOTIFICATION_ID, createNotification())

        // Request location updates
        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun checkProximityToDestination(currentLocation: Location) {
        val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val targetLat = sharedPreferences.getFloat("lat", 0f).toDouble()
        val targetLng = sharedPreferences.getFloat("lng", 0f).toDouble()
        val targetRadius = sharedPreferences.getInt("radius", 500)
        val alarmActive = sharedPreferences.getBoolean("alarmActive", false)

        if (!alarmActive || targetLat == 0.0 && targetLng == 0.0) {
            return
        }

        val targetLocation = Location("DestinationLocation").apply {
            latitude = targetLat
            longitude = targetLng
        }

        val distanceToTarget = currentLocation.distanceTo(targetLocation)
        if (distanceToTarget <= targetRadius) {
            triggerAlarm()

            // Deactivate alarm
            sharedPreferences.edit().putBoolean("alarmActive", false).apply()

            // Stop service
            stopSelf()
        }
    }

    private fun triggerAlarm() {
        val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val useSound = sharedPreferences.getBoolean("useSound", true)
        val useVibration = sharedPreferences.getBoolean("useVibration", true)

        // Start AlarmReceiver to handle the alarm UI and actions
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("useSound", useSound)
            putExtra("useVibration", useVibration)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Location Alarm Service"
            val descriptionText = "Tracks location to trigger proximity alarms"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Commuter Alarm Active")
            .setContentText("We'll wake you up when you arrive")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}