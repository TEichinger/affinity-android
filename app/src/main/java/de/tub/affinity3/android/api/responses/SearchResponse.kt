package de.tub.affinity3.android.api.responses

import com.google.gson.annotations.SerializedName
import de.tub.affinity3.android.classes.Search

data class SearchResponse(
    @SerializedName("Search") val search: List<Search>?,
    @SerializedName("totalResults") val totalResults: String?,
    @SerializedName("Response") val response: String
)
