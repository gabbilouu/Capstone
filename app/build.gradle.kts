import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.elevate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.elevate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = File(rootProject.rootDir, "local.properties")
        val properties = Properties().apply {
            if (localProps.exists()) {
                load(localProps.inputStream())
            }
        }
        val openaiKey = properties.getProperty("OPENAI_API_KEY") ?: ""
        buildConfigField("String", "OPENAI_API_KEY", "\"$openaiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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
}

dependencies {
    // Calendar view
    implementation("com.prolificinteractive:material-calendarview:1.4.3")



    // Networking + JSON
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.json:json:20210307")

    // Firebase (managed via BoM)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // EncryptedSharedPreferences for PIN
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Biometric prompt
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // AndroidX and Material
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.fragment)
    implementation(libs.work.runtime)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.1")
    implementation("com.google.android.material:material:1.12.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
