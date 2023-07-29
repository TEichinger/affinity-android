@file:Suppress("ClassName")

package de.tub.affinity3.android.classes.sealed

// For AffinityExperimentDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityExperimentDatabase.kt)
sealed class ExperimentSingleEvent {
    data class SHOW_SNACKBAR(val text: String) : ExperimentSingleEvent()
    object NONE : ExperimentSingleEvent()
}
