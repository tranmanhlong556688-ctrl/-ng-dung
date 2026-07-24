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
    namespace = "com.minh.autotouch.installtest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.minh.autotouch.installtest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
    val releaseRequested = allTasks.any { it.path.startsWith(":installtest:") && it.name.contains("Release", true) }
    if (releaseRequested && !hasReleaseSigning) {
        throw GradleException("Installation Test release signing variables are missing.")
    }
}
