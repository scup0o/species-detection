package com.project.speciesdetection.data.model.user.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn

@Module
@InstallIn
abstract class UserRepositoryModule {
    @Binds
    abstract fun bindRemoteUserRepository(remoteRepository: RemoteUserRepository) : UserRepository
}