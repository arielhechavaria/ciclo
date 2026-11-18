plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

    testImplementation("junit:junit:4.13.2")
}
