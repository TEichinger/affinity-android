package de.tub.affinity3.android.repositories

class LocalGeofenceRepository {
    //List of Geofences current location belongs to
    private val currentGeofences = HashSet<String>()

    fun addGeofence(id: String) {
        currentGeofences.add(id)
    }

    fun removeGeofence(id: String) {
        currentGeofences.remove(id)
    }

    fun repoIsEmpty() : Boolean {
        return currentGeofences.isEmpty()
    }

    fun clearRepository() {
        currentGeofences.clear()
    }
}