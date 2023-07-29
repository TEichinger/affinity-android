package de.tub.affinity3.android.fragments

import android.Manifest
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import de.tub.affinity3.android.R
import de.tub.affinity3.android.adapters.NearbyDevicesAdapter
import de.tub.affinity3.android.classes.DiscoveryServiceManager
import de.tub.affinity3.android.classes.ExperimentTracker
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService
import de.tub.affinity3.android.util.hasPermissions
import de.tub.affinity3.android.util.visible
import de.tub.affinity3.android.view_models.ExperimentExplorerViewModel
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.explorer_bottom_sheet.*
import kotlinx.android.synthetic.main.fragment_experiment_explorer.*


class ExperimentExplorerFragment : BaseRxFragment() {
    /**
     *
     */

    private val viewModel by lazy {
        ViewModelProvider(this).get(ExperimentExplorerViewModel::class.java)
    }

    private val nearbyDevicesAdapter = NearbyDevicesAdapter(emptyList())

    override fun onStart() {
        super.onStart()
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && !hasPermissions(REQUIRED_PERMISSIONS)) {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_REQUIRED_PERMISSIONS
            )
        }
        bindViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_experiment_explorer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setTitle(R.string.title_explorer)

        rippleBackground.startRippleAnimation()
        val sheetBehavior = BottomSheetBehavior.from(bottomSheet)
        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(p0: View, p1: Int) {
            }

            override fun onSlide(p0: View, p1: Float) {
            }
        })


        // Set the adapter
        with(recyclerDevices) {
            layoutManager = LinearLayoutManager(context)
            adapter = nearbyDevicesAdapter
        }
    }


    private fun hasAllPermissions(): Boolean {
        val locationPermissionsGranted = hasLocationPermissions()
        val activityRecognitionPermissionsGranted = hasQPermissions()
        if (locationPermissionsGranted.not() || activityRecognitionPermissionsGranted.not()
        ) {
            val neededPermissions = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                neededPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                neededPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            ActivityCompat.requestPermissions(
                requireActivity(),
                neededPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasQPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

    private fun verifyLocationSettings(onSuccess: () -> Unit, onError: () -> Unit) {
        val locationRequest = ExperimentTracker.buildLocationsRequest()
        val locationSettingsRequest =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        settingsClient?.checkLocationSettings(locationSettingsRequest)
            ?.addOnCompleteListener { _ ->
                try {
                    onSuccess()
                } catch (ex: ApiException) {
                    when (ex.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                val resolvableApiException = ex as ResolvableApiException
                                resolvableApiException
                                    .startResolutionForResult(
                                        requireActivity(),
                                        LOCATION_SETTINGS_REQUEST
                                    )
                            } catch (e: IntentSender.SendIntentException) {
                                onError()
                            }
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            onError()
                        }
                    }
                }
            }
    }


    private fun bindViewModel() {
        viewModel.getNearbyStatusText().subscribe(textNearbyStatus::setText).addTo(disposeBag)

        viewModel.getConnectedUsers().subscribe { users ->
            textNearbyStatus?.visible = users.isEmpty()
            nearbyDevicesAdapter.replaceData(users)
        }.addTo(disposeBag)

        buttonEnableSharing.setOnClickListener {

            val sharedPreferences: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                    requireContext()
                )

            val bck = DiscoveryServiceManager()

            val isSharingEnabled = sharedPreferences.getBoolean(
                getString(R.string.pref_is_service_running),
                false
            )

            if (!isSharingEnabled) {
                if (hasAllPermissions()) {

                    verifyLocationSettings({
                        val contextSensingEnabled = sharedPreferences.getBoolean(
                            getString(R.string.pref_context_sensing_key),
                            false
                        )

                        sharedPreferences.edit().putBoolean(
                            getString(R.string.pref_is_service_running),
                            true
                        ).apply()

                        buttonEnableSharing.text = getString(R.string.turn_off)

                        bottomSheet.visible = true
                        rippleBackground.visible = true

                        val type = if (contextSensingEnabled) {
                            AdvertiseAndDiscoveryService.ServiceType.ContextService(
                                trackLogs = false,
                                trackLocation = false,
                                rejectConnections = false
                            )
                        } else {
                            AdvertiseAndDiscoveryService.ServiceType.ContinuousService(
                                trackLogs = false,
                                trackLocation = false,
                                rejectConnections = false
                            )
                        }

                        bck.startDiscoveryService(
                            requireContext(),
                            type
                        )

                    }, {
                        Snackbar.make(
                            requireView(),
                            "Location Settings need to be updated",
                            Snackbar.LENGTH_SHORT
                        )
                    })

                } else {
                    Snackbar.make(
                        requireView(),
                        "All necessary permissions need to be granted in order to start the service",
                        Snackbar.LENGTH_SHORT
                    )
                }

            } else {
                buttonEnableSharing.text = getString(R.string.turn_on)
                bck.stopDiscoveryService(requireContext())

                bottomSheet.visible = false
                rippleBackground.visible = false
            }

        }
    }

    override fun onResume() {
        super.onResume()

        val myPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        )
        val isSharingEnabled = myPrefs.getBoolean(
            getString(R.string.pref_is_service_running),
            false
        )

        bottomSheet.visible = isSharingEnabled
        rippleBackground.visible = isSharingEnabled
        if (isSharingEnabled) {
            buttonEnableSharing.text = getString(R.string.turn_off)
        } else {
            buttonEnableSharing.text = getString(R.string.turn_on)
        }
    }

    companion object {

        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        private const val PERMISSION_REQUEST_CODE = 24
        private const val LOCATION_SETTINGS_REQUEST = 25
    }
}
