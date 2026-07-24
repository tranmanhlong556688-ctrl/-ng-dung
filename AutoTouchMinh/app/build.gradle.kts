plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystorePath = System.getenv("AUTOTOUCH_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("AUTOTOUCH_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("AUTOTOUCH_KEY_ALIAS")
val releaseKeyPassword = System.getenv("AUTOTOUCH_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.minh.autotouch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.minh.autotouch.personal"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("releaseKey") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    flavorDimensions += "releaseTrack"
    productFlavors {
        create("baseline") {
            dimension = "releaseTrack"
            versionCode = 3
            versionName = "1.2.0"
        }
        create("production") {
            dimension = "releaseTrack"
            versionCode = 4
            versionName = "1.2.1"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseKey")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// Release artifacts must never silently fall back to the Android debug key.
gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { it.name.contains("Release", ignoreCase = true) }
    if (releaseRequested && !hasReleaseSigning) {
        throw GradleException(
            "Release signing is missing. Set AUTOTOUCH_KEYSTORE_PATH, " +
                "AUTOTOUCH_STORE_PASSWORD, AUTOTOUCH_KEY_ALIAS and AUTOTOUCH_KEY_PASSWORD."
        )
    }
}
