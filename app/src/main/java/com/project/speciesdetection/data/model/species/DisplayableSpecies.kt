package com.project.speciesdetection.data.model.species

data class DisplayableSpecies(
    val id: String,
    var localizedName: String,
    var localizedClass: String,
    var scientific: Map<String, String>,
    var localizedFamily : String,
    /*val localizedDescription: String,*/
    var imageURL: List<String>? = null
){
    fun getScientificName() = this.scientific["name"]
    fun getScientificFamily() = this.scientific["family"]
    fun getScientificClass() = this.scientific["class"]
}