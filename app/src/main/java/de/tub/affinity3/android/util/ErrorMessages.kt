package de.tub.affinity3.android.util

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes
import de.tub.affinity3.android.R

object ErrorMessages {
    fun getGeofenceErrorMessage(context: Context, e: Exception): String {
        val resources = context.resources
        return if (e is ApiException) getGeofenceErrorMessageByCode(context, e.statusCode)
        else resources.getString(R.string.geofence_unknown_geofence_error)
    }

    fun getGeofenceErrorMessageByCode(context: Context, errCode: Int): String {
        val resources = context.resources
        return when (errCode) {
            GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION -> resources.getString(R.string.geofence_insufficient_location_permission)
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(R.string.geofence_not_available)
            GeofenceStatusCodes.GEOFENCE_REQUEST_TOO_FREQUENT -> resources.getString(R.string.geofence_request_too_frequent)
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(R.string.geofence_too_many_geofences)
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(R.string.geofence_too_many_pending_intents)
            else -> resources.getString(R.string.geofence_unknown_geofence_error)
        }

    }
}