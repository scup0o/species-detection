package com.project.speciesdetection.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.project.speciesdetection.core.helpers.LocaleHelper.setLanguage
import com.project.speciesdetection.core.navigation.AppNavigation
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.SpeciesDetectionTheme
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()



        val languageCode = getLanguagePreference(this)
        setLanguage(this, languageCode)

        super.onCreate(savedInstanceState)
        //WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            navController = rememberNavController()
            SpeciesDetectionTheme {
                AppNavigation(
                    activity = this,
                    navController = navController
                )
            }

            handleIntent(intent)

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Được gọi khi app đang chạy và nhận được intent mới (vd: click notification)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val postId = intent?.getStringExtra("notification_post_id")
        if (postId != null) {
            Log.d("DeepLink", "Cần điều hướng đến bài viết ID: $postId")
            navController.navigate(AppScreen.ObservationDetailScreen.createRoute(postId))

            intent.removeExtra("notification_post_id")
        }
    }

    private fun getLanguagePreference(context: Context): String {
        val pref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return pref.getString("selected_language", Locale.getDefault().language) ?: Locale.getDefault().language
    }
}