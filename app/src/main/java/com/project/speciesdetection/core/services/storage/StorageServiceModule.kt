package com.project.speciesdetection.core.services.storage

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageServiceModule {

    @Binds
    @Singleton
    abstract fun bindStorageService(
        cloudinaryStorageService: CloudinaryStorageService
    ): StorageService

}