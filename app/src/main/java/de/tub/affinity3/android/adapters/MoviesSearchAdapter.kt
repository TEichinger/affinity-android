package de.tub.affinity3.android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.Search
import de.tub.affinity3.android.util.PicassoTrustAll
import kotlinx.android.synthetic.main.item_movie_search.view.imagePoster
import kotlinx.android.synthetic.main.item_movie_search.view.textTitle
import kotlinx.android.synthetic.main.item_movie_search.view.textType
import kotlinx.android.synthetic.main.item_movie_search.view.textYear

class MoviesSearchAdapter(
    private var movies: List<Search>,
    private val listener: MovieItemClickListener
) : RecyclerView.Adapter<MoviesSearchAdapter.ViewHolder>() {

    interface MovieItemClickListener {
        fun onItemClicked(movie: Search)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_movie_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = movies[position]

        PicassoTrustAll.getInstance(holder.view.context)
            ?.load(movie.poster)
            ?.placeholder(R.drawable.ic_movie_placeholder)
            ?.into(holder.view.imagePoster)

        with(holder.view) {
            setOnClickListener {
                listener.onItemClicked(movie)
            }
        }

        holder.view.textTitle.text = movie.title

        holder.view.textYear.text = movie.year

        holder.view.textType.text = movie.type
    }

    fun replaceData(newMovies: List<Search>) {
        movies = newMovies
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = movies.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}
