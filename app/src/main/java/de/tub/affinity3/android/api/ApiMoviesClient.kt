package de.tub.affinity3.android.api

import de.tub.affinity3.android.api.responses.MovieResponse
import de.tub.affinity3.android.api.responses.SearchResponse
import de.tub.affinity3.android.constants.AppConstants
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class ApiMoviesClient(baseUrl: String, apiKey: String) {

    companion object Factory {
        fun imdb(): ApiMoviesClient {
            return ApiMoviesClient(AppConstants.IMDB_API_BASE_URL, AppConstants.IMDB_API_KEY)
        }
    }

    private val serviceApi: ApiMoviesService

    init {
        serviceApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(createHttpClient(apiKey))
            .build()
            .create(ApiMoviesService::class.java)
    }

    private fun createHttpClient(apiKey: String): OkHttpClient {
        if (apiKey.isEmpty()) {
            throw IllegalArgumentException("The API key is missing. Please get one from http://www.omdbapi.com")
        }

        return OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .build()
    }

    fun searchMovies(
        searchKey: String = "",
        year: String = "",
        type: String = ""
    ): Observable<SearchResponse> {
        return serviceApi.searchMovies(searchKey, year, type)
    }

    fun getMovie(imdbId: String): Observable<MovieResponse> {
        return serviceApi.getMovie(imdbId)
    }
}
