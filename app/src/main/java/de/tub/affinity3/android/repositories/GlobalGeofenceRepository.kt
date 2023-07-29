package de.tub.affinity3.android.repositories

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import de.tub.affinity3.android.classes.GeofenceBroadcastReceiver
import de.tub.affinity3.android.classes.data.CircularRegion
import de.tub.affinity3.android.util.ErrorMessages

class GlobalGeofenceRepository(private val context: Context) {
    val geofencingClient = LocationServices.getGeofencingClient(context)
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun addGeofence(
        circularRegion: CircularRegion,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(circularRegion.id)
            .setCircularRegion(
                circularRegion.coordinates.latitude,
                circularRegion.coordinates.longitude,
                circularRegion.radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        if (
            geofence != null
            &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient
                .addGeofences(
                    GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT) //EXIT needed in case of Geofence update
                        .addGeofences(
                            listOf(geofence)
                        )
                        .build(),
                    geofencePendingIntent
                )
                .addOnSuccessListener { success() }
                .addOnFailureListener {
                    failure(
                        ErrorMessages.getGeofenceErrorMessage(context, it)
                    )
                }
        }
    }

    fun removeGeofence(
        circularRegion: CircularRegion,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        geofencingClient
            .removeGeofences(
                listOf(circularRegion.id)
            )
            .addOnSuccessListener { success() }
            .addOnFailureListener {
                failure(
                    ErrorMessages.getGeofenceErrorMessage(context, it)
                )
            }
    }

    fun updateGeofence(
        circularRegion: CircularRegion,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        removeGeofence(circularRegion, success, failure)
        addGeofence(circularRegion, success, failure)
    }

}