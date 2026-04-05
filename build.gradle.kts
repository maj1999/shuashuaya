buildscript {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${Versions.KOTLIN}")
        classpath("com.android.tools.build:gradle:${Versions.AGP}")
        classpath("org.jetbrains.compose:compose-gradle-plugin:${Versions.COMPOSE_MULTIPLATFORM}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.KOTLIN}")
        // classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${Versions.KSP}")
        // classpath("com.tencent.kuikly-open:core-gradle-plugin:${Versions.KUIKLY_GRADLE_PLUGIN}")
    }
}

allprojects {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        jvmTargetValidationMode.set(
            org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING
        )
    }
}
