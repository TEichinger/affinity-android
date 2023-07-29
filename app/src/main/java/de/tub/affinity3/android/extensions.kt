package de.tub.affinity3.android

import com.google.android.gms.maps.model.LatLng
import org.apache.commons.math3.ml.clustering.Clusterable

fun LatLng.toClusterable() = Clusterable {
    DoubleArray(2) { id ->
        when (id) {
            0 -> latitude
            else -> longitude
        }
    }
}