package com.compass.mocklocation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.compass.mocklocation.R
import com.compass.mocklocation.service.MockService
import com.compass.mocklocation.utils.LocationCoordinate
import com.compass.mocklocation.utils.LocationUtils

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var edittextLongitude: EditText
    private lateinit var edittextLatitude: EditText
    private lateinit var buttonStartMock: Button
    private lateinit var buttonStopMock: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        edittextLongitude = findViewById(R.id.edittext_longitude)
        edittextLatitude = findViewById(R.id.edittext_latitude)
        buttonStartMock = findViewById<Button?>(R.id.button_start_mock).apply {
            setOnClickListener {
                var longitude = 0.0
                var latitude = 0.0
                val checkResult = try {
                    longitude = edittextLongitude.text.toString().toDouble()
                    latitude = edittextLatitude.text.toString().toDouble()
                    LocationCoordinate.checkIllegalPosition(latitude, longitude)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Please input illegal params", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, e.stackTraceToString())
                    false
                }
                Log.d(TAG, "Check position params: checkResult=$checkResult")
                if (checkResult) {
                    Toast.makeText(this@MainActivity, "start mock location", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "start mock location: latitude=$latitude, longitude=$longitude")
                    val loc = LocationUtils.gcj02ToWGS84(longitude, latitude)
                    val intent = Intent(this@MainActivity, MockService::class.java).apply {
                        putExtra(LocationCoordinate.LONGITUDE, loc[0])
                        putExtra(LocationCoordinate.LATITUDE, loc[1])
                    }
                    Log.d(TAG, "Ready to start service")
                    startService(intent)
                    Log.d(TAG, "Service already started")
                }
            }
        }
        buttonStopMock = findViewById<Button?>(R.id.button_stop_mock).apply {
            setOnClickListener {
                Toast.makeText(this@MainActivity, "stop mock location", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "stop mock location")
                val intent = Intent(this@MainActivity, MockService::class.java)
                stopService(intent)
            }
        }
    }
}