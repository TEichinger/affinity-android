package de.tub.affinity3.android.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.DetectedActivity

fun Fragment.hasPermissions(permissions: Array<String>): Boolean {
    return hasPermissions(requireContext(), permissions)
}

fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }
    return true
}

val Fragment.packageName: String
    get() = requireContext().packageName

var View.visible: Boolean
    get() = visibility == View.VISIBLE
    set(v) {
        visibility = if (v) View.VISIBLE else View.INVISIBLE
    }

var View.gone: Boolean
    get() = visibility == View.VISIBLE
    set(v) {
        visibility = if (v) View.GONE else View.VISIBLE
    }

fun Activity.hideKeyboard() {
    hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
}

fun Context.hideKeyboard(view: View?) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
}

fun Activity.showSoftKeyboard(view: View) {
    if (view.requestFocus()) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun Context.toast(message: CharSequence, centered: Boolean = false) {
    val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
    if (centered) {
        toast.setGravity(Gravity.CENTER, 0, 0)
    }
    toast.show()
}

fun DetectedActivity.typeString(): String {
    return when (this.type) {
        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
        DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
        DetectedActivity.ON_FOOT -> "ON_FOOT"
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.TILTING -> "TILTING"
        DetectedActivity.WALKING -> "WALKING"
        DetectedActivity.RUNNING -> "RUNNING"
        else -> "UNKNOWN"
    }
}
