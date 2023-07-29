package de.tub.affinity3.android.classes

import android.webkit.JavascriptInterface
import android.widget.Toast
import com.google.gson.Gson
import de.tub.affinity3.android.AffinityApp
import de.tub.affinity3.android.activities.PrivacyRegionsActivity
import de.tub.affinity3.android.classes.data.CircularRegion
import de.tub.affinity3.android.classes.data.Coordinates
import timber.log.Timber
import java.util.*


@Suppress("unused")
class LeafletWebInterface(private val context: PrivacyRegionsActivity) {

    private val application: AffinityApp = context.application as AffinityApp
    private val gson = Gson()
    private val globalGeofenceRepository = application.getGlobalGeofenceRepository()
    private val localGeofenceRepository = application.getLocalGeofenceRepository()
    private val circularRegionRepository = context.circularRegionRepository

    // TODO: Unify LatLng classes
    var footDensityMap: MutableList<com.google.android.gms.maps.model.LatLng> = mutableListOf()
    var recommendationRegions: MutableList<CircularRegion> = mutableListOf()
    var userCurrentLocationJson: String? = null

    @JavascriptInterface
    fun onCircleGeofenceCreated(lat: Double, lng: Double, radiusMeters: Float): String {
        val circleGeofenceId = UUID.randomUUID().toString()

        val newCircularRegion = CircularRegion(
            id = circleGeofenceId,
            coordinates = Coordinates(lat, lng),
            radius = radiusMeters
        )

        val createdCircularRegion = circularRegionRepository.addCircularRegion(newCircularRegion)

        globalGeofenceRepository.addGeofence(
            createdCircularRegion,
            success = {},
            failure = {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        )

        Timber.d("Circle geofence created (${gson.toJson(createdCircularRegion)})")

        return circleGeofenceId
    }

    @JavascriptInterface
    fun onCircleGeofenceUpdated(
        id: String,
        newLat: Double,
        newLng: Double,
        newRadiusMeters: Float
    ) {

        val circularRegion = circularRegionRepository.findCircularRegionById(id)

        circularRegion.apply {
            coordinates = Coordinates(newLat, newLng)
            radius = newRadiusMeters
        }

        val updatedCircularRegion = circularRegionRepository.updateCircularRegion(circularRegion)

        globalGeofenceRepository.updateGeofence(
            circularRegion,
            success = {},
            failure = {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        )

        Timber.d("Circle geofence updated (${gson.toJson(updatedCircularRegion)})")

    }

    @JavascriptInterface
    fun onCircleGeofenceDeleted(id: String) {

        val circularRegion = circularRegionRepository.findCircularRegionById(id)

        circularRegionRepository.deleteCircularRegion(circularRegion)

        globalGeofenceRepository.removeGeofence(
            circularRegion,
            success = {},
            failure = {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        )

        localGeofenceRepository.removeGeofence(id)

        if (localGeofenceRepository.repoIsEmpty()) {
            DiscoveryServiceManager().insideOfSharingRegion(context, false)
        }

        Timber.d("Circle geofence deleted (${gson.toJson(circularRegion)})")

    }

    @JavascriptInterface
    fun getUserCurrentLocationJSON(): String? {
        return userCurrentLocationJson
    }

    @JavascriptInterface
    fun getGeofencesJSON(): String {
        return gson.toJson(circularRegionRepository.findAll())
    }

    @JavascriptInterface
    fun getHeatMapJSON(): String {
        return gson.toJson(footDensityMap)
    }

    @JavascriptInterface
    fun getRecommendationsEnabled(): Int {
        return if (context.recommendationsEnabled) {
            1
        } else {
            0
        }
    }

    @JavascriptInterface
    fun finishActivity() {
        context.finish()
    }

    @JavascriptInterface
    fun getRecommendationsJSON(): String {
        return gson.toJson(recommendationRegions)
    }
}