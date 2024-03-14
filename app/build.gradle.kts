@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

val jvmTarget = JavaVersion.VERSION_17
val properties by lazy { gradleLocalProperties(rootDir, providers) }

android {
    namespace = "com.matthias.petfinder"
    compileSdk = libs.versions.sdk.compile.get().toInt()

    defaultConfig {
        applicationId = "com.matthias.petfinder"
        minSdk = libs.versions.sdk.min.get().toInt()
        targetSdk = libs.versions.sdk.target.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = true.toString()
        testOptions {
            execution = "ANDROIDX_TEST_ORCHESTRATOR"
            managedDevices {
                localDevices {
                    create("Pixel2-API28") {
                        device = "Pixel 2"
                        apiLevel = 28
                        systemImageSource = "google-atd"
                    }
                    create("Pixel7a-API33") {
                        device = "Pixel 7a"
                        apiLevel = 33
                        systemImageSource = "google-atd"
                    }
                    create("PixelTablet-API30") {
                        device = "Pixel Tablet"
                        apiLevel = 30
                        systemImageSource = "google-atd"
                    }
                    create("PixelFold-API34") {
                        device = "Pixel Fold"
                        apiLevel = 34
                        systemImageSource = "google-atd"
                    }
                }
                groups {
                    create("PhoneAndTablet") {
                        targetDevices.add(devices["Pixel2-API28"])
                        targetDevices.add(devices["Pixel7a-API33"])
                        targetDevices.add(devices["PixelTablet-API30"])
                        targetDevices.add(devices["PixelFold-API34"])
                    }
                }
            }
        }
        vectorDrawables.useSupportLibrary = true
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
        sourceCompatibility = jvmTarget
        targetCompatibility = jvmTarget
    }
    kotlinOptions {
        jvmTarget = jvmTarget.toString()
    }
    buildFeatures {
        compose = true
    }
    composeOptions.kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    platform(libs.compose.bom).let {
        implementation(it)
        androidTestImplementation(it)
    }
    implementation(project(":api"))

    implementation(libs.bundles.compose)
    implementation(libs.bundles.core)

    androidTestImplementation(libs.bundles.android.test)
    androidTestUtil(libs.test.orchestrator)

    debugImplementation(libs.bundles.debug)
    testImplementation(libs.junit)
}