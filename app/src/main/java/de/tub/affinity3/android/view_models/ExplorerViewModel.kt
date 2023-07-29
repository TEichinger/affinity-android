package de.tub.affinity3.android.view_models

import android.app.Application
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.gson.Gson
import de.tub.affinity3.android.R
import de.tub.affinity3.android.constants.NearbyStatus
import de.tub.affinity3.android.classes.data.User
import de.tub.affinity3.android.repositories.UserRepository
import de.tub.affinity3.android.clients.NearbyConnectionsClient
import de.tub.affinity3.android.classes.data.RequestPayload
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import java.util.Date

class ExplorerViewModel(application: Application) : BaseRxViewModel(application) {

    private val gson = Gson()
    private val nearbyClient = NearbyConnectionsClient.getInstance(application)
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val rxSharedPreferences = RxSharedPreferences.create(sharedPrefs)
    val shareRatingsPref = rxSharedPreferences.getBoolean(application.getString(R.string.pref_share_key), true)

    private val userRepository = UserRepository(context = application)

    init {

        shareRatingsPref
                .asObservable()
                .distinctUntilChanged()
                .subscribe { share ->
                    if (!share) {
                        nearbyClient.stopSharing()
                    } else {
                        nearbyClient.startSharing()
                    }
                }.addTo(disposeBag)
    }

    fun didClickEnableSharing() {
        sharedPrefs.edit().putBoolean(getApplication<Application>().getString(R.string.pref_share_key), true).apply()
    }

    fun getConnectedUsers(): Observable<List<User>> {
        return nearbyClient.activeConnections
                .flatMap { list ->
                    Observable.fromIterable(list).map {
                        val requestPayload = gson.fromJson(it.endpointName, RequestPayload::class.java)
                        val user = User(id = requestPayload.userId, name = requestPayload.userName, lastSeen = Date(), deviceName = requestPayload.deviceName)
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
