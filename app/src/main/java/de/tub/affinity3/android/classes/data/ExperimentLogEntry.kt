package de.tub.affinity3.android.classes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

// Part of AffinityExperimentDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityExperimentDatabase.kt)
@Entity(tableName = "experimentlogs")
data class ExperimentLogEntry(
    val experimentName: String = "",
    @PrimaryKey val timestamp: Long,
    val curerentlyConnectedClients: String = "",
    val batteryLevel: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val currentActivity: String = "",
    val isRunningDiscovery: Boolean = false
) : Serializable
