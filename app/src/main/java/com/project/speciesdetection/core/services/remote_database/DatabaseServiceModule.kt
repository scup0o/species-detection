package com.project.speciesdetection.core.services.remote_database

import com.project.speciesdetection.core.services.remote_database.firestore.FirestoreSpeciesService
import com.project.speciesdetection.data.model.species.Species
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseServiceModule {
    @Binds
    @Singleton
    abstract fun bindFireStoreSpeciesDatabaseService(
        fireStoreSpeciesService: FirestoreSpeciesService
    ): DatabaseService<Species, String>

}