package com.project.speciesdetection.data.model.species

import com.google.firebase.database.PropertyName

data class Species(
    @get:PropertyName("documentId")
    var id: String = "",
    @get:PropertyName("classId")
    var classId: String = "",
    @get:PropertyName("name")
    var name: Map<String, String> = emptyMap(),
    @get:PropertyName("scientificName")
    var scientificName: String = "",
    /*@get:PropertyName("description") @set:PropertyName("description")
    var descriptionTranslations: Map<String, String> = emptyMap(),*/
    @get:PropertyName("imageURL")
    var imageURL: String? = null
) {
    //constructor() : this("", emptyMap(), emptyMap()/*, emptyMap(), emptyMap(), null*/)

    fun toDisplayable(languageCode: String): DisplayableSpecies {
        return DisplayableSpecies(
            id = this.id,
            localizedName = this.name[languageCode]!!,
            localizedClass = this.classId,
            /*localizedDescription = this.getLocalizedDescription(languageCode),*/
            imageURL = this.imageURL,
            scientificName = this.scientificName
        )
    }
}
