package com.project.speciesdetection.data.model.species.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.project.speciesdetection.data.local.species.SpeciesDao
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.project.speciesdetection.data.local.species.LocalSpeciesPagingSource
import com.project.speciesdetection.data.local.species.toDisplayable
import javax.inject.Named

class CombinedSpeciesRepository @Inject constructor(
    private val remoteRepository: RemoteSpeciesRepository, // Repo online gốc
    private val localDao: SpeciesDao, // DAO để truy cập dữ liệu offline
    private val connectivityObserver: ConnectivityObserver,
    @Named("language_provider") private val languageProvider: LanguageProvider
) : SpeciesRepository {
    companion object {
        private const val TAG = "CombinedSpeciesRepo"
    }

    override fun getAll(
        sortByDesc: Boolean,
        uid: String?,
        searchQuery: List<String>?,
        languageCode: String
    ): Flow<PagingData<DisplayableSpecies>> {
        return connectivityObserver.observe().flatMapLatest { status ->
            if (status == ConnectivityObserver.Status.Available) {
                Log.d(TAG, "getAll: Network Available. Using remote.")
                getRemoteAll(uid, searchQuery, languageCode, sortByDesc)
            } else {
                Log.d(TAG, "getAll: Network Unavailable. Using local.")
                getLocalAll(searchQuery, sortByDesc)
            }
        }
    }

    private fun getRemoteAll(uid: String?, searchQuery: List<String>?, languageCode: String, sortByDesc: Boolean,): Flow<PagingData<DisplayableSpecies>> {
        return remoteRepository.getAll(sortByDesc, uid, searchQuery, languageCode)
    }

    private fun getLocalAll(searchQuery: List<String>?, sortByDesc: Boolean): Flow<PagingData<DisplayableSpecies>> {
        val currentLanguage = languageProvider.getCurrentLanguageCode()
        val queryStr = searchQuery?.joinToString(" ")?.trim()
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { LocalSpeciesPagingSource(localDao, currentLanguage, queryStr, null, sortByDesc) }
        ).flow.map { pagingData ->
            pagingData.map { localSpecies -> localSpecies.toDisplayable() }
        }
    }


    override fun getSpeciesByClassPaged(
        sortByDesc: Boolean,
        uid: String?,
        searchQuery: List<String>?,
        classIdValue: String,
        languageCode: String
    ): Flow<PagingData<DisplayableSpecies>> {
        return connectivityObserver.observe().flatMapLatest { status ->
            if (status == ConnectivityObserver.Status.Available) {
                Log.d(TAG, "getByClass: Network Available. Using remote.")
                getRemoteByClass(uid, searchQuery, classIdValue, languageCode, sortByDesc)
            } else {
                Log.d(TAG, "getByClass: Network Unavailable. Using local.")
                getLocalByClass(searchQuery, classIdValue, sortByDesc)
            }
        }
    }

    private fun getRemoteByClass(uid: String?, searchQuery: List<String>?, classIdValue: String, languageCode: String, sortByDesc: Boolean,): Flow<PagingData<DisplayableSpecies>> {
        return remoteRepository.getSpeciesByClassPaged(sortByDesc, uid, searchQuery, classIdValue, languageCode)
    }

    private fun getLocalByClass(searchQuery: List<String>?, classIdValue: String, sortByDesc:Boolean = false ): Flow<PagingData<DisplayableSpecies>> {
        val currentLanguage = languageProvider.getCurrentLanguageCode()
        val queryStr = searchQuery?.joinToString(" ")?.trim()
        Log.i("class", classIdValue)
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                LocalSpeciesPagingSource(localDao, currentLanguage, queryStr, if (classIdValue == "0") null else classIdValue, sortByDesc)
            }
        ).flow.map { pagingData ->
            pagingData.map { localSpecies -> localSpecies.toDisplayable() }
        }
    }

    /**
     * Lấy danh sách các loài theo ID.
     * Fallback về local nếu không có mạng.
     */
    override suspend fun getSpeciesById(
        uid: String?,
        idList: List<String>,
        languageCode: String
    ): List<DisplayableSpecies> {
        if (connectivityObserver.getCurrentStatus() == ConnectivityObserver.Status.Available) {
            return remoteRepository.getSpeciesById(uid, idList, languageCode)
        } else {
            // Lấy từng cái một từ local DB
            return idList.mapNotNull { id ->
                localDao.getSpeciesById(id, languageCode)?.toDisplayable()
            }
        }
    }

    /**
     * Lấy chi tiết một loài.
     * Fallback về local nếu không có mạng.
     */
    override suspend fun getSpeciesDetails(
        speciesDocId: String,
        languageCode: String
    ): DisplayableSpecies? {
        if (connectivityObserver.getCurrentStatus() == ConnectivityObserver.Status.Available) {
            return remoteRepository.getSpeciesDetails(speciesDocId, languageCode)
        } else {
            return localDao.getSpeciesById(speciesDocId, languageCode)?.toDisplayable()
        }
    }
}