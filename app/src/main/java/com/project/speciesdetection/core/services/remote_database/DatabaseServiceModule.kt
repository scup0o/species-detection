package com.project.speciesdetection.core.services.remote_database

import com.project.speciesdetection.core.services.remote_database.species_class.FirestoreSpeciesClassService
import com.project.speciesdetection.core.services.remote_database.species.FirestoreSpeciesService
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

}