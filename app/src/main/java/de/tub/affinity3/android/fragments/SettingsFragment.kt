package de.tub.affinity3.android.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.tub.affinity3.android.R
import de.tub.affinity3.android.repositories.RatingRepository
import de.tub.affinity3.android.services.ExportService
import de.tub.affinity3.android.util.DummyDataProvider
import de.tub.affinity3.android.util.getDeviceId
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import timber.log.Timber


class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var sharedPreferences: SharedPreferences
    private val disposeBag = CompositeDisposable()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setTitle(R.string.title_settings)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.fragment_user_settings)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        findPreference<CheckBoxPreference>(getString(R.string.pref_experiments_menu_key))!!.setOnPreferenceChangeListener { _, newValue ->
            val checked = (newValue as Boolean)
            with(sharedPreferences.edit()){
                putBoolean(getString(R.string.pref_experiments_menu_key), checked)
                apply()
            }

            return@setOnPreferenceChangeListener true
        }

        findPreference<Preference>(getString(R.string.pref_rate_again_key))!!.setOnPreferenceClickListener { _ ->
            val builder = MaterialAlertDialogBuilder(requireContext())

            builder.setTitle(R.string.clear_ratings)
            builder.setMessage(R.string.clear_own_ratings_message)
            builder.setPositiveButton(R.string.yes_im_sure) { _, _ ->
                val ratingRepository = RatingRepository(requireContext())
                ratingRepository.getOwnRatings()
                    .observeOn(AndroidSchedulers.mainThread())
                    .firstOrError() // Only take once here to avoid deleting new entries again
                    .flatMapCompletable {
                        ratingRepository.deleteRatings(*it.toTypedArray())
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            Toast.makeText(
                                    activity,
                                    R.string.cleared_own_ratings,
                                    Toast.LENGTH_SHORT
                            )::show
                    )
                    .addTo(disposeBag)
            }

            builder.setNegativeButton(R.string.cancel) { _, _ -> }

            val dialog = builder.create()
            dialog.show()

            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(getString(R.string.pref_clear_ratings_key))!!.setOnPreferenceClickListener { _ ->
            val builder = MaterialAlertDialogBuilder(requireContext())

            builder.setTitle(R.string.clear_ratings)
            builder.setMessage(R.string.clear_ratings_message)
            builder.setPositiveButton(R.string.yes_im_sure) { _, _ ->
                val ratingRepository = RatingRepository(requireContext())
                val deviceId = getDeviceId(requireContext())
                ratingRepository.getAllFromNearbyUsers(deviceId)
                    .flatMapCompletable {
                        ratingRepository.deleteRatings(*it.toTypedArray())
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            Toast.makeText(
                                    activity,
                                    R.string.cleared_ratings_from_nearby_users,
                                    Toast.LENGTH_SHORT
                            )::show
                    )
                    .addTo(disposeBag)
            }

            builder.setNegativeButton(
                    R.string.cancel
            ) { _, _ ->
            }

            val dialog = builder.create()
            dialog.show()

            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(getString(R.string.pref_export))!!.setOnPreferenceClickListener { _ ->
            val builder = MaterialAlertDialogBuilder(requireContext())

            builder.setTitle(R.string.export_data)
            builder.setMessage(getString(R.string.all_ratings_in_the_database_will_be_exported))
            builder.setPositiveButton(R.string.yes_im_sure) { _, _ ->
                ExportService(requireContext()).exportRatings()
            }

            builder.setNegativeButton(
                    R.string.cancel
            ) { _, _ ->
            }

            val dialog = builder.create()
            dialog.show()

            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(getString(R.string.pref_privacy_regions_settings_key))
            ?.setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_privacyRegionsSettingsFragment)
                return@setOnPreferenceClickListener true
            }

        val ownRatingsPref = findPreference<Preference>(getString(R.string.pref_own_ratings))

        ownRatingsPref!!.setOnPreferenceClickListener { _ ->
            val builder = MaterialAlertDialogBuilder(requireContext())
            val view = layoutInflater.inflate(R.layout.number_picker_dialog, null)
            builder.setView(view)
            builder.setTitle("Create Profile Ratings")
            builder.setMessage("How many ratings should be created?")
            val numberPicker = view.findViewById(R.id.number_picker) as NumberPicker
            val countValues: Array<String> =
                intArrayOf(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000).map { it.toString() }
                    .toTypedArray()
            numberPicker.minValue = 0
            numberPicker.maxValue = 9
            numberPicker.displayedValues = countValues

            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                DummyDataProvider.addRatings(
                        requireContext(),
                        countValues[numberPicker.value].toInt(),
                        true,
                        false
                ).subscribe {
                    Timber.d("Ratings added")
                }.addTo(disposeBag)
            }

            builder.setNegativeButton(
                    R.string.cancel
            ) { _, _ ->
            }

            val dialog = builder.create()
            dialog.show()

            return@setOnPreferenceClickListener true
        }

        RatingRepository(requireContext()).countOwnRatings()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                ownRatingsPref.summary = "$it ratings found"
            }.addTo(disposeBag)

        val externalRatingsPref =
            findPreference<Preference>(getString(R.string.pref_external_ratings))

        externalRatingsPref!!.setOnPreferenceClickListener { _ ->
            val builder = MaterialAlertDialogBuilder(requireContext())
            val view = layoutInflater.inflate(R.layout.number_picker_dialog, null)
            builder.setView(view)
            builder.setTitle("Create Nearby User Ratings")
            builder.setMessage("How many ratings should be created?")
            val numberPicker = view.findViewById(R.id.number_picker) as NumberPicker
            val countValues: Array<String> =
                intArrayOf(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000).map { it.toString() }
                    .toTypedArray()
            numberPicker.minValue = 0
            numberPicker.maxValue = 9
            numberPicker.displayedValues = countValues

            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                DummyDataProvider.addRatings(
                        requireContext(),
                        countValues[numberPicker.value].toInt(),
                        true,
                        true
                ).subscribe {
                    Timber.d("Ratings added")
                }.addTo(disposeBag)
            }

            builder.setNegativeButton(
                    R.string.cancel
            ) { _, _ ->
            }

            val dialog = builder.create()
            dialog.show()

            return@setOnPreferenceClickListener true
        }

        RatingRepository(requireContext()).countExternalRatings()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                externalRatingsPref.summary = "$it ratings found"
            }.addTo(disposeBag)

        // show version number
        val versionPref = findPreference<Preference>(getString(R.string.pref_version_key))
        appVersion()?.let {
            versionPref!!.summary = it
        }
    }

    override fun onDestroy() {
        disposeBag.clear()
        super.onDestroy()
    }

    private fun appVersion(): String? {
        val pInfo = activity?.packageManager?.getPackageInfo(activity?.packageName!!, 0)
        return pInfo?.versionName
    }
}
