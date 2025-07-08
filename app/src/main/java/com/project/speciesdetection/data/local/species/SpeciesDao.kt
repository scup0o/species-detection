package com.project.speciesdetection.data.local.species

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SpeciesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(speciesList: List<LocalSpecies>)

    @Query("DELETE FROM species_local WHERE languageCode = :languageCode")
    suspend fun deleteByLanguage(languageCode: String)

    @Query("""
        SELECT * FROM species_local 
        WHERE languageCode = :languageCode 
        AND (:searchQuery IS NULL OR localizedName LIKE '%' || :searchQuery || '%')
        AND (:classId IS NULL OR :classId = '' OR localizedClass = :classId)
    """)
    fun getSpeciesPagingSource(languageCode: String, searchQuery: String?, classId: String?): PagingSource<Int, LocalSpecies>

    @Query("SELECT * FROM species_local WHERE id = :id AND languageCode = :languageCode LIMIT 1")
    suspend fun getSpeciesById(id: String, languageCode: String): LocalSpecies?

    @Query("""
    SELECT * FROM species_local 
    WHERE languageCode = :languageCode 
    AND (:searchQuery IS NULL OR :searchQuery = '' OR localizedName LIKE '%' || :searchQuery || '%')
    AND (:classId IS NULL OR :classId = '' OR classId = :classId)
    -- <<< PHẦN ĐƯỢC SỬA ĐỔI >>>
    ORDER BY
        CASE WHEN :sortByDesc = 0 THEN localizedName END ASC,
        CASE WHEN :sortByDesc = 1 THEN localizedName END DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getSpeciesForPaging(
        sortByDesc : Boolean,
        languageCode: String,
        searchQuery: String?,
        classId: String?,
        limit: Int,
        offset: Int
    ): List<LocalSpecies>

    @Query("SELECT * FROM species_local WHERE languageCode = :languageCode")
    suspend fun getAllByLanguage(languageCode: String): List<LocalSpecies>

    @Query("SELECT * FROM species_local")
    suspend fun getAllSpecies(): List<LocalSpecies>
}