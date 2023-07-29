package de.tub.affinity3.android.classes.data

import com.google.gson.annotations.SerializedName

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
data class Coordinates(
    /**
     * Geographic coordinates associated with a latitude and a longitude.
     *
     * Coordinates are used for instance to define circular regions
     * (see ./app/src/main/java/de/tub/affinity3/android/classes/data/CircularRegion.kt)
     */
    @SerializedName("lat")
    val latitude: Double,
    @SerializedName("lng")
    val longitude: Double
)