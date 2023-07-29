package de.tub.affinity3.android.classes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.preference.PreferenceManager
import de.tub.affinity3.android.AffinityApp
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService.Companion.EXTRA_CONFIG
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService.Companion.EXTRA_NAME
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService.Companion.EXTRA_SHARING_REGION
import timber.log.Timber

class DiscoveryServiceManager {

    /**
     * launching the service
     */
    fun startDiscoveryService(
        context: Context,
        serviceType: AdvertiseAndDiscoveryService.ServiceType,
        experimentName: String? = null
    ) {
        val serviceIntent = Intent(context, AdvertiseAndDiscoveryService::class.java).apply {
            putExtra(EXTRA_NAME, experimentName ?: "")
            putExtra(EXTRA_CONFIG, serviceType)
        }

        // depending on the version of Android we either launch the simple service (version < O)
        // or we start a foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        Timber.d("Advertisement and Discovery service started!")

        val globalGeofenceRepository =
            (context.applicationContext as AffinityApp).getGlobalGeofenceRepository()

        val circularRegionRepository =
            (context.applicationContext as AffinityApp).getCircularRegionRepository()

        circularRegionRepository.findAll().forEach { circularRegion ->
            globalGeofenceRepository.addGeofence(
                circularRegion,
                success = {},
                failure = {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun stopDiscoveryService(context: Context) {
        context.startService(
            Intent(context, AdvertiseAndDiscoveryService::class.java).apply {
                action = AdvertiseAndDiscoveryService.ACTION_STOP
            }
        )

        Timber.d("Advertisement and Discovery service stopped!")

        val globalGeofenceRepository =
            (context.applicationContext as AffinityApp).getGlobalGeofenceRepository()

        val circularRegionRepository =
            (context.applicationContext as AffinityApp).getCircularRegionRepository()

        circularRegionRepository.findAll().forEach { circularRegion ->
            globalGeofenceRepository.removeGeofence(
                circularRegion,
                success = {},
                failure = {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun insideOfSharingRegion(
            context: Context,
            isInside: Boolean
    ) {
        val myPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isServiceRunning = myPrefs.getBoolean(
                "pref_is_service_running",
                false
        )
        if (isServiceRunning) {
            val serviceIntent = Intent(context, AdvertiseAndDiscoveryService::class.java).apply {
                putExtra(EXTRA_SHARING_REGION, isInside)
            }

            // depending on the version of Android we either launch the simple service (version < O)
            // or we start a foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

    }

}
