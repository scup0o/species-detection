package com.project.speciesdetection.data.model.species

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import com.project.speciesdetection.core.helpers.TimestampSerializer
import kotlinx.serialization.Serializable

@Serializable
data class DisplayableSpecies(
    var id: String = "",
    var info: Map<String, String> = emptyMap(),
    var conservation: String = "",
    var localizedName: String = "",
    var localizedGenus: String = "",
    var localizedFamily : String = "",
    var localizedOrder: String = "",
    var localizedClass: String = "",
    var localizedPhylum : String = "",
    var localizedKingdom : String = "",
    var localizedDomain : String = "",
    var scientific: Map<String, String> = emptyMap(),
    var localizedSummary : List<String> = emptyList(),
    var localizedPhysical: List<String> = emptyList(),
    var localizedHabitat: List<String> = emptyList(),
    var localizedDistribution: List<String> = emptyList(),
    var localizedBehavior: List<String> = emptyList(),
    var thumbnailImageURL : String = "",
    var imageURL: List<String> = emptyList(),
    /*var haveObservation : Boolean = false,
    @Serializable(with = TimestampSerializer::class)
    var firstFound : Timestamp? = null*/
){
    fun getScientificName() = this.scientific["name"]
    fun getScientificGenus() = this.scientific["genus"]
    fun getScientificFamily() = this.scientific["family"]
    fun getScientificOrder() = this.scientific["order"]
    fun getScientificClass() = this.scientific["class"]
    fun getScientificPhylum() = this.scientific["phylum"]
    fun getScientificKingdom() = this.scientific["kingdom"]
    fun getScientificDomain() = this.scientific["domain"]

}