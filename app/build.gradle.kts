plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.isora.ciclo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.isora.ciclo"
        minSdk = 26
        targetSdk = 34
        // The release workflow sets these from the git tag and the run number.
        versionCode = System.getenv("CICLO_VERSION_CODE")?.toInt() ?: 1
        versionName = System.getenv("CICLO_VERSION_NAME") ?: "0.1.0-dev"
    }

    // Stable release key so every new APK installs over the previous one.
    // See scripts/setup-release-signing.sh; CI decodes it from repository secrets.
    val keystorePath = System.getenv("CICLO_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storeType = "pkcs12"
                storePassword = System.getenv("CICLO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CICLO_KEY_ALIAS")
                keyPassword = System.getenv("CICLO_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
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
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric with Compose needs more than the 512 MB default heap of the test JVM.
        unitTests.all { it.maxHeapSize = "2g" }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
