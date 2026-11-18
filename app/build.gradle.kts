plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.epubreader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.epubreader"
        minSdk = 21          // Android 5.0: WebView actualizable desde Play Store
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}
// Sin dependencias externas: solo el SDK de Android y el WebView del sistema.
