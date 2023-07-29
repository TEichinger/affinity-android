package de.tub.affinity3.android.services

import de.tub.affinity3.android.classes.data.Recommendation
import io.reactivex.Observable

/**
 * This service calculates a list of movie recommendations based on the mined ratings of other users.
 */
abstract class RecommendationService {

    /**
     * Returns a [Observable] for Movie [Recommendation]s. Every time a new recommendation could be generated, a [Recommendation] event will be omitted.
     */
    abstract fun recommend(): Observable<List<Recommendation>>
}
