package de.tub.affinity3.android.api

import de.tub.affinity3.android.constants.AppConstants
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val url = request.url
            .newBuilder()
            .addQueryParameter(AppConstants.IMDB_API_KEY_QUERY_PARAM, apiKey)
            .build()

        val newRequest = request
            .newBuilder()
            .url(url)
            .build()

        return chain.proceed(newRequest)
    }
}
