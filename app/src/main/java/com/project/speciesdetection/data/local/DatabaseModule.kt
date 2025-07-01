package com.project.speciesdetection.data.local

import android.content.Context
import androidx.room.Room
import com.project.speciesdetection.data.local.species.SpeciesDao
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "species_detection_db"
        )
            // Trong quá trình phát triển, dòng này sẽ xóa và tạo lại DB nếu schema thay đổi.
            // Đối với bản release, bạn cần implement Migration.
            .fallbackToDestructiveMigration(false)
            .build()
    }

    // --- Cung cấp các DAO mới ---

    @Provides
    @Singleton
    fun provideSpeciesClassDao(appDatabase: AppDatabase): SpeciesClassDao {
        return appDatabase.speciesClassDao()
    }

    @Provides
    @Singleton
    fun provideSpeciesDao(appDatabase: AppDatabase): SpeciesDao {
        return appDatabase.speciesDao()
    }
}