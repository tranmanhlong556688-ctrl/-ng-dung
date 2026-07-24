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
    namespace = "com.minh.autotouch.redmilite"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.minh.autotouch.redmilite"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "1.3.0-redmi-lite"
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

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("releaseKey")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

gradle.taskGraph.whenReady {
    val requested = allTasks.any {
        it.path.startsWith(":redmilite:") && it.name.contains("Release", ignoreCase = true)
    }
    if (requested && !hasReleaseSigning) {
        throw GradleException("Redmi Lite release signing variables are missing.")
    }
}
