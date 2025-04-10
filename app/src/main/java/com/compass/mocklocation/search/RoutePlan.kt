package com.compass.mocklocation.search

import android.content.Context
import android.util.Log
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.BusRouteResultV2
import com.amap.api.services.route.DriveRouteResultV2
import com.amap.api.services.route.RideRouteResultV2
import com.amap.api.services.route.RouteSearchV2
import com.amap.api.services.route.WalkRouteResultV2
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object RoutePlan {

    const val TAG = "RoutePlan"
    private var drivingRouteResult: DriveRouteResultV2? = null
    private lateinit var latch: CountDownLatch
    private var mRouteSearch: RouteSearchV2? = null
    private var isSearching = false

    fun initMap(context: Context) {
        mRouteSearch = RouteSearchV2(context).apply {
            setRouteSearchListener(object : RouteSearchV2.OnRouteSearchListener {
                override fun onDriveRouteSearched(p0: DriveRouteResultV2?, p1: Int) {
                    if (p0 == null) {
                        Log.d(TAG, "Fetch no data")
                    } else {
                        Log.d(TAG, "Fetch data")
                        p0.apply {
                            drivingRouteResult = p0
                        }
                    }
                    latch.countDown()
                }

                override fun onBusRouteSearched(p0: BusRouteResultV2?, p1: Int) {
                }

                override fun onWalkRouteSearched(p0: WalkRouteResultV2?, p1: Int) {
                }

                override fun onRideRouteSearched(p0: RideRouteResultV2?, p1: Int) {
                }
            })
        }
    }

    fun routePlanSearch(
        startLatLng: LatLonPoint,
        endLatLng: LatLonPoint,
        strategy: RouteSearchV2.DrivingStrategy = RouteSearchV2.DrivingStrategy.DEFAULT,
        wayList: MutableList<LatLonPoint>? = null,
        avoidRoad: String = ""
    ) : DriveRouteResultV2? {
        latch = CountDownLatch(1)
        if (!isSearching) {
            mRouteSearch?.calculateDriveRouteAsyn(
                RouteSearchV2.DriveRouteQuery(
                    RouteSearchV2.FromAndTo(startLatLng, endLatLng),
                    strategy,
                    wayList,
                    null,
                    avoidRoad).apply { showFields = RouteSearchV2.ShowFields.POLINE }
            )
        }
        Log.d(TAG, "Wait for result")
        latch.await(30, TimeUnit.SECONDS)
        return drivingRouteResult
    }
}