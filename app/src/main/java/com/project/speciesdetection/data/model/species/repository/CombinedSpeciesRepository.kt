package com.project.speciesdetection.data.model.species.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.project.speciesdetection.core.services.backend.species.SpeciesApiService
import com.project.speciesdetection.data.OfflineDataRepository
import com.project.speciesdetection.data.local.species.LocalSpeciesPagingSource
import com.project.speciesdetection.data.local.species.SpeciesDao
import com.project.speciesdetection.data.local.species.toDisplayable
import com.project.speciesdetection.data.local.species.toLocal
import com.project.speciesdetection.data.local.species_class.LocalSpeciesClass
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.repository.SpeciesClassRepository
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named

class CombinedSpeciesRepository @Inject constructor(
    private val remoteRepository: RemoteSpeciesRepository,
    @Named("remote_species_class_repo") private val speciesClassRepository: SpeciesClassRepository,
    private val localDao: SpeciesDao,
    private val speciesClassDao: SpeciesClassDao,
    private val connectivityObserver: ConnectivityObserver,
    @Named("language_provider") private val languageProvider: LanguageProvider,
    private val offlineDataRepository: OfflineDataRepository,
    private val speciesApiService: SpeciesApiService
) : SpeciesRepository {

    companion object {
        private const val TAG = "CombinedSpeciesRepo"
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    // Cờ để đảm bảo việc đồng bộ chỉ chạy một lần mỗi khi ứng dụng được mở
    private val isSyncTriggered = AtomicBoolean(false)

    override fun getAll(
        sortByDesc: Boolean,
        uid: String?,
        searchQuery: List<String>?,
        languageCode: String
    ): Flow<PagingData<DisplayableSpecies>> {
        return connectivityObserver.observe().flatMapLatest { status ->
            if (status == ConnectivityObserver.Status.Available) {
                getRemoteAll(uid, searchQuery, languageCode, sortByDesc)
            } else {
                getLocalAll(searchQuery, sortByDesc)
            }
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
                // Tái sử dụng hàm getRemote, vì nó cũng kích hoạt đồng bộ
                getRemoteByClass(uid, searchQuery, classIdValue, languageCode, sortByDesc)
            } else {
                val classScientific = speciesClassDao
                getLocalByClass(searchQuery, classIdValue, sortByDesc, languageCode)
            }
        }
    }

    private fun getRemoteAll(uid: String?, searchQuery: List<String>?, languageCode: String, sortByDesc: Boolean): Flow<PagingData<DisplayableSpecies>> {
        val remotePagingFlow = remoteRepository.getAll(sortByDesc, uid, searchQuery, languageCode)
        // Kích hoạt đồng bộ một lần khi flow bắt đầu
        return remotePagingFlow.onStart {
            if (localDao.getAllByLanguage(languageCode).isNotEmpty()){
                triggerFullSyncIfNecessary(languageCode)
            }
        }
    }

    private fun getRemoteByClass(uid: String?, searchQuery: List<String>?, classIdValue: String, languageCode: String, sortByDesc: Boolean): Flow<PagingData<DisplayableSpecies>> {
        return remoteRepository.getSpeciesByClassPaged(sortByDesc, uid, searchQuery, classIdValue, languageCode)
        // Kích hoạt đồng bộ một lần khi flow bắt đầu
         /*remotePagingFlow.onStart {
            triggerFullSyncIfNecessary(languageCode)
        }*/
    }

    /**
     * Hàm chính điều khiển việc đồng bộ. Nó kiểm tra cờ và khởi chạy tác vụ nền.
     */
    private fun triggerFullSyncIfNecessary(languageCode: String) {
        // Chỉ chạy nếu cờ là false, và đặt nó thành true ngay lập tức để ngăn luồng khác chạy
        if (isSyncTriggered.compareAndSet(false, true)) {
            repositoryScope.launch {
                Log.i(TAG, ">>> Starting ONE-TIME FULL SYNC for language: $languageCode <<<")
                try {
                    // Bước 1: Đồng bộ SpeciesClass theo đúng ngôn ngữ
                    // <<< SỬA LỖI: Truyền languageCode vào đây >>>
                    syncSpeciesClasses(languageCode)

                    // Bước 2: Đồng bộ Species cho ngôn ngữ hiện tại
                    syncSpecies(languageCode)

                    Log.i(TAG, ">>> ONE-TIME FULL SYNC finished successfully. <<<")

                } catch (e: Exception) {
                    Log.e(TAG, ">>> ONE-TIME FULL SYNC FAILED <<<", e)
                    // Đặt lại cờ nếu có lỗi, để có thể thử lại ở lần gọi sau
                    isSyncTriggered.set(false)
                }
            }
        }
    }

    /**
     * Logic đồng bộ cho SpeciesClass (Thêm, Sửa, Xóa).
     * <<< SỬA LỖI: Hàm này giờ nhận languageCode để chỉ đồng bộ đúng ngôn ngữ đó >>>
     */
    private suspend fun syncSpeciesClasses(languageCode: String) {
        Log.d(TAG, "Syncing SpeciesClass for language '$languageCode'...")
        val remoteClasses = speciesClassRepository.getAll()
        val remoteClassIds = remoteClasses.map { it.id }.toSet()
        val localClassIds = speciesClassDao.getAllIds().toSet()

        // Xóa những lớp không còn trên server (Logic này đúng, giữ nguyên)
        val classIdsToDelete = localClassIds.filter { !remoteClassIds.contains(it) }
        if (classIdsToDelete.isNotEmpty()) {
            Log.d(TAG, "Deleting orphan classes: $classIdsToDelete")
            speciesClassDao.deleteByIds(classIdsToDelete)
        }

        // <<< SỬA LỖI LỚN Ở ĐÂY >>>
        // Thêm/Cập nhật các lớp CHỈ CHO NGÔN NGỮ HIỆN TẠI
        val classesToUpsert = remoteClasses.mapNotNull { remoteClass ->
            // Chỉ lấy tên của ngôn ngữ cần đồng bộ
            remoteClass.name[languageCode]?.let { localizedName ->
                LocalSpeciesClass(
                    id = remoteClass.id,
                    languageCode = languageCode,
                    localizedName = localizedName,
                    scientific = remoteClass.name["scientific"]?:""
                )
            }
        }

        if (classesToUpsert.isNotEmpty()){
            speciesClassDao.upsertAll(classesToUpsert)
        }
        Log.d(TAG, "SpeciesClass sync finished for language '$languageCode'.")
    }

    /**
     * Logic đồng bộ cho Species của một ngôn ngữ (Thêm, Sửa, Xóa).
     */
    private suspend fun syncSpecies(languageCode: String) {
        Log.d(TAG, "Syncing Species for language '$languageCode'...")
        val remoteSpeciesResponse = speciesApiService.getAllSpeciesForLanguage(languageCode)
        if (!remoteSpeciesResponse.success) {
            throw IllegalStateException("Failed to fetch species for sync")
        }
        val remoteSpecies = remoteSpeciesResponse.data
        val remoteSpeciesIds = remoteSpecies.map { it.id }.toSet()
        val localSpeciesIds = localDao.getAllIdsByLanguage(languageCode).toSet()

        // Xóa những loài không còn trên server
        val speciesIdsToDelete = localSpeciesIds.filter { !remoteSpeciesIds.contains(it) }
        if (speciesIdsToDelete.isNotEmpty()) {
            Log.d(TAG, "Deleting orphan species: $speciesIdsToDelete")
            localDao.deleteByIds(speciesIdsToDelete)
        }

        // Thêm/Cập nhật tất cả loài từ server, bao gồm tải ảnh
        if (remoteSpecies.isNotEmpty()) {
            val speciesToUpsert = remoteSpecies.map { ds ->
                val thumb = offlineDataRepository.downloadAndSaveImageIfNotExists(ds.id, ds.thumbnailImageURL, "thumbnail")
                val images = offlineDataRepository.downloadImagesForSpeciesIfNotExists(ds.id, ds.imageURL)
                ds.toLocal(languageCode).copy(imageURL = images, thumbnailImageURL = thumb ?: "")
            }
            localDao.upsertAll(speciesToUpsert)
        }
        Log.d(TAG, "Species sync finished for language '$languageCode'.")
    }


    // --- CÁC HÀM LẤY DỮ LIỆU LOCAL VÀ CHI TIẾT ---
    private fun getLocalAll(searchQuery: List<String>?, sortByDesc: Boolean): Flow<PagingData<DisplayableSpecies>> {
        val currentLanguage = languageProvider.getCurrentLanguageCode()
        val queryStr = searchQuery?.joinToString(" ")?.trim()
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { LocalSpeciesPagingSource(localDao, currentLanguage, queryStr, null, sortByDesc) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDisplayable() }
        }
    }

    private suspend fun getLocalByClass(searchQuery: List<String>?, classIdValue: String, sortByDesc:Boolean = false , languageCode: String): Flow<PagingData<DisplayableSpecies>> {
        val currentLanguage = languageProvider.getCurrentLanguageCode()
        val queryStr = searchQuery?.joinToString(" ")?.trim()
        val scientific = speciesClassDao.getByScientific(classIdValue, languageCode = languageCode)
        //Log.i("local_name",scientific)
        //Log.i("local_id",classIdValue)
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                LocalSpeciesPagingSource(localDao, currentLanguage, queryStr, if (classIdValue == "0") null else scientific, sortByDesc)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDisplayable() }
        }
    }

    override suspend fun getSpeciesDetails(speciesDocId: String, languageCode: String): DisplayableSpecies? {
        return if (connectivityObserver.getCurrentStatus() == ConnectivityObserver.Status.Available) {
            remoteRepository.getSpeciesDetails(speciesDocId, languageCode)
        } else {
            localDao.getSpeciesById(speciesDocId, languageCode)?.toDisplayable()
        }
    }

    override suspend fun getSpeciesById(uid: String?, idList: List<String>, languageCode: String): List<DisplayableSpecies> {
        return if (connectivityObserver.getCurrentStatus() == ConnectivityObserver.Status.Available) {
            remoteRepository.getSpeciesById(uid, idList, languageCode)
        } else {
            idList.mapNotNull { id ->
                localDao.getSpeciesById(id, languageCode)?.toDisplayable()
            }
        }
    }
}