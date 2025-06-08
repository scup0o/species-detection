package com.project.speciesdetection.core.services.map

import android.util.Log
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

// Data class để parse response từ Nominatim
@Serializable
data class NominatimPlace(
    @SerialName("place_id")
    val placeId: Long,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("name")
    val name: String,
    @SerialName("lat")
    val lat: String,
    @SerialName("lon")
    val lon: String
)

interface GeocodingService {
    suspend fun search(query: String): List<NominatimPlace>
    suspend fun reverseSearch(lat: Double, lon: Double): NominatimPlace?
}

class NominatimGeocodingService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    @Named("language_provider") languageProvider: LanguageProvider
) : GeocodingService {

    private val currentLanguage = languageProvider.getCurrentLanguageCode()
    private val baseUrl = "https://nominatim.openstreetmap.org"

    override suspend fun search(query: String): List<NominatimPlace> = withContext(Dispatchers.IO) {
        if (query.length < 3) return@withContext emptyList()

        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("search")
            .addQueryParameter("q", query)
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("limit", "10")
            // Thêm tham số để kết quả có địa chỉ chi tiết hơn
            .addQueryParameter("addressdetails", "1")
            .build()

        try {
            // SỬA ĐỔI: Thêm header Accept-Language
            val request = Request.Builder()
                .url(url)
                // Yêu cầu kết quả bằng tiếng Anh (en) hoặc tiếng Việt (vi)
                .header("Accept-Language", "$currentLanguage,en,;q=0.9")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank()) {
                Log.w("NominatimService", "Search request failed. Code: ${response.code}")
                return@withContext emptyList()
            }

            if (body.contains("\"error\"")) {
                Log.e("NominatimService", "Nominatim returned an error for search: $body")
                return@withContext emptyList()
            }

            json.decodeFromString<List<NominatimPlace>>(body)
                .filter { it.displayName != null && it.lat != null && it.lon != null }

        } catch (e: Exception) {
            Log.e("NominatimService", "Exception during search for query: '$query'", e)
            emptyList()
        }
    }

    override suspend fun reverseSearch(lat: Double, lon: Double): NominatimPlace? {
        return withContext(Dispatchers.IO) {
            Log.i("b", "$lat , $lon")
            val url = baseUrl.toHttpUrl().newBuilder()
                .addPathSegment("reverse")
                .addQueryParameter("lat", lat.toString())
                .addQueryParameter("lon", lon.toString())
                .addQueryParameter("format", "json")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept-Language", "$currentLanguage,en,;q=0.9")
                .build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            return@withContext json.decodeFromString<NominatimPlace>(body)
            //return@withContext null
        }
    }
}