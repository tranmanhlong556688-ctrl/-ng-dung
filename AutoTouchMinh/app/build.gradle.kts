plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ciKeystorePath = System.getenv("AUTOTOUCH_KEYSTORE_PATH")
val ciStorePassword = System.getenv("AUTOTOUCH_STORE_PASSWORD") ?: ""
val ciKeyAlias = System.getenv("AUTOTOUCH_KEY_ALIAS") ?: ""
val ciKeyPassword = System.getenv("AUTOTOUCH_KEY_PASSWORD") ?: ""

android {
    namespace = "com.minh.autotouch"
    compileSdk = 35

    defaultConfig {
        // Mã gói mới để tránh xung đột chữ ký với các APK thử nghiệm cũ.
        applicationId = "com.minh.autotouch.personal"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
    }

    signingConfigs {
        if (!ciKeystorePath.isNullOrBlank()) {
            create("personalTest") {
                storeFile = file(ciKeystorePath)
                storePassword = ciStorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
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
            signingConfig = if (!ciKeystorePath.isNullOrBlank()) {
                signingConfigs.getByName("personalTest")
            } else {
                // Build thủ công vẫn tạo APK thử nghiệm có chữ ký và cài được.
                signingConfigs.getByName("debug")
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
    kotlinOptions {
        jvmTarget = "17"
    }
}
