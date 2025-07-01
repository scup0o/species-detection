package com.project.speciesdetection.data.local.species_class
import androidx.room.Entity
@Entity(
    tableName = "species_class_local",
    primaryKeys = ["id", "languageCode"] // Sử dụng composite primary key để đảm bảo mỗi lớp loài
    // chỉ có một bản ghi duy nhất cho mỗi ngôn ngữ.
)
data class LocalSpeciesClass(
    val id: String,
    val languageCode: String,
    val localizedName: String
)