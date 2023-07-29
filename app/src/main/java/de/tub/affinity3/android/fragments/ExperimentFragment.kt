package de.tub.affinity3.android.fragments

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import de.tub.affinity3.android.R
import de.tub.affinity3.android.services.AdvertiseAndDiscoveryService
import de.tub.affinity3.android.classes.DiscoveryServiceManager
import de.tub.affinity3.android.classes.ExperimentTracker
import de.tub.affinity3.android.adapters.ExperimentListAdapter
import de.tub.affinity3.android.classes.sealed.ExperimentSingleEvent
import de.tub.affinity3.android.view_models.ExperimentViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_experiment.*

open class ExperimentFragment : BaseRxFragment() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(ExperimentViewModel::class.java)
    }

    private val experimentsAdapter by lazy {
        ExperimentListAdapter(
            deleteClickListener = viewModel::deleteExperiment,
            exportClickListener = viewModel::exportExperiment
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_experiment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.title_movies)
        setupRecycler()
        bindViewModel()
        start_experiment_button.setOnClickListener {
            onStartExperimentClicked()
        }
    }

    private fun setupRecycler() {
        // Set the adapter
        with(list_experiments) {
            layoutManager = LinearLayoutManager(context)
            adapter = experimentsAdapter
        }
    }

    private fun onStartExperimentClicked() {
        if (hasAllPermissions()) {
            verifyLocationSettings({
                val layout = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_new_experiment, view as ViewGroup?, false)
                val edittext = layout.findViewById<EditText>(R.id.input)
                val builder = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("New Experiment")
                    .setView(layout)
                    .setPositiveButton("Start") { _, _ ->
                        val name = edittext.text.toString().trim()
                        startExperimentWithName(name)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                    }
                builder.show()
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
    }

    fun hasAllPermissions(): Boolean {
        val locationPermissionsGranted = hasLocationPermissions()
        val activityRecognitionPermissionsGranted = hasQPermissions()
        if (locationPermissionsGranted.not() || activityRecognitionPermissionsGranted.not()
        ) {
            val neededPermissions = mutableListOf<String>(
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
            ?.addOnCompleteListener { task ->
                try {
                    val response = task.getResult(ApiException::class.java)
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

    private fun startExperimentWithName(name: String) {
        val bck =
            DiscoveryServiceManager()
        val trackLocations = location_switch.isChecked
        val rejectConnections = connections_switch.isChecked
        val continuousDiscovery = reference_switch.isChecked
        val type = if (continuousDiscovery) {
            AdvertiseAndDiscoveryService.ServiceType.ContinuousService(
                trackLogs = true,
                trackLocation = trackLocations,
                rejectConnections = rejectConnections
            )
        } else {
            AdvertiseAndDiscoveryService.ServiceType.ContextService(
                trackLogs = true,
                trackLocation = trackLocations,
                rejectConnections = rejectConnections
            )
        }
        bck.startDiscoveryService(
            requireContext(),
            type
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                onStartExperimentClicked()
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun bindViewModel() {
        viewModel.singleEvent.observe(this.viewLifecycleOwner, Observer { event ->
            onHandleSingleEvent(event)
        })
        viewModel.fetchAllExperiments()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(experimentsAdapter::submitList)
            .addTo(disposeBag)
    }

    private fun onHandleSingleEvent(singleEvent: ExperimentSingleEvent) {
        when (singleEvent) {
            ExperimentSingleEvent.NONE -> return
            is ExperimentSingleEvent.SHOW_SNACKBAR -> Snackbar.make(
                requireView(),
                singleEvent.text,
                Snackbar.LENGTH_SHORT
            )
                .show()
        }

        viewModel.singleEvent.value = ExperimentSingleEvent.NONE
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 24
        private const val LOCATION_SETTINGS_REQUEST = 25

    }
}
