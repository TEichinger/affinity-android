package de.tub.affinity3.android.classes.data

import com.google.gson.annotations.SerializedName

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
data class RequestPayload(
    /**
     *
     */
    @SerializedName("userId")
    val userId: String,
    @SerializedName("userName")
    val userName: String,
    @SerializedName("deviceName")
    val deviceName: String
)
