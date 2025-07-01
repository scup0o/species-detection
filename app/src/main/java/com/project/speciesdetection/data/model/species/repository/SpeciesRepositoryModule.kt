package com.project.speciesdetection.data.model.species.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeciesRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSpeciesRepository(
        // Khi ai đó cần SpeciesRepository, Hilt sẽ tạo CombinedSpeciesRepository.
        // Hilt sẽ tự tìm tất cả dependencies cho CombinedSpeciesRepository.
        // Bao gồm cả RemoteSpeciesRepository.
        combinedSpeciesRepository: CombinedSpeciesRepository
    ): SpeciesRepository}