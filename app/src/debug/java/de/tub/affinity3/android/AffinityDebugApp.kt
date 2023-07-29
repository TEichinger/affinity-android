package de.tub.affinity3.android

import android.app.Application
import android.preference.PreferenceManager
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.navigation.NavigationFlipperPlugin
import com.facebook.soloader.SoLoader
import de.tub.affinity3.android.repositories.UserRepository
import de.tub.affinity3.android.services.DummyRecommendationService
import de.tub.affinity3.android.clients.NearbyConnectionsClient
import de.tub.affinity3.android.util.DummyDataProvider
import timber.log.Timber

class AffinityDebugApp : AffinityApp() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        setupFlipper()

        DummyRecommendationService(this)
            .recommend()
            .subscribe()

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        // Create device user at first start
        if (sharedPrefs.getBoolean(FIRST_RUN_PREF_KEY, true)) {
            UserRepository(this).createDeviceUser().subscribe()
            DummyDataProvider.addDummyData(this)
            sharedPrefs.edit().putBoolean(FIRST_RUN_PREF_KEY, false).apply()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        NearbyConnectionsClient.getInstance(this).stopSharing()
        NearbyConnectionsClient.getInstance(this).clearDisposeBag()
    }

    private fun setupFlipper() {
        SoLoader.init(this, false)

        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)) {
            val client = AndroidFlipperClient.getInstance(this)
            client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
            client.addPlugin(NavigationFlipperPlugin.getInstance())
            client.addPlugin(DatabasesFlipperPlugin(this))
            client.start()
        }
    }

    companion object {
        const val FIRST_RUN_PREF_KEY = "first_run"
    }
}
