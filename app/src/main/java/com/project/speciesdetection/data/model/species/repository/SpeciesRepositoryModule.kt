package com.project.speciesdetection.data.model.species.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeciesRepositoryModule {
    @Binds
    abstract fun bindRemoteSpeciesRepository(remoteSpeciesRepository: RemoteSpeciesRepository) : SpeciesRepository
}