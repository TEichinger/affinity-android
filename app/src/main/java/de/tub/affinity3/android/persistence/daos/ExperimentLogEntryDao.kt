package de.tub.affinity3.android.persistence.daos

import androidx.room.Dao
import androidx.room.Query
import de.tub.affinity3.android.classes.data.Experiment
import de.tub.affinity3.android.classes.data.ExperimentLogEntry
import de.tub.affinity3.android.persistence.daos.BaseDao
import io.reactivex.Flowable

@Dao
interface ExperimentLogEntryDao : BaseDao<ExperimentLogEntry> {
    @Query("SELECT * FROM experimentlogs")
    fun findAll(): Flowable<List<ExperimentLogEntry>>

    @Query("SELECT * FROM experimentlogs WHERE experimentName = :experimentName")
    fun findAllFromExperimentWithName(experimentName: String): Flowable<List<ExperimentLogEntry>>

    @Query("SELECT experimentName as name, count(*) as logEntries FROM experimentlogs GROUP BY experimentName")
    fun findAllExperiments(): Flowable<List<Experiment>>

    @Query("DELETE FROM experimentlogs WHERE experimentName = :experimentName")
    fun deleteExperiment(experimentName: String)

    @Query("DELETE FROM experimentlogs")
    fun deleteAll()
}
