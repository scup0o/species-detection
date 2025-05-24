package com.project.speciesdetection.data.model.species

import com.google.firebase.database.PropertyName
import com.project.speciesdetection.data.model.species_class.SpeciesClass

data class Species(
    @get:PropertyName("documentId")
    var id: String = "",
    @get:PropertyName("classId")
    var classId: String = "",
    @get:PropertyName("name")
    var name: Map<String, String> = emptyMap(),
    @get:PropertyName("scientificName")
    var scientificName: String = "",
    @PropertyName("family")
    var family : Map<String, String> = emptyMap(),
    @get:PropertyName("imageURL")
    var imageURL: String? = null,
    @PropertyName("nameTokens")
    var nameTokens: Map<String, List<String>>? = null,
    @get:PropertyName("scientificNameToken")
    var scientificNameToken: List<String>? = null
) {
    //constructor() : this("", emptyMap(), emptyMap()/*, emptyMap(), emptyMap(), null*/)

    fun toDisplayable(languageCode: String): DisplayableSpecies {
        return DisplayableSpecies(
            id = this.id,
            localizedName =
                if (this.name[languageCode].isNullOrEmpty()) ""
                else this.name[languageCode]!!,
            localizedClass = this.classId,
            localizedFamily =
                        if (this.family[languageCode].isNullOrEmpty()) ""
                        else this.family[languageCode]!!,
            /*localizedDescription = this.getLocalizedDescription(languageCode),*/
            imageURL = this.imageURL,
            scientific = mapOf(
                "name" to this.scientificName,
                "family" to
                        if (this.family["scientific"].isNullOrEmpty()) ""
                        else this.family["scientific"]!!,
                )
        )
    }
}
