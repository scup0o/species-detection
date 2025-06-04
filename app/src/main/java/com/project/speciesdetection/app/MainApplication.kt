package com.project.speciesdetection.app

import android.app.Application
import com.project.speciesdetection.core.helpers.LocaleHelper
import com.project.speciesdetection.core.helpers.LocaleHelper.setLanguage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication(): Application(){
}