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
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_ENABLED", "false")
            buildConfigField("String", "UPDATE_API_URL", "\"\"")
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

    // direct flavor 的 UpdateWiring 直接使用 Compose foundation/material3/ui，
    // 因 :update 模块以 implementation 引入这些依赖不会传递到消费者，所以这里显式声明。
    "directImplementation"("androidx.compose.foundation:foundation:1.7.6")
    "directImplementation"("androidx.compose.material3:material3:1.3.1")
    "directImplementation"("androidx.compose.ui:ui:1.7.6")
}
