package com.compass.mocklocation

import android.app.Application
import android.util.Log
import com.amap.api.services.core.ServiceSettings

class MyApp: Application() {

    companion object {
        private const val TAG = "MyApp"
    }

    override fun onCreate() {
        Log.d(TAG, "MyApp start, privacy init!")
        super.onCreate()
        ServiceSettings.updatePrivacyShow(this, true, true)
        ServiceSettings.updatePrivacyAgree(this, true)
    }
}