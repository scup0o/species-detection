package com.project.speciesdetection.data.model.species_class

import com.google.firebase.database.PropertyName

data class SpeciesClass(
    @get:PropertyName("documentId")
    var id : String = "",
    @get:PropertyName("name")
    var name : Map<String, String> = emptyMap(),
    @get:PropertyName("icon")
    var icon : String = ""
) {

    fun toDisplayable(languageCode: String) : DisplayableSpeciesClass{
        return DisplayableSpeciesClass(
            id = this.id,
            localizedName = this.name[languageCode]!!,
            icon = this.icon
        )
    }
}