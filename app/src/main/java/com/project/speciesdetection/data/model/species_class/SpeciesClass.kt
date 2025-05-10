package com.project.speciesdetection.data.model.species_class

import com.google.firebase.database.PropertyName

data class SpeciesClass(
    @get:PropertyName("documentId") @set:PropertyName("documentId")
    var id : String = "",
    @get:PropertyName("name") @set:PropertyName("name")
    var name : Map<String, String> = emptyMap(),
    @get:PropertyName("icon") @set:PropertyName("icon")
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