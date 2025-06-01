package com.project.speciesdetection.core.services.authentication

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

private const val GOOGLE_WEB_CLIENT_ID = "1012000261168-h55p7b5hpoe3ub4v7ovoa0s2361j8qon.apps.googleusercontent.com"

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    abstract fun bindAuthService(authService: FirebaseAuthService) : AuthServiceInterface
}