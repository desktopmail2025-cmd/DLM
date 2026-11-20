plugins {
    id("com.android.application")
}

android {
    namespace = "com.desktopmail2025.dlm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.desktopmail2025.dlm"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
