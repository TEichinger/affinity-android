package de.tub.affinity3.android.clients


import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils

import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson

import de.tub.affinity3.android.classes.data.RequestPayload
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.Date
import java.util.HashMap
import kotlin.properties.Delegates.observable
import timber.log.Timber


class NearbyConnectionsClientV2 constructor(
    val context: Context,
    val requestPayload: RequestPayload
) {

    var debugAlwaysRejectConnections: Boolean = false
    val activeConnections: BehaviorSubject<List<ConnectionInfo>> =
        BehaviorSubject.createDefault(emptyList())

    var receiveDataListener: ((data: ByteArray) -> Unit)? = null

    var dataToSend: ByteArray by observable(ByteArray(0)) { _, _, newData ->
        broadcastDataToConnectedClients(newData)
    }







    private var start: Date? = null

    private var endPointIdToConnectionInfo = HashMap<String, ConnectionInfo>()
    private var endPointIdToConnectionResolution = HashMap<String, ConnectionResolution>()

    private var connectionsClient: ConnectionsClient? = Nearby.getConnectionsClient(context)

    private val gson = Gson()
    private val disposeBag = CompositeDisposable()

    private var status: Status = Status.STOPPED

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

                    if (dataToSend.isEmpty()) {
                        Timber.d("No data to send :(")
                    } else {
                        broadcastDataToConnectedClients(dataToSend)
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
            activeConnections.onNext(endPointIdToConnectionInfo.values.toList())
        }

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept the connection on both sides.
            Timber.d("connection to $endpointId initiated")
            if (debugAlwaysRejectConnections) {
                Timber.d("connection to $endpointId rejected")
                connectionsClient?.rejectConnection(endpointId)
                return
            }

            Timber.d("connection to $endpointId accepted")
            connectionsClient?.acceptConnection(endpointId, payloadCallback)
            endPointIdToConnectionInfo[endpointId] = connectionInfo

            // notify observers
            activeConnections.onNext(endPointIdToConnectionInfo.values.toList())
        }
    }

    // Callbacks for finding other devices
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Timber.d("onEndpointFound: endpoint $endpointId found, connecting")
            // User A should be the userId or userName from db
            val payload = getSerializedRequestPayload()
            Timber.d("OnEndpointFound payload: $payload")
            connectionsClient?.requestConnection(
                getSerializedRequestPayload(),
                endpointId,
                connectionLifecycleCallback
            )
                ?.addOnSuccessListener {
                    Timber.d("$endpointId - Request Connection Success ")
                }
                ?.addOnFailureListener {
                    Timber.d("$endpointId - Request Connection Failure.")
                    Timber.e(it)

                    connectionsClient?.requestConnection(
                        payload,
                        endpointId,
                        connectionLifecycleCallback
                    )
                }
                ?.addOnCanceledListener {
                    Timber.d("$endpointId - Request Connection Cancelled.")
                }
                ?.addOnCompleteListener {
                    Timber.d("$endpointId Request Connection completed.")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Timber.d("onEndpointLost: endpoint $endpointId lost")
        }
    }

    // Callbacks for receiving payloads
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // A new payload is being sent over
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

    @SuppressLint("MissingPermission")
    private fun handleReceivedPayload(payload: Payload) {
        payload.asBytes()?.let { bytes ->
            receiveDataListener?.invoke(bytes)
        }
    }

    /** Starts looking for other connectedUsers using Nearby Connections.  */
    private fun startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient?.startDiscovery(
            context.packageName, endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        )
    }

    /** Broadcasts our presence using Nearby Connections so other connectedUsers can find us. */
    private fun startAdvertising() {
        connectionsClient?.startAdvertising(
            getSerializedRequestPayload(),
            context.packageName,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        )
    }

    /** Finds connectedUsers to share data with **/
    fun startOrResume() {
        if (connectionsClient == null) {
            connectionsClient = Nearby.getConnectionsClient(context)
        }
        when (status) {
            Status.ACTIVE -> {
                Timber.d("Already Running")
            }
            Status.PAUSED -> {
                Timber.d("Was paused, now resumed")
                startAdvertising()
                startDiscovery()
            }
            Status.STOPPED -> {
                Timber.d("Starting sharing at ${Date()}")
                start = Date()
                startAdvertising()
                startDiscovery()
            }
        }
        status = Status.ACTIVE
    }

    fun pause() {
        when (status) {
            Status.ACTIVE -> {
                Timber.d("Now paused")
                connectionsClient?.stopDiscovery()
                connectionsClient?.stopAdvertising()
                status = Status.PAUSED
            }
            Status.PAUSED -> {
                Timber.d("Was already paused")
                status = Status.PAUSED
            }
            Status.STOPPED -> {
                Timber.d("Was not running, could not be paused")
                status = Status.STOPPED
            }
        }
    }

    fun stop() {
        when (status) {
            Status.STOPPED -> {
                Timber.d("Was already stopped")
                status = Status.STOPPED
            }
            else -> {
                Timber.d("Stopping sharing")
                connectionsClient?.stopAllEndpoints()
                connectionsClient?.stopDiscovery()
                connectionsClient?.stopAdvertising()
            }
        }
    }

    fun hasConnectedClients(): Boolean {
        return endPointIdToConnectionInfo.isEmpty().not()
    }

    fun getNumberOfConnectedClients(): Int {
        return endPointIdToConnectionInfo.size
    }

    fun isActive(): Boolean {
        return status == Status.ACTIVE
    }

    fun isPaused(): Boolean {
        return status == Status.PAUSED
    }

    fun destroy() {
        stop()
        disposeBag.clear()
        connectionsClient = null
    }

    private fun getSerializedRequestPayload(): String {
        return gson.toJson(requestPayload)
    }

    private fun broadcastDataToConnectedClients(data: ByteArray) {
        if (data.isEmpty()) {
            Timber.d("No data to send ...")
            return
        }

        if (endPointIdToConnectionInfo.isNullOrEmpty()) {
            Timber.d("Send data requested although device is not connected to any endpoints ...")
            return
        }

        val bytesPayload = Payload.fromBytes(data)
        Timber.d("Sending ${data.size} data items")

        for ((endpointId, resolution) in endPointIdToConnectionResolution) {
            if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectionsClient?.sendPayload(endpointId, bytesPayload)
            }
        }
    }

    companion object {
        private val STRATEGY = Strategy.P2P_CLUSTER
    }

    enum class Status {
        ACTIVE, PAUSED, STOPPED
    }
}
