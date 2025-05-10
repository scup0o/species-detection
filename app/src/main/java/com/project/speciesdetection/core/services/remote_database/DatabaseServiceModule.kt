package com.project.speciesdetection.core.services.remote_database

import com.project.speciesdetection.core.services.remote_database.firestore.FirestoreSpeciesClassService
import com.project.speciesdetection.core.services.remote_database.firestore.FirestoreSpeciesService
import com.project.speciesdetection.data.model.species.Species
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseServiceModule {
    @Binds
    @Singleton
    @Named("firestore_species_db")
    abstract fun bindFireStoreSpeciesDatabaseService(
        fireStoreSpeciesService: FirestoreSpeciesService
    ): DatabaseService<Species, String>

    @Binds
    @Singleton
    @Named("firestore_species_class_db")
    abstract fun bindFireStoreSpeciesClassDatabaseService(
        fireStoreSpeciesClassService: FirestoreSpeciesClassService
    ): DatabaseService<SpeciesClass, String>

}