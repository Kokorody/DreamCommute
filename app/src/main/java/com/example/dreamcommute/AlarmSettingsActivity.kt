package com.example.dreamcommute

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlarmSettingsActivity : AppCompatActivity() {

    private lateinit var checkBoxSound: CheckBox
    private lateinit var checkBoxVibration: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_settings)

        checkBoxSound = findViewById(R.id.checkBoxSound)
        checkBoxVibration = findViewById(R.id.checkBoxVibration)

        // Load saved preferences
        val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        checkBoxSound.isChecked = sharedPreferences.getBoolean("useSound", true)
        checkBoxVibration.isChecked = sharedPreferences.getBoolean("useVibration", true)

        // Set up save button
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            // Save settings
            sharedPreferences.edit().apply {
                putBoolean("useSound", checkBoxSound.isChecked)
                putBoolean("useVibration", checkBoxVibration.isChecked)
                apply()
            }

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set up cancel button
        findViewById<Button>(R.id.btnCancelSettings).setOnClickListener {
            finish()
        }
    }
}
