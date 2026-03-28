/**
 * CleanPic 项目构建配置常量
 *
 * 集中管理版本号、依赖坐标等构建配置，
 * 避免在多个 build.gradle.kts 中硬编码。
 */

object Versions {
    const val KOTLIN = "2.1.21"
    const val AGP = "7.4.2"
    const val COMPOSE_MULTIPLATFORM = "1.7.3"
    const val KSP = "2.1.21-2.0.1"
    const val KUIKLY_GRADLE_PLUGIN = "2.14.1-2.0.21"

    // Android SDK
    const val ANDROID_COMPILE_SDK = 34
    const val ANDROID_MIN_SDK = 26
    const val ANDROID_TARGET_SDK = 34

    // iOS deployment target
    const val IOS_DEPLOYMENT_TARGET = "14.0"
}

object Deps {
    // KuiklyUI (从腾讯 Maven 仓库拉取)
    const val KUIKLY_CORE = "com.tencent.kuikly-open:core"
    const val KUIKLY_COMPOSE = "com.tencent.kuikly-open:compose"
    const val KUIKLY_CORE_ANNOTATIONS = "com.tencent.kuikly-open:core-annotations"
    const val KUIKLY_CORE_KSP = "com.tencent.kuikly-open:core-ksp"
    const val KUIKLY_RENDER_ANDROID = "com.tencent.kuikly-open:core-render-android"

    // AndroidX
    const val ANDROIDX_APPCOMPAT = "androidx.appcompat:appcompat:1.6.1"
    const val ANDROIDX_CORE_KTX = "androidx.core:core-ktx:1.12.0"
    const val MATERIAL = "com.google.android.material:material:1.11.0"

    // Test
    const val KOTLIN_TEST = "org.jetbrains.kotlin:kotlin-test"
}

object AppConfig {
    const val APPLICATION_ID = "com.cleanpic.android"
    const val GROUP = "com.cleanpic"
    const val VERSION_NAME = "0.1.0"
    const val VERSION_CODE = 1
}
