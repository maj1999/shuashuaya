import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("com.tencent.kuikly-open.kuikly")
}

group = AppConfig.GROUP
version = AppConfig.VERSION_NAME

kotlin {

    // Android target
    androidTarget {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // HarmonyOS NEXT target (ohos arm64)
    ohosArm64 {
        binaries.sharedLib("shared") {
            freeCompilerArgs += "-Xadd-light-debug=enable"
            linkerOpts += "--build-id=sha1"
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    // commonMain — 跨平台共享代码
    val commonMain by sourceSets.getting {
        dependencies {
            // KuiklyUI 核心依赖（后续任务中补充具体版本号）
            // implementation(Deps.KUIKLY_CORE)
            // implementation(Deps.KUIKLY_COMPOSE)
            // implementation(Deps.KUIKLY_CORE_ANNOTATIONS)
        }
    }

    // commonTest — 共享测试
    val commonTest by sourceSets.getting {
        dependencies {
            implementation(kotlin("test"))
        }
    }

    // Android 平台
    val androidMain by sourceSets.getting {
        dependsOn(commonMain)
    }

    // Apple 平台（iOS 共享）
    sourceSets.appleMain {
        dependsOn(commonMain)
    }

    // 让各 iOS target 的 main sourceSet 继承 appleMain
    targets.withType<KotlinNativeTarget> {
        val mainSourceSet = compilations.getByName("main").defaultSourceSet
        if (konanTarget.family.isAppleFamily) {
            mainSourceSet.dependsOn(sourceSets.getByName("appleMain"))
        }
    }

    // CocoaPods 配置（iOS 集成）
    cocoapods {
        summary = "CleanPic shared KMP module"
        homepage = "https://github.com/cleanpic"
        version = AppConfig.VERSION_NAME
        ios.deploymentTarget = Versions.IOS_DEPLOYMENT_TARGET
        framework {
            isStatic = true
            baseName = "shared"
        }
    }
}

android {
    compileSdk = Versions.ANDROID_COMPILE_SDK
    namespace = "${AppConfig.GROUP}.shared"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Versions.ANDROID_MIN_SDK
        targetSdk = Versions.ANDROID_TARGET_SDK
    }
}
