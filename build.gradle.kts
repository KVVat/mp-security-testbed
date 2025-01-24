import org.jetbrains.compose.desktop.application.dsl.TargetFormat
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenLocal()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.3")
    }

}
plugins {
    id("com.android.application") version "8.3.0-alpha06" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    //alias(libs.plugins.androidLibrary) apply false
    //kotlin("multiplatform") apply false
    kotlin("jvm") version "1.9.20" apply false
    id("org.jetbrains.compose") version "1.5.10" apply false
    id("com.google.protobuf") version "0.9.3" apply false
    //id("org.jetbrains.kotlin.jvm") apply false
}

ext["grpcVersion"] = "1.57.2"
ext["grpcKotlinVersion"] = "1.3.1" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.24.1"
ext["coroutinesVersion"] = "1.7.3"