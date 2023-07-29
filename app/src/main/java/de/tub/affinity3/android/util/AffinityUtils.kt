package de.tub.affinity3.android.util

import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager

fun getDisplaySize(windowManager: WindowManager): Point {
    return try {
        val display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        Point(displayMetrics.widthPixels, displayMetrics.heightPixels)
    } catch (e: Exception) {
        e.printStackTrace()
        Point(0, 0)
    }
}


