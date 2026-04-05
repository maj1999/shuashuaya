plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    compileSdk = Versions.ANDROID_COMPILE_SDK
    namespace = AppConfig.APPLICATION_ID

    defaultConfig {
        applicationId = AppConfig.APPLICATION_ID
        minSdk = Versions.ANDROID_MIN_SDK
        targetSdk = Versions.ANDROID_TARGET_SDK
        versionCode = AppConfig.VERSION_CODE
        versionName = AppConfig.VERSION_NAME

        buildConfigField("boolean", "ENABLE_UPDATE_CHECK", "${AppConfig.ENABLE_UPDATE_CHECK}")
        buildConfigField("String", "UPDATE_API_URL", "\"${AppConfig.UPDATE_API_URL}\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "刷刷鸭.apk"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(Deps.ANDROIDX_APPCOMPAT)
    implementation(Deps.ANDROIDX_CORE_KTX)
    implementation(Deps.MATERIAL)
    implementation("androidx.activity:activity-compose:1.8.2")
}
