package com.project.speciesdetection.data.local.species

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesDao {


    /**
     * Chèn hoặc cập nhật một danh sách loài. Nếu loài đã tồn tại (dựa trên primary key),
     * nó sẽ được thay thế bằng dữ liệu mới.
     * Quan trọng cho việc đồng bộ.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(speciesList: List<LocalSpecies>)

    /**
     * Nhận vào một danh sách ID và trả về một Set chứa các ID thực sự
     * tồn tại trong cơ sở dữ liệu. Dùng cho việc kiểm tra đồng bộ.
     */
    @Query("SELECT id FROM species_local WHERE id IN (:speciesIds)")
    suspend fun getExistingIds(speciesIds: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(speciesList: List<LocalSpecies>)

    @Query("DELETE FROM species_local WHERE languageCode = :languageCode")
    suspend fun deleteByLanguage(languageCode: String)

    @Query("DELETE FROM species_local WHERE id IN (:speciesIds)")
    suspend fun deleteByIds(speciesIds: List<String>)

    @Query("SELECT id FROM species_local WHERE languageCode = :languageCode")
    suspend fun getAllIdsByLanguage(languageCode: String): List<String>

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
    AND (:classId IS NULL OR :classId = '' OR localizedClass = :classId)
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

    @Query("SELECT DISTINCT languageCode FROM species_local")
    fun getDownloadedLanguageCodes(): Flow<List<String>>
}