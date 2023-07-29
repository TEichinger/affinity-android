package de.tub.affinity3.android.classes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.JsonObject

// Part of AffinityDatabase (./app/src/main/java/de/tub/affinity3/android/persistence/AffinityDatabase.kt)
@Entity
data class Movie(
    /**
     * Movie objects are abstractions of records in the Internet Movie Database (IMDb) (see https://www.imdb.com/).
     * Information on moview in the IMDb can be retrieved via the Open Media Databse (OMDd) API (see https://www.omdbapi.com/).
     *
     */

    // IMDB movie attributes
    @PrimaryKey @ColumnInfo(name = "imdbID") var imdbID: String,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "year") var year: String,
    @ColumnInfo(name = "genre") var genre: String,
    @ColumnInfo(name = "runtime") var runtime: String? = null,
    @ColumnInfo(name = "director") var director: String? = null,
    @ColumnInfo(name = "writer") var writer: String? = null,
    @ColumnInfo(name = "actors") var actors: String? = null,
    @ColumnInfo(name = "plot") var plot: String,
    @ColumnInfo(name = "language") var language: String? = null,
    @ColumnInfo(name = "imdbRating") var imdbRating: String? = null,
    @ColumnInfo(name = "imdbVotes") var imdbVotes: String? = null,
    @ColumnInfo(name = "metascore") var metascore: String? = null,
    @ColumnInfo(name = "type") var type: String? = null,
    @ColumnInfo(name = "poster") var poster: String? = null,

    // if true, movie has been added to watch list
    @ColumnInfo(name = "isOnWatchlist")
    var isOnWatchlist: Boolean = false

) {

    companion object {

        /**
         * Maps a [movieJsonObject] to a [Movie] entity.
         */
        fun fromJSON(movieJsonObject: JsonObject): Movie {
            return Movie(
                imdbID = movieJsonObject["imdbID"].asString,
                title = movieJsonObject["Title"].asString,
                year = movieJsonObject["Year"].asString,
                genre = movieJsonObject["Genre"].asString,
                runtime = movieJsonObject["Runtime"].asString,
                director = movieJsonObject["Director"].asString,
                imdbRating = movieJsonObject["imdbRating"].asString,
                imdbVotes = movieJsonObject["imdbVotes"].asString,
                metascore = movieJsonObject["Metascore"].asString,
                type = movieJsonObject["Type"].asString,
                language = movieJsonObject["Language"].asString,
                writer = movieJsonObject["Writer"].asString,
                actors = movieJsonObject["Actors"].asString,
                plot = movieJsonObject["Plot"].asString,
                poster = movieJsonObject["Poster"].asString
            )
        }
    }
}

fun Movie.toMovieAllRatings(): MovieAllRatings {
    return MovieAllRatings(
        imdbID = this.imdbID,
        title = this.title,
        runtime = this.runtime,
        poster = this.poster,
        genre = this.genre,
        isOnWatchlist = false
    )
}
