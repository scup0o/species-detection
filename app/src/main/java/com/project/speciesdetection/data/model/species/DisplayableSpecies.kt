package com.project.speciesdetection.data.model.species

data class DisplayableSpecies(
    val id: String,
    var localizedName: String,
    var localizedClass: String,
    var scientificName: String,
    var localizedFamily : String,
    /*val localizedDescription: String,*/
    var imageURL: String? = null
)