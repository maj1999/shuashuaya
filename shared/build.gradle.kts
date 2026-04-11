import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
    id("com.android.library")
    // id("com.google.devtools.ksp")  // 暂不使用 KSP（Compose DSL 无需 @Page 注解）
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
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
            // KuiklyUI 核心依赖（非 UI 部分）
            implementation("com.tencent.kuikly-open:core:${Versions.KUIKLY}")
            implementation("com.tencent.kuikly-open:core-annotations:${Versions.KUIKLY}")
            // Compose Multiplatform UI
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            // Network (版本检查)
            implementation("io.ktor:ktor-client-core:${Versions.KTOR}")
            implementation("io.ktor:ktor-client-content-negotiation:${Versions.KTOR}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.KTOR}")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}")
        }
    }

    // commonTest — 共享测试
    val commonTest by sourceSets.getting {
        dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            implementation("io.ktor:ktor-client-mock:${Versions.KTOR}")
        }
    }

    // androidUnitTest — Android Compose UI 测试（Robolectric）
    val androidUnitTest by sourceSets.getting {
        dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation("org.robolectric:robolectric:4.16.1")
        }
    }

    // Android 平台
    val androidMain by sourceSets.getting {
        dependsOn(commonMain)
        dependencies {
            implementation("androidx.core:core-ktx:1.12.0")
            // Coil 3: 图片加载 + 视频帧解码
            implementation("io.coil-kt.coil3:coil-compose:${Versions.COIL}")
            implementation("io.coil-kt.coil3:coil-video:${Versions.COIL}")
            // Media3 ExoPlayer: 视频播放
            implementation("androidx.media3:media3-exoplayer:${Versions.MEDIA3}")
            implementation("androidx.media3:media3-ui:${Versions.MEDIA3}")
            // Ktor Android engine
            implementation("io.ktor:ktor-client-okhttp:${Versions.KTOR}")
        }
    }

    // Apple 平台（iOS 共享）
    sourceSets.appleMain {
        dependsOn(commonMain)
        dependencies {
            // Ktor iOS engine
            implementation("io.ktor:ktor-client-darwin:${Versions.KTOR}")
        }
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
