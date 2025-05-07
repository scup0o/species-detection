package com.project.speciesdetection.data.model.user.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UserRepositoryModule {
    @Binds
    abstract fun bindRemoteUserRepository(remoteRepository: RemoteUserRepository) : UserRepository
}