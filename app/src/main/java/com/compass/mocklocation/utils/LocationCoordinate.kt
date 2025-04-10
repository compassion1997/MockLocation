package com.compass.mocklocation.utils

import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.amap.api.maps.model.LatLng
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationCoordinate(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 1f,
    val speed: Float = 0f,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {

    companion object {
        private const val TAG = "LocationCoordinate"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val START_LATITUDE = "start_latitude"
        const val START_LONGITUDE = "start_longitude"
        const val END_LATITUDE = "end_latitude"
        const val END_LONGITUDE = "end_longitude"
        private const val ACCURACY = "accuracy"
        const val SPEED = "speed"
        private const val ALTITUDE = "altitude"

        fun intentToLocationCoordinate(intent: Intent): LocationCoordinate? {
            try {
                val latitude = intent.getFloatExtra(LATITUDE, 0.0f).toDouble()
                val longitude = intent.getFloatExtra(LONGITUDE, 0.0f).toDouble()
                Log.d(TAG, "In intentToLocationCoordinate: $LONGITUDE=$longitude, $LATITUDE=$latitude")
                return LocationCoordinate(
                    latitude, longitude,
                    intent.getFloatExtra(ACCURACY, 10f),
                    intent.getFloatExtra(SPEED, 0f),
                    intent.getDoubleExtra(ALTITUDE, 0.0),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Translate intent params fail!")
                return null
            }
        }

        fun paramsToLocationCoordinate(latitude: Double, longitude: Double, speed: Float = 0f): LocationCoordinate {
            return LocationCoordinate(
                latitude, longitude,
                1f,
                speed / 3.6f,
                0.0
            )
        }

        fun checkIllegalPosition(latitude: Double, longitude: Double) : Boolean{
            Log.d(TAG, "Check position: latitude=$latitude, longitude=$longitude")
            return (latitude == 0.0 && longitude == 0.0) || latitude >= -90 || latitude <= 90 || longitude >= -180 || longitude <= 180
        }

        fun splitPolyline(base: MutableList<LatLonPointWithDistance>): MutableList<LatLonPointWithDistance> {
            val tempLatLng = mutableListOf<LatLonPointWithDistance>()
            if (base.isNotEmpty()) {
                base.zipWithNext().forEach { (first, second) ->
                    val splitCount = (first.distance / 1).toInt()
                    val splitDistance = first.distance / (splitCount + 1)
                    val splitLat = (second.latLng.latitude - first.latLng.latitude) / splitCount
                    val splitLon = (second.latLng.longitude - first.latLng.longitude) / splitCount
                    for (i in 0..splitCount) {
                        try {
                            val tempLat = (first.latLng.latitude + ( splitLat * i )).roundToDecimal()
                            val tempLon = (first.latLng.longitude + ( splitLon * i )).roundToDecimal()
                            tempLatLng.add(LatLonPointWithDistance(LatLng(tempLat, tempLon), splitDistance))
                        } catch (e: Exception) {
                            Log.d(TAG, e.stackTraceToString())
                        }
                    }
                }
                tempLatLng.add(base.last())
            }
            return tempLatLng
        }
    }

    fun toLocation(provider: String = LocationManager.GPS_PROVIDER): Location {
        return Location(provider).apply {
            latitude = this@LocationCoordinate.latitude
            longitude = this@LocationCoordinate.longitude
            accuracy = this@LocationCoordinate.accuracy
            speed = this@LocationCoordinate.speed
            altitude = this@LocationCoordinate.altitude
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }
}

data class LatLonPointWithDistance(
    val latLng: LatLng,
    val distance: Float,
)

fun Double.roundToDecimal(): Double {
    return BigDecimal(this).setScale(6, RoundingMode.HALF_UP).toDouble()
}