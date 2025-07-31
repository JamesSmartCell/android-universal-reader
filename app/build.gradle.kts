plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("plugin.serialization") version "2.0.0"
}

configurations.all {
    resolutionStrategy {
        // Ensure these module names exactly match what's being pulled in transitively
        // and what you intend to force.
        force("org.bouncycastle:bcprov-jdk18on:1.77")
        force("org.bouncycastle:bcpkix-jdk18on:1.77")
        // If web3j or another library pulls in a different variant like bcprov-jdk15to18
        // you might need to force that specific one or ensure your forced version
        // is compatible and correctly overrides it.
    }
}

android {
    namespace = "com.kormax.universalreader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kormax.universalreader"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        // Enable desugaring
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"

        freeCompilerArgs +=
            arrayListOf(
                "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
                "-Xopt-in=kotlin.ExperimentalStdlibApi",
                "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
            )
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" + "META-INF/native-image/**" } }
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
    implementation(libs.androidx.appcompat)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.compose.material:material-icons-core:1.6.7") // Or your current version
    implementation("androidx.compose.material:material-icons-extended:1.6.7") // Add this line (use the same version as core)
    implementation("org.web3j:core:4.10.3")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Or use kotlinx-serialization converter
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // For logging API requests (optional)

// For Kotlinx Serialization (alternative to Gson)
// implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
// implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

// For Jetpack DataStore (recommended for saving credentials over SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
