package com.project.speciesdetection.data.model.species_class.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeciesClassRepositoryModule {
    @Binds
    @Named("remote_species_class_repo")
    abstract fun bindRemoteSpeciesClassRepository(remoteSpeciesClassRepository: RemoteSpeciesClassRepository) : SpeciesClassRepository
}