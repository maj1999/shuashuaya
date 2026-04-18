import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

group = AppConfig.GROUP
version = AppConfig.VERSION_NAME

kotlin {
    androidTarget {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    val commonMain by sourceSets.getting {
        dependencies {
            implementation(project(":shared"))
            api(compose.foundation)
            api(compose.material3)
            api(compose.runtime)
            api(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            implementation("io.ktor:ktor-client-core:${Versions.KTOR}")
            implementation("io.ktor:ktor-client-content-negotiation:${Versions.KTOR}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.KTOR}")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}")
        }
    }

    val commonTest by sourceSets.getting {
        dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            implementation("io.ktor:ktor-client-mock:${Versions.KTOR}")
        }
    }

    val androidUnitTest by sourceSets.getting {
        dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation("org.robolectric:robolectric:4.16.1")
        }
    }

    val androidMain by sourceSets.getting {
        dependsOn(commonMain)
        dependencies {
            implementation("androidx.core:core-ktx:1.12.0")
            implementation("io.ktor:ktor-client-okhttp:${Versions.KTOR}")
        }
    }

    sourceSets.appleMain {
        dependsOn(commonMain)
        dependencies {
            implementation("io.ktor:ktor-client-darwin:${Versions.KTOR}")
        }
    }

    targets.withType<KotlinNativeTarget> {
        val mainSourceSet = compilations.getByName("main").defaultSourceSet
        if (konanTarget.family.isAppleFamily) {
            mainSourceSet.dependsOn(sourceSets.getByName("appleMain"))
        }
    }
}

android {
    compileSdk = Versions.ANDROID_COMPILE_SDK
    namespace = "${AppConfig.GROUP}.update"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Versions.ANDROID_MIN_SDK
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
