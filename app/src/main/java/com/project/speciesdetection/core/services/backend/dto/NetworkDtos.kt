package com.project.speciesdetection.core.services.backend.dto
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class ApiPaginationInfo(
    @SerialName("totalItems") val totalItems: Int,
    @SerialName("currentPage") val currentPage: Int,
    @SerialName("pageSize") val pageSize: Int,
    @SerialName("totalPages") val totalPages: Int,
    @SerialName("lastVisibleDocId") val lastVisibleDocId: String? = null,
    @SerialName("hasNextPage") val hasNextPage: Boolean = false
)

@Serializable
data class ApiPagedResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String,
    @SerialName("data") val data: List<DisplayableSpecies>,
    @SerialName("pagination") val pagination: ApiPaginationInfo
)

@Serializable
data class ApiSingleResponse<T>(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String,
    @SerialName("data") val data: T
)

@Serializable
data class ApiListResponse<T>(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null, // Có thể null
    @SerialName("data") val data: List<T>
)

/*@Serializable
data class ApiSpeciesClassDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: Map<String, String>
) {
    fun toDisplayable(languageCode: String): DisplayableSpeciesClass {
        val localized = this.name[languageCode]
            ?: this.name["en"]
            ?: this.id.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        return DisplayableSpeciesClass(
            id = this.id,
            localizedName = localized
        )
    }
}*/