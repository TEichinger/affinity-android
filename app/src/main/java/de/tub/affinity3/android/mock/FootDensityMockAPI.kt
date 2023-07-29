package de.tub.affinity3.android.mock

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.random.Random

class FootDensityMockAPI {

    private fun getRandomPointInCircle(center: LatLng, radiusMeters: Double): LatLng {
        val heading = Random.nextDouble(360.0)

        var distance = 0.0
        for (i in 1..4) {
            distance += Random.nextDouble(radiusMeters * Random.nextDouble(0.25))
        }

        return SphericalUtil.computeOffset(center, distance, heading)
    }

    fun getFootDensity(
        center: LatLng,
        radiusMeters: Double,
        densityRegions: Int = 5,
        densityRegionRadiusMeters: Double = 250.0,
        densityRegionPointsAmount: Int = 200,
        noisePointsAmount: Int = 1000
    ): MutableList<LatLng> {

        val heatMap = mutableListOf<LatLng>()

        for (i in 1..densityRegions) {
            val randomPoint = getRandomPointInCircle(center, radiusMeters)

            heatMap.add(randomPoint)

            for (j in 1..densityRegionPointsAmount) {
                heatMap.add(
                    getRandomPointInCircle(randomPoint, densityRegionRadiusMeters)
                )
            }
        }

        for (i in 1..noisePointsAmount) {
            heatMap.add(
                getRandomPointInCircle(center, radiusMeters)
            )
        }

        return heatMap
    }
}