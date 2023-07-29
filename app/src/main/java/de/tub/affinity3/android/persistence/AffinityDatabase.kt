package de.tub.affinity3.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.tub.affinity3.android.classes.Converters
import de.tub.affinity3.android.classes.data.*
import de.tub.affinity3.android.persistence.daos.*

@Database(
    entities = [Movie::class, Rating::class, Recommendation::class, User::class, CircularRegion::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AffinityDatabase : RoomDatabase() {
    /**
     * The <AffinityDatabase> database is the data that enabled the app's core functionality.
     * More specifically, it persists ratings from both the user and other users.
     * Persisted ratings are used to provide recommendations to the user. The recommendations
     * can be added to the watchlist or marked as something the user is not interested in.
     *
     * The core entities comprise:
     *  * movie
     *  * rating
     *  * recommendation
     *  * user
     *
     * Optional entities comprise:
     *  * circularRegion: <circularRegion> entries define circular geofences (center, radius) only
     *              within which to enable the AdvertiseAndDiscovery service. <circularRegion>s
     *              are thus an optional entry that is only (!) of relevance, if the privacy
     *              regions feature is enabled in the Settings:
     *                  Settings > Privacy Regions Settings > Enable privacy regions > TICK.
     */

    // Core entities
    abstract fun movieDao(): MovieDao

    abstract fun ratingDao(): RatingDao

    abstract fun recommendationDao(): RecommendationDao

    abstract fun userDao(): UserDao

    // Optional entities
    abstract fun circularRegionDao(): CircularRegionDao


    companion object {

        var TEST_MODE = false

        private const val databaseName = "affinity.db"

        private var INSTANCE: AffinityDatabase? = null

        fun getInstance(context: Context): AffinityDatabase {
            if (INSTANCE == null) {
                if (TEST_MODE) {
                    INSTANCE = Room.inMemoryDatabaseBuilder(context, AffinityDatabase::class.java)
                        .allowMainThreadQueries()
                        .build()
                } else {
                    synchronized(AffinityDatabase::class) {
                        INSTANCE = Room
                            .databaseBuilder(
                                context.applicationContext,
                                AffinityDatabase::class.java, databaseName
                            )
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}
