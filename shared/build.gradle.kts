import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
    id("com.android.library")
    // id("com.google.devtools.ksp")  // 暂不使用 KSP（Compose DSL 无需 @Page 注解）
    id("org.jetbrains.compose")
    // id("com.tencent.kuikly-open.kuikly")  // 暂不使用 Kuikly Gradle 插件（避免 KSP 版本冲突）
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
    // 注意：ohosArm64 需要 KuiklyUI Gradle 插件提供支持
    // 当 KuiklyUI 插件正确加载后取消注释
    // ohosArm64 {
    //     binaries.sharedLib("shared") {
    //         freeCompilerArgs += "-Xadd-light-debug=enable"
    //         linkerOpts += "--build-id=sha1"
    //     }
    // }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    // commonMain — 跨平台共享代码
    val commonMain by sourceSets.getting {
        dependencies {
            // KuiklyUI 核心依赖
            implementation("com.tencent.kuikly-open:core:${Versions.KUIKLY}")
            implementation("com.tencent.kuikly-open:compose:${Versions.KUIKLY}")
            implementation("com.tencent.kuikly-open:core-annotations:${Versions.KUIKLY}")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }
    }

    // commonTest — 共享测试
    val commonTest by sourceSets.getting {
        dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        }
    }

    // Android 平台
    val androidMain by sourceSets.getting {
        dependsOn(commonMain)
        dependencies {
            implementation("androidx.core:core-ktx:1.12.0")
        }
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
