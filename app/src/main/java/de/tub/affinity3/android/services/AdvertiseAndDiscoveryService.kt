package de.tub.affinity3.android.services

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.google.gson.Gson
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.DiscoveryDecider
import de.tub.affinity3.android.classes.DiscoveryServiceManager
import de.tub.affinity3.android.classes.ExperimentTracker
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.classes.data.RequestPayload
import de.tub.affinity3.android.clients.ContextRecognitionClient
import de.tub.affinity3.android.clients.NearbyConnectionsClientV2
import de.tub.affinity3.android.constants.AppConstants
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.repositories.UserRepository
import de.tub.affinity3.android.util.PayloadRating
import de.tub.affinity3.android.util.getDeviceName
import de.tub.affinity3.android.util.typeString
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class AdvertiseAndDiscoveryService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1337
        private const val NOTIFICATION_INFO_ID = 7331
        private const val NOTIFICATION_CHANNEL_ID = "de.tub.de.tub.affinity3.android.service.ads"
        private const val NOTIFICATION_INFO_CHANNEL_ID =
            "de.tub.de.tub.affinity3.android.service.info"
        private const val NOTIFICATION_CHANNEL_NAME = "affinity foreground service"
        private const val NOTIFICATION_INFO_CHANNEL_NAME = "affinity service infos"
        const val ACTION_STOP = "ACTION_STOP"
        private const val ACTION_ACTIVITY_RECOGNITION_UPDATE = "ACTION_ACTIVITY_RECOGNITION_UPDATE"
        const val EXTRA_NAME = "experimentName"
        const val EXTRA_SHARING_REGION = "insideOfSharingRegion"
        const val EXTRA_CONFIG = "serviceConfig"

        private var timer: Timer? = null
        private var timerTask: TimerTask? = null
        private var experimentName: String = ""
        private var serviceType: ServiceType = ServiceType.ContextService()
    }

    // location
    val locationRequest = LocationRequest.create().apply {
        interval = AppConstants.USER_LOCATION_UPDATE_INTERVAL
        fastestInterval = AppConstants.USER_LOCATION_UPDATE_SHORTEST_INTERVAL
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                if (location == null) {
                    continue
                }
                break
            }
        }
    }
    val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
    val coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION
    val permissionGranted = PackageManager.PERMISSION_GRANTED
    var isInsideOfSharingRegion = false


    private var experimentTracker: ExperimentTracker? = null
    private var discoveryDecider: DiscoveryDecider? = null

    private val contextRecognitionClient: ContextRecognitionClient by lazy {
        ContextRecognitionClient(
            this
        )
    }

    private val ratingRepository: RatingRepository by lazy { RatingRepository(this) }
    private val userRepository: UserRepository by lazy { UserRepository(this) }
    private val nearbyClient: NearbyConnectionsClientV2 by lazy {
        NearbyConnectionsClientV2(
            this,
            getRequestPayload()
        )
    }

    private lateinit var notificationManager: NotificationManager
    private val gson = Gson()
    private val disposeBag = CompositeDisposable()
    private var ratings = BehaviorSubject.create<List<Rating>>()

    private var passedTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (
                ActivityCompat.checkSelfPermission(this, fineLocation) == permissionGranted
                &&
                ActivityCompat.checkSelfPermission(this, coarseLocation) == permissionGranted
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        ratingRepository.getOwnRatings()
                .subscribe(this.ratings::onNext)
                .addTo(disposeBag)
        // send ratings as they arrive
        ratings
                .throttleWithTimeout(500, TimeUnit.MILLISECONDS)
                .subscribe(this::broadcastRatingsToConnectedClients).addTo(disposeBag)

        nearbyClient.receiveDataListener = ::receivePayload
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // it has been killed by Android and now it is restarted. We must make sure to have reinitialised everything
        if (intent == null) {
            startTimer()
            passedTime = 0
            discoveryDecider?.reset()
            val serviceLauncher = DiscoveryServiceManager()
            serviceLauncher.startDiscoveryService(
                    this,
                    serviceType,
                    experimentName
            )
        } else {
            handleIntent(intent)
        }

        // make sure you call the startForeground on onStartCommand because otherwise
        // when we hide the notification on onScreen it will not restart in Android 6 and 7
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startForegroundService()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        experimentTracker?.destroy()
        contextRecognitionClient.removeActivityUpdates(activityRecognitionUpdatePendingIntent)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        with(sharedPreferences.edit()){
            putBoolean(getString(R.string.pref_is_service_running), false)
            apply()
        }
        discoveryDecider?.reset()
        disposeBag.clear()
        stopTimer()
        nearbyClient.destroy()
    }

    private fun handleIntent(intent: Intent) {
        Timber.d("New Intent to handle: $intent")
        when (intent.action) {
            ACTION_STOP -> stopForegroundService()
            ACTION_ACTIVITY_RECOGNITION_UPDATE -> contextRecognitionClient.handleActivityUpdateIntent(
                intent
            )
            else -> {
            }
        }
        if (intent.hasExtra(EXTRA_NAME) && intent.getStringExtra(EXTRA_NAME) != null) {
            setupExperiment(intent.getStringExtra(EXTRA_NAME)!!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService()
            }
        }
        if (intent.hasExtra(EXTRA_SHARING_REGION)) {
            this.isInsideOfSharingRegion = intent.getBooleanExtra(EXTRA_SHARING_REGION, false)
        }
        if (intent.hasExtra(EXTRA_CONFIG) && intent.getParcelableExtra<ServiceType>(EXTRA_CONFIG) != null) {
            val type: ServiceType = intent.getParcelableExtra(EXTRA_CONFIG)!!
            serviceType = type
            when (type) {
                is ServiceType.ContextService -> {
                    discoveryDecider = DiscoveryDecider(
                            serviceConfig = type,
                            contextRecognitionClient = contextRecognitionClient,
                            nearbyClient = nearbyClient
                    )
                    contextRecognitionClient.requestActivityUpdates(
                            activityRecognitionUpdatePendingIntent
                    )
                }
                is ServiceType.ContinuousService -> {
                }
            }
            if (serviceType.isTracking) {
                experimentTracker = ExperimentTracker(
                        this,
                        trackLocation = type.debugTrackLocation
                )

                experimentTracker?.init()
                setupExperiment(experimentName)
            }
            nearbyClient.debugAlwaysRejectConnections = type.debugRejectConnections
        }
    }

    private fun setupExperiment(name: String) {
        experimentName = name
        ExperimentTracker.experimentName = name
    }

    private fun broadcastRatingsToConnectedClients(ratings: List<Rating>) {
        val payloadRatings = ratings.map {
            serializeRating(it)
        }
        Timber.d("Sending ${payloadRatings.size} ratings")
        nearbyClient.dataToSend = payloadRatings.toString().toByteArray()
    }

    private fun getRequestPayload(): RequestPayload {
        val user = userRepository.findDeviceUser().blockingFirst()
        return RequestPayload(user.id, user.name, getDeviceName())
    }

    private fun serializeRating(rating: Rating): String {
        val payloadRating = PayloadRating(rating.score, rating.movieId, rating.userId)
        return gson.toJson(payloadRating)
    }

    private fun receivePayload(data: ByteArray) {
        val payloadString = String(data, Charsets.UTF_8)
        Timber.d("Received payload: $payloadString")
        val payloadRatings = gson.fromJson(payloadString, Array<PayloadRating>::class.java)
        val ratings = payloadRatings.map { it.toRating() }
        Timber.d("${ratings.size} Ratings received")

        ratingRepository.addRating(*ratings.toTypedArray()).subscribe {
            Timber.d("Added ${ratings.size} ratings to Database")
        }.addTo(disposeBag)
        val notification = buildInfoNotification("Received ${ratings.size} ratings")
        notificationManager.notify(NOTIFICATION_INFO_ID, notification)
    }

    private fun stopForegroundService() {
        Timber.d("Stop foreground service.")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        with(sharedPreferences.edit()){
            putBoolean(getString(R.string.pref_is_service_running), false)
            apply()
        }
        // Stop foreground service and remove the notification.
        stopForeground(true)
        // Stop the foreground service.
        stopSelf()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Timber.d("restarting foreground")
            try {
                startForeground(
                    NOTIFICATION_ID,
                    buildForegroundNotification("Experiment '$experimentName' is running!")
                )
                Timber.d("restarting foreground successful")
                startTimer()
            } catch (e: Exception) {
                Timber.e("Error in notification %s", e.message)
            }
        }
    }

    private fun buildForegroundNotification(text: String): Notification? {

        val builder = NotificationCompat.Builder(this)
            .setContentTitle("Affinity Advertise and Discovery Service")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(buildStopServiceAction())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID) // Channel ID
        }

        return builder.build()
    }

    private fun buildInfoNotification(text: String): Notification? {

        val builder = NotificationCompat.Builder(this)
            .setContentTitle("Affinity Advertise and Discovery Service")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_INFO_CHANNEL_ID) // Channel ID
        }

        return builder.build()
    }

    private fun buildStopServiceAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, AdvertiseAndDiscoveryService::class.java)
        stopIntent.action =
            ACTION_STOP
        val pendingStopIntent =
            PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(
            android.R.drawable.ic_media_pause,
            "Stop Service",
            pendingStopIntent
        )
    }

    private val activityRecognitionUpdatePendingIntent by lazy {
        val intent = Intent(this, AdvertiseAndDiscoveryService::class.java)
        intent.action =
            ACTION_ACTIVITY_RECOGNITION_UPDATE
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val mainChan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val infoChan = NotificationChannel(
            NOTIFICATION_INFO_CHANNEL_ID,
            NOTIFICATION_INFO_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        mainChan.lightColor = Color.BLUE
        mainChan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(mainChan)
        notificationManager.createNotificationChannel(infoChan)
    }

    private fun startTimer() {
        Timber.d("Starting timer")
        // set a new Timer - if one is already running, cancel it to avoid two running at the same time
        stopTimer()
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                onTick()
            }
        }
        passedTime = 0L
        val interval = getTimerIntervalInSeconds() * 1000L
        timer!!.schedule(
            timerTask, 0, interval
        )
    }

    private fun getTimerIntervalInSeconds(): Int {
        return when (serviceType) {
            is ServiceType.ContextService -> (serviceType as ServiceType.ContextService).defaultIntervalInSeconds
            is ServiceType.ContinuousService -> (serviceType as ServiceType.ContinuousService).defaultIntervalInSeconds
        }
    }

    private fun onTick() {
        passedTime++
        val interval = getTimerIntervalInSeconds()
        val passedMinutes = TimeUnit.SECONDS.toMinutes(interval * passedTime)
        Timber.d("%d minutes passed", passedMinutes)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val privacyRegionsEnabled =
            sharedPreferences.getBoolean(getString(R.string.pref_privacy_regions_enabled_key), true)

        var isRunning = false

        if (!isInsideOfSharingRegion && privacyRegionsEnabled) {
            isRunning = false
            nearbyClient.stop()
            discoveryDecider?.nearbyClient?.stop()
        } else if (serviceType is ServiceType.ContinuousService){
            this.nearbyClient.startOrResume()
            isRunning = true
        } else if (serviceType is ServiceType.ContextService) {
            discoveryDecider?.nearbyClient?.startOrResume()
            isRunning = discoveryDecider?.toggleAdvertiseAndDiscovery() ?: false
        }

        val notificationMessage = if (isRunning) {
            "Sharing active"
        } else {
            "Sharing deactive"
        }

        notificationManager.notify(NOTIFICATION_ID, buildForegroundNotification(notificationMessage))

        experimentTracker?.trackNow(
            isRunning,
            contextRecognitionClient.currentActivity?.typeString() ?: "null",
            this.nearbyClient.getNumberOfConnectedClients()
        )
    }

    /**
     * not needed
     */
    private fun stopTimer() { // stop the timer, if it's not already null
        if (timer != null) {
            timer?.cancel()
            timer = null
            nearbyClient.stop()
        }
    }

    sealed class ServiceType(
        val isTracking: Boolean = false,
        val debugTrackLocation: Boolean = false,
        val debugRejectConnections: Boolean = false
    ) : Parcelable {
        /**
         * defaultIntervalInSeconds: Default polling interval (every x Seconds)
         * minDiscoveryRuntimeInSeconds: Time in Seconds a Discovery will run minimum before it can be stopped
         * maxDiscoveryRuntimeInSeconds: Time in Seconds after which a started Discovery is stopped latest
         * maxTimeoutInSeconds: Time in Seconds after a discovery was stopped when a new discovery is started
         */
        @Parcelize
        data class ContextService(
            private val trackLogs: Boolean = true,
            private val trackLocation: Boolean = true,
            private val rejectConnections: Boolean = false,
            val defaultIntervalInSeconds: Int = 5,
            val minRuntimeInSeconds: Int = 60,
            val maxRuntimeInSeconds: Int = 180,
            val maxTimeoutInSeconds: Int = 120,
            val sleepTimeInSeconds: Int = 30
        ) : ServiceType(
            isTracking = trackLogs,
            debugRejectConnections = rejectConnections,
            debugTrackLocation = trackLocation
        )

        @Parcelize
        data class ContinuousService(
            private val trackLogs: Boolean = true,
            private val trackLocation: Boolean = true,
            private val rejectConnections: Boolean = false,
            val defaultIntervalInSeconds: Int = 5
        ) : ServiceType(
            isTracking = trackLogs,
            debugRejectConnections = rejectConnections,
            debugTrackLocation = trackLocation
        )
    }
}
