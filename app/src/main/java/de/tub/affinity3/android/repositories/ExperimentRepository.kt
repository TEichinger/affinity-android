package de.tub.affinity3.android.repositories

import android.content.Context
import de.tub.affinity3.android.persistence.AffinityExperimentDatabase
import de.tub.affinity3.android.classes.data.Experiment
import de.tub.affinity3.android.classes.data.ExperimentLogEntry
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers

/**
 * This repository will be used as @single truth for accessing movies.
 */
class ExperimentRepository(val context: Context) {

    private val experimentLogEntryDao =
        AffinityExperimentDatabase.getInstance(context).experimentLogEntryDao()

    /**
     * Adds the [recommendations] to the database.
     */
    fun addLogEntry(logEntry: ExperimentLogEntry): Completable {
        return Completable.create { observable ->
            experimentLogEntryDao.insert(logEntry)
            observable.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun getAllExperiments(): Flowable<List<Experiment>> {
        return experimentLogEntryDao.findAllExperiments()
            .subscribeOn(Schedulers.io())
    }

    fun getAllLogsForExperiment(experimentName: String): Flowable<List<ExperimentLogEntry>> {
        return experimentLogEntryDao.findAllFromExperimentWithName(experimentName = experimentName)
            .subscribeOn(Schedulers.io())
    }

    fun deleteExperiment(name: String): Completable {
        return Completable.create { observable ->
            experimentLogEntryDao.deleteExperiment(name)
            observable.onComplete()
        }.subscribeOn(Schedulers.io())
    }
}
