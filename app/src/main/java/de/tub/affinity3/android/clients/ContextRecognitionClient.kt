package de.tub.affinity3.android.clients

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.nearby.connection.Strategy
import de.tub.affinity3.android.util.SingletonHolder
import timber.log.Timber

class ContextRecognitionClient(val context: Context) {
    /**
     * ContextRecognitionClient is an OPTIONAL client that keeps track of the device user's context.
     * It is for instance used to reduce battery drain, that is the AdvertiseAndDiscovery service
     * is enabled/disabled automatically on the basis of the device user's current context
     * (see WHERE).
     *
     * This client listens on activity updates as per Google's ActivityRecognitionClient
     * (see https://developers.google.com/android/reference/com/google/android/gms/location/ActivityRecognition)
     * Listening can be activated via the <requestActivityUpdates> method and deactivated
     * via the <removeActivityUpdates> method.
     *
     * ActivityUpdates are requested every 30000 milliseconds.
     *
     * The <currentActivity> is only updated, if the confidence for the most probable activity
     * exceeds a value of 60.
     *
     */

    var currentActivity: DetectedActivity? = null
        private set



    fun requestActivityUpdates(pendingIntent: PendingIntent) {
        Timber.d("Try to request Activity Updates")
        ActivityRecognition.getClient(context)
            .requestActivityUpdates(updateInterval, pendingIntent)//request updates every 30000 milliseconds
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Timber.d("Sucessfully started listening for activity updates")
                } else {
                    Timber.e(it.exception)
                }
            }
    }

    fun removeActivityUpdates(pendingIntent: PendingIntent) {
        ActivityRecognition.getClient(context)
            .removeActivityUpdates(pendingIntent)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Timber.d("Sucessfully stopped listening for activity updates")
                } else {
                    Timber.e(it.exception)
                }
            }
    }

    fun handleActivityUpdateIntent(intent: Intent) {
        Timber.d("handleActivityUpdateIntent intent: %s", intent.toString())
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            var mostProbableActivity = result!!.probableActivities.first()
            for (event in result.probableActivities) { // Do something useful here...
                Timber.d("activity  %s", event.toString())
                if (event.confidence >= mostProbableActivity.confidence) {
                    mostProbableActivity = event
                }
            }
            if (mostProbableActivity.confidence > confidenceThreshold) {
                currentActivity = mostProbableActivity
            }
        }
    }


    companion object {
        val updateInterval: Long = 30000
        val confidenceThreshold: Byte = 60
    }
}
