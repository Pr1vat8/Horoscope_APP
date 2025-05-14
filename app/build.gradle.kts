import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Assuming you have this if you use Compose extensively
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    try {
        localProperties.load(localPropertiesFile.inputStream())
    } catch (e: Exception) {
        println("Warning: Could not load local.properties: ${e.message}")
    }
}

android {
    namespace = "com.example.horoscopes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.horoscopes"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Updated to use RAPIDAPI_KEY
        buildConfigField("String", "RAPIDAPI_KEY", "\"${localProperties.getProperty("RAPIDAPI_KEY") ?: "YOUR_DEFAULT_RAPIDAPI_KEY"}\"")
    }


    buildTypes {
        release {
            isMinifyEnabled = false // Consider enabling this for production releases
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
        compose = true // Keep if you are using Jetpack Compose
        buildConfig = true // Necessary for buildConfigField
    }
}

dependencies {

    // Removed Gemini API dependency:
    // implementation("com.google.ai.client.generativeai:generativeai:0.5.0")

    implementation("com.google.android.material:material:1.12.0")

    // OkHttp for network requests (already present, which is good)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // If using Jetpack Compose
    implementation(platform(libs.androidx.compose.bom)) // If using Jetpack Compose
    implementation(libs.androidx.ui) // If using Jetpack Compose
    implementation(libs.androidx.ui.graphics) // If using Jetpack Compose
    implementation(libs.androidx.ui.tooling.preview) // If using Jetpack Compose
    implementation(libs.androidx.material3) // If using Jetpack Compose with Material 3
    implementation(libs.androidx.appcompat) // For AppCompatActivity

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // If using Jetpack Compose
    androidTestImplementation(libs.androidx.ui.test.junit4) // If using Jetpack Compose
    debugImplementation(libs.androidx.ui.tooling) // If using Jetpack Compose
    debugImplementation(libs.androidx.ui.test.manifest) // If using Jetpack Compose
}