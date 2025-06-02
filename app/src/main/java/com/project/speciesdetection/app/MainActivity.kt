package com.project.speciesdetection.app

import android.content.Context
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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    @Named("language_provider") lateinit var languageProvider: LanguageProvider // Hilt sẽ inject cái này

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()


        val languageCode = getLanguagePreference(this)
        setLanguage(this, languageCode)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeciesDetectionTheme {
                AppNavigation(
                    activity = this,

                )
            }
        }
    }

    private fun getLanguagePreference(context: Context): String {
        val pref = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        return pref.getString("selected_language", Locale.getDefault().language) ?: Locale.getDefault().language
    }
}