package de.tub.affinity3.android.ui.movielist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.MovieAllRatings
import de.tub.affinity3.android.classes.data.Rating
import de.tub.affinity3.android.constants.ListType
import de.tub.affinity3.android.util.PicassoTrustAll
import de.tub.affinity3.android.util.getDeviceId
import de.tub.affinity3.android.util.visible
import kotlinx.android.synthetic.main.item_movie.view.*

class MoviesAdapter(
    private var listType: ListType = ListType.LIST,
    private var movies: List<MovieAllRatings>,
    private val listener: MovieItemClickListener
) : RecyclerView.Adapter<MoviesAdapter.ViewHolder>() {

    interface MovieItemClickListener {
        fun onItemClicked(movie: MovieAllRatings)
        fun onAddToWatchlistClicked(movie: MovieAllRatings, position: Int)
        fun onRatingClicked(movie: MovieAllRatings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = if (listType == ListType.LIST) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deviceUserId = getDeviceId(holder.view.context)
        val movie = movies[position]
        // only show own ratings
        val movieRatings = movie.ratings.filter { rating: Rating -> rating.userId == deviceUserId }

        PicassoTrustAll.getInstance(holder.view.context)
            ?.load(movie.poster)
            ?.placeholder(R.drawable.ic_movie_placeholder)
            ?.into(holder.view.imagePoster)

        with(holder.view) {
            setOnClickListener {
                listener.onItemClicked(movie)
            }
        }

        if (listType == ListType.GRID) {
            holder.view.ratedMask.visible = movieRatings.isNotEmpty()
            holder.view.textRating.visible = movieRatings.isNotEmpty()
        }

        if (movieRatings.isNotEmpty()) {
            val rating = movieRatings.first()
            holder.view.textRating.text = rating.score.toString()
        } else {
            holder.view.textRating.text = "--"
        }

        if (listType != ListType.LIST) {
            return
        }

        // only for list
        holder.view.textTitle.text = movie.title
        holder.view.textDuration.text = movie.runtime
        holder.view.textGenre.text = movie.genre

        holder.view.buttonAddToWatchlist.apply {
            if (movie.isOnWatchlist) {
                setImageResource(R.drawable.ic_bookmark_enabled)
            } else {
                setImageResource(R.drawable.ic_bookmark_disabled)
            }

            setOnClickListener {
                listener.onAddToWatchlistClicked(movie, position)
            }
        }

        holder.view.buttonRating.apply {
            setOnClickListener {
                listener.onRatingClicked(movie)
            }
        }
    }

    fun replaceData(newMovies: List<MovieAllRatings>) {
        movies = newMovies
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = movies.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}
