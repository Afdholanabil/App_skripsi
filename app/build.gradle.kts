plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.app_skripsi"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.app_skripsi"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation (libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation (libs.androidx.datastore.preferences)
    implementation (libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler) // Pastikan compiler ada
    implementation (libs.androidx.room.ktx) // Coroutine Support
    implementation (libs.androidx.work.runtime)

    // Media3 (ExoPlayer)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)

    // Untuk mengambil gambar thumbnail
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
}