package de.tub.affinity3.android.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import de.tub.affinity3.android.AffinityApp
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.LatLng
import de.tub.affinity3.android.classes.LeafletWebInterface
import de.tub.affinity3.android.classes.data.CircularRegion
import de.tub.affinity3.android.classes.data.Coordinates
import de.tub.affinity3.android.constants.AppConstants
import de.tub.affinity3.android.mock.FootDensityMockAPI
import de.tub.affinity3.android.repositories.CircularRegionRepository
import de.tub.affinity3.android.toClusterable
import kotlinx.android.synthetic.main.activity_privacy_regions.*
import org.apache.commons.math3.ml.clustering.Cluster
import org.apache.commons.math3.ml.clustering.Clusterable
import org.apache.commons.math3.ml.clustering.DBSCANClusterer
import java.util.*

class PrivacyRegionsActivity : AppCompatActivity() {

    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var circularRegionRepository: CircularRegionRepository

    var recommendationsEnabled: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_privacy_regions)

        recommendationsEnabled = intent.getBooleanExtra(
            AppConstants.RECOMMENDATIONS_FLAG_EXTRA,
            false
        )

        val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION
        val permissionGranted = PackageManager.PERMISSION_GRANTED

        val footDensityMockAPI = FootDensityMockAPI()
        val gson = Gson()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        circularRegionRepository = (application as AffinityApp).getCircularRegionRepository()

        val leafletWebInterface = LeafletWebInterface(this)

        val locationRequest = LocationRequest.create().apply {
            interval = AppConstants.USER_LOCATION_UPDATE_INTERVAL
            fastestInterval = AppConstants.USER_LOCATION_UPDATE_SHORTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location == null) {
                        continue
                    }

                    // this ensures that footDensityMap is initialized only once
                    // when the user's current location was successfully detected
                    if (leafletWebInterface.userCurrentLocationJson == null && recommendationsEnabled) {
                        val footDensityMap = footDensityMockAPI.getFootDensity(
                            // TODO: Unify LatLng classes
                            com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude),
                            AppConstants.REGION_OF_INTEREST_RADIUS_METERS
                        )

                        val recommendationClusters = DBSCANClusterer<Clusterable>(
                            AppConstants.DBSCAN_EPS,
                            AppConstants.DBSCAN_MIN_POINTS
                        ).cluster(
                            footDensityMap.map {
                                it.toClusterable()
                            }
                        )

                        leafletWebInterface.footDensityMap = footDensityMap
                        leafletWebInterface.recommendationRegions =
                            getRecommendationRegions(recommendationClusters)
                    }

                    leafletWebInterface.userCurrentLocationJson = gson.toJson(
                        LatLng(location.latitude, location.longitude)
                    )

                    break
                }
            }
        }

        val storedGeoFences = circularRegionRepository.findAll()

        val fencesQueryParameter =
            "${AppConstants.FENCES_QUERY_PARAM}=${gson.toJson(storedGeoFences)}"

        // Setup WebView that hosts Leaflet map
        pra_leaflet_map_wv.settings.apply {
            domStorageEnabled = true
            loadsImagesAutomatically = true
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            databaseEnabled = true
            domStorageEnabled = true
            setAppCacheEnabled(true)
            setAppCacheEnabled(true)
            setSupportZoom(false)
        }

        pra_leaflet_map_wv.run {
            addJavascriptInterface(
                leafletWebInterface,
                AppConstants.LEAFLET_WEB_INTERFACE
            )

            loadUrl("${AppConstants.LEAFLET_HTML_PATH}?$fencesQueryParameter")
        }

        // Use fusedLocationClient to dynamically update user's location on the Leaflet map
        if (
            ActivityCompat.checkSelfPermission(this, fineLocation) == permissionGranted
            &&
            ActivityCompat.checkSelfPermission(this, coarseLocation) == permissionGranted
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }

    }

    private fun getRecommendationRegions(
        recommendationClusters: List<Cluster<Clusterable>>
    ): MutableList<CircularRegion> {

        val geofences = circularRegionRepository.findAll()

        val recommendationRegions = mutableListOf<CircularRegion>()

        recommendationClusters.forEach recommendationClustersForEach@{ cluster ->
            val clusterSize = cluster.points.size

            var tmpDistance = 0.0
            var tmpLatSum = 0.0
            var tmpLngSum = 0.0

            cluster.points.forEach { pointClusterable ->
                tmpLatSum += pointClusterable.point[0]
                tmpLngSum += pointClusterable.point[1]
            }

            // circleCenter is the mean of all points within one cluster
            val circleCenter = com.google.android.gms.maps.model.LatLng(
                tmpLatSum / clusterSize,
                tmpLngSum / clusterSize,
            )

            cluster.points.forEach { pointClusterable ->
                val currentPoint = com.google.android.gms.maps.model.LatLng(
                    pointClusterable.point[0],
                    pointClusterable.point[1]
                )

                tmpDistance += SphericalUtil.computeDistanceBetween(currentPoint, circleCenter)
            }

            // circleRadiusMeters is the mean distance of all points from circleCenter
            // scaled by the factor 1.5
            val circleRadiusMeters = tmpDistance * 1.5 / clusterSize

            // skip recommendation circle if its center is inside of existent geofence
            geofences.forEach geofencesForEach@{
                val distanceToGeofenceCenterMeters = SphericalUtil.computeDistanceBetween(
                    circleCenter,
                    com.google.android.gms.maps.model.LatLng(
                        it.coordinates.latitude,
                        it.coordinates.longitude
                    )
                )

                // skip
                if (distanceToGeofenceCenterMeters <= it.radius) {
                    return@recommendationClustersForEach
                }
            }

            val circularRegion = CircularRegion(
                id = UUID.randomUUID().toString(),
                coordinates = Coordinates(circleCenter.latitude, circleCenter.longitude),
                radius = circleRadiusMeters.toFloat()
            )

            recommendationRegions.add(circularRegion)
        }

        return recommendationRegions
    }
}
