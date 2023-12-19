import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenLocal()
    }
}

plugins {

    id("com.android.application") version "8.3.0-alpha06" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    //kotlin("multiplatform") apply false
    kotlin("jvm") version "1.9.20" apply false
    id("org.jetbrains.compose") version "1.5.10" apply false
    id("com.google.protobuf") version "0.9.3" apply false
    //id("org.jetbrains.kotlin.jvm") apply false
}
