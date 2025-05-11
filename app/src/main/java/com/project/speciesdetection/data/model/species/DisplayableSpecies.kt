package com.project.speciesdetection.data.model.species

data class DisplayableSpecies(
    val id: String,
    val localizedName: String,
    val localizedClass: String,
    /*val localizedDescription: String,*/
    val imageURL: String? = null
)