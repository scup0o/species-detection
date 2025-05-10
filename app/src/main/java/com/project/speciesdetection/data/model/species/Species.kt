package com.project.speciesdetection.data.model.species

import com.google.firebase.database.PropertyName

data class Species(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("class") @set:PropertyName("class")
    var speciesClass: Map<String, String> = emptyMap(),
    @get:PropertyName("name") @set:PropertyName("name")
    var name: Map<String, String> = emptyMap(),
    /*@get:PropertyName("description") @set:PropertyName("description")
    var descriptionTranslations: Map<String, String> = emptyMap(),
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String? = null*/
) {
    constructor() : this("", emptyMap(), emptyMap()/*, emptyMap(), emptyMap(), null*/)

    fun toDisplayable(languageCode: String): DisplayableSpecies {
        return DisplayableSpecies(
            id = this.id,
            localizedName = this.name[languageCode]!!,
            localizedClass = this.speciesClass[languageCode]!!,
            /*localizedDescription = this.getLocalizedDescription(languageCode),
            imageUrl = this.imageUrl*/
        )
    }
}
