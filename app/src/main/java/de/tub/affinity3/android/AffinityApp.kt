package de.tub.affinity3.android

import android.app.Application
import de.tub.affinity3.android.repositories.CircularRegionRepository
import de.tub.affinity3.android.repositories.GlobalGeofenceRepository
import de.tub.affinity3.android.repositories.LocalGeofenceRepository
import timber.log.Timber
import timber.log.Timber.DebugTree


open class AffinityApp : Application() {
    private lateinit var globalGeofenceRepository: GlobalGeofenceRepository
    private lateinit var localGeofenceRepository: LocalGeofenceRepository
    private lateinit var circularRegionRepository: CircularRegionRepository

    override fun onCreate() {
        super.onCreate()

        globalGeofenceRepository = GlobalGeofenceRepository(this)
        localGeofenceRepository = LocalGeofenceRepository()
        circularRegionRepository = CircularRegionRepository(this)

        // Initialize Timber logging in DEBUG build mode
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        localGeofenceRepository.clearRepository()
    }


    fun getGlobalGeofenceRepository() = globalGeofenceRepository
    fun getLocalGeofenceRepository() = localGeofenceRepository
    fun getCircularRegionRepository() = circularRegionRepository

}