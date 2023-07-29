package de.tub.affinity3.android.classes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import de.tub.affinity3.android.AffinityApp
import timber.log.Timber


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val event = GeofencingEvent.fromIntent(intent).apply {
            if (hasError()) {
                return
            }
        }

        val application = context.applicationContext
        val bck = DiscoveryServiceManager()

        val triggeringGeofences = event.triggeringGeofences

        val localGeofenceRepository = (application as AffinityApp).getLocalGeofenceRepository()

        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            for (item in triggeringGeofences) {
                localGeofenceRepository.addGeofence(item.requestId)
            }
        } else if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            for (item in triggeringGeofences) {
                localGeofenceRepository.removeGeofence(item.requestId)
            }
        }

        if (localGeofenceRepository.repoIsEmpty()) {
            bck.insideOfSharingRegion(context, false)
            Timber.d("Stop sharing.")

        } else {
            bck.insideOfSharingRegion(context, true)
            Timber.d("Start sharing.")
        }

    }
}