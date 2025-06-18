package com.project.speciesdetection.core.services.remote_database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speciesdetection.core.services.remote_database.observation.FirestoreObservationService
import com.project.speciesdetection.core.services.remote_database.species_class.FirestoreSpeciesClassService
import com.project.speciesdetection.core.services.remote_database.species.FirestoreSpeciesService
import com.project.speciesdetection.data.model.species.Species
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseServiceModule {

    @Binds
    @Singleton
    @Named("species_db")
    abstract fun bindSpeciesDatabaseService(
        speciesService: FirestoreSpeciesService
    ): SpeciesDatabaseService<Species, String>

    @Binds
    @Singleton
    @Named("species_class_db")
    abstract fun bindSpeciesClassDatabaseService(
        speciesClassService: FirestoreSpeciesClassService
    ): SpeciesClassDatabaseService<SpeciesClass, String>

    @Binds
    @Singleton
    @Named("observation_db")
    abstract fun bindObservationDatabaseService(
        observationService: FirestoreObservationService
    ): ObservationDatabaseService
}