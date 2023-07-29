package de.tub.affinity3.android.classes.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
@Entity
data class CircularRegion(
    /**
     * A circular region used to define privacy regions. Privacy regions allow the user to
     * define regions only within which to automatically enable/disable the AdvertiseAndDiscovery
     * service. See [WHERE]
     *
     * A circular region is define by its center <coordinates> and its radius <radius>.
     */
    @PrimaryKey
    var id: String,

    @SerializedName("latlng")
    @Embedded var coordinates: Coordinates,

    @SerializedName("mRadius")
    var radius: Float
)
