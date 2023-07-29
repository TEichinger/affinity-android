package de.tub.affinity3.android.api

import org.junit.Test

class ApiServiceTest {

    @Test
    fun testGetMovie() {
        val client = ApiMoviesClient.imdb()
        val movie = client.getMovie("tt0372784").blockingFirst()
        assert(movie.title == "Batman Begins")
        assert(movie.imdbId == "tt0372784")
    }

    @Test
    fun testSearchMovie() {
        val client = ApiMoviesClient.imdb()
        val searchResult = client.searchMovies("Batman").blockingFirst()
        assert(searchResult.search.isNullOrEmpty().not())
    }
}
