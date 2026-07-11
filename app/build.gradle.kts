plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.npsai.fbdownloader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.npsai.fbdownloader"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ndk {
            // Une seule architecture : arm64-v8a couvre la quasi-totalité
            // des téléphones Android actuels (dont le Tecno Spark 10).
            // Ça réduit fortement la taille de l'APK par rapport à
            // embarquer plusieurs architectures.
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Librairie yt-dlp pour Android (télécharge Facebook, Twitter/X, YouTube, etc.)
    // Anciennement sur JitPack (com.github.yausername...), migrée vers Maven Central.
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
