package com.project.speciesdetection.domain.provider

import com.project.speciesdetection.domain.provider.language.DeviceLanguageProvider
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    @Named("language_provider")
    abstract fun bindDeviceLanguageProvider(deviceLanguageProvider: DeviceLanguageProvider): LanguageProvider

}
