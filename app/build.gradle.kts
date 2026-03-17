import java.util.Properties;
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Load API key from local.properties
// Load API key from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.roamio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.roamio"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Makes MAPS_API_KEY accessible as BuildConfig.MAPS_API_KEY in Java
        buildConfigField(
            "String",
            "MAPS_API_KEY",
            "\"${localProperties.getProperty("MAPS_API_KEY", "")}\""
        )

        // ✅ Injects key into AndroidManifest.xml as ${mapsApiKey}
        manifestPlaceholders["mapsApiKey"] =
            localProperties.getProperty("MAPS_API_KEY", "")
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

    // ✅ Required — enables BuildConfig class generation
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // UI
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ✅ Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // ✅ GPS / Fused Location
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // ✅ OkHttp — for Places Nearby Search API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ✅ Glide — for loading place photos from Google
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}