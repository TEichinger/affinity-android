package de.tub.affinity3.android.clients

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson

import de.tub.affinity3.android.constants.NearbyStatus
import de.tub.affinity3.android.classes.data.Coordinates
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.classes.data.RequestPayload
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.repositories.UserRepository
import de.tub.affinity3.android.util.PayloadRating
import de.tub.affinity3.android.util.SingletonHolder
import de.tub.affinity3.android.util.getDeviceName
import de.tub.affinity3.android.util.hasPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import java.util.Date
import java.util.HashMap
import java.util.concurrent.TimeUnit
import timber.log.Timber


class NearbyConnectionsClient private constructor(val context: Context) {
    /**
     *
     */

    val activeConnections: BehaviorSubject<List<ConnectionInfo>> = BehaviorSubject.createDefault(emptyList())

    var status = BehaviorSubject.createDefault(NearbyStatus.LOOKING_FOR_OTHER_DEVICES)

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private var start: Date? = null

    private var endPointIdToConnectionInfo = HashMap<String, ConnectionInfo>()
    private var endPointIdToConnectionResolution = HashMap<String, ConnectionResolution>()

    private var connectionsClient = Nearby.getConnectionsClient(context)

    private val ratingRepository = RatingRepository(context)
    private val userRepository = UserRepository(context)

    private val gson = Gson()
    private val disposeBag = CompositeDisposable()

    private var ratings = BehaviorSubject.create<List<Rating>>()
    private var started = false

    init {
        // TODO: send all ratings
        ratingRepository.getOwnRatings()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this.ratings::onNext)
            .addTo(disposeBag)

        // send ratings as they arrive
        ratings
            .throttleWithTimeout(500, TimeUnit.MILLISECONDS)
            .subscribe(this::broadcastRatingsToConnectedClients).addTo(disposeBag)
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Timber.d("connection endpoint id: $endpointId")
            endPointIdToConnectionResolution[endpointId] = result

            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    // We're connected! Can now start sending and receiving data.
                    start?.let {
                        val elapsedSeconds = Date().time - it.time / 1000
                        Timber.d("Connection Status OK at ${Date()}, took ${DateUtils.formatElapsedTime(elapsedSeconds)}")
                    }

                    status.onNext(NearbyStatus.PAYLOAD_SEND)
                    if (ratings.hasValue()) {
                        Timber.d("We don't have any ratings to send :(")
                    }

                    ratings.value?.let {
                        broadcastRatingsToConnectedClients(it)
                    }
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    // The connection was rejected by one or both sides.
                    Timber.d("Connection REJECTED")
                }

                else -> {
                    Timber.d("Connection interrupted")
                    // The connection was broken before it was accepted.
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Timber.d("endpoint $endpointId disconnected")
            endPointIdToConnectionInfo.remove(endpointId)
            status.onNext(NearbyStatus.LOOKING_FOR_OTHER_DEVICES)
            activeConnections.onNext(endPointIdToConnectionInfo.values.toList())
        }

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept the connection on both sides.
            Timber.d("connection $endpointId initiated")
            status.onNext(NearbyStatus.CONNECTION_INITIATED)
            connectionsClient.acceptConnection(endpointId, payloadCallback)

            endPointIdToConnectionInfo[endpointId] = connectionInfo

            // notify observers
            activeConnections.onNext(endPointIdToConnectionInfo.values.toList())
        }
    }

    // Callbacks for finding other devices
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Timber.d("onEndpointFound: endpoint $endpointId found, connecting")
            status.onNext(NearbyStatus.ENDPOINT_FOUND)

            // User A should be the userId or userName from db
            val payload = getSerializedRequestPayload()
            Timber.d("OnEndpointFound payload: $payload")
            connectionsClient.requestConnection(getSerializedRequestPayload(), endpointId, connectionLifecycleCallback)
                .addOnSuccessListener {
                    Timber.d("$endpointId - Request Connection Success ")
                }
                .addOnFailureListener {
                    Timber.d("$endpointId - Request Connection Failure.")
                    Timber.e(it)

                    connectionsClient.requestConnection(payload, endpointId, connectionLifecycleCallback)
                }
                .addOnCanceledListener {
                    Timber.d("$endpointId - Request Connection Cancelled.")
                }
                .addOnCompleteListener {
                    Timber.d("$endpointId Request Connection completed.")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Timber.d("onEndpointLost: endpoint $endpointId lost")
            status.onNext(NearbyStatus.ENDPOINT_LOST)
        }
    }

    // Callbacks for receiving payloads
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // A new payload is being sent over
            status.onNext(NearbyStatus.PAYLOAD_RECEIVED)
            Timber.d("Received payload")
            handleReceivedPayload(payload)
            Timber.d("end of receive")
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Payload progress has updated.
            Timber.d("payload transfer update")
            Timber.d(update.toString())
        }
    }

    private fun hasLocationPermission(): Boolean {
        return hasPermissions(
            context,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    @SuppressLint("MissingPermission")
    private fun handleReceivedPayload(payload: Payload) {
        payload.asBytes()?.let { bytes ->
            val payloadString = String(bytes, Charsets.UTF_8)
            Timber.d("Received payload: $payloadString")
            val payloadRatings = gson.fromJson(payloadString, Array<PayloadRating>::class.java)
            val ratings = payloadRatings.map { it.toRating() }
            Timber.d("${ratings.size} Ratings received")

            // check permission
            if (hasLocationPermission()) {
                fusedLocationClient.lastLocation.addOnCompleteListener {
                    it.result?.let { location ->
                        val coordinates = Coordinates(location.latitude, location.longitude)
                        ratings.forEach { rating ->
                            rating.coordinates = coordinates
                        }
                    }
                    saveRatings(ratings)
                }.addOnFailureListener {
                    Timber.e(it)
                    // save ratings without location
                    saveRatings(ratings)
                }.addOnCanceledListener {
                    // save ratings without location
                    saveRatings(ratings)
                }
            } else {
                Timber.d("No location permissions")
                saveRatings(ratings)
            }
        }
    }

    private fun saveRatings(ratings: List<Rating>) {
        ratingRepository.addRating(*ratings.toTypedArray()).subscribe {
            Timber.d("Added ${ratings.size} ratings to Database")
        }.addTo(disposeBag)
    }

    /** Starts looking for other connectedUsers using Nearby Connections.  */
    private fun startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
            context.packageName, endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        )
    }

    /** Broadcasts our presence using Nearby Connections so other connectedUsers can find us. */
    private fun startAdvertising() {
        connectionsClient.startAdvertising(
            getSerializedRequestPayload(),
            context.packageName,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        )
    }

    /** Finds connectedUsers to share data with **/
    fun startSharing() {
        if (!started) {
            Timber.d("Starting sharing at ${Date()}")
            start = Date()
            startAdvertising()
            startDiscovery()
            status.onNext(NearbyStatus.LOOKING_FOR_OTHER_DEVICES)
            started = true
        }
    }

    fun stopSharing() {
        if (started) {
            Timber.d("Stopping sharing")
            connectionsClient.stopAllEndpoints()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAdvertising()
            status.onNext(NearbyStatus.SHARING_DISABLED)
            started = false
        }
    }

    fun clearDisposeBag() {
        disposeBag.clear()
    }

    private fun broadcastRatingsToConnectedClients(ratings: List<Rating>) {
        if (ratings.isEmpty()) {
            Timber.d("No ratings to send ...")
            return
        }

        if (endPointIdToConnectionInfo.isNullOrEmpty()) {
            Timber.d("Send Ratings requested although device is not connected to any endpoints ...")
            return
        }

        val payloadRatings = ratings.map { serializeRating(it) }
        val bytesPayload = Payload.fromBytes(payloadRatings.toString().toByteArray())
        Timber.d("Sending ${payloadRatings.size} ratings")

        for ((endpointId, resolution) in endPointIdToConnectionResolution) {
            if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectionsClient.sendPayload(endpointId, bytesPayload)
            }
        }
    }

    private fun serializeRating(rating: Rating): String {
        val payloadRating = PayloadRating(rating.score, rating.movieId, rating.userId)
        return gson.toJson(payloadRating)
    }

    private fun getSerializedRequestPayload(): String {
        val user = userRepository.findDeviceUser().blockingFirst()
        val payload = RequestPayload(user.id, user.name, getDeviceName())
        return gson.toJson(payload)
    }

    companion object : SingletonHolder<NearbyConnectionsClient, Context>(::NearbyConnectionsClient) {
        private val STRATEGY = Strategy.P2P_CLUSTER
    }
}
