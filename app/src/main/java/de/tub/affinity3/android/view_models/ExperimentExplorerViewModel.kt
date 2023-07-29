package de.tub.affinity3.android.view_models

import android.app.Application
import com.google.gson.Gson
import de.tub.affinity3.android.classes.ExperimentTracker
import de.tub.affinity3.android.classes.data.User
import de.tub.affinity3.android.repositories.UserRepository
import de.tub.affinity3.android.clients.NearbyConnectionsClient
import de.tub.affinity3.android.constants.NearbyStatus
import de.tub.affinity3.android.classes.data.RequestPayload
import io.reactivex.Observable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class ExperimentExplorerViewModel(application: Application) : BaseRxViewModel(application) {

    private val gson = Gson()
    private val nearbyClient = NearbyConnectionsClient.getInstance(application)

    private val userRepository = UserRepository(context = application)

    init {
        val df: DateFormat = SimpleDateFormat("yyyy.MM.dd")
        val date: String = df.format(Calendar.getInstance().time)
        ExperimentTracker.experimentName = "${date}_old"
    }

    fun getConnectedUsers(): Observable<List<User>> {
        return nearbyClient.activeConnections
            .flatMap { list ->
                Observable.fromIterable(list).map {
                    val requestPayload = gson.fromJson(it.endpointName, RequestPayload::class.java)
                    val user = User(
                        id = requestPayload.userId,
                        name = requestPayload.userName,
                        lastSeen = Date(),
                        deviceName = requestPayload.deviceName
                    )
                    userRepository.addUser(user).subscribe().dispose()
                    return@map user
                }.toList().toObservable()
            }
    }

    fun getNearbyStatusText(): Observable<String> {
        return nearbyClient.status.map { status ->
            return@map when (status) {
                NearbyStatus.SHARING_DISABLED -> "Sharing disabled!"
                NearbyStatus.LOOKING_FOR_OTHER_DEVICES -> "Looking for nearby users..."
                NearbyStatus.ENDPOINT_FOUND -> "Found another user! Connecting..."
                NearbyStatus.ENDPOINT_LOST -> "Endpoint lost!"
                NearbyStatus.CONNECTION_INITIATED -> "Connection initiated"
                NearbyStatus.PAYLOAD_SEND -> "Sending payload..."
                NearbyStatus.PAYLOAD_RECEIVED -> "Received payload!"
            }
        }
    }
}
