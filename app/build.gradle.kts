import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.project.speciesdetection"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.project.speciesdetection"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đọc properties từ local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        //manifestPlaceholders["MAPS_API_KEY"] = "\"${localProperties.getProperty("MAPS_API_KEY")}\""

        // Tạo các trường trong BuildConfig
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperties.getProperty("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProperties.getProperty("CLOUDINARY_UPLOAD_PRESET")}\"")
        //buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Ngăn không nén file TFLite
    androidResources.noCompress.add("tflite")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.hilt.navigation.compose)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation("com.google.firebase:firebase-messaging-ktx")

    //Glide
    implementation(libs.glide)

    // Glide Compose Integration
    implementation (libs.compose)

    //Paging 3
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    //Valentinilk
    implementation(libs.compose.shimmer)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    //ImageCropper
    implementation(libs.android.image.cropper)

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")
    //implementation ("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1") //EB0

    //Serizalization
    implementation(libs.kotlinx.serialization.json)

    // Credential Manager
    implementation(libs.androidx.credentials)
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-alpha02")
    implementation ("com.google.android.gms:play-services-auth:21.3.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // THÊM THƯ VIỆN CHO OPENSTREETMAP:
    // Thư viện chính để hiển thị bản đồ OSM
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // Thư viện để tích hợp osmdroid với Jetpack Compose
    //implementation("com.github.utsman:osmandcompose:0.2.2")
    //implementation("com.github.krizzu:compose-maps-osmdroid:2.1.0")
    //implementation("org.osmdroid:osmdroid-compose:1.0.0-alpha02")

    /*implementation("com.google.maps.android:maps-compose:4.3.3") // Kiểm tra phiên bản mới nhất
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.4.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")*/

    implementation ("com.google.android.gms:play-services-location:21.0.1")


    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")

    configurations.all {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
}

kapt {
    correctErrorTypes = true
}