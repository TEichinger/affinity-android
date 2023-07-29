package de.tub.affinity3.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.startsWith(manufacturer)) {
        model.capitalize()
    } else {
        manufacturer.capitalize() + " " + model
    }
}

@SuppressLint("HardwareIds")
fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}
