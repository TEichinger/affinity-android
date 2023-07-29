package de.tub.affinity3.android.view_models

import android.app.Application
import android.net.Uri
import android.os.StrictMode
import androidx.lifecycle.MutableLiveData
import com.opencsv.CSVWriter
import de.tub.affinity3.android.classes.data.Experiment
import de.tub.affinity3.android.classes.data.ExperimentLogEntry
import de.tub.affinity3.android.repositories.ExperimentRepository
import de.tub.affinity3.android.classes.sealed.ExperimentSingleEvent
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import java.io.File
import java.io.FileWriter
import java.io.IOException
import timber.log.Timber

open class ExperimentViewModel(application: Application) : BaseRxViewModel(application) {

    private val experimentRepository: ExperimentRepository =
        ExperimentRepository(
            application
        )

    var singleEvent = MutableLiveData<ExperimentSingleEvent>(ExperimentSingleEvent.NONE)

    init {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    fun fetchAllExperiments(): Flowable<List<Experiment>> = experimentRepository.getAllExperiments()

    fun deleteExperiment(experiment: Experiment) {
        experimentRepository.deleteExperiment(experiment.name)
            .subscribe()
            .addTo(disposeBag)
    }

    fun exportExperiment(experiment: Experiment) {
        experimentRepository.getAllLogsForExperiment(experiment.name)
            .firstOrError()
            .flatMap {
                writeLogsToFileAndReturnFilename(it)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { fileName ->
                    singleEvent.value =
                            ExperimentSingleEvent.SHOW_SNACKBAR("Logs for ${experiment.name} successfully exported to $fileName")
                },
                { error ->
                    Timber.e(error)
                    singleEvent.value =
                            ExperimentSingleEvent.SHOW_SNACKBAR("Logs for ${experiment.name} could not be exported!")
                }
            )
            .addTo(disposeBag)
    }

    private fun logEntryToArray(entry: ExperimentLogEntry): Array<String> {
        return arrayOf(
            entry.timestamp.toString(),
            entry.curerentlyConnectedClients,
            entry.batteryLevel.toString(),
            entry.latitude.toString(),
            entry.longitude.toString(),
            entry.accuracy.toString(),
            entry.isRunningDiscovery.toString(),
            entry.currentActivity
        )
    }

    private fun headersArray(): Array<String> {
        return arrayOf(
            "timestamp",
            "day",
            "batteryLevel",
            "latitude",
            "longitude",
            "accuracy",
            "isRunningDiscovery",
            "currentActivity"
        )
    }

    private fun writeLogsToFileAndReturnFilename(ratings: List<ExperimentLogEntry>): Single<String> {
        return Single.create<String> {
            val experimentName = ratings.first().experimentName.replace(" ", "+")
            val filename = "affinity_experiment-$experimentName.csv"
            try {
                val file = File(getApplication<Application>().externalCacheDir, filename)
                val data = ratings.map { logEntryToArray(it) }

                writeCSVFile(data, file)
                val uri = Uri.fromFile(file)
                it.onSuccess(filename)
            } catch (e: IOException) {
                it.onError(e)
            }

//        if (file.exists()) {
//            sendEmail(uri)
//        } else {
//            context.toast("Could not find file!")
//        }
        }
    }

    private fun writeCSVFile(data: List<Array<String>>, file: File) {
        val writer: CSVWriter

        writer = if (file.exists() && !file.isDirectory) {
            val fileWriter = FileWriter(file, false)
            CSVWriter(fileWriter)
        } else {
            CSVWriter(FileWriter(file))
        }
        writer.writeNext(headersArray())
        writer.writeAll(data)
        writer.close()
    }
}
