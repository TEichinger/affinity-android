package de.tub.affinity3.android.api

import de.tub.affinity3.android.api.responses.MovieResponse
import de.tub.affinity3.android.api.responses.SearchResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiMoviesService {
    @GET("/")
    fun searchMovies(
        @Query("s") searchKey: String,
        @Query("y") year: String,
        @Query("type") type: String
    ): Observable<SearchResponse>

    @GET("/")
    fun getMovie(
        @Query("i") id: String
    ): Observable<MovieResponse>
}
