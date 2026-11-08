import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing: from environment variables in CI, or from a keystore.properties file locally (never committed).
val signing = Properties().apply {
    rootDir.resolve("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun secret(name: String): String? = System.getenv(name) ?: signing.getProperty(name)

android {
    namespace = "io.github.quezka.cadence"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.quezka.cadence"
        minSdk = 26
        targetSdk = 35
        versionCode = 500
        versionName = "0.5.0"
    }
    signingConfigs {
        create("release") {
            secret("CADENCE_KEYSTORE")?.let { store ->
                storeFile = file(store)
                storePassword = secret("CADENCE_KEYSTORE_PASSWORD")
                keyAlias = secret("CADENCE_KEY_ALIAS") ?: "cadence"
                keyPassword = secret("CADENCE_KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            if (secret("CADENCE_KEYSTORE") != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    // Real blur behind the glass bars (Android 13+; older phones get a see-through tint). Apache 2.0.
    implementation("dev.chrisbanes.haze:haze:1.1.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
