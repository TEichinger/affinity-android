package de.tub.affinity3.android.classes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
@Entity
data class User(
    /**
     * What do "name", "lastSeen", and "deviceName" refer to?
     */
    @PrimaryKey var id: String,
    var name: String,
    var lastSeen: Date? = null,
    var deviceName: String? = null
)
