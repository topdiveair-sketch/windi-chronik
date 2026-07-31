plugins {
    id("com.android.application")
}

android {
    namespace = "at.zuhauseambach.windichronik"
    compileSdk = 35

    defaultConfig {
        applicationId = "at.zuhauseambach.windichronik"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
}
