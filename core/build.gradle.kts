plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

ext.set("PUBLISH_ARTIFACT_ID", "core")
apply(from = "${rootProject.projectDir}/scripts/publish-module.gradle")

android {
    namespace = "com.attafitamim.sensor.core.hardware"
    compileSdk = 33

    defaultConfig {
        minSdk = 16
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
}