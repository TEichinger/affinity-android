package de.tub.affinity3.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.tub.affinity3.android.persistence.daos.ExperimentLogEntryDao
import de.tub.affinity3.android.classes.data.ExperimentLogEntry

@Database(
    entities = [ExperimentLogEntry::class],
    version = 1
)
abstract class AffinityExperimentDatabase : RoomDatabase() {
    /**
     * The <AffinityExperimentDatabase> database is exclusively used for experimental purposes.
     * It is purely a developer feature to measure battery drain due to the AdvertiseAndDiscovery service.
     * It does not (!) provide the core functionality.
     *
     * More specifically, it persists battery drain statistics of experiments.
     * Experiments can be accessed in the app by enabling the experiments menu via the
     *          Settings > Show experiments menu > ENABLE
     * The experiments menu is then accessible as a fourth sub-menu point in the bottom tab
     * in the main menu.
     *
     * Experiments run over 24 hours. Configurations include:
     *      * Use continuous AdvertiseAndDiscovery: Continously search for other users nearby.
     *        That is, do not use any business logic to enable/disable sharing. Else, a context-dependent
     *        business logic automatically enables/disables the AdvertiseAndDiscovery service in
     *        the background. More information on the context-logic can be found in [WHERE?].
     *      * Track location: The user's current location is tracked. More specifically, the
     *        user's current location is accessed through the Android OS that already keeps track
     *        of the user's current location. Else, the user's current location is not accessed.
     *      * Always reject connections: All connections that could potentially be established are
     *        not established. Else, all connections are tried to be established.
     *
     */

    abstract fun experimentLogEntryDao(): ExperimentLogEntryDao

    companion object {

        var TEST_MODE = false

        private const val databaseName = "affinity_experiment.db"

        private var INSTANCE: AffinityExperimentDatabase? = null

        fun getInstance(context: Context): AffinityExperimentDatabase {
            if (INSTANCE == null) {
                if (TEST_MODE) {
                    INSTANCE = Room.inMemoryDatabaseBuilder(
                        context,
                        AffinityExperimentDatabase::class.java
                    )
                        .allowMainThreadQueries()
                        .build()
                } else {
                    synchronized(AffinityExperimentDatabase::class) {
                        INSTANCE = Room
                            .databaseBuilder(
                                context.applicationContext,
                                AffinityExperimentDatabase::class.java, databaseName
                            )
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}
