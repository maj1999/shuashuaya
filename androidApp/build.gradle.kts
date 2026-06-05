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
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("direct") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_ENABLED", "true")
            buildConfigField("String", "UPDATE_API_URL", "\"${AppConfig.UPDATE_API_URL}\"")
            buildConfigField("String", "UPDATE_API_URL_CN", "\"${AppConfig.UPDATE_API_URL_CN}\"")
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_ENABLED", "false")
            buildConfigField("String", "UPDATE_API_URL", "\"\"")
            buildConfigField("String", "UPDATE_API_URL_CN", "\"\"")
        }
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
            val flavorName = productFlavors.first().name
            output.outputFileName = "刷刷鸭-${flavorName}.apk"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    "directImplementation"(project(":update"))
    implementation(Deps.ANDROIDX_APPCOMPAT)
    implementation(Deps.ANDROIDX_CORE_KTX)
    implementation(Deps.MATERIAL)
    implementation("androidx.activity:activity-compose:1.8.2")
}
