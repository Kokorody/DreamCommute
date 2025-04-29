package com.example.dreamcommute

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.content.SharedPreferences

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private var alarmRadius: Int = 500 // Default 500m
    private lateinit var radiusText: TextView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)

        // Get the SupportMapFragment and request notification when map is ready
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up radius slider
        radiusText = findViewById(R.id.tvRadius)
        val radiusSeekBar = findViewById<SeekBar>(R.id.seekBarRadius)
        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Convert progress to radius between 100m and 2000m
                alarmRadius = 100 + (progress * 19)
                radiusText.text = "Alarm Radius: ${alarmRadius}m"

                // Update circle on map if location is selected
                updateMapCircle()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set up set alarm button
        findViewById<Button>(R.id.btnSetAlarm).setOnClickListener {
            if (selectedLocation != null) {
                // Save location and radius to shared preferences
                sharedPreferences.edit().apply {
                    putFloat("lat", selectedLocation!!.latitude.toFloat())
                    putFloat("lng", selectedLocation!!.longitude.toFloat())
                    putInt("radius", alarmRadius)
                    putBoolean("alarmActive", true)
                    apply()
                }

                // Start location service
                val serviceIntent = Intent(this, LocationService::class.java)
                startService(serviceIntent)

                // Notify user
                val message = "Alarm set! We'll wake you up when you're ${alarmRadius}m from your destination."
                android.app.AlertDialog.Builder(this)
                    .setTitle("Alarm Set")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .show()
            } else {
                android.app.AlertDialog.Builder(this)
                    .setTitle("No Destination Selected")
                    .setMessage("Please tap on the map to select your destination")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Default location (user's current location would be better in a real app)
        val defaultLocation = LatLng(37.7749, -122.4194) // San Francisco
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14f))

        // Set click listener for the map
        mMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            updateMapCircle()
        }
    }

    private fun updateMapCircle() {
        mMap.clear()
        selectedLocation?.let { location ->
            // Add marker at selected location
            mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Destination")
            )

            // Add circle with the selected radius
            mMap.addCircle(
                CircleOptions()
                    .center(location)
                    .radius(alarmRadius.toDouble())
                    .strokeWidth(2f)
                    .strokeColor(getColor(R.color.design_default_color_primary))
                    .fillColor(getColor(R.color.design_default_color_primary_dark) and 0x33FFFFFF)
            )
        }
    }
}