package de.tub.affinity3.android.persistence.daos

import androidx.room.Dao
import androidx.room.Query
import de.tub.affinity3.android.classes.Search
import de.tub.affinity3.android.classes.data.Movie
import de.tub.affinity3.android.classes.data.MovieAllRatings
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface MovieDao : BaseDao<Movie> {

    @Query("SELECT * FROM movie")
    fun findAll(): List<Movie>

    @Query("SELECT imdbID, title, runtime, poster, genre, isOnWatchlist FROM movie")
    fun findAllWithRatings(): Flowable<List<MovieAllRatings>>

    @Query("SELECT * FROM movie")
    fun findAllAsync(): Flowable<List<Movie>>

    @Query("SELECT imdbID, title, runtime, poster, genre, isOnWatchlist FROM movie WHERE isOnWatchlist = 1")
    fun findAllOnWatchlist(): Flowable<List<MovieAllRatings>>

    @Query("SELECT * FROM movie WHERE imdbID = :id")
    fun findById(id: String): Flowable<Movie>

    @Query("SELECT imdbID, title, runtime, poster, genre, isOnWatchlist FROM movie WHERE imdbID IN (:idList)")
    fun findAllByIds(idList: List<String>): Flowable<List<MovieAllRatings>>

    @Query("SELECT * FROM movie WHERE imdbID = :id")
    fun findByIdSync(id: String): Movie?

    @Query("SELECT imdbID, title, year, type, poster FROM movie WHERE title LIKE '%' || :title || '%'")
    fun searchByTitle(title: String): Single<List<Search>>

    @Query("DELETE FROM movie")
    fun deleteAll()
}
