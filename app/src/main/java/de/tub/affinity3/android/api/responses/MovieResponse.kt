package de.tub.affinity3.android.api.responses

import com.google.gson.annotations.SerializedName
import de.tub.affinity3.android.classes.data.Movie

data class MovieResponse(
    @SerializedName("imdbID") val imdbId: String,
    @SerializedName("Title") val title: String,
    @SerializedName("Year") var year: String,
    @SerializedName("Genre") var genre: String,
    @SerializedName("Actors") val actors: String,
    @SerializedName("Plot") val plot: String,
    @SerializedName("Writer") var writer: String? = null,
    @SerializedName("Runtime") var runtime: String? = null,
    @SerializedName("Director") var director: String? = null,
    @SerializedName("imdbRating") val imdbRating: String,
    @SerializedName("imdbVotes") var imdbVotes: String? = null,
    @SerializedName("Poster") val poster: String,
    @SerializedName("Language") var language: String? = null,
    @SerializedName("Metascore") var metascore: String? = null,
    @SerializedName("Type") var type: String? = null
) {
    fun toPersistenceMovie(): Movie {
        return Movie(
            title = this.title,
            plot = this.plot,
            imdbID = this.imdbId,
            actors = this.actors,
            poster = this.poster,
            imdbRating = this.imdbRating,
            imdbVotes = this.imdbVotes,
            genre = this.genre,
            year = this.year,
            language = this.language,
            writer = this.writer,
            type = this.type,
            metascore = this.metascore,
            director = this.director,
            runtime = this.runtime
        )
    }
}
