package com.compass.mocklocation.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import com.compass.mocklocation.R
import com.compass.mocklocation.search.RoutePlan
import com.compass.mocklocation.utils.LatLonPointWithDistance
import com.compass.mocklocation.utils.LocationCoordinate
import com.compass.mocklocation.utils.LocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MockService : Service() {

    companion object {
        private const val TAG = "MockService"
        private const val DURATION = 1000L
        private const val CHANNEL_ID = "mock_service_channel"
        const val ACTION_START_MOCK_LOCATION = "com.ivimock.ACTION_START_MOCK_LOCATION"
        const val ACTION_STOP_MOCK_LOCATION = "com.ivimock.ACTION_STOP_MOCK_LOCATION"
        const val ACTION_START_MOCK_NAVIGATION = "com.ivimock.ACTION_START_MOCK_NAVIGATION"
        const val ACTION_STOP_MOCK_NAVIGATION = "com.ivimock.ACTION_STOP_MOCK_NAVIGATION"
    }

    private lateinit var locationManager: LocationManager
    private var gpsProvider = LocationManager.GPS_PROVIDER
    private var networkProvider = LocationManager.NETWORK_PROVIDER
    private var scope: CoroutineScope? = null
    private var mSpeed = 40f

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "Notification mock location", NotificationManager.IMPORTANCE_DEFAULT
        ))
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IVIMock")
            .setContentText("Mocking Location...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.osiris)
            .build()
        startForeground(1201, notification)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Init location manager")
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        RoutePlan.initMap(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            stopMockCommand()
            initLocationTestProvider()
            when (intent.action) {
                ACTION_START_MOCK_LOCATION -> {
                    Log.d(TAG, "Start mock location")
                    val lng = intent.getFloatExtra(LocationCoordinate.LONGITUDE, 0.0f).toDouble()
                    val lat = intent.getFloatExtra(LocationCoordinate.LATITUDE, 0.0f).toDouble()
                    if (!LocationCoordinate.checkIllegalPosition(lat, lng)) {
                        throw IllegalArgumentException("Please input illegal position")
                    }
                    scope = CoroutineScope(Dispatchers.Default).apply {
                        launch {
                            while (true) {
                                startMockLocation(LocationCoordinate.paramsToLocationCoordinate(lat, lng))
                                delay(DURATION)
                            }
                        }
                    }
                }

                ACTION_START_MOCK_NAVIGATION -> {
                    Log.d(TAG, "Start mock navigation")
                    val startLat = intent.getFloatExtra(LocationCoordinate.START_LATITUDE, 0.0f).toDouble()
                    val startLng = intent.getFloatExtra(LocationCoordinate.START_LONGITUDE, 0.0f).toDouble()
                    val endLat = intent.getFloatExtra(LocationCoordinate.END_LATITUDE, 0.0f).toDouble()
                    val endLng = intent.getFloatExtra(LocationCoordinate.END_LONGITUDE, 0.0f).toDouble()
                    mSpeed = intent.getFloatExtra(LocationCoordinate.SPEED, mSpeed)
                    if (!(LocationCoordinate.checkIllegalPosition(startLat, startLng)
                                && LocationCoordinate.checkIllegalPosition(endLat, endLng))){
                        throw IllegalArgumentException("Please input illegal position")
                    }
                    scope = CoroutineScope(Dispatchers.Default).also { s ->
                        s.launch {
                            val drivingRouteResult = RoutePlan.routePlanSearch(LatLonPoint(startLat, startLng), LatLonPoint(endLat, endLng))
                            drivingRouteResult?.paths?.apply {
                                Log.d(TAG, "Fetch path count: $size")
                                if (isNotEmpty()) {
                                    val wayPointWithDistance = mutableListOf<LatLonPointWithDistance>()
                                    mutableListOf<LatLonPoint>().also {
                                        for (step in get(0).steps) {
                                            for (p in step.polyline.dropLast(1)) {
                                                LocationUtils.gcj02ToWGS84(p.longitude, p.latitude).also { loc ->
                                                    it.add(LatLonPoint(loc[1], loc[0]))
                                                }
                                            }
                                        }
                                    }.zipWithNext().forEach{ (current, next) ->
                                        wayPointWithDistance.add(
                                            LatLonPointWithDistance(
                                                LatLng(current.latitude, current.longitude),
                                                AMapUtils.calculateLineDistance(
                                                    LatLng(current.latitude, current.longitude),
                                                    LatLng(next.latitude, next.longitude)
                                                )
                                            )
                                        )
                                    }
                                    LocationCoordinate.splitPolyline(wayPointWithDistance).forEach {
                                        startMockLocation(it)
                                        (it.distance / (mSpeed / 3.6) * 1000).toLong().also { dur ->
                                            delay(dur)
                                            Log.d(TAG, "Longitude: ${it.latLng.longitude}, latitude: ${it.latLng.latitude}, current speed $mSpeed km/s, distance ${it.distance} meter(s), cost time $dur ms")
                                        }
                                    }
                                    Log.d(TAG, "Mock navigation complete")
                                }
                            }
                        }
                    }
                }

                ACTION_STOP_MOCK_LOCATION -> {
                    stopMockCommand()
                    Log.d(TAG, "Stop mock location")
                }

                ACTION_STOP_MOCK_NAVIGATION -> {
                    stopMockCommand()
                    Log.d(TAG, "Stop mock navigation")
                }
            }
        }
        return START_STICKY
    }

    private fun removeLocationTestProvider() {
        locationManager.apply{
            try {
                setTestProviderEnabled(gpsProvider, false)
                setTestProviderEnabled(networkProvider, false)
                locationManager.removeTestProvider(gpsProvider)
                locationManager.removeTestProvider(networkProvider)
            } catch (e: Exception) {
                Log.d(TAG, e.stackTraceToString())
            }
        }
    }

    private fun initLocationTestProvider() {
        locationManager.apply {
            addTestProvider(
                gpsProvider, false, true, true,
                true, true, true, true,
                ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE
            )
            addTestProvider(
                networkProvider, true, false, true,
                true, true, true, true,
                ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE
            )
            setTestProviderEnabled(gpsProvider, true)
            setTestProviderEnabled(networkProvider, true)
        }
    }

    private fun startMockLocation(locationCoordinate: LocationCoordinate) {
        locationManager.setTestProviderLocation(
            gpsProvider,
            locationCoordinate.toLocation(gpsProvider)
        )
        locationManager.setTestProviderLocation(
            networkProvider,
            locationCoordinate.toLocation(networkProvider)
        )
    }

    private fun startMockLocation(latLonPointWithDistance: LatLonPointWithDistance) {
        val locationCoordinate = LocationCoordinate.paramsToLocationCoordinate(
            latLonPointWithDistance.latLng.latitude,
            latLonPointWithDistance.latLng.longitude,
            mSpeed
        )
        try {
            locationManager.setTestProviderLocation(
                gpsProvider,
                locationCoordinate.toLocation(gpsProvider)
            )
            locationManager.setTestProviderLocation(
                networkProvider,
                locationCoordinate.toLocation(networkProvider)
            )
        } catch (e: Exception) {
            Log.d(TAG, e.stackTraceToString())
        }
    }

    private fun stopMockCommand() {
        scope?.cancel()
        removeLocationTestProvider()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMockCommand()
    }
}