package com.project.speciesdetection.data.model.observation.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ObservationRepositoryModule {
    @Binds
    abstract fun bindRemoteObservationRepository(remoteObservationRepository: RemoteObservationRepository) : ObservationRepository
}