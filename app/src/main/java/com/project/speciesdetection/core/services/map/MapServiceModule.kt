package com.project.speciesdetection.core.services.map

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class MapServiceModule {
    @Binds
    @Singleton
    abstract fun bindGeocodingService(
        nominatimGeocodingService: NominatimGeocodingService
    ): GeocodingService
}