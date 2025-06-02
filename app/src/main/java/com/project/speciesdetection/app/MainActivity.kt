package com.project.speciesdetection.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.project.speciesdetection.core.helpers.LocaleHelper.setLanguage
import com.project.speciesdetection.core.navigation.AppNavigation
import com.project.speciesdetection.core.theme.SpeciesDetectionTheme
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    @Named("language_provider") lateinit var languageProvider: LanguageProvider // Hilt sẽ inject cái này

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val languageCode = languageProvider.getCurrentLanguageCode()
        setLanguage(this, languageCode)

        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            SpeciesDetectionTheme {
                AppNavigation(
                    activity = this,

                )
            }
        }
    }
}