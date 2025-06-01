package com.project.speciesdetection.data.model.species_class

import com.project.speciesdetection.R

data class DisplayableSpeciesClass(
    val id: String,
    val localizedName : String,
) {
    fun getIcon() :Int?{
        return when (this.id){
            "mammalia" -> R.drawable.mammal
            else -> {null}
        }
    }
}