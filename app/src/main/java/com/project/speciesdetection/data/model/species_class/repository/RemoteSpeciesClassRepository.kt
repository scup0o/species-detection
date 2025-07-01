package com.project.speciesdetection.data.model.species_class.repository

import android.util.Log
import com.project.speciesdetection.core.services.backend.message.MessageApiService
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.SpeciesClassDatabaseService
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Named

class RemoteSpeciesClassRepository @Inject constructor(
    @Named("species_class_db") private val databaseService: SpeciesClassDatabaseService<SpeciesClass, String>,
    private val localDao: SpeciesClassDao,
    private val connectivityObserver: ConnectivityObserver,
    // Inject LanguageProvider vào đây để nó tự biết ngôn ngữ hiện tại
    @Named("language_provider") private val languageProvider: LanguageProvider,
) : SpeciesClassRepository {

    companion object {
        private const val TAG = "FinalSpeciesClassRepo"
    }

    /**
     * Hàm chính để UseCase sử dụng.
     * Logic được làm lại hoàn toàn để đơn giản và chính xác.
     */
    override fun getAllSpeciesClass(): Flow<DataResult<List<SpeciesClass>>> = flow {
        emit(DataResult.Loading)

        if (connectivityObserver.getCurrentStatus() == ConnectivityObserver.Status.Available) {
            // CÓ MẠNG: Luôn cố gắng lấy từ remote
            try {
                Log.d(TAG, "Network available. Fetching from remote.")
                val remoteData = databaseService.getAll()
                emit(DataResult.Success(remoteData))
            } catch (e: Exception) {
                Log.e(TAG, "Remote fetch failed, trying local as fallback.", e)
                // Nếu remote lỗi, thử lấy từ local
                emit(fetchFromLocal())
            }
        } else {
            // KHÔNG CÓ MẠNG: Chỉ lấy từ local
            Log.d(TAG, "Network unavailable. Fetching from local.")
            emit(fetchFromLocal())
        }
    }

    /**
     * Hàm helper mới, trả về DataResult trực tiếp.
     */
    private suspend fun fetchFromLocal(): DataResult<List<SpeciesClass>> {
        // Sử dụng hàm suspend fun mới trong DAO
        val allLocalClasses = localDao.getAllByLanguageSuspend("%")

        if (allLocalClasses.isNotEmpty()) {
            Log.i(TAG, "Found ${allLocalClasses.size} records in local DB. Grouping them.")
            // Gộp chúng lại thành cấu trúc SpeciesClass
            val groupedData = allLocalClasses
                .groupBy { it.id }
                .map { (id, localClasses) ->
                    val nameMap = localClasses.associate { it.languageCode to it.localizedName }
                    SpeciesClass(id = id, name = nameMap)
                }
            return DataResult.Success(groupedData)
        } else {
            Log.w(TAG, "Local DB is empty for species classes.")
            return DataResult.Error(Exception("No cached data available."))
        }
    }


    /**
     * Hàm này được OfflineDataRepository sử dụng.
     * Nó chỉ nên lấy từ remote. Nếu lỗi thì ném ra exception.
     */
    override suspend fun getAll(): List<SpeciesClass> {
        Log.d(TAG, "getAll (suspend) called. Fetching directly from remote.")
        try {
            return databaseService.getAll()
        } catch (e: Exception) {
            Log.e(TAG, "getAll (suspend) failed.", e)
            throw e // Ném lỗi để OfflineDataRepository biết và xử lý
        }
    }
}