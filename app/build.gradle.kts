plugins {
    id("com.android.application")
}

android {
    namespace = "at.zuhauseambach.mobil"
    compileSdk = 35

    defaultConfig {
        applicationId = "at.zuhauseambach.mobil"
        minSdk = 26
        targetSdk = 35
        versionCode = 915
        versionName = "91.5"
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
