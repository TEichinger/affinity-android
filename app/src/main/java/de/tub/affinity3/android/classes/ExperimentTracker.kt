package de.tub.affinity3.android.classes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import de.tub.affinity3.android.classes.data.ExperimentLogEntry
import de.tub.affinity3.android.repositories.ExperimentRepository
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.util.Calendar
import timber.log.Timber

class ExperimentTracker(
    val context: Context,
    val trackLocation: Boolean = true
) {

    private val experimentRepository: ExperimentRepository by lazy {
        ExperimentRepository(
            context
        )
    }

    private val disposeBag = CompositeDisposable()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var settingsClient: SettingsClient? = null

    fun init() {
        if (trackLocation) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            settingsClient = LocationServices.getSettingsClient(context)
            locationRequest =
                buildLocationsRequest()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    Timber.d("onLocationResult $locationResult")
                    super.onLocationResult(locationResult)
                    onNewLocation(locationResult?.lastLocation)
                }
            }

            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(locationRequest!!)
            locationSettingsRequest = builder.build()
            requestLocationUpdates()
        }
    }

    fun destroy() {
        disposeBag.clear()
        removeLocationUpdates()
    }

    fun trackNow(
        isRunningDiscovery: Boolean,
        currentActivity: String,
        numberOfConnectedClients: Int
    ) {
        if (experimentName.isEmpty()) return
        disposeBag.add(
            trackBatteryLevel().onErrorReturnItem(-1F)
                .map { batteryLevel: Float ->
                    val date = Calendar.getInstance()
                    val location = lastLocation
                    val data =
                        ExperimentLogEntry(
                            experimentName = experimentName,
                            curerentlyConnectedClients = numberOfConnectedClients.toString(),
                            timestamp = date.timeInMillis,
                            latitude = location?.latitude,
                            longitude = location?.longitude,
                            accuracy = location?.accuracy,
                            batteryLevel = batteryLevel,
                            currentActivity = currentActivity,
                            isRunningDiscovery = isRunningDiscovery
                        )
                    Timber.d(data.toString())
                    data
                }
                .flatMapCompletable { experimentRepository.addLogEntry(it) }
                .subscribe()
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        settingsClient?.checkLocationSettings(locationSettingsRequest)
            ?.addOnSuccessListener {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                ).addOnFailureListener {
                    Timber.e(it)
                }
            }?.addOnFailureListener {
                Timber.e(it)
            }
    }

    private fun removeLocationUpdates() {
        if (trackLocation) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun onNewLocation(location: Location?) {
        this.lastLocation = location
    }

    private fun trackBatteryLevel(): Single<Float> {
        return Single.create { emitter ->
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val batteryPct: Float? = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            }
            if (batteryPct != null) {
                emitter.onSuccess(batteryPct)
            } else {
                emitter.onError(NullPointerException("Couldn't get the Battery level"))
            }
        }
    }

    companion object {
        var experimentName: String = ""

        const val LOCATION_REQUEST_INTERVAL = 10000L
        const val LOCATION_REQUEST_INTERVAL_FASTEST = 5000L
        const val LOCATION_REQUEST_PRIOTIY = LocationRequest.PRIORITY_HIGH_ACCURACY

        fun buildLocationsRequest(): LocationRequest {
            val locationRequest = LocationRequest()
            locationRequest.interval =
                LOCATION_REQUEST_INTERVAL
            locationRequest.fastestInterval =
                LOCATION_REQUEST_INTERVAL_FASTEST
            locationRequest.priority =
                LOCATION_REQUEST_PRIOTIY
            return locationRequest
        }
    }
}
