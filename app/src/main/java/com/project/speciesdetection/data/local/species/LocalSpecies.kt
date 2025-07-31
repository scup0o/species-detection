package com.project.speciesdetection.data.local.species

import androidx.room.Entity
import com.google.firebase.Timestamp
import com.project.speciesdetection.data.model.species.DisplayableSpecies

@Entity(
    tableName = "species_local",
    primaryKeys = ["id", "languageCode"]
)
data class LocalSpecies(
    var id: String,
    var languageCode: String,

    var localizedName: String,
    var localizedGenus: String,
    var localizedFamily : String,
    var localizedOrder: String,
    var localizedClass: String,
    var localizedPhylum : String,
    var localizedKingdom : String,
    var localizedDomain : String,
    var localizedSummary : List<String>,
    var localizedPhysical: List<String>,
    var localizedHabitat: List<String>,
    var localizedDistribution: List<String>,
    var localizedBehavior: List<String>,

    var info: Map<String, String>,
    var conservation: String,
    var scientific: Map<String, String>,
    var thumbnailImageURL : String,
    var imageURL: List<String>,
    var haveObservation : Boolean,
    var firstFound : Timestamp?,
    var classId : String,
    var otherConvo : Map<String, String> = emptyMap()

)

fun LocalSpecies.toDisplayable(): DisplayableSpecies {
    return DisplayableSpecies(
        id = this.id,
        localizedName = this.localizedName,
        localizedGenus = this.localizedGenus,
        localizedFamily = this.localizedFamily,
        localizedOrder = this.localizedOrder,
        localizedClass = this.localizedClass,
        localizedPhylum = this.localizedPhylum,
        localizedKingdom = this.localizedKingdom,
        localizedDomain = this.localizedDomain,
        localizedSummary = this.localizedSummary,
        localizedPhysical = this.localizedPhysical,
        localizedHabitat = this.localizedHabitat,
        localizedDistribution = this.localizedDistribution,
        localizedBehavior = this.localizedBehavior,
        info = this.info,
        conservation = this.conservation,
        scientific = this.scientific,
        thumbnailImageURL = this.thumbnailImageURL,
        imageURL = this.imageURL,
        haveObservation = this.haveObservation,
        firstFound = this.firstFound,
        otherConvo = this.otherConvo
    )
}

fun DisplayableSpecies.toLocal(languageCode: String): LocalSpecies {
    return LocalSpecies(
        id = this.id,
        languageCode = languageCode,
        localizedName = this.localizedName,
        localizedGenus = this.localizedGenus,
        localizedFamily = this.localizedFamily,
        localizedOrder = this.localizedOrder,
        localizedClass = this.localizedClass,
        localizedPhylum = this.localizedPhylum,
        localizedKingdom = this.localizedKingdom,
        localizedDomain = this.localizedDomain,
        localizedSummary = this.localizedSummary,
        localizedPhysical = this.localizedPhysical,
        localizedHabitat = this.localizedHabitat,
        localizedDistribution = this.localizedDistribution,
        localizedBehavior = this.localizedBehavior,
        info = this.info,
        conservation = this.conservation,
        scientific = this.scientific,
        thumbnailImageURL = this.thumbnailImageURL,
        imageURL = this.imageURL,
        haveObservation = false,
        firstFound = this.firstFound,
        classId = this.getScientificClass()?:"",
        otherConvo = this.otherConvo
    )
}