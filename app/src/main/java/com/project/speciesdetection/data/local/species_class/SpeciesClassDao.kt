package com.project.speciesdetection.data.local.species_class

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesClassDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(speciesClasses: List<LocalSpeciesClass>)

    @Query("DELETE FROM species_class_local WHERE languageCode = :languageCode")
    suspend fun deleteByLanguage(languageCode: String)

    @Query("SELECT DISTINCT languageCode FROM species_class_local")
    fun getDownloadedLanguageCodes(): Flow<List<String>>

    @Query("SELECT * FROM species_class_local WHERE languageCode LIKE :languageCode")
    suspend fun getAllByLanguageSuspend(languageCode: String): List<LocalSpeciesClass>
}