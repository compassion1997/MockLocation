package com.compass.mocklocation.utils

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.compass.mocklocation.R

class FloatWindow{

    companion object {
        private const val TAG = "FloatWindow"
    }

    private lateinit var windowManager: WindowManager
    private var floatView: View? = null
    private lateinit var floatParams: WindowManager.LayoutParams
    private var isShow = false

    fun isShowFloatWindow() : Boolean{
        return isShow
    }

    fun show(context: Context) {
        Log.d(TAG, "Window manager add float Window")
        windowManager = context.getSystemService(WindowManager::class.java)
        floatView = LayoutInflater.from(context).inflate(R.layout.layout_float_window, null)
        floatParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.START or Gravity.TOP
            x = 0
            y = 100
        }
        windowManager.addView(floatView, floatParams)
        isShow = true
    }

    fun remove(){
        try {
            if(isShow) {
                Log.d(TAG, "Float Window has been removed")
                windowManager.removeView(floatView)
                floatView = null
                isShow = false
            }
        } catch (e: Exception) {
            Log.d(TAG, e.stackTraceToString())
        }
    }
}